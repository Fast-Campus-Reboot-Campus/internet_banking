param(
    [Parameter(Mandatory = $true)]
    [string] $DrawioPath,

    [string] $HostName = "localhost",
    [int] $Port = 5432,
    [string] $Database = "deposit_db",
    [string] $User = "postgres",
    [string] $PsqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    [string] $PythonPath = "python",
    [switch] $KeepGeneratedSql
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $DrawioPath)) {
    throw "drawio file not found: $DrawioPath"
}
if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}

$drawioFullPath = (Resolve-Path -LiteralPath $DrawioPath).Path
$pythonScriptPath = Join-Path ([System.IO.Path]::GetTempPath()) ("drawio-erd-" + [System.Guid]::NewGuid().ToString("N") + ".py")
$generatedSqlPath = Join-Path ([System.IO.Path]::GetTempPath()) ("drawio-erd-" + [System.Guid]::NewGuid().ToString("N") + ".sql")

$pythonScript = @'
import html
import hashlib
import re
import sys
import xml.etree.ElementTree as ET

drawio_path = sys.argv[1]
output_path = sys.argv[2]

tree = ET.parse(drawio_path)
root = tree.getroot()

cells = {}
children = {}
for cell in root.findall(".//mxCell"):
    cid = cell.attrib.get("id")
    if not cid:
        continue
    cells[cid] = cell
    children.setdefault(cell.attrib.get("parent"), []).append(cell)

def clean(value):
    value = html.unescape(value or "")
    value = re.sub(r"<br\s*/?>", " ", value, flags=re.I)
    value = re.sub(r"<[^>]+>", " ", value)
    return re.sub(r"\s+", " ", value).strip()

def sql_ident(name):
    return '"' + name.replace('"', '""') + '"'

def constraint_name(prefix, *parts):
    raw = "_".join([prefix, *parts])
    normalized = re.sub(r"[^A-Za-z0-9_]+", "_", raw).lower()
    digest = hashlib.sha1(normalized.encode("utf-8")).hexdigest()[:10]
    head = normalized[:48].rstrip("_")
    return f"{head}_{digest}"

TABLE_ALIASES = {
    "banking_products": "deposit_banking_products",
    "deposit_products": "banking_deposit_products",
}

def canonical_table_name(name):
    return TABLE_ALIASES.get(name, name)

def canonical_column_name(table_name, column_name, marker, seen_columns):
    if table_name == "deposit_banking_products":
        if column_name == "bangking_product_type":
            return "deposit_product_type"
        if column_name == "bangking_product_name":
            return "deposit_product_name"

    if marker == "FK" and table_name in (
        "banking_deposit_products",
        "deposit_savings_products",
        "deposit_subscription_products",
    ) and column_name in ("deposit_product_id", "savings_product_id", "subscription_product_id"):
        return "banking_product_id"

    if column_name in seen_columns:
        return None

    return column_name

def normalize_type(raw):
    raw = raw.strip().upper()
    raw = raw.replace("TEXT(8)", "CHAR(8)")
    raw = raw.replace("TINYINT", "SMALLINT")
    return raw

def default_for(col_name, col_type):
    if col_name in ("created_at", "updated_at"):
        return " DEFAULT NOW()"
    if col_type == "BOOLEAN":
        return " DEFAULT FALSE"
    return ""

def parse_table_title(value):
    text = clean(value)
    if not text:
        return None
    return text.split()[0]

tables = []
table_by_id = {}

