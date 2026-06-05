# -*- coding: utf-8 -*-
"""
Internet Banking MVP - 아키텍처 다이어그램 생성기.
/tmp/logos 의 공식 브랜드 로고(SVG)를 base64 로 임베드해 자체 포함 SVG 를 만든다.
출력: docs/architecture.svg
"""
import base64, os, html

LOGO_DIR = "D:/internet_banking/build/logos"
OUT = "D:/internet_banking/docs/architecture.svg"

def L(name):
    """로고 파일을 data-URI 로."""
    for fn in (f"{name}.svg", f"si-{name}.svg"):
        p = os.path.join(LOGO_DIR, fn)
        if os.path.exists(p):
            with open(p, "rb") as f:
                b = base64.b64encode(f.read()).decode()
            return f"data:image/svg+xml;base64,{b}"
    raise FileNotFoundError(name)

S = []   # svg fragments
def add(x): S.append(x)
def esc(t): return html.escape(str(t), quote=True)

def img(name, x, y, w, h=None):
    h = h or w
    add(f'<image href="{L(name)}" x="{x}" y="{y}" width="{w}" height="{h}"/>')

def box(x, y, w, h, title, sub="", logo=None, cls="svc", port=None):
    add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" class="{cls}"/>')
    tx = x + w/2
    if logo:
        img(logo, x+9, y+9, 22)
    add(f'<text x="{tx}" y="{y+(h/2)- (4 if sub else -4)}" class="bxt">{esc(title)}</text>')
    if sub:
        add(f'<text x="{tx}" y="{y+h-12}" class="bxs">{esc(sub)}</text>')
    if port:
        add(f'<rect x="{x+w-46}" y="{y+8}" width="38" height="16" rx="8" class="port"/>')
        add(f'<text x="{x+w-27}" y="{y+20}" class="portt">{esc(port)}</text>')

def label(x, y, t, cls="albl"):
    add(f'<text x="{x}" y="{y}" class="{cls}">{esc(t)}</text>')

def line(x1, y1, x2, y2, marker, cls="ln", dash=None):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    add(f'<path d="M{x1},{y1} L{x2},{y2}" class="{cls}"{d} marker-end="url(#{marker})"/>')

def poly(pts, marker, cls="ln", dash=None):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    p = "M" + " L".join(f"{x},{y}" for x, y in pts)
    add(f'<path d="{p}" fill="none" class="{cls}"{d} marker-end="url(#{marker})"/>')

W, H = 1900, 1560

# ---- docker boundary ----
DX, DY, DW, DH = 40, 150, 1500, 1140   # docker spans x:40..1540

# ---- column geometry (5 columns, fit inside docker) ----
BW = 178
_step = 320
cx = [60 + i*_step for i in range(5)]   # 60,380,700,1020,1340  (right edge 1518)
cc = [x + BW/2 for x in cx]             # centers
CENTER = DX + DW/2                       # 790

# ============================ header / defs ============================
add(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
    f'viewBox="0 0 {W} {H}" font-family="\'Malgun Gothic\',\'맑은 고딕\',\'Segoe UI\',sans-serif">')
