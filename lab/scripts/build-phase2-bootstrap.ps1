$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$migrationDir = Join-Path $projectRoot "src\main\resources\db\migration"
$outputDir = Join-Path $projectRoot "src\main\resources\db\bootstrap"
$outputFile = Join-Path $outputDir "phase2-wave1-bootstrap.sql"
$migrationOnlyOutputFile = Join-Path $outputDir "phase2-wave1-migrations-only.sql"

$migrationFiles = @(
    "V1__search_and_list_indexes.sql",
    "V2__student_profile_scope.sql",
    "V3__equipment_management_upgrade.sql",
    "V4__audit_notification_center.sql",
    "V5__attendance_leave_and_change_log.sql",
    "V6__search_keyword_hot.sql",
    "V7__file_object_and_relation.sql",
    "V8__phase2_core_indexes.sql",
    "V9__restore_foreign_keys.sql"
)

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

function New-Header {
    param(
        [string]$Title,
        [string]$SourceLabel
    )

    return @(
        "-- $Title"
        "-- Source: $SourceLabel"
        "-- Regenerate with: powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-phase2-bootstrap.ps1"
        ""
        "SET NAMES utf8mb4;"
        ""
    )
}

function Add-SqlFile {
    param(
        [string[]]$Content,
        [string]$Path,
        [string]$Label
    )

    if (-not (Test-Path $Path)) {
        throw "Missing SQL file: $Path"
    }

    $fileContent = Get-Content $Path
    return $Content + @(
        "-- =================================================="
        "-- BEGIN $Label"
        "-- =================================================="
        ""
    ) + $fileContent + @(
        ""
        "-- =================================================="
        "-- END $Label"
        "-- =================================================="
        ""
    )
}

$bootstrapContent = New-Header `
    -Title "Phase 2 Wave 1 bootstrap SQL" `
    -SourceLabel "src/main/resources/db/migration/V1-V9"

$migrationOnlyContent = New-Header `
    -Title "Phase 2 Wave 1 migrations-only SQL" `
    -SourceLabel "src/main/resources/db/migration/V1-V9"

foreach ($file in $migrationFiles) {
    $path = Join-Path $migrationDir $file
    $bootstrapContent = Add-SqlFile -Content $bootstrapContent -Path $path -Label $file
    $migrationOnlyContent = Add-SqlFile -Content $migrationOnlyContent -Path $path -Label $file
}

Set-Content -Path $outputFile -Value $bootstrapContent -Encoding UTF8
Set-Content -Path $migrationOnlyOutputFile -Value $migrationOnlyContent -Encoding UTF8
Write-Output $outputFile
Write-Output $migrationOnlyOutputFile
