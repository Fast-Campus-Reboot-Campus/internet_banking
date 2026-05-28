# synthetic-data-generator

자동심사 합성 데이터 파이프라인. Python 기반, Spring 모듈 외부.

## 구성

```
src/loaders/         외부 공공 데이터 API 로더
  config.py          .env 로드 (프로젝트 루트 .env 자동 인식)
  base.py            공통 HTTP 헬퍼·재시도·결과 저장
  ecos.py            한국은행 ECOS
  kosis.py           통계청 KOSIS
  data_go_kr.py      공공데이터포털
  fisis.py           금감원 FISIS
scripts/
  fetch_all.py       전체 일괄 수집 진입점
```

## 출력

`data/external/korean/<source>/<stat_id>__<period>.parquet`

`.gitignore` 의 `data/` 에 커버됨. 레포 미포함.

## 환경변수

프로젝트 루트 `.env` 에 다음 키 필요:

| 키 | 출처 |
|----|------|
| `KOREA_BANK_API_KEY` | <https://ecos.bok.or.kr/api/> |
| `KOSIS_API_KEY` | <https://kosis.kr/openapi/index/index.jsp> |
| `PUBLIC_DATA_API_KEY` | <https://www.data.go.kr> |
| `FISIS_API_KEY` | <https://fisis.fss.or.kr> |

## 설치 / 실행

```bash
cd synthetic-data-generator
pip install -r requirements.txt
python -m scripts.fetch_all                 # 전체
python -m scripts.fetch_all --source ecos   # 일부
```
