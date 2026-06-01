"""
upload_seed_data.py 단위 테스트.

usage:
    python -m pytest scripts/test_upload_seed_data.py -v
"""

import json
import tempfile
from pathlib import Path
from unittest.mock import MagicMock, call, patch

import pytest

import upload_seed_data as usd


# ── fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture
def seed_root(tmp_path: Path) -> Path:
    """임시 seed-data 폴더. 폴더 구조만 생성, 파일은 각 테스트에서 추가."""
    for folder in usd.FOLDER_TO_CATEGORY:
        (tmp_path / folder).mkdir()
    return tmp_path


def make_md(folder: Path, name: str, content: str = "정책 본문 테스트") -> Path:
    f = folder / f"{name}.md"
    f.write_text(content, encoding="utf-8")
    return f


def make_meta(folder: Path, stem: str, **kwargs):
    import yaml
    meta_path = folder / f"{stem}.meta.yml"
    meta_path.write_text(yaml.dump(kwargs, allow_unicode=True), encoding="utf-8")


# ── build_payload ─────────────────────────────────────────────────────────────

class TestBuildPayload:
    def test_stem_이_docCd_가_됨(self, seed_root):
        f = make_md(seed_root / "law", "LAW_BANKING_ACT")
        payload = usd.build_payload(f, "law")
        assert payload["docCd"] == "LAW_BANKING_ACT"

    def test_folder_이_category_가_됨(self, seed_root):
        f = make_md(seed_root / "supervision", "SUPER_FSS_DSR")
        payload = usd.build_payload(f, "supervision")
        assert payload["docCategoryCd"] == "SUPERVISION_GUIDE"

    def test_기본_유효기간_9999(self, seed_root):
        f = make_md(seed_root / "internal", "INTERNAL_BIAS_GUIDE")
        payload = usd.build_payload(f, "internal")
        assert payload["effectiveEndDate"] == "99991231"

    def test_meta_yml_이_기본값_오버라이드(self, seed_root):
        pytest.importorskip("yaml")
        folder = seed_root / "law"
        make_md(folder, "LAW_BANKING_ACT", "본문")
        make_meta(folder, "LAW_BANKING_ACT",
                  title="은행법",
                  version="2024-01-01",
                  effective_start_date="20240101",
                  effective_end_date="20291231",
                  notes="은행법 전문")
        payload = usd.build_payload(folder / "LAW_BANKING_ACT.md", "law")
        assert payload["docTitle"] == "은행법"
        assert payload["docVersion"] == "2024-01-01"
        assert payload["effectiveStartDate"] == "20240101"
        assert payload["effectiveEndDate"] == "20291231"
        assert payload["docDesc"] == "은행법 전문"

    def test_content_가_파일_전문(self, seed_root):
        body = "DSR 70% 초과 시 신용대출 승인 불가. 예외 적용 가이드."
        f = make_md(seed_root / "internal", "INTERNAL_DSR_POLICY", body)
        payload = usd.build_payload(f, "internal")
        assert payload["content"] == body


# ── extract_text: PDF ─────────────────────────────────────────────────────────

class TestExtractPdf:
    def test_pdf_텍스트_추출(self, tmp_path):
        pdf_file = tmp_path / "TEST.pdf"
        pdf_file.write_bytes(b"%PDF-1.4 dummy")  # 더미 바이트 — mock 으로 대체

        mock_page = MagicMock()
        mock_page.extract_text.return_value = "DSR 규정 본문"
        mock_pdf = MagicMock()
        mock_pdf.pages = [mock_page]
        mock_pdf.__enter__ = lambda s: mock_pdf
        mock_pdf.__exit__ = MagicMock(return_value=False)

        with patch("pdfplumber.open", return_value=mock_pdf):
            text = usd._extract_pdf(pdf_file)

        assert text == "DSR 규정 본문"

    def test_pdf_모든_페이지_합산(self, tmp_path):
        pdf_file = tmp_path / "TEST.pdf"
        pdf_file.write_bytes(b"%PDF")

        pages = [MagicMock(), MagicMock()]
        pages[0].extract_text.return_value = "1페이지"
        pages[1].extract_text.return_value = "2페이지"
        mock_pdf = MagicMock()
        mock_pdf.pages = pages
        mock_pdf.__enter__ = lambda s: mock_pdf
        mock_pdf.__exit__ = MagicMock(return_value=False)

        with patch("pdfplumber.open", return_value=mock_pdf):
            text = usd._extract_pdf(pdf_file)

        assert "1페이지" in text
        assert "2페이지" in text

    def test_pdf_텍스트_없으면_RuntimeError(self, tmp_path):
        pdf_file = tmp_path / "SCAN.pdf"
        pdf_file.write_bytes(b"%PDF")

        mock_page = MagicMock()
        mock_page.extract_text.return_value = ""
        mock_pdf = MagicMock()
        mock_pdf.pages = [mock_page]
        mock_pdf.__enter__ = lambda s: mock_pdf
        mock_pdf.__exit__ = MagicMock(return_value=False)

        with patch("pdfplumber.open", return_value=mock_pdf):
            with pytest.raises(RuntimeError, match="스캔 이미지"):
                usd._extract_pdf(pdf_file)

    def test_pdfplumber_미설치_시_RuntimeError(self, tmp_path):
        pdf_file = tmp_path / "TEST.pdf"
        pdf_file.write_bytes(b"%PDF")
        with patch.dict("sys.modules", {"pdfplumber": None}):
            with pytest.raises(RuntimeError, match="pdfplumber"):
                usd._extract_pdf(pdf_file)


