param(
    [string]$BaseUrl = "http://localhost:8080/api",
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

Write-Step "Health"
$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
$health | ConvertTo-Json -Depth 5

Write-Step "Login"
$loginBody = @{
    usernameOrEmail = $UsernameOrEmail
    password = $Password
} | ConvertTo-Json

$login = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

$accessToken = $login.data.accessToken
$refreshToken = $login.data.refreshToken
$headers = @{ Authorization = "Bearer $accessToken" }

Write-Host "Login OK. Token recebido." -ForegroundColor Green

Write-Step "Dashboard"
$dashboard = Invoke-RestMethod -Method Get -Uri "$BaseUrl/dashboard" -Headers $headers
$dashboard | ConvertTo-Json -Depth 5

Write-Step "Perfil"
$profile = Invoke-RestMethod -Method Get -Uri "$BaseUrl/users/me" -Headers $headers
$profile | ConvertTo-Json -Depth 5

Write-Step "Questoes"
$questions = Invoke-RestMethod -Method Get -Uri "$BaseUrl/questions?page=0&size=5" -Headers $headers
$questions | ConvertTo-Json -Depth 5

Write-Step "Refresh token"
$refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
$refresh = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/auth/refresh" `
    -ContentType "application/json" `
    -Body $refreshBody

$refresh | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host "Todos os testes principais passaram." -ForegroundColor Green
