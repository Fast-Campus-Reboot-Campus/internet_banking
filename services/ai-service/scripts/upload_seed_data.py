"""
advisory-service RAG 정책문서 시드 업로더.

services/ai-service/seed-data/ 하위 문서를 읽어
advisory-service POST /api/internal/advisory/documents 로 적재한다.

지원 포맷:
    .md / .txt  → 직접 읽기
    .pdf        → pdfplumber 로 텍스트 추출  (pip install pdfplumber)
    .hwp        → hwp5txt CLI 로 텍스트 추출 (pip install hwp5)
    .png / .jpg → pytesseract OCR             (pip install Pillow pytesseract)
                  ※ Tesseract 엔진 별도 설치 필요 — https://github.com/tesseract-ocr/tesseract

폴더 → docCategoryCd 매핑:
    law/          → LAW
    supervision/  → SUPERVISION_GUIDE
    internal/     → INTERNAL_RULE
    product/      → PRODUCT_TERMS
    fair-lending/ → FAIR_LENDING

파일 옆에 동일 stem .meta.yml 이 있으면 메타데이터를 덮어쓴다.
없으면 파일명 stem → doc_cd, 오늘 날짜 → effectiveStartDate.

usage:
    pip install requests pyyaml pdfplumber hwp5 Pillow pytesseract
    python scripts/upload_seed_data.py [--host http://localhost:8083] [--dry-run] [--force]

options:
    --host    advisory-service 주소 (기본: http://localhost:8083)
    --dry-run 실제 API 호출 없이 업로드 목록만 출력
    --force   doc_cd 충돌 시 기존 문서 비활성화 후 재등록
"""

import argparse
import os
import subprocess
import sys
import time
from datetime import date
from pathlib import Path

