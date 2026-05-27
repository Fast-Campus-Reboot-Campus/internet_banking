param(
    [string] $Database = "deposit_db",
    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $AdminUser = "postgres",
    [string] $AppUser = "deposit",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

$sql = @"
GRANT USAGE ON SCHEMA public TO "$AppUser";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO "$AppUser";
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO "$AppUser";
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "$AppUser";
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO "$AppUser";
"@

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    $output = $sql | & $PsqlPath `
        -h $HostName `
        -p $Port `
        -U $AdminUser `
        -d $Database `
        -v ON_ERROR_STOP=1
    $exitCode = $LASTEXITCODE
}
finally {
    $ErrorActionPreference = $previousErrorActionPreference
}

$output | ForEach-Object { Write-Host $_ }

if ($exitCode -ne 0) {
    throw "permission grant failed with exit code $exitCode"
}

Write-Host "DEPOSIT_ERD_PERMISSIONS_OK"