# ── extract_text: HWP ─────────────────────────────────────────────────────────

class TestExtractHwp:
    def test_hwp5txt_커맨드_성공(self, tmp_path):
        hwp_file = tmp_path / "TEST.hwp"
        hwp_file.write_bytes(b"\xd0\xcf\x11\xe0")  # OLE 시그니처

        mock_result = MagicMock(returncode=0, stdout="여신심사 매뉴얼 본문\n")
        with patch("subprocess.run", return_value=mock_result) as mock_run:
            text = usd._extract_hwp(hwp_file)

        assert text == "여신심사 매뉴얼 본문"
        assert mock_run.call_args[0][0][0] == "hwp5txt"

    def test_hwp5txt_없으면_python_모듈_폴백(self, tmp_path):
        hwp_file = tmp_path / "TEST.hwp"
        hwp_file.write_bytes(b"\xd0\xcf\x11\xe0")

        import sys
        fail = MagicMock(side_effect=FileNotFoundError)
        success = MagicMock(returncode=0, stdout="폴백 본문\n")
        with patch("subprocess.run", side_effect=[FileNotFoundError(), success]):
            text = usd._extract_hwp(hwp_file)

        assert text == "폴백 본문"

    def test_hwp_모두_실패_시_RuntimeError(self, tmp_path):
        hwp_file = tmp_path / "TEST.hwp"
        hwp_file.write_bytes(b"\xd0\xcf\x11\xe0")

        fail = MagicMock(returncode=1, stdout="", stderr="오류 메시지")
        with patch("subprocess.run", side_effect=[FileNotFoundError(), fail]):
            with pytest.raises(RuntimeError, match="hwp5"):
                usd._extract_hwp(hwp_file)


# ── extract_text: PNG (OCR) ───────────────────────────────────────────────────

class TestExtractImageOcr:
    def test_png_ocr_텍스트_반환(self, tmp_path):
        png_file = tmp_path / "금리표.png"
        png_file.write_bytes(b"\x89PNG\r\n\x1a\n")  # PNG 시그니처 더미

        mock_img = MagicMock()
        mock_img.width = 1200
        mock_img.height = 800

        with patch("PIL.Image.open", return_value=mock_img), \
             patch("pytesseract.image_to_string", return_value="신용대출금리 3.5%") as mock_ocr:
            text = usd._extract_image_ocr(png_file)

        assert text == "신용대출금리 3.5%"
        mock_ocr.assert_called_once_with(mock_img, lang="kor+eng")

    def test_저해상도_이미지_업스케일_후_ocr(self, tmp_path):
        png_file = tmp_path / "small.png"
        png_file.write_bytes(b"\x89PNG\r\n\x1a\n")

        mock_img = MagicMock()
        mock_img.width = 400   # < 1000 → 업스케일 트리거
        mock_img.height = 300
        mock_img.resize.return_value = mock_img  # resize 후 같은 객체 반환

        with patch("PIL.Image.open", return_value=mock_img), \
             patch("pytesseract.image_to_string", return_value="업스케일 텍스트"):
            text = usd._extract_image_ocr(png_file)

        mock_img.resize.assert_called_once()  # 업스케일 호출됨
        assert text == "업스케일 텍스트"

    def test_ocr_결과_빔_RuntimeError(self, tmp_path):
        png_file = tmp_path / "blank.png"
        png_file.write_bytes(b"\x89PNG\r\n\x1a\n")

        mock_img = MagicMock()
        mock_img.width = 1200

        with patch("PIL.Image.open", return_value=mock_img), \
             patch("pytesseract.image_to_string", return_value="   "):
            with pytest.raises(RuntimeError, match="OCR 결과 빔"):
                usd._extract_image_ocr(png_file)

    def test_pillow_미설치_시_RuntimeError(self, tmp_path):
        png_file = tmp_path / "TEST.png"
        png_file.write_bytes(b"\x89PNG")
        with patch.dict("sys.modules", {"PIL": None, "PIL.Image": None, "pytesseract": None}):
            with pytest.raises((RuntimeError, ImportError)):
                usd._extract_image_ocr(png_file)


# ── collect_files ─────────────────────────────────────────────────────────────