add('''<defs>
  <marker id="aR" markerWidth="11" markerHeight="11" refX="8" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#1f6feb"/></marker>
  <marker id="aE" markerWidth="11" markerHeight="11" refX="8" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#e8830c"/></marker>
  <marker id="aX" markerWidth="11" markerHeight="11" refX="8" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#7c4dff"/></marker>
  <marker id="aG" markerWidth="11" markerHeight="11" refX="8" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#64748b"/></marker>
  <marker id="aB" markerWidth="11" markerHeight="11" refX="8" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#334155"/></marker>
  <style>
    .bg     { fill:#f7fafc; }
    .ttl    { font-size:24px; font-weight:800; fill:#0f2333; text-anchor:middle; }
    .sub    { font-size:13px; fill:#5b6b7a; text-anchor:middle; }
    .zone   { fill:none; stroke-width:1.6; rx:16; }
    .zlbl   { font-size:14px; font-weight:700; text-anchor:start; }
    .svc    { fill:#ffffff; stroke:#2f6f4f; stroke-width:1.7; }
    .ai     { fill:#ffffff; stroke:#7c4dff; stroke-width:1.7; }
    .py     { fill:#ffffff; stroke:#3776ab; stroke-width:1.7; }
    .infra  { fill:#ffffff; stroke:#64748b; stroke-width:1.6; }
    .ext    { fill:#faf7ff; stroke:#7c4dff; stroke-width:1.6; }
    .gate   { fill:#eafaf0; stroke:#43a047; stroke-width:2; }
    .client { fill:#eef4ff; stroke:#1f6feb; stroke-width:1.8; }
    .bus    { fill:#fff4e2; stroke:#e8830c; stroke-width:2; }
    .bxt    { font-size:14px; font-weight:700; fill:#16242f; text-anchor:middle; }
    .bxs    { font-size:10.5px; fill:#5b6b7a; text-anchor:middle; }
    .port   { fill:#eef2f6; stroke:#b6c2cd; stroke-width:1; }
    .portt  { font-size:10px; font-weight:700; fill:#43536a; text-anchor:middle; }
    .ln     { stroke:#334155; stroke-width:1.8; }
    .lnR    { stroke:#1f6feb; stroke-width:1.8; }
    .lnE    { stroke:#e8830c; stroke-width:1.8; }
    .lnX    { stroke:#7c4dff; stroke-width:1.8; }
    .lnG    { stroke:#64748b; stroke-width:1.5; }
    .albl   { font-size:11px; fill:#1f6feb; text-anchor:middle; font-weight:600; }
    .elbl   { font-size:11px; fill:#c2700a; text-anchor:middle; font-weight:600; }
    .xlbl   { font-size:11px; fill:#7c4dff; text-anchor:middle; font-weight:600; }
    .note   { font-size:11px; fill:#5b6b7a; text-anchor:start; }
    .ntt    { font-size:12.5px; font-weight:700; fill:#16242f; text-anchor:start; }
  </style>''')
add('</defs>')
add(f'<rect class="bg" x="0" y="0" width="{W}" height="{H}"/>')

# ============================ title ============================
label(CENTER, 42, "Internet Banking MVP — System Architecture", "ttl")
label(CENTER, 64, "MSA · Java 17 / Spring Boot 3 · Python FastAPI · PostgreSQL 16 · Kafka · Redis · AI/RAG", "sub")

# ============================ Docker boundary ============================
add(f'<rect x="{DX}" y="{DY}" width="{DW}" height="{DH}" rx="20" fill="#eef7fd" stroke="#2496ed" stroke-width="2"/>')
img("docker", DX+16, DY+14, 30)
add(f'<text x="{DX+54}" y="{DY+34}" class="zlbl" fill="#1b7fc4">Docker Compose 런타임</text>')

# ============================ Client band ============================
# Users
ux = CENTER
add(f'<g transform="translate({ux},96)">'
    '<circle cx="-26" cy="2" r="9" fill="#9aa7b3"/><rect x="-38" y="12" width="24" height="18" rx="8" fill="#9aa7b3"/>'
    '<circle cx="26" cy="2" r="9" fill="#9aa7b3"/><rect x="14" y="12" width="24" height="18" rx="8" fill="#9aa7b3"/>'
    '<circle cx="0" cy="-4" r="12" fill="#5b6b7a"/><rect x="-15" y="9" width="30" height="24" rx="10" fill="#5b6b7a"/>'
    '</g>')
label(ux, 150, "Users", "bxs")
# Web (Next.js) — outside docker (browser/client)
box(CENTER-110, 168, 220, 50, "web (Frontend)", "Next.js 15 · TypeScript", logo="nextjs", cls="client", port="3001")
line(CENTER, 132, CENTER, 168, "aB")

# ============================ Gateway ============================
GY = 250
box(CENTER-150, GY, 300, 58, "gateway-service", "Spring Cloud Gateway · 라우팅 · JWT · Rate-limit", logo="spring", cls="gate", port="8080")
line(CENTER, 218, CENTER, GY, "aB")

