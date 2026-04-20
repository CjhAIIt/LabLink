[CmdletBinding()]
param(
    [string]$ServerHost = "101.35.79.76",
    [string]$User = "ubuntu",
    [Parameter(Mandatory = $true)]
    [string]$Password,
    [ValidateSet("preserve", "reset-init")]
    [string]$DbMode = "preserve",
    [string]$RemoteReleaseScript = "/usr/local/bin/lablink-release.sh",
    [switch]$SkipBackendBuild,
    [switch]$SkipFrontendBuild
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendJar = Join-Path $projectRoot "target\lab-recruitment-1.0.0.jar"
$frontendRoot = Join-Path $projectRoot "frontend"
$frontendArchive = Join-Path $projectRoot ("frontend-dist-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".tgz")
$initSql = Join-Path $projectRoot "src\main\resources\init.sql"
$releaseDir = "/home/$User/lablink-upload-" + (Get-Date -Format "yyyyMMdd-HHmmss")

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

function Invoke-Step {
    param(
        [string]$Label,
        [scriptblock]$Action
    )
    Write-Host "==> $Label"
    & $Action
}

Require-Command mvn
Require-Command tar

$pscp = "C:\Program Files\PuTTY\pscp.exe"
$plink = "C:\Program Files\PuTTY\plink.exe"
if (-not (Test-Path $pscp)) {
    throw "Missing pscp: $pscp"
}
if (-not (Test-Path $plink)) {
    throw "Missing plink: $plink"
}

try {
    if (-not $SkipBackendBuild) {
        Invoke-Step "Build backend" {
            & mvn "-DskipTests" "package"
            if ($LASTEXITCODE -ne 0) {
                throw "Backend build failed"
            }
        }
    }

    if (-not $SkipFrontendBuild) {
        Invoke-Step "Build frontend" {
            if (-not (Test-Path (Join-Path $frontendRoot "node_modules"))) {
                Push-Location $frontendRoot
                try {
                    & npm ci
                    if ($LASTEXITCODE -ne 0) {
                        throw "npm ci failed"
                    }
                } finally {
                    Pop-Location
                }
            }
            Push-Location $frontendRoot
            try {
                & npm run build
                if ($LASTEXITCODE -ne 0) {
                    throw "Frontend build failed"
                }
            } finally {
                Pop-Location
            }
        }
    }

    if (-not (Test-Path $backendJar)) {
        throw "Backend jar not found: $backendJar"
    }

    if (Test-Path $frontendArchive) {
        Remove-Item -Force $frontendArchive
    }

    Invoke-Step "Package frontend dist" {
        & tar "-czf" $frontendArchive "-C" $frontendRoot "dist"
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend archive packaging failed"
        }
    }

    Invoke-Step "Create remote release directory" {
        & $plink -ssh -batch -no-antispoof -pw $Password "$User@$ServerHost" "mkdir -p $releaseDir"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to create remote release directory"
        }
    }

    Invoke-Step "Upload backend jar" {
        & $pscp -batch -pw $Password $backendJar "${User}@${ServerHost}:$releaseDir/"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to upload backend jar"
        }
    }

    Invoke-Step "Upload frontend archive" {
        & $pscp -batch -pw $Password $frontendArchive "${User}@${ServerHost}:$releaseDir/"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to upload frontend archive"
        }
    }

    Invoke-Step "Upload init.sql" {
        & $pscp -batch -pw $Password $initSql "${User}@${ServerHost}:$releaseDir/"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to upload init.sql"
        }
    }

    Invoke-Step "Run remote release script" {
        & $plink -ssh -batch -no-antispoof -pw $Password "$User@$ServerHost" "sudo $RemoteReleaseScript --release-dir $releaseDir --db-mode $DbMode"
        if ($LASTEXITCODE -ne 0) {
            throw "Remote release script failed"
        }
    }

    Write-Host ""
    Write-Host "Deployment finished."
    Write-Host "Target: http://$ServerHost/"
    Write-Host "DB mode: $DbMode"
} finally {
    if (Test-Path $frontendArchive) {
        Remove-Item -Force $frontendArchive -ErrorAction SilentlyContinue
    }
}
