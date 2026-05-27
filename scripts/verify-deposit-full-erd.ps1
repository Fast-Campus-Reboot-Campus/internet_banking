param(
    [string] $Database = "deposit_db",
    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $User = "postgres",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

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
    "chatbot_node_flow",
    "common_product",
    "deposit_product",
    "common_contract",
    "deposit_contract",
    "common_account",
    "deposit_account",
    "common_terms_template",
    "terms_target_map",
    "common_terms_consent",
    "party",
    "customer"
)

$knownLegacyTables = @(
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

function Invoke-PsqlScalar([string] $Sql) {
    $result = & $PsqlPath `
        -h $HostName `
        -p $Port `
        -U $User `
        -d $Database `
        -At `
        -v ON_ERROR_STOP=1 `
        -c $Sql

    if ($LASTEXITCODE -ne 0) {
        throw "psql query failed: $Sql"
    }

    return ($result | Out-String).Trim()
}

foreach ($table in $requiredTables) {
    $exists = Invoke-PsqlScalar "SELECT to_regclass('public.""$table""') IS NOT NULL;"
    if ($exists -ne "t") {
        throw "Required full ERD table is missing: $table"
    }
}

foreach ($table in $requiredTables) {
    if ($table -eq "chatbot_node_flow") {
        continue
    }

    $pkCount = Invoke-PsqlScalar "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema='public' AND table_name='$table' AND constraint_type='PRIMARY KEY';"
    if ([int] $pkCount -lt 1) {
        throw "Primary key missing: $table"
    }
}

$fkCount = Invoke-PsqlScalar "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema='public' AND constraint_type='FOREIGN KEY';"
if ([int] $fkCount -lt 1) {
    throw "Foreign keys are missing"
}

$existingLegacyTables = @()
foreach ($table in $knownLegacyTables) {
    $exists = Invoke-PsqlScalar "SELECT to_regclass('public.""$table""') IS NOT NULL;"
    if ($exists -eq "t") {
        $existingLegacyTables += $table
    }
}

if ($existingLegacyTables.Count -gt 0) {
    Write-Host "WARN_LEGACY_TABLES_PRESENT"
    $existingLegacyTables | ForEach-Object { Write-Host " - $_" }
    Write-Host "These tables are not created by the final full ERD runner anymore."
}

$tableCount = Invoke-PsqlScalar "SELECT COUNT(*) FROM pg_tables WHERE schemaname='public';"

Write-Host "VERIFY_FULL_ERD_OK"
Write-Host "Tables currently in public schema: $tableCount"
Write-Host "Required tables verified: $($requiredTables.Count)"
Write-Host "Foreign keys verified: $fkCount"
