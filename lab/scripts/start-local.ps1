$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env.cloud"
$composeFile = Join-Path $root "docker-compose.local.yml"
$frontendDir = Join-Path $root "frontend"
$logsDir = Join-Path $root "logs"
$backendJar = Join-Path $root "target\lab-recruitment-1.0.0.jar"
$backendPidFile = Join-Path $logsDir "backend-local.pid"
$frontendPidFile = Join-Path $logsDir "frontend-local.pid"
$backendOutLog = Join-Path $logsDir "backend-local.out"
$backendErrLog = Join-Path $logsDir "backend-local.err"
$frontendOutLog = Join-Path $logsDir "frontend-local.out"
$frontendErrLog = Join-Path $logsDir "frontend-local.err"
$judgeWorkDir = "C:\lab-judge-temp"

function Find-MavenCommand {
    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($mavenCommand) {
        return $mavenCommand.Source
    }

    $searchRoots = @(
        (Join-Path $env:USERPROFILE ".m2\wrapper\dists"),
        (Join-Path $env:USERPROFILE "tools"),
        (Join-Path $env:ProgramFiles "JetBrains")
    )

    foreach ($root in $searchRoots) {
        if (-not (Test-Path $root)) {
            continue
        }

        $candidate = Get-ChildItem $root -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName

        if ($candidate) {
            return $candidate
        }
    }

    return $null
}

function Find-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
        $javaHome = $env:JAVA_HOME.Trim()
        if ((Split-Path $javaHome -Leaf) -ieq "bin") {
            $javaHome = Split-Path $javaHome -Parent
        }
        if (Test-Path (Join-Path $javaHome "bin\java.exe")) {
            return $javaHome
        }
    }

    $javaHomeLine = java -XshowSettings:properties -version 2>&1 |
        Select-String "java.home" |
        Select-Object -First 1

    if ($javaHomeLine) {
        $javaHome = ($javaHomeLine.ToString().Split("=", 2)[1]).Trim()
        if ($javaHome -and (Test-Path $javaHome)) {
            return $javaHome
        }
    }

    return $null
}

if (-not (Test-Path $envFile)) {
    throw "Missing env file: $envFile"
}

New-Item -ItemType Directory -Force -Path $logsDir | Out-Null
New-Item -ItemType Directory -Force -Path $judgeWorkDir | Out-Null

$envMap = @{}
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
        return
    }
    $parts = $_ -split '=', 2
    if ($parts.Length -eq 2) {
        $envMap[$parts[0]] = $parts[1]
    }
}

if (-not (Test-Path $backendJar)) {
    $javaHome = Find-JavaHome
    if (-not $javaHome) {
        throw "Java is installed but JAVA_HOME could not be resolved."
    }

    $env:JAVA_HOME = $javaHome
    $maven = Find-MavenCommand
    if (-not $maven) {
        throw "Missing backend jar and no Maven installation was found."
    }

    Write-Host "Backend jar missing. Building with Maven..."
    Push-Location $root
    try {
        & $maven "-DskipTests" "clean" "package"
    } finally {
        Pop-Location
    }

    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $backendJar)) {
        throw "Backend jar build failed."
    }
}

docker compose --env-file $envFile -f $composeFile up -d mysql

$mysqlHealthy = $false
for ($i = 0; $i -lt 30; $i++) {
    $health = docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" lab-mysql 2>$null
    if ($LASTEXITCODE -eq 0 -and $health -eq "healthy") {
        $mysqlHealthy = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $mysqlHealthy) {
    throw "MySQL container is not healthy. Run 'docker logs lab-mysql' to inspect."
}

$backendListening = Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue
if (-not $backendListening) {
    $backendArgs = @(
        "-Dspring.datasource.url=jdbc:mysql://localhost:3307/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true",
        "-Dspring.datasource.username=root",
        "-Dspring.datasource.password=$($envMap["MYSQL_ROOT_PASSWORD"])",
        "-Djwt.secret=$($envMap["JWT_SECRET"])",
        "-Dfile.upload-path=./uploads/",
        "-Djudge.work-dir=$judgeWorkDir",
        "-jar",
        $backendJar
    )
    $backendProcess = Start-Process -FilePath "java" `
        -ArgumentList $backendArgs `
        -WorkingDirectory $root `
        -RedirectStandardOutput $backendOutLog `
        -RedirectStandardError $backendErrLog `
        -PassThru

    Set-Content -Path $backendPidFile -Value $backendProcess.Id

    $backendReady = $false
    for ($i = 0; $i -lt 20; $i++) {
        $backendListening = Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue
        if ($backendListening) {
            $backendReady = $true
            break
        }
        Start-Sleep -Seconds 1
    }

    if (-not $backendReady) {
        if (Test-Path $backendErrLog) {
            Get-Content $backendErrLog -Tail 40
        }
        if (Test-Path (Join-Path $logsDir "application.log")) {
            Get-Content (Join-Path $logsDir "application.log") -Tail 40
        }
        throw "Backend failed to start on port 8081."
    }
} elseif (-not (Test-Path $backendPidFile)) {
    Set-Content -Path $backendPidFile -Value ($backendListening | Select-Object -First 1 -ExpandProperty OwningProcess)
}

$frontendListening = Get-NetTCPConnection -State Listen -LocalPort 3000 -ErrorAction SilentlyContinue
if (-not $frontendListening) {
    $frontendProcess = Start-Process -FilePath "npm.cmd" `
        -ArgumentList "run", "dev" `
        -WorkingDirectory $frontendDir `
        -RedirectStandardOutput $frontendOutLog `
        -RedirectStandardError $frontendErrLog `
        -PassThru

    Set-Content -Path $frontendPidFile -Value $frontendProcess.Id

    $frontendReady = $false
    for ($i = 0; $i -lt 20; $i++) {
        $frontendListening = Get-NetTCPConnection -State Listen -LocalPort 3000 -ErrorAction SilentlyContinue
        if ($frontendListening) {
            $frontendReady = $true
            break
        }
        Start-Sleep -Seconds 1
    }

    if (-not $frontendReady) {
        if (Test-Path $frontendErrLog) {
            Get-Content $frontendErrLog -Tail 40
        }
        throw "Frontend failed to start on port 3000."
    }
} elseif (-not (Test-Path $frontendPidFile)) {
    Set-Content -Path $frontendPidFile -Value ($frontendListening | Select-Object -First 1 -ExpandProperty OwningProcess)
}

Write-Host "Frontend: http://localhost:3000/"
Write-Host "Backend:  http://localhost:8081/"
Write-Host "MySQL:    localhost:3307"
