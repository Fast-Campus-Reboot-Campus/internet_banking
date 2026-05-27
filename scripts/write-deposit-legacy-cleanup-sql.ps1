param(
    [string] $OutputPath = "build\deposit-legacy-cleanup.sql"
)

$ErrorActionPreference = "Stop"

$legacyTables = @(
    "accounts",
    "banking_products",
    "branches",
    "contracts",
    "departments",
    "deposit_products",
    "employees",
    "join_methods",
    "products"
)

$outputFullPath = Join-Path (Get-Location) $OutputPath
$outputDir = Split-Path -Parent $outputFullPath
if ($outputDir -and -not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("-- Review before running. This drops known legacy/alias tables only.")
$lines.Add("BEGIN;")
foreach ($table in $legacyTables) {
    $lines.Add("DROP TABLE IF EXISTS public.""$table"" CASCADE;")
}
$lines.Add("COMMIT;")

Set-Content -LiteralPath $outputFullPath -Value ($lines -join "`n") -Encoding UTF8
Write-Host "Wrote cleanup SQL: $outputFullPath"
