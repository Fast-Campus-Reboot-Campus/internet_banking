param(
    [string] $BaseUrl      = "http://localhost:8090",
    [int]    $RetryCount   = 15,
    [int]    $RetryInterval = 3
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
    param([string]$Method = "GET", [string]$Path, [hashtable]$Body = $null)
    $uri  = "$BaseUrl$Path"
    $args = @{ Uri = $uri; Method = $Method; UseBasicParsing = $true; TimeoutSec = 10 }
    if ($Body) {
        $args.Body        = ($Body | ConvertTo-Json)
        $args.ContentType = "application/json"
    }
    $r = Invoke-WebRequest @args
    if ($r.StatusCode -notin 200,201) { throw "HTTP $($r.StatusCode) $Method $Path" }
    return $r.Content | ConvertFrom-Json
}

# ── STEP 1: health ──────────────────────────────────────────────────────────
Write-Host "STEP 1/9: health check ..."
$ready = $false
for ($i = 1; $i -le $RetryCount; $i++) {
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/health" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($r.StatusCode -eq 200) { $ready = $true; break }
    } catch {
        Write-Host "  attempt $i/$RetryCount failed, retrying in $RetryInterval s..."
        Start-Sleep -Seconds $RetryInterval
    }
}
if (-not $ready) { throw "consultation-service not ready" }
Write-Host "  -> OK"

# ── STEP 2: categories & features ──────────────────────────────────────────
Write-Host "STEP 2/9: categories / features ..."
$cats     = Invoke-Api -Path "/chatbot/categories"
$features = Invoke-Api -Path "/chatbot/features"
if ($cats.Count -ne 3)   { throw "expected 3 categories, got $($cats.Count)" }
if ($features.Count -ne 16) { throw "expected 16 features, got $($features.Count)" }
Write-Host "  -> OK ($($cats.Count) categories, $($features.Count) features)"

# ── STEP 3: feature execute ─────────────────────────────────────────────────
Write-Host "STEP 3/9: feature execute (PRODUCT_GUIDE, MY_ACCOUNTS, STAFF_TRANSFER_FLOW) ..."
$r = Invoke-Api -Method POST -Path "/chatbot/features/PRODUCT_GUIDE/execute" -Body @{}
if ($r.status -ne "OK") { throw "PRODUCT_GUIDE: expected OK, got $($r.status)" }

$r = Invoke-Api -Method POST -Path "/chatbot/features/MY_ACCOUNTS/execute" -Body @{ customer_no = "CUST001" }
if ($r.status -ne "OK") { throw "MY_ACCOUNTS: expected OK, got $($r.status)" }

$r = Invoke-Api -Method POST -Path "/chatbot/features/STAFF_TRANSFER_FLOW/execute" -Body @{ customer_no = "CUST001"; staff_id = "EMP001" }
if ($r.status -ne "OK") { throw "STAFF_TRANSFER_FLOW: expected OK, got $($r.status)" }
Write-Host "  -> OK"

# ── STEP 4: seed scenario ───────────────────────────────────────────────────
Write-Host "STEP 4/9: seed default scenario ..."
$seed = Invoke-Api -Method POST -Path "/chatbot/scenarios/default"
if (-not $seed.scenario_id) { throw "scenario seed failed" }
Write-Host "  -> OK (scenario_id=$($seed.scenario_id))"

# ── STEP 5: chatbot start ───────────────────────────────────────────────────
Write-Host "STEP 5/9: chatbot consultation start ..."
$started = Invoke-Api -Method POST -Path "/chatbot/consultations/start" -Body @{
    customer_no  = "CUST001"
    entry_screen = "HOME"
    app_version  = "1.0.0"
}
if (-not $started.chatbot_consultation_id) { throw "missing chatbot_consultation_id" }
$chatbotId = $started.chatbot_consultation_id
Write-Host "  -> OK (chatbot_consultation_id=$chatbotId)"

# ── STEP 6: chatbot message → agent transfer ────────────────────────────────
Write-Host "STEP 6/9: chatbot message -> AGENT transfer ..."
$msg = Invoke-Api -Method POST -Path "/chatbot/consultations/$chatbotId/messages" -Body @{
    message      = "상담사 연결해주세요"
    button_value = "AGENT"
}
if (-not $msg.agent_transfer_required) { throw "agent_transfer_required should be true" }
Write-Host "  -> OK (agent_transfer_required=True)"

# ── STEP 7: agent queue ─────────────────────────────────────────────────────
Write-Host "STEP 7/9: agent waiting queue ..."
$queue = Invoke-Api -Path "/chat/queue"
if ($queue.Count -lt 1) { throw "queue should have at least 1 item" }
$chatId = $queue[0].chat_consultation_id
Write-Host "  -> OK (chat_consultation_id=$chatId, queue=$($queue.Count)건)"

# ── STEP 8: connect agent ───────────────────────────────────────────────────
Write-Host "STEP 8/9: connect agent (employee_id=999) ..."
$connected = Invoke-Api -Method POST -Path "/chat/consultations/$chatId/connect" -Body @{ employee_id = 999 }
if ($connected.status -ne "CONNECTED") { throw "expected CONNECTED, got $($connected.status)" }
Write-Host "  -> OK (status=CONNECTED, employee_id=$($connected.employee_id))"

# ── STEP 9: messages + end ──────────────────────────────────────────────────
Write-Host "STEP 9/9: send messages + end consultation ..."
Invoke-Api -Method POST -Path "/chat/consultations/$chatId/messages" -Body @{
    message     = "무엇을 도와드릴까요?"
    sender_type = "AGENT"
} | Out-Null
Invoke-Api -Method POST -Path "/chat/consultations/$chatId/messages" -Body @{
    message     = "예금 만기 확인 부탁드립니다."
    sender_type = "USER"
} | Out-Null

$msgs = Invoke-Api -Path "/chat/consultations/$chatId/messages"
if ($msgs.Count -lt 2) { throw "expected >=2 messages, got $($msgs.Count)" }

$ended = Invoke-Api -Method POST -Path "/chat/consultations/$chatId/end" -Body @{ satisfaction_score = 5 }
if ($ended.status -ne "ENDED")  { throw "expected ENDED, got $($ended.status)" }
if ($ended.satisfaction_score -ne 5) { throw "satisfaction_score mismatch" }
Write-Host "  -> OK (status=ENDED, messages=$($msgs.Count)건, score=$($ended.satisfaction_score))"

Write-Host ""
Write-Host "============================================"
Write-Host "  CONSULTATION_SERVICE_OK  (9/9 steps)"
Write-Host "============================================"
