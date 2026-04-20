$root = "C:\Users\cjh\IdeaProjects\labLink\lab"
$logOut = Join-Path $root "logs\phase2-smoke.out.log"
$logErr = Join-Path $root "logs\phase2-smoke.err.log"
$pidFile = Join-Path $root "logs\phase2-smoke.pid"

New-Item -ItemType Directory -Force -Path (Join-Path $root "logs") | Out-Null
Remove-Item $logOut, $logErr, $pidFile -Force -ErrorAction SilentlyContinue

$env:DB_URL = "jdbc:mysql://localhost:3307/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "LabLinkLocalRoot!2026"
$env:JWT_SECRET = "LabLinkLocalJwtSecretForPhase2Wave1!2026"
$env:LABLINK_CACHE_REDIS_ENABLED = "true"
$env:LABLINK_MQ_RABBIT_ENABLED = "true"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:RABBITMQ_HOST = "localhost"
$env:RABBITMQ_PORT = "5672"
$env:RABBITMQ_USERNAME = "guest"
$env:RABBITMQ_PASSWORD = "guest"
$env:RABBITMQ_VHOST = "/"
$env:SERVER_PORT = "8081"

$process = Start-Process -FilePath "mvn" `
  -ArgumentList "spring-boot:run" `
  -WorkingDirectory $root `
  -RedirectStandardOutput $logOut `
  -RedirectStandardError $logErr `
  -PassThru

$process.Id | Set-Content $pidFile
Write-Output $process.Id