# ============================ Backing Core zone ============================
CZY = 350
add(f'<rect x="{DX+20}" y="{CZY}" width="{DW-40}" height="138" class="zone" stroke="#2f6f4f"/>')
label(DX+36, CZY+22, "Backing Core  ·  Java 17 / Spring Boot 3", "zlbl"); add(f'<text x="{DX+36}" y="{CZY+22}" class="zlbl" fill="#2f6f4f"></text>')
CBY = CZY+40
core = [
    ("customer-service", "고객·인증", "8081", "spring"),
    ("deposit-service",  "수신·계좌·이체·추천", "8082", "spring"),
    ("loan-service",     "여신 생애주기 + RAG", "8083", "spring"),
    ("payment-service",  "결제·이체 이벤트", "8084", "spring"),
    ("master-service",   "공통코드·마스터", "8085", "spring"),
]
for i,(n,s,p,lg) in enumerate(core):
    box(cx[i], CBY, BW, 82, n, s, logo=lg, cls="svc", port=p)

# gateway fan-out (REST)
for i in range(5):
    line(CENTER, GY+58, cc[i], CBY, "aR", cls="lnR")

# ============================ AI / Review zone ============================
AZY = 520
add(f'<rect x="{DX+20}" y="{AZY}" width="{DW-40}" height="150" class="zone" stroke="#7c4dff"/>')
label(DX+36, AZY+22, "AI / Review Services  ·  RAG · 자동심사 · 편향검증 · 챗봇", "zlbl"); add(f'<text x="{DX+36}" y="{AZY+22}" class="zlbl" fill="#7c4dff"></text>')
ABY = AZY+42
ai = [
    ("ai-service",        "임베딩·pgvector 벡터검색", "8086", "spring", "ai"),
    ("auto-loan-review",  "자동심사·편향·4-eye", "8086", "spring", "ai"),
    ("review-ai-gateway", "심사 AI 라우팅·LLM", "8088", "spring", "ai"),
    ("advisory-service",  "RAG 자문 클라이언트", "—", "spring", "ai"),
    ("consultation-svc",  "챗봇·상담 (FastAPI)", "8090", "fastapi", "py"),
]
for i,(n,s,p,lg,cls) in enumerate(ai):
    box(cx[i], ABY, BW, 84, n, s, logo=lg, cls=cls, port=p)

# ---- inter-service REST (blue) ----
# payment -> deposit (Feign): route just below core boxes
poly([(cc[3], CBY+82),(cc[3], CBY+104),(cc[1], CBY+104),(cc[1], CBY+82)], "aR", cls="lnR")
label((cc[1]+cc[3])/2, CBY+118, "Feign: 잔액·계좌 조회", "albl")
# loan -> auto-loan-review (평가요청)
line(cc[2]-30, CBY+82, cc[1]+40, ABY, "aR", cls="lnR")
label((cc[2]+cc[1])/2-10, CBY+150, "심사평가 REST", "albl")
# review-ai-gateway -> loan (심사조회)  vertical col3
line(cc[2]+34, ABY, cc[2]+34, CBY+82, "aR", cls="lnR")
label(cc[2]+96, (ABY+CBY+82)/2, "심사 조회", "albl")
# advisory -> ai-service (RAG)  route below AI boxes
poly([(cc[3], ABY+84),(cc[3], ABY+104),(cc[0], ABY+104),(cc[0], ABY+84)], "aR", cls="lnR")
label((cc[0]+cc[3])/2, ABY+118, "RAG 유사사례 조회", "albl")
# review-ai-gateway -> advisory (short)
line(cc[2]+BW/2, ABY+42, cc[3]-BW/2, ABY+42, "aR", cls="lnR")

# ============================ Kafka event bus ============================
KY = 700
add(f'<rect x="{DX+20}" y="{KY}" width="{DW-40}" height="52" rx="12" class="bus"/>')
img("kafka", DX+34, KY+12, 28)
add(f'<text x="{DX+74}" y="{KY+24}" class="ntt">Apache Kafka 3.8 · 이벤트 버스</text>')
add(f'<text x="{DX+74}" y="{KY+42}" class="note">Confluent Schema Registry 7.6  ·  payment 3-클러스터(KFTC/BOK/internal)</text>')

