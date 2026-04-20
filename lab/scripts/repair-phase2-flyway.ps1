param(
    [string]$DbUrl = "jdbc:mysql://localhost:3307/lab_recruitment",
    [string]$DbUser = "root",
    [string]$DbPassword = "LabLinkLocalRoot!2026",
    [string]$Locations = "filesystem:src/main/resources/db/migration"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$arguments = @(
    "-q"
    "org.flywaydb:flyway-maven-plugin:8.5.13:repair"
    "-Dflyway.url=$DbUrl"
    "-Dflyway.user=$DbUser"
    "-Dflyway.password=$DbPassword"
    "-Dflyway.locations=$Locations"
)

Write-Host "Running Flyway repair against $DbUrl"
& mvn @arguments

if ($LASTEXITCODE -ne 0) {
    throw "Flyway repair failed."
}

Write-Host "Flyway repair completed."