for cell in root.findall(".//mxCell"):
    style = cell.attrib.get("style", "")
    if "shape=table" not in style:
        continue

    table_name = parse_table_title(cell.attrib.get("value", ""))
    if not table_name:
        continue
    table_name = canonical_table_name(table_name)

    table = {
        "id": cell.attrib["id"],
        "name": table_name,
        "columns": [],
        "pk": [],
    }

    seen_columns = set()
    for row in children.get(cell.attrib["id"], []):
        row_cells = children.get(row.attrib.get("id"), [])
        marker = ""
        detail = ""
        for row_cell in row_cells:
            value = clean(row_cell.attrib.get("value", ""))
            if not value:
                continue
            if value in ("PK", "FK", "PK/FK"):
                marker = value
            elif "/" in value:
                detail = value

        if not detail:
            continue

        parts = [part.strip() for part in detail.split("/")]
        if len(parts) < 3:
            continue

        column_name = canonical_column_name(table_name, parts[1], marker, seen_columns)
        if column_name is None:
            continue
        column_type = normalize_type(parts[2])
        if not re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", column_name):
            continue
        seen_columns.add(column_name)

        nullable = "NOT NULL" if "PK" in marker else "NULL"
        column = {
            "name": column_name,
            "type": column_type,
            "marker": marker,
            "nullable": nullable,
        }
        table["columns"].append(column)
        if "PK" in marker:
            table["pk"].append(column_name)

    if table["columns"]:
        tables.append(table)
        table_by_id[table["id"]] = table

table_by_name = {table["name"]: table for table in tables}

edge_pairs = []
for cell in root.findall(".//mxCell"):
    if cell.attrib.get("edge") != "1":
        continue
    source = cell.attrib.get("source")
    target = cell.attrib.get("target")
    if source in table_by_id and target in table_by_id:
        edge_pairs.append((table_by_id[source], table_by_id[target]))

fk_constraints = []
seen_fk = set()
for source, target in edge_pairs:
    if not target["pk"]:
        continue
    target_pk = target["pk"][0]
    source_columns = {column["name"] for column in source["columns"]}
    if target_pk not in source_columns:
        continue

    key = (source["name"], target["name"], target_pk)
    if key in seen_fk:
        continue
    seen_fk.add(key)
    fk_constraints.append((source["name"], target["name"], target_pk))

lines = [
    "-- Generated from drawio ERD without modifying the source file.",
    "BEGIN;",
]

for table in tables:
    lines.append("")
    lines.append(f"CREATE TABLE IF NOT EXISTS {sql_ident(table['name'])} (")
    column_lines = []
    for column in table["columns"]:
        suffix = default_for(column["name"], column["type"])
        column_lines.append(
            f"    {sql_ident(column['name'])} {column['type']} {column['nullable']}{suffix}"
        )

    if table["pk"]:
        pk_cols = ", ".join(sql_ident(col) for col in table["pk"])
        column_lines.append(f"    CONSTRAINT {sql_ident('pk_' + table['name'])} PRIMARY KEY ({pk_cols})")

    lines.append(",\n".join(column_lines))
    lines.append(");")

for source_name, target_name, column_name in fk_constraints:
    fk_name = constraint_name("fk", source_name, target_name, column_name)
    lines.append("")
    lines.append(f"""DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'public'
          AND t.relname = '{source_name}'
          AND c.conname = '{fk_name}'
    ) THEN
        ALTER TABLE {sql_ident(source_name)}
        ADD CONSTRAINT {sql_ident(fk_name)}
        FOREIGN KEY ({sql_ident(column_name)})
        REFERENCES {sql_ident(target_name)} ({sql_ident(column_name)});
    END IF;
END
$$;""")

lines.append("")
lines.append("COMMIT;")

with open(output_path, "w", encoding="utf-8", newline="\n") as fp:
    fp.write("\n".join(lines))

print("Generated tables:")
for table in tables:
    print(" - " + table["name"])
'@

try {
    Set-Content -LiteralPath $pythonScriptPath -Value $pythonScript -Encoding UTF8
    & $PythonPath $pythonScriptPath $drawioFullPath $generatedSqlPath
    if ($LASTEXITCODE -ne 0) {
        throw "drawio conversion failed"
    }

    Write-Host "Drawio source: $drawioFullPath"
    Write-Host "Generated SQL: $generatedSqlPath"

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $psqlOutput = & $PsqlPath `
            -h $HostName `
            -p $Port `
            -U $User `
            -d $Database `
            -v ON_ERROR_STOP=1 `
            -f $generatedSqlPath 2>&1
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
    if (Test-Path -LiteralPath $pythonScriptPath) {
        Remove-Item -LiteralPath $pythonScriptPath -Force
    }
    if (-not $KeepGeneratedSql -and (Test-Path -LiteralPath $generatedSqlPath)) {
        Remove-Item -LiteralPath $generatedSqlPath -Force
    }
}
