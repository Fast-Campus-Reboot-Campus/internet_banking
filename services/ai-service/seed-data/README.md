# ai-service RAG 운영 시드 데이터

> **목적**: `ai-service` RAG (`RAG_DOCUMENT` / `RAG_CHUNK`) 에 적재할 **운영 정책문서 원본** 을 보관한다.
> **dev/staging 합성 데이터는 별도**: `services/synthetic-data-generator/scripts/seed_hmda_rag.py` 참고. 본 폴더는 prod 시드 전용.

---

## 1. 폴더 구조와 doc_type 매핑

```
seed-data/
├── README.md                         (본 파일)
├── law/                              → doc_type_cd = LAW
├── supervision/                      → doc_type_cd = SUPERVISION_GUIDE
├── internal/                         → doc_type_cd = INTERNAL_RULE
├── product/                          → doc_type_cd = PRODUCT_TERMS
└── fair-lending/                     → doc_type_cd = FAIR_LENDING (법령) / BIAS_CASE (사례)
```

폴더와 doc_type 의 대응은 업로더 스크립트가 자동 매핑한다. 새 doc_type 을 추가하려면 폴더 신설 + 업로더의 매핑 테이블 갱신.

---

## 2. 파일명 규칙

```
{doc_cd}.{ext}
```

- `doc_cd` = `RAG_DOCUMENT.doc_cd` 컬럼 값 그대로. 영문 대문자 + 언더스코어. 도메인 + 식별자 패턴.
- `ext` = `pdf` / `docx` / `md` / `txt`. Tika 가 처리 가능한 포맷이면 무엇이든 OK.

**예시:**
| 파일 | doc_cd | doc_type |
|---|---|---|
| `law/LAW_BANKING_ACT.pdf` | `LAW_BANKING_ACT` | `LAW` |
| `supervision/SUPER_FSS_DSR.pdf` | `SUPER_FSS_DSR` | `SUPERVISION_GUIDE` |
| `internal/INTERNAL_BIAS_GUIDE.md` | `INTERNAL_BIAS_GUIDE` | `INTERNAL_RULE` |
| `product/PRODUCT_MORTGAGE_TERMS.pdf` | `PRODUCT_MORTGAGE_TERMS` | `PRODUCT_TERMS` |
| `fair-lending/BIAS_CASE_2024_01.md` | `BIAS_CASE_2024_01` | `BIAS_CASE` |

---

## 3. 최소 시드 권장 구성 (10건)

포트폴리오 시연에 필요한 최소 시드. 사용자가 직접 채워 넣는다.

| # | 폴더 | 파일 | 출처 후보 |
|---|---|---|---|
| 1 | `law/` | `LAW_BANKING_ACT.pdf` | 은행법 (국가법령정보센터) |
| 2 | `law/` | `LAW_CREDIT_INFO.pdf` | 신용정보의 이용 및 보호에 관한 법률 |
| 3 | `supervision/` | `SUPER_FSS_LOAN.pdf` | 금융감독원 여신업무감독규정 |
| 4 | `supervision/` | `SUPER_FSS_DSR.pdf` | DSR 산정기준 (금감원 보도자료) |
| 5 | `internal/` | `INTERNAL_REVIEW_MANUAL.md` | 자체 작성 — 여신심사 매뉴얼 |
| 6 | `internal/` | `INTERNAL_BIAS_GUIDE.md` | 자체 작성 — 차별 금지·공정 심사 가이드 |
| 7 | `product/` | `PRODUCT_MORTGAGE_TERMS.pdf` | 주택담보대출 표준약관 |
| 8 | `product/` | `PRODUCT_CREDIT_TERMS.pdf` | 신용대출 표준약관 |
| 9 | `fair-lending/` | `FAIR_LENDING_ACT.pdf` | 공정대출 관련 법령 / 차별금지법 |
| 10 | `fair-lending/` | `BIAS_CASE_2024_01.md` | 금융위 보도자료의 분쟁·제재 사례 요약 |

---

## 4. 업로드 절차

본 폴더에 파일을 두는 것만으로는 DB 적재가 일어나지 않는다. 업로더 스크립트가 본 폴더를 스캔해 `POST /internal/rag/documents` 로 적재한다.

### 4-1. 업로더 (plan 13 Stage 2 에서 추가 예정)

```
services/ai-service/scripts/upload_seed_data.py  (또는 Java CLI)
```

동작:
1. `seed-data/` 재귀 스캔
2. 폴더명 → doc_type_cd 매핑
3. 파일명 stem → doc_cd
4. 본문 추출 (PDF/DOCX: Tika, MD/TXT: 그대로)
5. `POST /internal/rag/documents` 호출 → 청크·임베딩 적재
6. `doc_cd + version` 기준 멱등 (기존 active 가 있으면 NO-OP, `--force` 옵션 시 deactivate → 신규 active)

### 4-2. 실행 (예시)

```bash
# dry-run: 무엇이 업로드될지만 출력
python services/ai-service/scripts/upload_seed_data.py --dry-run

# 실제 업로드
python services/ai-service/scripts/upload_seed_data.py
```

---

## 5. 메타데이터 (선택)

각 파일 옆에 동일 stem 의 `.meta.yml` 을 두면 업로더가 추가 메타데이터를 읽는다. 없으면 기본값 사용.

```yaml
# law/LAW_BANKING_ACT.meta.yml
title: 은행법
version: "2024-01-01"
effective_start_date: "20240101"
effective_end_date: "99991231"
source_url: "https://www.law.go.kr/..."
notes: "법령 개정 시 version 갱신"
```

기본값:
- `version` = 파일 mtime 기반 `YYYY-MM-DD`
- `effective_start_date` = 오늘
- `effective_end_date` = `99991231`

---

## 6. 보안·라이선스 주의

- **PII 금지**: 본 폴더에 들어가는 모든 문서는 공개 가능한 자료여야 한다. 실제 고객 정보·내부 비공개 문서 금지.
- **라이선스**: 약관·매뉴얼이 저작권 대상일 수 있으니 포트폴리오 공개 저장소에 커밋 전 출처·이용 가능성 확인. 본 README 의 시드 권장 목록은 모두 공개 자료 기준.
- **`.gitignore` 권장**: 라이선스가 불분명한 PDF 는 `seed-data/local/` 하위에 두고 gitignore 처리.

---

## 7. 합성 데이터와의 관계

| 폴더 | 용도 | 환경 |
|---|---|---|
| `services/ai-service/seed-data/` (본 폴더) | 공개 정책 문서, 실 임베딩 | **prod** |
| `services/synthetic-data-generator/scripts/seed_hmda_rag.py` | 합성 HMDA 정책 5건 | dev / staging |

두 시드 경로는 절대 섞이지 않는다. prod 빌드는 본 폴더만 적재.

---

## 8. 관련 plan

- `docs/plan/13_rag_operationalization.md` Stage 2 — 본 폴더 + 업로더 추가 절차
- `docs/plan/13_rag_operationalization.md` Stage 5 — advisory ↔ ai-service RAG 통합 후 본 시드가 단일 진실
