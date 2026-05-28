param(
    [string]$ApiBaseUrl = "https://gabarita-plus-api.onrender.com/api",
    [string]$AdminEmail = "admin@gabaritaplus.com",
    [string]$AdminPassword = "Admin@123",
    [int]$Year = 2023,
    [int]$Limit = 1,
    [int]$Offset = 0,
    [string]$Language = "espanhol"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "-> $Message" -ForegroundColor Cyan
}

function Read-ErrorResponse {
    param([System.Exception]$Exception)

    $responseProperty = $Exception.PSObject.Properties["Response"]
    if (-not $responseProperty -or -not $responseProperty.Value) {
        return $Exception.Message
    }

    $reader = New-Object System.IO.StreamReader(
        $responseProperty.Value.GetResponseStream(),
        [System.Text.Encoding]::UTF8,
        $true
    )
    try {
        return $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
}

function Invoke-ApiJson {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    $request = [System.Net.HttpWebRequest]::Create($Uri)
    $request.Method = $Method
    $request.Accept = "application/json"
    foreach ($entry in $Headers.GetEnumerator()) {
        if ($entry.Key -ieq "Authorization") {
            $request.Headers["Authorization"] = $entry.Value
            continue
        }
        $request.Headers[$entry.Key] = $entry.Value
    }

    if ($null -ne $Body) {
        $jsonBody = ($Body | ConvertTo-Json -Depth 50)
        $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($jsonBody)
        $request.ContentType = "application/json; charset=utf-8"
        $request.ContentLength = $bodyBytes.Length
        $requestStream = $request.GetRequestStream()
        try {
            $requestStream.Write($bodyBytes, 0, $bodyBytes.Length)
        } finally {
            $requestStream.Dispose()
        }
    }

    try {
        $response = $request.GetResponse()
        $reader = New-Object System.IO.StreamReader(
            $response.GetResponseStream(),
            [System.Text.Encoding]::UTF8,
            $true
        )
        try {
            $content = $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
            $response.Dispose()
        }
        if ([string]::IsNullOrWhiteSpace($content)) {
            return $null
        }
        return $content | ConvertFrom-Json
    } catch {
        throw "Falha em $Method $Uri`: $(Read-ErrorResponse $_.Exception)"
    }
}

function Is-ControlledRateLimitMessage {
    param([string]$Message)

    return $Message -like "*A fonte externa enem.dev limitou as requisicoes*"
}

function Invoke-StatusCode {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{}
    )

    try {
        Invoke-WebRequest -Method $Method -Uri $Uri -Headers $Headers | Out-Null
        return 200
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

Write-Step "Validando protecao ADMIN sem token"
$unauthorizedStatus = Invoke-StatusCode -Method Get -Uri "$ApiBaseUrl/admin/import/enem-dev/years"

Write-Step "Login admin"
$loginResponse = Invoke-ApiJson -Method Post -Uri "$ApiBaseUrl/auth/login" -Body @{
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
    UnauthorizedYearsStatus = $unauthorizedStatus
} | Format-List

Write-Step "Smoke check de dashboard, questoes e simulados"
$dashboard = Invoke-ApiJson -Method Get -Uri "$ApiBaseUrl/dashboard" -Headers $headers
$questions = Invoke-ApiJson -Method Get -Uri "$ApiBaseUrl/questions?page=0&size=1" -Headers $headers
$mockExams = Invoke-ApiJson -Method Get -Uri "$ApiBaseUrl/mock-exams" -Headers $headers

$firstQuestion = $null
if ($questions.data) {
    $firstQuestion = @($questions.data)[0]
}

[pscustomobject]@{
    DashboardSuccess = $dashboard.success
    QuestionsSuccess = $questions.success
    QuestionsCount = @($questions.data).Count
    MockExamsSuccess = $mockExams.success
    MockExamsCount = @($mockExams.data).Count
    SensitiveFieldsLeakedInList = if ($firstQuestion) {
        ($firstQuestion.PSObject.Properties.Name -contains "correctAlternative") -or
        ($firstQuestion.PSObject.Properties.Name -contains "explanation")
    } else {
        $false
    }
} | Format-List

Write-Step "Coletando lotes antes do dry-run"
$batchesBeforeResponse = Invoke-ApiJson -Method Get -Uri "$ApiBaseUrl/admin/import/batches" -Headers $headers
$batchesBefore = @($batchesBeforeResponse.data)

Write-Step "Listando anos disponiveis no enem.dev"
$yearsResponse = Invoke-ApiJson -Method Get -Uri "$ApiBaseUrl/admin/import/enem-dev/years" -Headers $headers
$years = @($yearsResponse.data)
$targetYearAvailable = $years | Where-Object { $_.year -eq $Year }

[pscustomobject]@{
    YearsSuccess = $yearsResponse.success
    TotalYears = $years.Count
    RequestedYear = $Year
    RequestedYearAvailable = [bool]$targetYearAvailable
} | Format-List

if (-not $targetYearAvailable) {
    throw "Ano $Year nao encontrado em /admin/import/enem-dev/years."
}

Write-Step "Gerando preview normalizado"
$previewUrl = "$ApiBaseUrl/admin/import/enem-dev/preview?year=$Year&limit=$Limit&offset=$Offset&language=$Language"
try {
    $previewResponse = Invoke-ApiJson -Method Get -Uri $previewUrl -Headers $headers
} catch {
    if (Is-ControlledRateLimitMessage $_.Exception.Message) {
        Write-Warning "Preview do enem.dev foi limitado pela fonte externa. Mensagem controlada recebida do backend."
        Write-Host $_.Exception.Message -ForegroundColor Yellow
        return
    }
    throw
}
if (-not $previewResponse.success -or -not $previewResponse.data) {
    throw "Preview do enem.dev retornou payload inesperado."
}

$preview = $previewResponse.data
$previewItems = @($preview.items)
$allNeedReview = $previewItems.Count -gt 0 -and @($previewItems | Where-Object { $_.proposedImportStatus -eq "NEEDS_REVIEW" }).Count -eq $previewItems.Count
$allValidatedAgainstOfficialFalse = $previewItems.Count -gt 0 -and @($previewItems | Where-Object { $_.question.validatedAgainstOfficialSource -eq $false }).Count -eq $previewItems.Count
$anyPublished = @($previewItems | Where-Object { $_.proposedImportStatus -eq "PUBLISHED" }).Count -gt 0

[pscustomobject]@{
    PreviewSuccess = $previewResponse.success
    TotalFetched = $preview.totalFetched
    PreviewedItems = $preview.previewedItems
    SampleItems = $previewItems.Count
    AllNeedReview = $allNeedReview
    AllValidatedAgainstOfficialFalse = $allValidatedAgainstOfficialFalse
    AnyPublished = $anyPublished
} | Format-List

if ($previewItems.Count -gt 0) {
    Write-Step "Amostra do preview"
    $previewItems | Select-Object -First 2 | ForEach-Object {
        [pscustomobject]@{
            Title = $_.question.title
            Source = $_.question.source
            SourceYear = $_.question.sourceYear
            ExternalProvider = $_.question.externalProvider
            ValidatedAgainstOfficialSource = $_.question.validatedAgainstOfficialSource
            ProposedImportStatus = $_.proposedImportStatus
            Duplicate = $_.duplicate
            Warnings = ($_.warnings -join " | ")
        }
    } | Format-Table -AutoSize
}

Write-Step "Executando dry-run do enem.dev"
try {
    $dryRunResponse = Invoke-ApiJson -Method Post -Uri "$ApiBaseUrl/admin/import/enem-dev/dry-run" -Headers $headers -Body @{
        year = $Year
        limit = $Limit
        offset = $Offset
        language = $Language
    }
} catch {
    if (Is-ControlledRateLimitMessage $_.Exception.Message) {
        Write-Warning "Dry-run do enem.dev foi limitado pela fonte externa. Mensagem controlada recebida do backend."
        Write-Host $_.Exception.Message -ForegroundColor Yellow
        return
    }
    throw
}

if (-not $dryRunResponse.success -or -not $dryRunResponse.data) {
    throw "Dry-run do enem.dev retornou payload inesperado."
}

$report = $dryRunResponse.data
$predictedImportable = [Math]::Max(($report.totalProcessed - $report.skippedDuplicates - $report.invalid), 0)

Write-Step "Coletando lotes apos dry-run"
$batchesAfterResponse = Invoke-ApiJson -Method Get -Uri "$ApiBaseUrl/admin/import/batches" -Headers $headers
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
    AnyPublishedAutomatically = $false
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

if ($unauthorizedStatus -ne 401 -and $unauthorizedStatus -ne 403) {
    throw "O endpoint /admin/import/enem-dev/years deveria exigir ADMIN, mas retornou status $unauthorizedStatus sem token."
}

if (-not $allNeedReview) {
    throw "Nem todas as questoes do preview ficaram em NEEDS_REVIEW."
}

if (-not $allValidatedAgainstOfficialFalse) {
    throw "Nem todas as questoes do preview retornaram validatedAgainstOfficialSource=false."
}

if ($anyPublished) {
    throw "O preview retornou questao com status PUBLISHED, o que nao deveria acontecer."
}

if (-not $nothingSaved) {
    throw "O dry-run nao confirmou ausencia de persistencia. Revise o endpoint antes de importar dados reais."
}

Write-Host ""
Write-Host "Dry-run do enem.dev concluido com sucesso. Nenhum dado foi salvo no banco." -ForegroundColor Green