# producers/consumers (orange dashed, with topic labels)
# payment -> kafka : payment.completed
poly([(cc[3], CBY+82),(cc[3]+70, CBY+82),(cc[3]+70, KY)], "aE", cls="lnE", dash="6 4")
label(cc[3]+150, KY-12, "payment.completed", "elbl")
# kafka -> loan : payment.completed (virtual account deposit)
poly([(cc[2]+90, KY),(cc[2]+90, CBY+150),(cc[2]+90, CBY+82)], "aE", cls="lnE", dash="6 4")
# loan -> kafka : loan-domain-events / bias-check-requested
poly([(cc[2]-90, CBY+82),(cc[2]-90, KY-30),(cc[2]-90, KY)], "aE", cls="lnE", dash="6 4")
label(cc[2]-90, KY-36, "loan-domain-events / loan.bias-check-requested", "elbl")
# kafka -> auto-loan-review
poly([(cc[1], KY),(cc[1], ABY+84)], "aE", cls="lnE", dash="6 4")
# kafka -> review-ai-gateway
poly([(cc[2], KY),(cc[2], ABY+84)], "aE", cls="lnE", dash="6 4")
# auto-loan-review -> kafka : case-indexed (RAG enrich)
poly([(cc[1]-60, ABY+84),(cc[1]-60, KY)], "aE", cls="lnE", dash="6 4")
label(cc[1]-60, ABY+102, "loan-review.case-indexed.v1", "elbl")

# ============================ Data layer ============================
DLY = 800
add(f'<rect x="{DX+20}" y="{DLY}" width="{DW-40}" height="120" class="zone" stroke="#64748b"/>')
label(DX+36, DLY+22, "Data Layer  ·  서비스별 독립 PostgreSQL 16  +  Redis 7  (pgvector: loan / ai)", "zlbl"); add(f'<text x="{DX+36}" y="{DLY+22}" class="zlbl" fill="#475569"></text>')
DBY = DLY+38
dbs = [
    ("customer-db","5432","postgresql",False),
    ("deposit-db","5433","postgresql",False),
    ("loan-db","5434 · pgvector","postgresql",False),
    ("payment-db","5435","postgresql",False),
    ("master-db","5436","postgresql",False),
    ("ai-db","5437 · pgvector","postgresql",False),
    ("common-db","5438","postgresql",False),
    ("Redis","6379","redis",True),
]
n = len(dbs); dbw=160; gap=(DW-40-40-dbw*n)/(n-1); sx=DX+40
for i,(n2,p2,lg,red) in enumerate(dbs):
    x = sx + i*(dbw+gap)
    box(x, DBY, dbw, 60, n2, p2, logo=lg, cls="infra")
# JPA arrow from core/ai region into data
line(CENTER, KY+52, CENTER, DLY, "aG", cls="lnG", dash="2 4")
label(CENTER+150, (KY+52+DLY)/2, "JPA / R2DBC · 캐시", "albl")

# ============================ bottom infra row: Search/RAG · Observability · doc-agent ============================
IZY = 950
colw = (DW-40-2*24)/3
ix0 = DX+20
# --- Search / RAG ---
add(f'<rect x="{ix0}" y="{IZY}" width="{colw}" height="180" class="zone" stroke="#f9b110"/>')
label(ix0+16, IZY+22, "Search / RAG  (profile: rag)", "zlbl"); add(f'<text x="{ix0+16}" y="{IZY+22}" class="zlbl" fill="#c98f00"></text>')
box(ix0+24, IZY+40, 150, 56, "Elasticsearch", "8.15 · 9200", logo="elasticsearch", cls="infra")
box(ix0+24, IZY+106, 150, 56, "Kibana", "5601", logo="kibana", cls="infra")
box(ix0+200, IZY+72, 180, 58, "Kafka Connect", "ES Sink Connector", logo="kafka", cls="infra")
line(ix0+200, IZY+100, ix0+174, IZY+68, "aE", cls="lnE", dash="6 4")
# --- Observability ---
ox = ix0+colw+24
add(f'<rect x="{ox}" y="{IZY}" width="{colw}" height="180" class="zone" stroke="#e6522c"/>')
label(ox+16, IZY+22, "Observability", "zlbl"); add(f'<text x="{ox+16}" y="{IZY+22}" class="zlbl" fill="#e6522c"></text>')
obs = [
    ("Prometheus","9090","prometheus"),
    ("Grafana","3000","grafana"),
    ("Loki","3100","grafana"),
    ("Langfuse","3001","grafana"),
]
ow=150; og=(colw-32-ow*2)/1
positions=[(ox+16,IZY+40),(ox+16+ow+20,IZY+40),(ox+16,IZY+106),(ox+16+ow+20,IZY+106)]
for (nm,pt,lg),(px,py) in zip(obs,positions):
    box(px,py,ow,56,nm,pt,logo=lg,cls="infra")