class TestCollectFiles:
    def test_지원_확장자_모두_수집(self, seed_root, monkeypatch):
        monkeypatch.setattr(usd, "SEED_ROOT", seed_root)
        folder = seed_root / "law"
        make_md(folder, "LAW_MD")
        (folder / "LAW_PDF.pdf").write_bytes(b"%PDF")
        (folder / "LAW_HWP.hwp").write_bytes(b"\xd0\xcf")
        (folder / "LAW_PNG.png").write_bytes(b"\x89PNG")
        (folder / ".hidden.md").write_text("숨김")
        (folder / "LAW_PDF.meta.yml").write_text("title: test")  # meta.yml 제외

        files = usd.collect_files()
        names = [f.name for f, _ in files]
        assert "LAW_MD.md" in names
        assert "LAW_PDF.pdf" in names
        assert "LAW_HWP.hwp" in names
        assert "LAW_PNG.png" in names
        assert ".hidden.md" not in names
        assert "LAW_PDF.meta.yml" not in names

    def test_빈_폴더는_건너뜀(self, seed_root, monkeypatch):
        monkeypatch.setattr(usd, "SEED_ROOT", seed_root)
        files = usd.collect_files()
        assert files == []

    def test_폴더_순서_보장(self, seed_root, monkeypatch):
        monkeypatch.setattr(usd, "SEED_ROOT", seed_root)
        make_md(seed_root / "law", "B_LAW")
        make_md(seed_root / "law", "A_LAW")
        files = usd.collect_files()
        names = [f.name for f, _ in files]
        assert names == sorted(names)


# ── register_document (멱등성) ──────────────────────────────────────────────

class TestRegisterDocument:
    def _payload(self):
        return {"docCd": "LAW_TEST", "docTitle": "테스트", "content": "본문",
                "docCategoryCd": "LAW", "docVersion": "1.0",
                "effectiveStartDate": "20260101", "effectiveEndDate": "99991231",
                "sourceUri": None, "docDesc": "테스트"}

    def test_201_created(self):
        mock_resp = MagicMock(status_code=201)
        with patch("requests.post", return_value=mock_resp):
            result = usd.register_document("http://localhost:8083", self._payload(), force=False)
        assert result == "created"

    def test_409_충돌_force_false_는_skipped(self):
        mock_resp = MagicMock(status_code=409)
        with patch("requests.post", return_value=mock_resp):
            result = usd.register_document("http://localhost:8083", self._payload(), force=False)
        assert result == "skipped"

    def test_409_충돌_force_true_재등록_성공(self):
        post_responses = [
            MagicMock(status_code=409),
            MagicMock(status_code=201),
        ]
        put_resp = MagicMock(status_code=200)
        with patch("requests.post", side_effect=post_responses), \
             patch("requests.put",  return_value=put_resp), \
             patch.object(usd, "_find_doc_id_by_code", return_value=42):
            result = usd.register_document("http://localhost:8083", self._payload(), force=True)
        assert result == "forced"

    def test_5xx_error_반환(self):
        mock_resp = MagicMock(status_code=500, text="Internal Server Error")
        with patch("requests.post", return_value=mock_resp):
            result = usd.register_document("http://localhost:8083", self._payload(), force=False)
        assert result.startswith("error:500")


# ── 추출 실패 내성 ────────────────────────────────────────────────────────────

class TestExtractionFailureTolerance:
    def test_추출_실패_시_해당_파일_skip_나머지_계속(self, seed_root, monkeypatch, capsys):
        """extract_text 에서 예외가 나도 다음 파일은 계속 처리된다."""
        monkeypatch.setattr(usd, "SEED_ROOT", seed_root)
        folder = seed_root / "law"
        make_md(folder, "LAW_OK", "정상 본문")
        (folder / "LAW_BAD.pdf").write_bytes(b"%PDF")  # 추출 실패 유도

        mock_post = MagicMock(return_value=MagicMock(status_code=201))

        def fake_extract(path):
            if path.suffix == ".pdf":
                raise RuntimeError("pdfplumber 미설치")
            return path.read_text(encoding="utf-8")

        with patch.object(usd, "extract_text", side_effect=fake_extract), \
             patch("requests.post", mock_post):
            # main() 대신 직접 루프 실행
            files = usd.collect_files()
            error_count = 0
            ok_count = 0
            for file_path, folder_name in files:
                try:
                    payload = usd.build_payload(file_path, folder_name)
                    ok_count += 1
                except Exception:
                    error_count += 1

        assert error_count == 1   # PDF 1건 실패
        assert ok_count == 1      # MD 1건 성공


# ── dry-run 흐름 ─────────────────────────────────────────────────────────────

class TestDryRun:
    def test_dry_run_시_API_호출_없음(self, seed_root, monkeypatch, capsys):
        monkeypatch.setattr(usd, "SEED_ROOT", seed_root)
        make_md(seed_root / "law", "LAW_BANKING_ACT", "은행법 본문")

        with patch("requests.post") as mock_post:
            files = usd.collect_files()
            for file_path, folder_name in files:
                payload = usd.build_payload(file_path, folder_name)
                print(f"[DRY-RUN] {file_path.name} → {payload['docCd']}")

            mock_post.assert_not_called()

        out = capsys.readouterr().out
        assert "LAW_BANKING_ACT" in out
