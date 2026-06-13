-- =============================================================================
-- V26: V24 데모 시드의 비표준 컴플라이언스 코드값 교정
--
--  배경: V24(어드민 데모 고객 시드)가 도메인 상수(ComplianceInfo.*)에 없는 값을 넣었다.
--    - cdd_level_code   : 'CDD'/'EDD'  →  코드 정본은 SIMPLIFIED/STANDARD/ENHANCED
--    - fatca/crs_status : 'NONE'        →  코드 정본은 EXEMPT/REPORTABLE/PENDING
--    실제 런타임(RegisterService)은 STANDARD/PENDING 등 정본값만 write 하므로,
--    'CDD'/'EDD'/'NONE'은 코드가 절대 만들지 않는 손수 데모값이다.
--
--  안전성: 컴플라이언스 검토 화면 쿼리(ComplianceInfoRepository)는 모두 불리언 플래그·
--    정본 상태로만 필터한다 — EDD=edd_required_yn, FATCA/CRS=*_reportable_yn,
--    제재=is_*_sanctioned_yn, KYC=kyc_status='COMPLETED'. cdd_level·fatca_status·
--    crs_status는 "표시 전용" 컬럼이라 값 교정이 어떤 목록 필터도 깨지 않는다.
--
--  매핑 근거:
--    - 'EDD'  → 'ENHANCED'   (강화된 고객확인 = Enhanced Due Diligence)
--    - 'CDD'  → 'STANDARD'   (일반 고객확인)
--    - 'NONE' → 'EXEMPT'     (보고 비대상 = 면제. reportable_yn='F'와 정합)
--
--  V24는 이미 적용된 마이그레이션이라 체크섬 보호를 위해 수정하지 않고 본 버전에서 정정한다.
--  값 기준 UPDATE라 데모 시드가 없는 환경(운영 등)에서는 0건 처리(no-op)된다.
-- =============================================================================

-- 1. CDD 수준 코드 정본화
UPDATE compliance_info SET cdd_level_code = 'ENHANCED', updated_at = NOW()
 WHERE cdd_level_code = 'EDD';

UPDATE compliance_info SET cdd_level_code = 'STANDARD', updated_at = NOW()
 WHERE cdd_level_code = 'CDD';

-- 2. FATCA/CRS 신고상태 코드 정본화 (비대상 → EXEMPT)
UPDATE compliance_info SET fatca_status_code = 'EXEMPT', updated_at = NOW()
 WHERE fatca_status_code = 'NONE';

UPDATE compliance_info SET crs_status_code = 'EXEMPT', updated_at = NOW()
 WHERE crs_status_code = 'NONE';
