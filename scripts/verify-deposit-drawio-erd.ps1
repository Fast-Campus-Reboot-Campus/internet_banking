param(
    [Parameter(Mandatory = $true)]
    [string] $DrawioPath,

    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $User = "postgres",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    [string] $CreatedbPath = "C:\Program Files\PostgreSQL\16\bin\createdb.exe",
    [string] $DropdbPath = "C:\Program Files\PostgreSQL\16\bin\dropdb.exe"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $DrawioPath)) {
    throw "drawio file not found: $DrawioPath"
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

$drawioFullPath = (Resolve-Path -LiteralPath $DrawioPath).Path
$beforeHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $drawioFullPath).Hash
$database = "ib_drawio_verify_" + (Get-Date -Format "yyyyMMddHHmmss")

$requiredTables = @(
    "deposit_banking_products",
    "banking_deposit_products",
    "deposit_savings_products",
    "deposit_subscription_products",
    "banking_deposit_product_join_channels",
    "banking_deposit_product_interest_rates",
    "deposit_contracts",
    "deposit_accounts",
    "deposit_interest_history",
    "deposit_special_terms",
    "banking_deposit_product_special_terms",
    "deposit_contract_special_term_agreements",
    "deposit_contract_applied_rates",
    "deposit_departments",
    "deposit_subscription_payment_recognition_history",
    "deposit_target_groups",
    "banking_deposit_product_target_groups",
    "deposit_transactions",
    "deposit_term_application_management",
    "chatbot_node_flow"
)

$forbiddenTables = @(
    "banking_products",
    "deposit_products"
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

    & (Join-Path $PSScriptRoot "run-deposit-drawio-erd.ps1") `
        -DrawioPath $drawioFullPath `
        -HostName $HostName `
        -Port $Port `
        -Database $database `
        -User $User `
        -PsqlPath $PsqlPath

    if ($LASTEXITCODE -ne 0) {
        throw "drawio ERD runner failed"
    }

    foreach ($table in $requiredTables) {
        $exists = Invoke-PsqlScalar "SELECT to_regclass('public.""$table""') IS NOT NULL;"
        if ($exists -ne "t") {
            throw "Required drawio table was not created: $table"
        }
    }

    foreach ($table in $forbiddenTables) {
        $exists = Invoke-PsqlScalar "SELECT to_regclass('public.""$table""') IS NOT NULL;"
        if ($exists -ne "f") {
            throw "Forbidden drawio alias table was created: $table"
        }
    }

    $tableCount = Invoke-PsqlScalar "SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public';"
    if ([int] $tableCount -ne $requiredTables.Count) {
        throw "Unexpected table count. expected=$($requiredTables.Count), actual=$tableCount"
    }

    $fkCount = Invoke-PsqlScalar "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema='public' AND constraint_type='FOREIGN KEY';"
    if ([int] $fkCount -lt 1) {
        throw "No foreign keys were created from drawio edges"
    }

    $tooLongNames = Invoke-PsqlScalar "SELECT COUNT(*) FROM pg_constraint WHERE length(conname) > 63;"
    if ([int] $tooLongNames -ne 0) {
        throw "Constraint name longer than PostgreSQL identifier limit was generated"
    }

    $afterHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $drawioFullPath).Hash
    if ($beforeHash -ne $afterHash) {
        throw "drawio source was modified during verification"
    }

    Write-Host "VERIFY_DRAWIO_OK $database"
    Write-Host "Drawio hash unchanged: $afterHash"
    Write-Host "Tables verified: $tableCount"
    Write-Host "Foreign keys verified: $fkCount"
}
finally {
    & $DropdbPath -h $HostName -p $Port -U $User $database 2>$null
}
