param(
    [string]$BaseUrl = "https://gabarita-plus-api.onrender.com/api",
    [string]$Origin = "https://gabarita-plus.vercel.app",
    [string]$UsernameOrEmail = "user@gabaritaplus.com",
    [string]$Password = "User@123"
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

Write-Step "Health"
$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
[pscustomobject]@{
    Status = $health.status
} | ConvertTo-Json -Depth 5

Write-Step "CORS preflight"
try {
    $request = [System.Net.HttpWebRequest]::Create("$BaseUrl/auth/login")
    $request.Method = "OPTIONS"
    $request.Headers.Add("Origin", $Origin)
    $request.Headers.Add("Access-Control-Request-Method", "POST")
    $request.Headers.Add("Access-Control-Request-Headers", "content-type,authorization")
    $response = $request.GetResponse()
    [pscustomobject]@{
        StatusCode = [int]$response.StatusCode
        AllowOrigin = $response.Headers["Access-Control-Allow-Origin"]
        AllowMethods = $response.Headers["Access-Control-Allow-Methods"]
        AllowCredentials = $response.Headers["Access-Control-Allow-Credentials"]
    } | ConvertTo-Json -Depth 5
} catch {
    throw "Falha no preflight CORS: $(Read-ErrorResponse $_.Exception)"
}

Write-Step "Login"
$loginBody = @{
    usernameOrEmail = $UsernameOrEmail
    password = $Password
} | ConvertTo-Json

try {
    $login = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/auth/login" `
        -ContentType "application/json" `
        -Body $loginBody
} catch {
    $errorBody = Read-ErrorResponse $_.Exception
    throw "Falha no login: $errorBody"
}

if (-not $login.success -or -not $login.data) {
    throw "Login retornou payload inesperado."
}

$accessToken = $login.data.accessToken
$refreshToken = $login.data.refreshToken
$headers = @{ Authorization = "Bearer $accessToken" }

[pscustomobject]@{
    Success = $login.success
    Message = $login.message
    HasAccessToken = [bool]$accessToken
    HasRefreshToken = [bool]$refreshToken
    UserFields = ($login.data.user.PSObject.Properties.Name -join ",")
} | ConvertTo-Json -Depth 5

Write-Step "Cadastro com token invalido ignorado em rota publica"
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$registerBody = @{
    fullName = "Teste Producao $suffix"
    email = "teste.producao.$suffix@example.com"
    username = "testeprod$suffix"
    password = "User@12345"
    targetCourse = "Medicina"
} | ConvertTo-Json

try {
    $register = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/auth/register" `
        -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer token-antigo-ou-invalido" } `
        -Body $registerBody
} catch {
    $errorBody = Read-ErrorResponse $_.Exception
    throw "Falha no cadastro: $errorBody"
}

[pscustomobject]@{
    Success = $register.success
    Message = $register.message
    HasAccessToken = [bool]$register.data.accessToken
    HasRefreshToken = [bool]$register.data.refreshToken
    UserEmail = $register.data.user.email
} | ConvertTo-Json -Depth 5

Write-Step "Dashboard"
$dashboard = Invoke-RestMethod -Method Get -Uri "$BaseUrl/dashboard" -Headers $headers
[pscustomobject]@{
    Success = $dashboard.success
    Message = $dashboard.message
    DataFields = ($dashboard.data.PSObject.Properties.Name -join ",")
} | ConvertTo-Json -Depth 5

Write-Step "Usuario logado"
$profile = Invoke-RestMethod -Method Get -Uri "$BaseUrl/users/me" -Headers $headers
[pscustomobject]@{
    Success = $profile.success
    Message = $profile.message
    UserId = $profile.data.id
    Email = $profile.data.email
} | ConvertTo-Json -Depth 5

Write-Step "Questoes"
$questions = Invoke-RestMethod -Method Get -Uri "$BaseUrl/questions?page=0&size=2" -Headers $headers
if (-not $questions.success) {
    throw "Endpoint de questoes retornou falha."
}

$firstQuestion = if ($questions.data.Count -gt 0) { $questions.data[0] } else { $null }
$firstQuestionHasSensitiveFields = $false
if ($firstQuestion) {
    $firstQuestionHasSensitiveFields =
        ($firstQuestion.PSObject.Properties.Name -contains "correctAlternative") -or
        ($firstQuestion.PSObject.Properties.Name -contains "explanation") -or
        ($firstQuestion.alternatives -and $firstQuestion.alternatives[0].PSObject.Properties.Name -contains "correct")
}

[pscustomobject]@{
    Success = $questions.success
    Message = $questions.message
    Count = $questions.data.Count
    MetadataFields = ($questions.metadata.PSObject.Properties.Name -join ",")
    SensitiveFieldsLeaked = $firstQuestionHasSensitiveFields
} | ConvertTo-Json -Depth 5

Write-Step "Refresh"
$refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
$refresh = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/auth/refresh" `
    -ContentType "application/json" `
    -Body $refreshBody

[pscustomobject]@{
    Success = $refresh.success
    Message = $refresh.message
    HasAccessToken = [bool]$refresh.data.accessToken
    ReturnedSameRefreshToken = ($refresh.data.refreshToken -eq $refreshToken)
} | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host "Checklist de producao concluido com sucesso." -ForegroundColor Green
