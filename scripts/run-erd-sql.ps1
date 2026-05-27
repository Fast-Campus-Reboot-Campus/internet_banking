param(
    [Parameter(Mandatory = $true)]
    [string] $SourceSql,

    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $Database = "postgres",
    [string] $User = "postgres",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    [switch] $KeepConvertedFile
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $SourceSql)) {
    throw "Source SQL file not found: $SourceSql"
}

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

$sourcePath = (Resolve-Path -LiteralPath $SourceSql).Path
$convertedPath = Join-Path ([System.IO.Path]::GetTempPath()) ("erd-postgres-" + [System.Guid]::NewGuid().ToString("N") + ".sql")

try {
    $sql = Get-Content -LiteralPath $sourcePath -Raw -Encoding UTF8

    $converted = $sql
    $converted = $converted -replace "`r`n", "`n"
    $converted = $converted -replace "`r", "`n"
    $converted = $converted -replace "COMMENT\s+'([^']|'')*'", ""
    $converted = $converted -replace '`', '"'
    $converted = $converted -replace "\bTINYINT\b", "SMALLINT"

    Set-Content -LiteralPath $convertedPath -Value $converted -Encoding UTF8

    Write-Host "Source SQL:    $sourcePath"
    Write-Host "Converted SQL: $convertedPath"

    & $PsqlPath `
        -h $HostName `
        -p $Port `
        -U $User `
        -d $Database `
        -v ON_ERROR_STOP=1 `
        -f $convertedPath

    if ($LASTEXITCODE -ne 0) {
        throw "psql failed with exit code $LASTEXITCODE"
    }
}
finally {
    if (-not $KeepConvertedFile -and (Test-Path -LiteralPath $convertedPath)) {
        Remove-Item -LiteralPath $convertedPath -Force
    }
}