# Windows cp949 터미널에서 한글/특수문자 인코딩 오류 방지
if sys.stdout.encoding and sys.stdout.encoding.lower() not in ("utf-8", "utf-8-sig"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if sys.stderr.encoding and sys.stderr.encoding.lower() not in ("utf-8", "utf-8-sig"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

import requests

# ── Tesseract 경로 자동 설정 (Windows 기본 설치 위치 폴백) ──────────────────
_TESSERACT_DEFAULT = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
_TESSDATA_USER = os.path.join(os.environ.get("APPDATA", ""), "tessdata")

if os.name == "nt":
    # TESSDATA_PREFIX: 사용자 tessdata 폴더 우선 (한국어 팩 포함)
    if not os.environ.get("TESSDATA_PREFIX") and os.path.isdir(_TESSDATA_USER):
        os.environ["TESSDATA_PREFIX"] = _TESSDATA_USER
    # tesseract_cmd: PATH 에 없으면 기본 설치 경로 사용
    try:
        import pytesseract as _pt
        if not Path(_pt.pytesseract.tesseract_cmd).exists() and Path(_TESSERACT_DEFAULT).exists():
            _pt.pytesseract.tesseract_cmd = _TESSERACT_DEFAULT
    except ImportError:
        pass

try:
    import yaml
    YAML_AVAILABLE = True
except ImportError:
    YAML_AVAILABLE = False

# ── 설정 ───────────────────────────────────────────────────────────────────

SEED_ROOT = Path(__file__).parent.parent / "seed-data"

# 폴더명 → docCategoryCd
FOLDER_TO_CATEGORY = {
    "law":           "LAW",
    "supervision":   "SUPERVISION_GUIDE",
    "internal":      "INTERNAL_RULE",
    "product":       "PRODUCT_TERMS",
    "fair-lending":  "FAIR_LENDING",
}

SUPPORTED_EXTENSIONS = {".md", ".txt", ".pdf", ".hwp", ".png", ".jpg", ".jpeg"}

REGISTER_URL  = "/api/internal/advisory/documents"
ACTIVATE_URL  = "/api/internal/advisory/documents/{doc_id}/activate"
HEADERS = {"Content-Type": "application/json", "X-Actor-Role": "ADMIN"}

# ── 텍스트 추출 ────────────────────────────────────────────────────────────

def extract_text(file_path: Path) -> str:
    """파일 확장자에 따라 텍스트를 추출한다."""
    ext = file_path.suffix.lower()

    if ext in {".md", ".txt"}:
        return file_path.read_text(encoding="utf-8")

    if ext == ".pdf":
        return _extract_pdf(file_path)

    if ext == ".hwp":
        return _extract_hwp(file_path)

    if ext in {".png", ".jpg", ".jpeg"}:
        return _extract_image_ocr(file_path)

    raise ValueError(f"지원하지 않는 확장자: {ext}")


def _extract_pdf(file_path: Path) -> str:
    """pdfplumber 로 PDF 전 페이지 텍스트 추출."""
    try:
        import pdfplumber
    except ImportError:
        raise RuntimeError(
            f"PDF 추출 실패 [{file_path.name}]: pdfplumber 미설치 — pip install pdfplumber"
        )
    with pdfplumber.open(file_path) as pdf:
        pages = []
        for page in pdf.pages:
            text = page.extract_text()
            if text:
                pages.append(text)
    result = "\n\n".join(pages).strip()
    if not result:
        # 텍스트 레이어 없음 → 스캔 이미지 PDF: pdf2image + OCR 시도
        return _extract_pdf_ocr(file_path)
    return result


# poppler 설치 경로 (winget 기본 위치)
_POPPLER_PATH = r"C:\Users\asitw\AppData\Local\Microsoft\WinGet\Packages\oschwartz10612.Poppler_Microsoft.Winget.Source_8wekyb3d8bbwe\poppler-25.07.0\Library\bin"


def _extract_pdf_ocr(file_path: Path) -> str:
    """스캔 이미지 PDF: pdf2image 로 페이지를 이미지 변환 후 pytesseract OCR."""
    try:
        from pdf2image import convert_from_path
    except ImportError:
        raise RuntimeError(
            f"스캔 PDF OCR 실패 [{file_path.name}]: pdf2image 미설치 — pip install pdf2image"
        )
    try:
        from PIL import Image
        import pytesseract
    except ImportError:
        raise RuntimeError(
            f"스캔 PDF OCR 실패 [{file_path.name}]: Pillow·pytesseract 미설치"
        )

    poppler_path = _POPPLER_PATH if os.path.isdir(_POPPLER_PATH) else None
    images = convert_from_path(str(file_path), dpi=150, poppler_path=poppler_path)
    texts = []
    for img in images:
        if img.width < 1000:
            scale = 1000 / img.width
            img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)
        text = pytesseract.image_to_string(img, lang="kor+eng").strip()
        if text:
            texts.append(text)
    result = "\n\n".join(texts).strip()
    if not result:
        raise RuntimeError(
            f"스캔 PDF OCR 결과 빔 [{file_path.name}] — Tesseract 한국어 팩(kor) 설치 여부 확인"
        )
    return result


def _extract_hwp(file_path: Path) -> str:
    """hwp5txt CLI 로 HWP 텍스트 추출.

    설치: pip install hwp5
    Windows 에서 hwp5txt 경로가 PATH 에 없으면 python -m hwp5.hwp5txt 로 시도.
    """
    # 방법 1: hwp5txt 커맨드
    try:
        result = subprocess.run(
            ["hwp5txt", str(file_path)],
            capture_output=True,
            encoding="utf-8",
            errors="replace",
            timeout=30,
        )
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()
    except FileNotFoundError:
        pass

    # 방법 2: python -m hwp5.hwp5txt (Windows PATH 문제 우회)
    try:
        result = subprocess.run(
            [sys.executable, "-m", "hwp5.hwp5txt", str(file_path)],
            capture_output=True,
            encoding="utf-8",
            errors="replace",
            timeout=30,
        )
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()
        if result.stderr:
            raise RuntimeError(f"hwp5 오류: {result.stderr[:200]}")
    except ImportError:
        pass

    raise RuntimeError(
        f"HWP 추출 실패 [{file_path.name}]: hwp5 미설치 — pip install hwp5"
    )


def _extract_image_ocr(file_path: Path) -> str:
    """pytesseract + Pillow 로 이미지 OCR (한국어+영어)."""
    try:
        from PIL import Image
        import pytesseract
    except ImportError:
        raise RuntimeError(
            f"이미지 OCR 실패 [{file_path.name}]: Pillow·pytesseract 미설치 — "
            "pip install Pillow pytesseract  (Tesseract 엔진도 별도 설치 필요)"
        )
    img = Image.open(file_path)
    # 해상도가 낮으면 업스케일해서 인식률 향상
    if img.width < 1000:
        scale = 1000 / img.width
        img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)

    text = pytesseract.image_to_string(img, lang="kor+eng")
    result = text.strip()
    if not result:
        raise RuntimeError(
            f"OCR 결과 빔 [{file_path.name}] — Tesseract 한국어 팩(kor) 설치 여부 확인"
        )
    return result


# ── 유틸 ───────────────────────────────────────────────────────────────────

def today_str() -> str:
    return date.today().strftime("%Y%m%d")


