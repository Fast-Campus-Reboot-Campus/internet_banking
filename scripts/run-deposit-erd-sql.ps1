param(
    [Parameter(Mandatory = $true)]
    [string] $SourceSql,

    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $Database = "deposit_db",
    [string] $User = "postgres",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    [switch] $KeepConvertedFile
)

$ErrorActionPreference = "Stop"

$depositRootTables = @(
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

if (-not (Test-Path -LiteralPath $SourceSql)) {
    throw "Source SQL file not found: $SourceSql"
}

$sourcePath = (Resolve-Path -LiteralPath $SourceSql).Path
$sql = Get-Content -LiteralPath $sourcePath -Raw -Encoding UTF8
$normalizedSql = $sql -replace "`r`n", "`n" -replace "`r", "`n"

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
    $createByTable[$match.Groups[1].Value] = $match.Value
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
    $selected[$table] = $true
}

$changed = $true
while ($changed) {
    $changed = $false
    foreach ($ref in $references) {
        if ($selected.ContainsKey($ref.BaseTable) -and -not $selected.ContainsKey($ref.RefTable)) {
            $selected[$ref.RefTable] = $true
            $changed = $true
        }
    }
}

$missingTables = $selected.Keys | Where-Object { -not $createByTable.ContainsKey($_) } | Sort-Object
if ($missingTables.Count -gt 0) {
    throw "Source SQL does not contain required table(s): $($missingTables -join ', ')"
}

$selectedSqlParts = New-Object System.Collections.Generic.List[string]
$selectedSet = @{}
foreach ($table in $selected.Keys) {
    $selectedSet[$table] = $true
}

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

$filteredPath = Join-Path ([System.IO.Path]::GetTempPath()) ("deposit-erd-source-" + [System.Guid]::NewGuid().ToString("N") + ".sql")
Set-Content -LiteralPath $filteredPath -Value ($selectedSqlParts -join "`n`n") -Encoding UTF8

Write-Host "Deposit ERD root scope:"
$depositRootTables | ForEach-Object { Write-Host " - $_" }
Write-Host "Resolved execution tables:"
$selected.Keys | Sort-Object | ForEach-Object { Write-Host " - $_" }

$runner = Join-Path $PSScriptRoot "run-erd-sql.ps1"

$args = @{
    SourceSql = $filteredPath
    HostName = $HostName
    Port = $Port
    Database = $Database
    User = $User
    PsqlPath = $PsqlPath
}

if ($KeepConvertedFile) {
    $args.KeepConvertedFile = $true
}

try {
    & $runner @args
}
finally {
    if (-not $KeepConvertedFile -and (Test-Path -LiteralPath $filteredPath)) {
        Remove-Item -LiteralPath $filteredPath -Force
    }
}
