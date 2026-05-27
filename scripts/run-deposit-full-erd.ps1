param(
    [string] $DrawioPath = "C:\Users\green\Desktop\teamproject\teamproject.drawio",

    [string[]] $SourceSql = @(
        "services\deposit-service\src\main\resources\db\migration\V1__initial_schema.sql",
        "services\deposit-service\src\main\resources\db\migration\V5__full_erd_schema.sql"
    ),

    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $Database = "deposit_db",
    [string] $User = "postgres",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe"
)

$ErrorActionPreference = "Stop"

Write-Host "STEP 1/2: Run SQL-based deposit ERD features"
& (Join-Path $PSScriptRoot "run-deposit-expanded-erd-sql.ps1") `
    -SourceSql $SourceSql `
    -HostName $HostName `
    -Port $Port `
    -Database $Database `
    -User $User `
    -PsqlPath $PsqlPath

if ($LASTEXITCODE -ne 0) {
    throw "SQL-based deposit ERD execution failed"
}

Write-Host "STEP 2/2: Run drawio-based deposit ERD features"
& (Join-Path $PSScriptRoot "run-deposit-drawio-erd.ps1") `
    -DrawioPath $DrawioPath `
    -HostName $HostName `
    -Port $Port `
    -Database $Database `
    -User $User `
    -PsqlPath $PsqlPath

if ($LASTEXITCODE -ne 0) {
    throw "drawio-based deposit ERD execution failed"
}

Write-Host "DEPOSIT_FULL_ERD_OK"