def load_meta(file_path: Path) -> dict:
    """동일 stem .meta.yml 이 있으면 읽어서 반환. 없으면 빈 dict."""
    if not YAML_AVAILABLE:
        return {}
    meta_path = file_path.with_suffix(".meta.yml")
    if not meta_path.exists():
        return {}
    with open(meta_path, encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


def build_payload(file_path: Path, folder_name: str) -> dict:
    """파일과 메타 정보로 DocumentRegisterRequest payload 구성."""
    meta = load_meta(file_path)
    doc_cd  = meta.get("doc_cd",  file_path.stem.upper())[:50]   # API 최대 50자
    content = extract_text(file_path)

    return {
        "docCd":              doc_cd,
        "docTitle":           meta.get("title",   doc_cd.replace("_", " ").title()),
        "docCategoryCd":      meta.get("category", FOLDER_TO_CATEGORY.get(folder_name, "POLICY")),
        "docVersion":         meta.get("version", "1.0"),
        "effectiveStartDate": meta.get("effective_start_date", today_str()),
        "effectiveEndDate":   meta.get("effective_end_date", "99991231"),
        "sourceUri":          meta.get("source_url", None),
        "docDesc":            meta.get("notes", f"{doc_cd} 시드 문서"),
        "content":            content,
    }


def collect_files() -> list[tuple[Path, str]]:
    """seed-data/ 하위 지원 확장자 파일을 (path, folder_name) 목록으로 반환.
    .meta.yml 파일은 제외한다."""
    results = []
    if not SEED_ROOT.exists():
        print(f"[ERROR] seed-data 폴더 없음: {SEED_ROOT}", file=sys.stderr)
        sys.exit(1)

    for folder_name in FOLDER_TO_CATEGORY:
        folder = SEED_ROOT / folder_name
        if not folder.exists():
            continue
        for file in sorted(folder.iterdir()):
            if (file.suffix.lower() in SUPPORTED_EXTENSIONS
                    and not file.name.startswith(".")
                    and not file.name.endswith(".meta.yml")):
                results.append((file, folder_name))

    return results


# ── 업로드 ─────────────────────────────────────────────────────────────────

def register_document(host: str, payload: dict, force: bool) -> str:
    """
    문서 등록. 반환값: 'created' | 'skipped' | 'forced' | 'error:<msg>'
    """
    url = host + REGISTER_URL
    resp = requests.post(url, json=payload, headers=HEADERS, timeout=30)

    if resp.status_code == 201:
        return "created"

    if resp.status_code == 409:
        # doc_cd 중복
        if not force:
            return "skipped"
        # --force: 기존 문서 비활성화 후 재등록
        doc_id = _find_doc_id_by_code(host, payload["docCd"])
        if doc_id:
            deactivate_url = host + ACTIVATE_URL.format(doc_id=doc_id) + "?active=false"
            requests.put(deactivate_url, headers=HEADERS, timeout=10)
            time.sleep(0.1)
        resp2 = requests.post(url, json=payload, headers=HEADERS, timeout=30)
        if resp2.status_code == 201:
            return "forced"
        return f"error:{resp2.status_code} {resp2.text[:100]}"

    return f"error:{resp.status_code} {resp.text[:100]}"


def _find_doc_id_by_code(host: str, doc_cd: str) -> int | None:
    """활성 문서 목록에서 doc_cd 로 docId 를 찾는다."""
    try:
        resp = requests.get(
            host + "/api/internal/advisory/documents",
            params={"docCd": doc_cd, "size": 1},
            headers=HEADERS,
            timeout=10,
        )
        if resp.status_code == 200:
            data = resp.json().get("data", {})
            content = data.get("content", [])
            if content:
                return content[0].get("docId")
    except Exception:
        pass
    return None


# ── 메인 ───────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="advisory-service RAG 정책문서 시드 업로더")
    parser.add_argument("--host",    default="http://localhost:8083",
                        help="advisory-service 주소 (기본: http://localhost:8083)")
    parser.add_argument("--dry-run", action="store_true",
                        help="실제 API 호출 없이 업로드 예정 목록만 출력")
    parser.add_argument("--force",   action="store_true",
                        help="doc_cd 충돌 시 기존 문서 비활성화 후 재등록")
    args = parser.parse_args()

    files = collect_files()
    if not files:
        print("[INFO] 업로드할 파일 없음. seed-data/ 하위 파일을 추가하세요.")
        return

    print(f"[INFO] 발견된 파일: {len(files)}건  host={args.host}  dry-run={args.dry_run}  force={args.force}")
    print()

    counts = {"created": 0, "skipped": 0, "forced": 0, "error": 0}

    for file_path, folder_name in files:
        rel = file_path.relative_to(SEED_ROOT)

        # 텍스트 추출 (이진 포맷 포함)
        try:
            payload = build_payload(file_path, folder_name)
        except Exception as e:
            print(f"  [FAIL] {rel}  ->  추출 실패: {e}")
            counts["error"] += 1
            continue

        if args.dry_run:
            print(f"  [DRY-RUN] {rel}  ->  doc_cd={payload['docCd']}"
                  f"  category={payload['docCategoryCd']}  chars={len(payload['content'])}")
            counts["created"] += 1
            continue

        result = register_document(args.host, payload, args.force)
        icon = {"created": "[OK]", "skipped": "[SKIP]", "forced": "[FORCE]"}.get(result.split(":")[0], "[FAIL]")
        print(f"  {icon} {rel}  ->  {result}")

        key = result.split(":")[0] if result.startswith("error") else result
        counts[key] = counts.get(key, 0) + 1

        time.sleep(0.05)  # rate limit 방지

    print()
    print(f"[완료] 등록={counts['created']}  건너뜀={counts['skipped']}"
          f"  재등록={counts['forced']}  오류={counts.get('error', 0)}")


if __name__ == "__main__":
    main()
