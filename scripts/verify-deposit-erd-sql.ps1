param(
    [Parameter(Mandatory = $true)]
    [string] $SourceSql,

    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $AdminDatabase = "postgres",
    [string] $User = "postgres",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    [string] $CreatedbPath = "C:\Program Files\PostgreSQL\16\bin\createdb.exe",
    [string] $DropdbPath = "C:\Program Files\PostgreSQL\16\bin\dropdb.exe"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $SourceSql)) {
    throw "Source SQL file not found: $SourceSql"
}
if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}
if (-not (Test-Path -LiteralPath $CreatedbPath)) {
    throw "createdb not found: $CreatedbPath"
}
if (-not (Test-Path -LiteralPath $DropdbPath)) {
    throw "dropdb not found: $DropdbPath"
}

$sourcePath = (Resolve-Path -LiteralPath $SourceSql).Path
$beforeHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourcePath).Hash
$database = "ib_deposit_verify_" + (Get-Date -Format "yyyyMMddHHmmss")

$requiredTables = @(
    "common_product",
    "deposit_product",
    "common_contract",
    "deposit_contract",
    "common_account",
    "deposit_account",
    "common_terms_template",
    "terms_target_map",
    "common_terms_consent"
)

$forbiddenTables = @(
    "product_common_terms",
    "deposit_term_application_management"
)

function Invoke-PsqlScalar([string] $Sql) {
    $result = & $PsqlPath `
        -h $HostName `
        -p $Port `
        -U $User `
        -d $database `
        -At `
        -v ON_ERROR_STOP=1 `
        -c $Sql

    if ($LASTEXITCODE -ne 0) {
        throw "psql query failed: $Sql"
    }

    return ($result | Out-String).Trim()
}

try {
    & $CreatedbPath -h $HostName -p $Port -U $User $database
    if ($LASTEXITCODE -ne 0) {
        throw "createdb failed for $database"
    }

    & (Join-Path $PSScriptRoot "run-deposit-erd-sql.ps1") `
        -SourceSql $sourcePath `
        -HostName $HostName `
        -Port $Port `
        -Database $database `
        -User $User `
        -PsqlPath $PsqlPath

    if ($LASTEXITCODE -ne 0) {
        throw "deposit ERD runner failed"
    }

    foreach ($table in $requiredTables) {
        $exists = Invoke-PsqlScalar "SELECT to_regclass('public.$table') IS NOT NULL;"
        if ($exists -ne "t") {
            throw "Required table was not created: $table"
        }
    }

    foreach ($table in $forbiddenTables) {
        $exists = Invoke-PsqlScalar "SELECT to_regclass('public.$table') IS NOT NULL;"
        if ($exists -ne "f") {
            throw "Forbidden table was created: $table"
        }
    }

    foreach ($table in $requiredTables) {
        $pkCount = Invoke-PsqlScalar "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema='public' AND table_name='$table' AND constraint_type='PRIMARY KEY';"
        if ([int]$pkCount -lt 1) {
            throw "Primary key missing: $table"
        }
    }

    $expectedFkPairs = @(
        @("deposit_product", "common_product"),
        @("deposit_contract", "common_contract"),
        @("deposit_account", "common_account"),
        @("terms_target_map", "common_terms_template"),
        @("common_terms_consent", "common_terms_template")
    )

    foreach ($pair in $expectedFkPairs) {
        $baseTable = $pair[0]
        $refTable = $pair[1]
        $fkCount = Invoke-PsqlScalar @"
SELECT COUNT(*)
FROM information_schema.referential_constraints rc
JOIN information_schema.table_constraints fk
  ON rc.constraint_schema = fk.constraint_schema
 AND rc.constraint_name = fk.constraint_name
JOIN information_schema.table_constraints pk
  ON rc.unique_constraint_schema = pk.constraint_schema
 AND rc.unique_constraint_name = pk.constraint_name
WHERE fk.table_schema = 'public'
  AND fk.table_name = '$baseTable'
  AND pk.table_schema = 'public'
  AND pk.table_name = '$refTable';
"@

        if ([int]$fkCount -lt 1) {
            throw "Foreign key missing: $baseTable -> $refTable"
        }
    }

    $afterHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourcePath).Hash
    if ($beforeHash -ne $afterHash) {
        throw "Source SQL was modified during verification"
    }

    Write-Host "VERIFY_OK $database"
    Write-Host "Source SQL hash unchanged: $afterHash"
}
finally {
    & $DropdbPath -h $HostName -p $Port -U $User $database 2>$null
}
