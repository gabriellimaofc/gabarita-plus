param(
    [string]$EnvFile = ".env"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendRoot = Split-Path -Parent $scriptRoot
$javaLauncherPath = Join-Path $backendRoot "java.cmd"
$mavenLauncherPath = Join-Path $backendRoot "mvn.cmd"
$javaLauncher = if (Test-Path $javaLauncherPath) { $javaLauncherPath } else { "java" }
$mavenLauncher = if (Test-Path $mavenLauncherPath) { $mavenLauncherPath } else { "mvn" }

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ===" -ForegroundColor Cyan
}

function Test-Tool {
    param(
        [string]$Name,
        [scriptblock]$VersionCommand
    )

    try {
        $output = & $VersionCommand 2>&1
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne $null -and $exitCode -ne 0) {
            throw ($output | Out-String)
        }
        Write-Host "[OK] $Name" -ForegroundColor Green
        $output | Select-Object -First 2 | ForEach-Object { Write-Host "     $_" }
        return $true
    } catch {
        Write-Host "[FAIL] $Name nao encontrado ou com erro." -ForegroundColor Red
        Write-Host "       $_"
        return $false
    }
}

function Load-DotEnv {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return @{}
    }

    $map = @{}
    foreach ($line in Get-Content $Path) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith("#")) {
            continue
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            continue
        }

        $map[$parts[0].Trim()] = $parts[1].Trim()
    }

    return $map
}

function Parse-JdbcUrl {
    param([string]$JdbcUrl)

    if ($JdbcUrl -match "^jdbc:postgresql://(?<host>[^:/?]+)(:(?<port>\d+))?/(?<db>[^?]+)") {
        return @{
            Host = $Matches["host"]
            Port = if ($Matches["port"]) { [int]$Matches["port"] } else { 5432 }
            Database = $Matches["db"]
        }
    }

    return $null
}
$envPath = Join-Path $backendRoot $EnvFile
$envValues = Load-DotEnv -Path $envPath

Write-Section "Ferramentas"
$javaOk = Test-Tool -Name "Java" -VersionCommand { & $javaLauncher -version }
$mavenOk = Test-Tool -Name "Maven" -VersionCommand { & $mavenLauncher -version }
$dockerOk = Test-Tool -Name "Docker" -VersionCommand { docker --version }

Write-Section "Arquivo .env"
if (Test-Path $envPath) {
    Write-Host "[OK] Encontrado: $envPath" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Arquivo nao encontrado: $envPath" -ForegroundColor Red
    Write-Host "       Copie .env.example para .env antes de subir o backend."
}

$serverPort = if ($envValues.ContainsKey("SERVER_PORT")) { [int]$envValues["SERVER_PORT"] } else { 8080 }
$dbUrl = if ($envValues.ContainsKey("DB_URL")) { $envValues["DB_URL"] } else { "" }

Write-Section "Porta local da API"
try {
    $tcp = Get-NetTCPConnection -LocalPort $serverPort -ErrorAction Stop
    $owners = $tcp | Select-Object -ExpandProperty OwningProcess -Unique
    Write-Host "[WARN] Porta $serverPort em uso. PID(s): $($owners -join ', ')" -ForegroundColor Yellow
} catch {
    Write-Host "[OK] Porta $serverPort livre." -ForegroundColor Green
}

Write-Section "Banco de dados"
if ($dbUrl) {
    Write-Host "DB_URL: $dbUrl"
    $jdbc = Parse-JdbcUrl -JdbcUrl $dbUrl
    if ($jdbc) {
        Write-Host "Host: $($jdbc.Host)"
        Write-Host "Port: $($jdbc.Port)"
        Write-Host "DB  : $($jdbc.Database)"

        try {
            $dbTest = Test-NetConnection -ComputerName $jdbc.Host -Port $jdbc.Port -WarningAction SilentlyContinue
            if ($dbTest.TcpTestSucceeded) {
                Write-Host "[OK] Conexao TCP com banco disponivel." -ForegroundColor Green
            } else {
                Write-Host "[WARN] Nao foi possivel conectar ao host do banco." -ForegroundColor Yellow
            }
        } catch {
            Write-Host "[WARN] Falha ao testar rede com o banco: $_" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[WARN] Nao foi possivel interpretar a DB_URL." -ForegroundColor Yellow
    }
} else {
    Write-Host "[WARN] DB_URL nao definida no .env." -ForegroundColor Yellow
}

Write-Section "Resumo"
if ($javaOk -and $mavenOk) {
    Write-Host "Voce ja pode tentar subir com:" -ForegroundColor Green
    Write-Host "powershell -ExecutionPolicy Bypass -File .\scripts\run-local.ps1"
} else {
    Write-Host "Instale Java 21 e Maven antes de continuar." -ForegroundColor Yellow
}

if (-not $dockerOk) {
    Write-Host "Docker nao e obrigatorio para rodar com Supabase, mas e recomendado para o ambiente local completo." -ForegroundColor Yellow
}
