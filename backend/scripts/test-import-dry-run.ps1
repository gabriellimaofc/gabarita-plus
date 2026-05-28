param(
    [string]$ApiBaseUrl = "https://gabarita-plus-api.onrender.com/api",
    [string]$AdminEmail = "admin@gabaritaplus.com",
    [string]$AdminPassword = "Admin@123",
    [string]$ImportFile = "..\\..\\database\\examples\\enem-official-import.example.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "-> $Message" -ForegroundColor Cyan
}

function Read-ErrorResponse {
    param([System.Exception]$Exception)

    if (-not $Exception.Response) {
        return $Exception.Message
    }

    $reader = New-Object System.IO.StreamReader($Exception.Response.GetResponseStream())
    return $reader.ReadToEnd()
}

function Invoke-ApiJson {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers,
        [object]$Body = $null
    )

    $params = @{
        Method      = $Method
        Uri         = $Uri
        Headers     = $Headers
        ContentType = "application/json"
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 30)
    }

    try {
        return Invoke-RestMethod @params
    } catch {
        throw "Falha em $Method $Uri`: $(Read-ErrorResponse $_.Exception)"
    }
}

function Resolve-ImportFile {
    param([string]$PathValue)

    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }

    return [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot $PathValue))
}

Write-Step "Validando arquivo de entrada"
$resolvedImportFile = Resolve-ImportFile $ImportFile
if (-not (Test-Path $resolvedImportFile)) {
    throw "Arquivo de importacao nao encontrado: $resolvedImportFile"
}

$questions = Get-Content -Path $resolvedImportFile -Raw | ConvertFrom-Json
if (-not $questions) {
    throw "O arquivo de importacao esta vazio."
}

$dryRunPayload = @{
    questions = @($questions)
}

[pscustomobject]@{
    ImportFile = $resolvedImportFile
    Items = @($questions).Count
} | Format-List

Write-Step "Login admin"
$loginResponse = Invoke-ApiJson -Method Post -Uri "$ApiBaseUrl/auth/login" -Headers @{} -Body @{
    usernameOrEmail = $AdminEmail
    password = $AdminPassword
}

if (-not $loginResponse.success -or -not $loginResponse.data.accessToken) {
    throw "Login admin retornou payload inesperado."
}

$accessToken = $loginResponse.data.accessToken
$headers = @{
    Authorization = "Bearer $accessToken"
}

[pscustomobject]@{
    LoginSuccess = $loginResponse.success
    Message = $loginResponse.message
    TokenLength = $accessToken.Length
    TokenMasked = "***hidden***"
} | Format-List

Write-Step "Confirmando endpoint ADMIN e coletando lotes antes do dry-run"
$batchesBeforeResponse = Invoke-ApiJson -Method Get -Uri "$ApiBaseUrl/admin/import/batches" -Headers $headers
if (-not $batchesBeforeResponse.success) {
    throw "Falha ao consultar lotes de importacao antes do dry-run."
}

$batchesBefore = @($batchesBeforeResponse.data)

[pscustomobject]@{
    Endpoint = "POST /admin/import/questions/dry-run"
    ProtectedByAdmin = $true
    BatchesBefore = $batchesBefore.Count
} | Format-List

Write-Step "Executando dry-run em producao"
$dryRunResponse = Invoke-ApiJson -Method Post -Uri "$ApiBaseUrl/admin/import/questions/dry-run" -Headers $headers -Body $dryRunPayload
if (-not $dryRunResponse.success -or -not $dryRunResponse.data) {
    throw "Dry-run retornou payload inesperado."
}

$report = $dryRunResponse.data
$predictedImportable = [Math]::Max(($report.totalProcessed - $report.skippedDuplicates - $report.invalid), 0)

Write-Step "Coletando lotes apos dry-run"
$batchesAfterResponse = Invoke-ApiJson -Method Get -Uri "$ApiBaseUrl/admin/import/batches" -Headers $headers
if (-not $batchesAfterResponse.success) {
    throw "Falha ao consultar lotes de importacao apos o dry-run."
}

$batchesAfter = @($batchesAfterResponse.data)
$batchCountUnchanged = ($batchesBefore.Count -eq $batchesAfter.Count)
$nothingSaved = ($report.dryRun -eq $true) -and ($null -eq $report.batchId) -and $batchCountUnchanged

Write-Step "Relatorio do dry-run"
[pscustomobject]@{
    TotalProcessado = $report.totalProcessed
    ImportadasPrevistas = $predictedImportable
    ImportadasSalvas = $report.imported
    DuplicadasPrevistas = $report.skippedDuplicates
    Invalidas = $report.invalid
    NeedsReview = $report.needsReview
    Erros = $report.errors
    DryRun = $report.dryRun
    BatchId = $report.batchId
} | Format-List

if ($report.itemErrors -and @($report.itemErrors).Count -gt 0) {
    Write-Step "Erros por item"
    $report.itemErrors | ForEach-Object {
        [pscustomobject]@{
            ItemIndex = $_.itemIndex
            Title = $_.title
            SourceQuestionNumber = $_.sourceQuestionNumber
            Errors = ($_.errors -join " | ")
        }
    } | Format-Table -AutoSize
} else {
    Write-Host "Nenhum erro por item foi retornado." -ForegroundColor Green
}

Write-Step "Confirmacao de nao persistencia"
[pscustomobject]@{
    BatchCountBefore = $batchesBefore.Count
    BatchCountAfter = $batchesAfter.Count
    BatchCountUnchanged = $batchCountUnchanged
    DryRunReturnedNullBatchId = ($null -eq $report.batchId)
    ConfirmedNothingSaved = $nothingSaved
} | Format-List

if (-not $nothingSaved) {
    throw "O dry-run nao confirmou explicitamente ausencia de persistencia. Revise o endpoint antes de importar dados reais."
}

Write-Host ""
Write-Host "Dry-run concluido com sucesso. Nenhum dado foi salvo no banco." -ForegroundColor Green
