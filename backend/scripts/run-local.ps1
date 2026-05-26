param(
    [string]$EnvFile = ".env"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Load-DotEnvIntoProcess {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Arquivo .env nao encontrado em: $Path"
    }

    foreach ($line in Get-Content $Path) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith("#")) {
            continue
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            continue
        }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        Set-Item -Path "Env:$name" -Value $value
    }
}

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendRoot = Split-Path -Parent $scriptRoot
$envPath = Join-Path $backendRoot $EnvFile
$javaLauncherPath = Join-Path $backendRoot "java.cmd"
$mavenLauncherPath = Join-Path $backendRoot "mvn.cmd"
$javaLauncher = if (Test-Path $javaLauncherPath) { $javaLauncherPath } else { "java" }
$mavenLauncher = if (Test-Path $mavenLauncherPath) { $mavenLauncherPath } else { "mvn" }

Push-Location $backendRoot
try {
    Load-DotEnvIntoProcess -Path $envPath
    Set-Item -Path "Env:JAVA_TOOL_OPTIONS" -Value "-Xms128m -Xmx384m -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC"

    Write-Host ""
    Write-Host "Subindo Gabarita+ API com as variaveis de $envPath" -ForegroundColor Cyan
    Write-Host "SERVER_PORT=$env:SERVER_PORT"
    Write-Host "DB_URL=$env:DB_URL"
    Write-Host "CORS_ALLOWED_ORIGINS=$env:CORS_ALLOWED_ORIGINS"
    Write-Host "JAVA_TOOL_OPTIONS=$env:JAVA_TOOL_OPTIONS"
    Write-Host ""

    & $javaLauncher -version
    & $mavenLauncher -version

    & $mavenLauncher clean spring-boot:run
} finally {
    Pop-Location
}