label(ox+16, IZY+176, "+ Promtail · Alertmanager(9095) · Blackbox(9115) · Phoenix(6006)", "note")
# --- doc-agent profile ---
gx = ix0+2*(colw+24)
add(f'<rect x="{gx}" y="{IZY}" width="{colw}" height="180" class="zone" stroke="#0a7ea4"/>')
label(gx+16, IZY+22, "doc-agent  (profile: doc)", "zlbl"); add(f'<text x="{gx+16}" y="{IZY+22}" class="zlbl" fill="#0a7ea4"></text>')
box(gx+16, IZY+40, 170, 58, "doc-agent", "신분증 OCR·서류 · 8087", logo="spring", cls="infra")
box(gx+16, IZY+108, 110, 54, "MinIO", "9000", logo="minio", cls="infra")
box(gx+140, IZY+108, 110, 54, "Vault", "8200", cls="infra")
box(gx+264, IZY+72, 110, 54, "Ollama", "11434", cls="infra")
# loan -> doc-agent  (관계는 doc-agent 존 안에 주석으로 표기 — 긴 배선 대신)
add(f'<text x="{gx+16}" y="{IZY+176}" class="note">↶ loan-service 가 서류/OCR REST 호출</text>')

# ============================ External (purple) — outside docker, right lane ============================
EX = DX+DW+38                       # 1576
EW = 250
box(EX, 358, EW, 52, "Anthropic Claude", "review-ai-gateway LLM", logo="claude", cls="ext")
box(EX, 426, EW, 52, "OpenAI GPT-4o-mini", "consultation · advisory", logo="openai", cls="ext")
box(EX, 494, EW, 52, "KFTC / BOK 망", "payment 외부 결제망", cls="ext")
# review-ai-gateway -> Claude
line(cc[2]+BW/2, ABY+18, EX, 384, "aX", cls="lnX", dash="5 4")
# consultation -> OpenAI
line(cc[4]+BW/2, ABY+30, EX, 452, "aX", cls="lnX", dash="5 4")
# payment -> KFTC/BOK
line(cc[3]+BW/2, CBY+28, EX, 520, "aX", cls="lnX", dash="5 4")

# ============================ Legend ============================
LGY = IZY+150
lx = DX+20
add(f'<rect x="{lx}" y="{LGY+24}" width="{DW-40}" height="0" fill="none"/>')

# ============================ CI/CD (outside docker) ============================
CY = DY+DH+60
steps = [("intellij","IntelliJ IDEA"),("github","GitHub"),("github","GitHub Actions"),("gitlab","GitLab CI")]
n=len(steps); sw=210; gap=( DW - sw*n )/(n-1); bx=DX
label(CENTER, CY-18, "CI / CD 파이프라인", "ntt")
for i,(lg,nm) in enumerate(steps):
    x=bx+i*(sw+gap)
    cls="infra"
    add(f'<rect x="{x}" y="{CY}" width="{sw}" height="58" rx="10" class="{cls}"/>')
    img("github" if nm=="GitHub Actions" else lg, x+12, CY+14, 30)
    add(f'<text x="{x+sw/2+16}" y="{CY+35}" class="bxt">{esc(nm)}</text>')
    if i<n-1:
        line(x+sw, CY+29, x+sw+gap, CY+29, "aB")

# legend chips (top-right under title)
lgx=DX+20; lgy=DY+DH-30
items=[("#1f6feb","REST 호출"),("#e8830c","Kafka 이벤트"),("#7c4dff","외부 API"),("#64748b","DB/캐시")]
add(f'<text x="{lgx}" y="{lgy-8}" class="ntt">범례</text>')
xx=lgx
for col,txt in items:
    add(f'<line x1="{xx}" y1="{lgy+6}" x2="{xx+26}" y2="{lgy+6}" stroke="{col}" stroke-width="3"/>')
    add(f'<text x="{xx+32}" y="{lgy+10}" class="note">{esc(txt)}</text>')
    xx += 150

add('</svg>')

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    f.write("\n".join(S))
print("wrote", OUT, "bytes=", sum(len(x) for x in S))
