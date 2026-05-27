param(
    [Parameter(Mandatory = $true)]
    [string[]] $SourceSql,

    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $Database = "deposit_db",
    [string] $User = "postgres",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    [switch] $KeepConvertedFile
)

$ErrorActionPreference = "Stop"

$depositRootTables = @(
    "deposit_departments",
    "deposit_banking_products",
    "banking_deposit_products",
    "deposit_savings_products",
    "deposit_subscription_products",
    "banking_deposit_product_join_channels",
    "banking_deposit_product_interest_rates",
    "deposit_target_groups",
    "banking_deposit_product_target_groups",
    "deposit_special_terms",
    "banking_deposit_product_special_terms",
    "deposit_contracts",
    "deposit_contract_applied_rates",
    "deposit_contract_special_term_agreements",
    "deposit_accounts",
    "deposit_interest_history",
    "deposit_subscription_payment_recognition_history",
    "deposit_transactions",
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

$sourcePaths = @()
foreach ($source in $SourceSql) {
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Source SQL file not found: $source"
    }
    $sourcePaths += (Resolve-Path -LiteralPath $source).Path
}

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

$combinedSql = New-Object System.Text.StringBuilder
foreach ($path in $sourcePaths) {
    [void] $combinedSql.AppendLine((Get-Content -LiteralPath $path -Raw -Encoding UTF8))
    [void] $combinedSql.AppendLine()
}

$normalizedSql = $combinedSql.ToString() -replace "`r`n", "`n" -replace "`r", "`n"

$createPattern = '(?is)CREATE\s+TABLE\s+[`"]?([^`"\s(]+)[`"]?\s*\(.*?\);'
$alterPattern = '(?is)ALTER\s+TABLE\s+[`"]?([^`"\s(]+)[`"]?\s+ADD\s+CONSTRAINT\s+[`"]?([^`"\s]+)[`"]?\s+.*?;'
$alterFkPattern = '(?is)ALTER\s+TABLE\s+[`"]?([^`"\s(]+)[`"]?\s+ADD\s+CONSTRAINT\s+[`"]?([^`"\s]+)[`"]?\s+FOREIGN\s+KEY\s*\(.*?\)\s*REFERENCES\s+[`"]?([^`"\s(]+)[`"]?\s*\(.*?\);'
$createIndexPattern = '(?is)CREATE\s+(?:UNIQUE\s+)?INDEX\s+[`"]?[^`"\s]+[`"]?\s+ON\s+[`"]?([^`"\s(]+)[`"]?\s*\(.*?\)\s*(?:WHERE\s+.*?)*;'

$createMatches = [regex]::Matches($normalizedSql, $createPattern)
$alterMatches = [regex]::Matches($normalizedSql, $alterPattern)
$alterFkMatches = [regex]::Matches($normalizedSql, $alterFkPattern)
$createIndexMatches = [regex]::Matches($normalizedSql, $createIndexPattern)

$createByTable = @{}
foreach ($match in $createMatches) {
    $table = $match.Groups[1].Value
    if (-not $createByTable.ContainsKey($table)) {
        $createByTable[$table] = $match.Value
    }
}

$references = @()
foreach ($match in $alterFkMatches) {
    $references += [PSCustomObject]@{
        BaseTable = $match.Groups[1].Value
        RefTable = $match.Groups[3].Value
    }
}

foreach ($match in $createMatches) {
    $baseTable = $match.Groups[1].Value
    foreach ($refMatch in [regex]::Matches($match.Value, '(?is)\bREFERENCES\s+[`"]?([^`"\s(]+)[`"]?\s*\(')) {
        $references += [PSCustomObject]@{
            BaseTable = $baseTable
            RefTable = $refMatch.Groups[1].Value
        }
    }
}

$selected = @{}
foreach ($table in $depositRootTables) {
    if ($createByTable.ContainsKey($table)) {
        $selected[$table] = $true
    }
}

$changed = $true
while ($changed) {
    $changed = $false
    foreach ($ref in $references) {
        if ($selected.ContainsKey($ref.BaseTable) -and -not $selected.ContainsKey($ref.RefTable)) {
            if (-not $createByTable.ContainsKey($ref.RefTable)) {
                throw "Referenced table not found in source SQL: $($ref.BaseTable) -> $($ref.RefTable)"
            }
            $selected[$ref.RefTable] = $true
            $changed = $true
        }
    }
}

$missingRootTables = $depositRootTables | Where-Object { -not $createByTable.ContainsKey($_) } | Sort-Object
if ($missingRootTables.Count -gt 0) {
    Write-Host "Skipped root tables not present in source SQL:"
    $missingRootTables | ForEach-Object { Write-Host " - $_" }
}

$selectedSet = @{}
foreach ($table in $selected.Keys) {
    $selectedSet[$table] = $true
}

$selectedSqlParts = New-Object System.Collections.Generic.List[string]
foreach ($match in $createMatches) {
    $table = $match.Groups[1].Value
    if ($selectedSet.ContainsKey($table)) {
        $selectedSqlParts.Add($match.Value)
    }
}

