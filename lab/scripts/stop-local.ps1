$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env.cloud"
$composeFile = Join-Path $root "docker-compose.local.yml"
$logsDir = Join-Path $root "logs"
$backendPidFile = Join-Path $logsDir "backend-local.pid"
$frontendPidFile = Join-Path $logsDir "frontend-local.pid"

foreach ($pidFile in @($backendPidFile, $frontendPidFile)) {
    if (-not (Test-Path $pidFile)) {
        continue
    }

    $pidValue = Get-Content $pidFile | Select-Object -First 1
    if ($pidValue) {
        Stop-Process -Id ([int]$pidValue) -Force -ErrorAction SilentlyContinue
    }

    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

foreach ($port in @(8081, 3000)) {
    $listeners = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}

if ((Test-Path $envFile) -and (Test-Path $composeFile)) {
    docker compose --env-file $envFile -f $composeFile stop mysql | Out-Null
}

Write-Host "Local services stopped."