foreach ($match in $alterMatches) {
    $table = $match.Groups[1].Value
    if (-not $selectedSet.ContainsKey($table)) {
        continue
    }

    $refMatch = [regex]::Match($match.Value, '(?is)\bREFERENCES\s+[`"]?([^`"\s(]+)[`"]?\s*\(')
    if ($refMatch.Success -and -not $selectedSet.ContainsKey($refMatch.Groups[1].Value)) {
        continue
    }

    $selectedSqlParts.Add($match.Value)
}

foreach ($match in $createIndexMatches) {
    $table = $match.Groups[1].Value
    if ($selectedSet.ContainsKey($table)) {
        $selectedSqlParts.Add($match.Value)
    }
}

$filteredPath = Join-Path ([System.IO.Path]::GetTempPath()) ("deposit-expanded-source-" + [System.Guid]::NewGuid().ToString("N") + ".sql")
$convertedPath = Join-Path ([System.IO.Path]::GetTempPath()) ("deposit-expanded-postgres-" + [System.Guid]::NewGuid().ToString("N") + ".sql")

function Convert-ToIdempotentPostgresSql {
    param([string[]] $Statements)

    $convertedStatements = New-Object System.Collections.Generic.List[string]

    foreach ($statement in $Statements) {
        $converted = $statement
        $converted = $converted -replace "`r`n", "`n"
        $converted = $converted -replace "`r", "`n"
        $converted = $converted -replace "COMMENT\s+'([^']|'')*'", ""
        $converted = $converted -replace '`', '"'
        $converted = $converted -replace "\bTINYINT\b", "SMALLINT"

        if ($converted -match '(?is)^\s*CREATE\s+TABLE\s+') {
            $converted = $converted -replace '(?is)^\s*CREATE\s+TABLE\s+', 'CREATE TABLE IF NOT EXISTS '
            $convertedStatements.Add($converted)
            continue
        }

        if ($converted -match '(?is)^\s*CREATE\s+UNIQUE\s+INDEX\s+') {
            $converted = $converted -replace '(?is)^\s*CREATE\s+UNIQUE\s+INDEX\s+', 'CREATE UNIQUE INDEX IF NOT EXISTS '
            $convertedStatements.Add($converted)
            continue
        }

        if ($converted -match '(?is)^\s*CREATE\s+INDEX\s+') {
            $converted = $converted -replace '(?is)^\s*CREATE\s+INDEX\s+', 'CREATE INDEX IF NOT EXISTS '
            $convertedStatements.Add($converted)
            continue
        }

        $constraintMatch = [regex]::Match($converted, '(?is)^\s*ALTER\s+TABLE\s+"?([^"\s]+)"?\s+ADD\s+CONSTRAINT\s+"?([^"\s]+)"?\s+(.+);\s*$')
        if ($constraintMatch.Success) {
            $tableName = $constraintMatch.Groups[1].Value.Replace("'", "''")
            $constraintName = $constraintMatch.Groups[2].Value.Replace("'", "''")
            $body = $constraintMatch.Groups[3].Value
            $convertedStatements.Add(@"
DO `$`$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'public'
          AND t.relname = '$tableName'
          AND c.conname = '$constraintName'
    ) THEN
        ALTER TABLE "$tableName" ADD CONSTRAINT "$constraintName" $body;
    END IF;
END
`$`$;
"@)
            continue
        }

        $convertedStatements.Add($converted)
    }

    return $convertedStatements -join "`n`n"
}

try {
    Set-Content -LiteralPath $filteredPath -Value ($selectedSqlParts -join "`n`n") -Encoding UTF8

    $converted = Convert-ToIdempotentPostgresSql -Statements $selectedSqlParts
    Set-Content -LiteralPath $convertedPath -Value $converted -Encoding UTF8

    Write-Host "Deposit expanded ERD source files:"
    $sourcePaths | ForEach-Object { Write-Host " - $_" }
    Write-Host "Resolved execution tables:"
    $selected.Keys | Sort-Object | ForEach-Object { Write-Host " - $_" }
    Write-Host "Converted SQL: $convertedPath"

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $psqlOutput = & $PsqlPath `
            -h $HostName `
            -p $Port `
            -U $User `
            -d $Database `
            -v ON_ERROR_STOP=1 `
            -f $convertedPath 2>&1
        $psqlExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    $psqlOutput | ForEach-Object { Write-Host $_ }

    if ($psqlExitCode -ne 0) {
        throw "psql failed with exit code $psqlExitCode"
    }
}
finally {
    if (-not $KeepConvertedFile) {
        if (Test-Path -LiteralPath $filteredPath) {
            Remove-Item -LiteralPath $filteredPath -Force
        }
        if (Test-Path -LiteralPath $convertedPath) {
            Remove-Item -LiteralPath $convertedPath -Force
        }
    }
}
