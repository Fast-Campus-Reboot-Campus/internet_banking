-- ============================================================================
-- Internet Banking database structure reference
-- ============================================================================
-- Source: local PostgreSQL dumps under db-dump혜민
-- Purpose: reviewed snapshot of the current database structures
--
-- Excluded intentionally:
--   * table data (COPY / INSERT)
--   * Flyway history tables and indexes
--   * database owners, grants, and role-specific ACLs
--   * sequence current values
--   * pg_dump session directives
--
-- Usage notes:
--   * Databases must already exist before running this file with psql.
--   * The payment-service section applies to both payment_db and payment_b.
--   * PostgreSQL extensions used by loan_db and ai_db must be available.
--   * This file is a reference snapshot. Flyway migrations remain the source of
--     truth for application-managed schema evolution.
-- ============================================================================


-- ============================================================================
-- SERVICE: customer-service
-- DATABASE: customer_db
-- ============================================================================
\connect customer_db

CREATE TABLE public.api_token (
    token_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    session_id character varying(64) NOT NULL,
    token_type_code character varying(20) NOT NULL,
    token_hash character varying(255) NOT NULL,
    token_issued_channel_code character varying(20) NOT NULL,
    token_scope character varying(500),
    token_client_id character varying(50),
    token_issued_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    token_expiry_at timestamp(3) with time zone NOT NULL,
    token_revoked_at timestamp(3) with time zone,
    token_revoke_reason_code character varying(20),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3),
    created_by bigint,
    updated_at timestamp(3) with time zone,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_api_token_type CHECK (((token_type_code)::text = ANY ((ARRAY['ACCESS'::character varying, 'REFRESH'::character varying, 'OAUTH'::character varying])::text[])))
);

ALTER TABLE public.api_token ALTER COLUMN token_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.api_token_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.auth_method (
    auth_method_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    auth_method_type_code character varying(20) NOT NULL,
    auth_method_alias_name character varying(50),
    auth_method_status_code character varying(20) NOT NULL,
    primary_auth_method_yn character varying(1) DEFAULT 'F'::bpchar NOT NULL,
    auth_method_registered_date character varying(8) NOT NULL,
    auth_method_expiry_date character varying(8),
    auth_method_last_used_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_auth_method_primary CHECK (((primary_auth_method_yn)::bpchar = ANY (ARRAY['T'::bpchar, 'F'::bpchar]))),
    CONSTRAINT chk_auth_method_type CHECK (((auth_method_type_code)::text = ANY ((ARRAY['SMS'::character varying, 'PASS'::character varying, 'CERT_FIN'::character varying, 'CERT_COMMON'::character varying, 'CERT_AXFUL'::character varying, 'PIN'::character varying, 'BIO_FACE'::character varying, 'BIO_FINGER'::character varying])::text[])))
);

ALTER TABLE public.auth_method ALTER COLUMN auth_method_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.auth_method_auth_method_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.business_info (
    business_info_id bigint NOT NULL,
    party_id bigint NOT NULL,
    biz_reg_no character varying(12) NOT NULL,
    biz_status_code character varying(20) NOT NULL,
    trade_name character varying(200) NOT NULL,
    english_trade_name character varying(400),
    opening_date character varying(8) NOT NULL,
    closing_date character varying(8),
    nts_industry_code character varying(6) NOT NULL,
    ksic_code character varying(5) NOT NULL,
    biz_type_code character varying(10),
    biz_item_code character varying(10) NOT NULL,
    tax_type_code character varying(10) NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

ALTER TABLE public.business_info ALTER COLUMN business_info_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.business_info_business_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.certificate (
    certificate_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    auth_method_id bigint NOT NULL,
    certificate_type_code character varying(20) NOT NULL,
    certificate_serial_number character varying(100) NOT NULL,
    certificate_issuer_name character varying(50) NOT NULL,
    certificate_subject_dn text NOT NULL,
    certificate_issuer_dn text NOT NULL,
    certificate_public_key text NOT NULL,
    certificate_purpose_code character varying(50) NOT NULL,
    certificate_issued_date character varying(8) NOT NULL,
    certificate_expiry_date character varying(8) NOT NULL,
    certificate_renewal_scheduled_date character varying(8),
    certificate_status_code character varying(20) NOT NULL,
    certificate_revoke_reason_code character varying(200),
    certificate_revoked_at timestamp(3) with time zone,
    cert_login_failure_count integer,
    max_cert_login_failure_count integer,
    last_cert_login_failure_at timestamp(3) with time zone,
    cert_login_locked_at timestamp(3) with time zone,
    cert_login_unlocked_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    cert_pin_hash character varying(255),
    CONSTRAINT chk_certificate_status CHECK (((certificate_status_code)::text = ANY ((ARRAY['ACTIVE'::character varying, 'EXPIRED'::character varying, 'REVOKED'::character varying, 'SUSPENDED'::character varying])::text[])))
);

ALTER TABLE public.certificate ALTER COLUMN certificate_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.certificate_certificate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.certificate_use (
    certificate_use_id bigint NOT NULL,
    certificate_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    purpose_code character varying(30) NOT NULL,
    certificate_use_target_transaction_id character varying(50),
    certificate_use_target_system_code character varying(20),
    certificate_use_signed_data_hash character varying(255) NOT NULL,
    certificate_use_signature_value text NOT NULL,
    certificate_use_verification_result_code character varying(20) NOT NULL,
    certificate_use_failure_reason_code character varying(200),
    certificate_use_request_ip character varying(45) NOT NULL,
    certificate_use_request_channel_code character varying(20) NOT NULL,
    certificate_used_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

ALTER TABLE public.certificate_use ALTER COLUMN certificate_use_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.certificate_use_certificate_use_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.compliance_info (
    party_id bigint NOT NULL,
    aml_risk_level_code character varying(20) NOT NULL,
    aml_last_assessed_at timestamp(3) with time zone,
    aml_next_review_date character varying(8),
    is_ofac_sanctioned_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    is_un_sanctioned_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    is_eu_sanctioned_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    is_kr_sanctioned_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    is_sanctioned_yn character varying(1) GENERATED ALWAYS AS (
CASE
    WHEN ((is_ofac_sanctioned_yn = 'T'::bpchar) OR (is_un_sanctioned_yn = 'T'::bpchar) OR (is_eu_sanctioned_yn = 'T'::bpchar) OR (is_kr_sanctioned_yn = 'T'::bpchar)) THEN 'T'::text
    ELSE 'F'::text
END) STORED,
    sanction_last_screened_at timestamp(3) with time zone,
    sanction_next_screen_date character(8),
    kyc_status_code character varying(20) NOT NULL,
    kyc_completed_at timestamp(3) with time zone,
    kyc_expiry_date character(8),
    kyc_next_review_date character(8),
    identity_verification_method_code character varying(10),
    cdd_level_code character varying(20) NOT NULL,
    cdd_last_reviewed_at timestamp(3) with time zone,
    cdd_next_review_date character varying(8),
    edd_required_yn character varying(1) DEFAULT 'F'::bpchar NOT NULL,
    edd_last_reviewed_at timestamp(3) with time zone,
    edd_next_review_date character varying(8),
    fatca_status_code character varying(20) NOT NULL,
    fatca_last_reviewed_at timestamp(3) with time zone,
    fatca_next_review_date character varying(8),
    fatca_reportable_yn character varying(1) DEFAULT 'F'::bpchar NOT NULL,
    crs_status_code character varying(20) NOT NULL,
    crs_last_reviewed_at timestamp(3) with time zone,
    crs_next_review_date character varying(8),
    crs_reportable_yn character varying(1) DEFAULT 'F'::bpchar NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    aml_last_assessed_by_employee_id bigint,
    kyc_completed_by_employee_id bigint
);

CREATE TABLE public.credential (
    credential_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    login_id character varying(50) NOT NULL,
    password_hash character varying(255) NOT NULL,
    password_changed_at timestamp(3) with time zone NOT NULL,
    password_expiry_at timestamp(3) with time zone,
    account_status_code character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    password_login_failure_count integer DEFAULT 0 NOT NULL,
    max_password_login_failure_count integer DEFAULT 5 NOT NULL,
    password_login_locked_at timestamp(3) with time zone,
    password_login_unlocked_at timestamp(3) with time zone,
    password_last_login_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_credential_account_status CHECK (((account_status_code)::text = ANY ((ARRAY['ACTIVE'::character varying, 'LOCKED'::character varying, 'DORMANT'::character varying, 'CLOSED'::character varying])::text[])))
);

ALTER TABLE public.credential ALTER COLUMN credential_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.credential_credential_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.cust_code_master (
    code_group_id character varying(30) NOT NULL,
    code_value character varying(20) NOT NULL,
    code_name character varying(100) NOT NULL,
    description character varying(500),
    sort_order integer,
    effective_start_date character(8) NOT NULL,
    effective_end_date character(8),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE TABLE public.customer (
    customer_id bigint NOT NULL,
    party_id bigint NOT NULL,
    customer_grade_code character varying(10),
    customer_status_code character varying(20) NOT NULL,
    main_customer_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    credit_rating_code character varying(10),
    credit_evaluation_date character(8),
    credit_agency_code character varying(10),
    preferred_language_code character(2),
    sms_receive_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    email_receive_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    postal_receive_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    notification_method_code character varying(10),
    email character varying(255),
    phone character varying(20),
    zip_code character varying(10),
    address character varying(255),
    address_detail character varying(255),
    join_channel_code character varying(20),
    first_join_date character(8),
    joined_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_transaction_at timestamp(3) with time zone,
    dormant_at timestamp(3) with time zone,
    closed_at timestamp(3) with time zone,
    close_reason_code character varying(20),
    privacy_expiry_date character(8),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    suspended_at timestamp(3) with time zone,
    CONSTRAINT chk_customer_lifecycle CHECK (((((customer_status_code)::text = 'CLOSED'::text) AND (closed_at IS NOT NULL) AND (close_reason_code IS NOT NULL)) OR (((customer_status_code)::text = 'DORMANT'::text) AND (dormant_at IS NOT NULL)) OR (((customer_status_code)::text = 'SUSPENDED'::text) AND (suspended_at IS NOT NULL)) OR ((customer_status_code)::text = 'ACTIVE'::text)))
);

CREATE TABLE public.customer_access_log (
    customer_access_log_id bigint NOT NULL,
    accessor_employee_id bigint NOT NULL,
    accessor_name character varying(100),
    accessor_role character varying(40),
    accessor_branch_code character varying(10),
    target_customer_id bigint NOT NULL,
    target_customer_name character varying(100),
    access_action_code character varying(40) NOT NULL,
    access_reason character varying(500),
    accessed_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL
);

ALTER TABLE public.customer_access_log ALTER COLUMN customer_access_log_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.customer_access_log_customer_access_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.customer ALTER COLUMN customer_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.customer_customer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.customer_grade_history (
    customer_grade_history_id bigint NOT NULL,
    previous_customer_grade_history_id bigint,
    customer_id bigint NOT NULL,
    customer_grade_code character varying(10) NOT NULL,
    previous_customer_grade_code character varying(10),
    customer_grade_change_reason_code character varying(20) NOT NULL,
    customer_grade_change_reason_detail character varying(500),
    customer_grade_effective_start_date character(8) NOT NULL,
    customer_grade_effective_end_date character(8),
    customer_grade_evaluated_at timestamp(3) with time zone NOT NULL,
    system_auto_triggered_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    changed_by_employee_id bigint
);

ALTER TABLE public.customer_grade_history ALTER COLUMN customer_grade_history_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.customer_grade_history_customer_grade_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.customer_status_history (
    customer_status_history_id bigint NOT NULL,
    previous_customer_status_history_id bigint,
    customer_id bigint NOT NULL,
    customer_status_code character varying(20) NOT NULL,
    previous_customer_status_code character varying(20),
    customer_status_change_reason_code character varying(20) NOT NULL,
    customer_status_change_reason_detail character varying(500),
    customer_status_effective_start_at timestamp(3) with time zone NOT NULL,
    customer_status_effective_end_at timestamp(3) with time zone,
    system_auto_triggered_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    changed_by_employee_id bigint
);

ALTER TABLE public.customer_status_history ALTER COLUMN customer_status_history_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.customer_status_history_customer_status_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.duplicate_review_case (
    duplicate_review_case_id bigint NOT NULL,
    new_party_id bigint NOT NULL,
    existing_party_id bigint NOT NULL,
    match_type_code character varying(20) NOT NULL,
    review_status_code character varying(20) NOT NULL,
    reviewer_employee_id bigint,
    review_comment character varying(500),
    detected_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    reviewed_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_duplicate_review_case_distinct_parties CHECK ((new_party_id <> existing_party_id))
);

ALTER TABLE public.duplicate_review_case ALTER COLUMN duplicate_review_case_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.duplicate_review_case_duplicate_review_case_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.employee (
    employee_id bigint NOT NULL,
    party_id bigint NOT NULL,
    branch_code character varying(10) NOT NULL,
    grade_code character varying(30) NOT NULL,
    status_code character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_employee_status CHECK (((status_code)::text = ANY ((ARRAY['ACTIVE'::character varying, 'CLOSED'::character varying])::text[])))
);

ALTER TABLE public.employee ALTER COLUMN employee_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.employee_employee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.fds_detection (
    fds_detection_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    fds_rule_id bigint NOT NULL,
    fds_detection_event_type_code character varying(30) NOT NULL,
    fds_detection_event_reference_id bigint NOT NULL,
    fds_detected_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    fds_detection_status_code character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_fds_detection_status CHECK (((fds_detection_status_code)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'FALSE_POSITIVE'::character varying])::text[])))
);

ALTER TABLE public.fds_detection ALTER COLUMN fds_detection_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.fds_detection_fds_detection_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.fds_incident (
    fds_incident_id bigint NOT NULL,
    fds_detection_id bigint NOT NULL,
    fds_incident_handler_employee_id bigint,
    fds_incident_type_code character varying(20) NOT NULL,
    fds_incident_process_status_code character varying(20) NOT NULL,
    fds_incident_fss_reported_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    fds_incident_reported_at timestamp(3) with time zone,
    fds_incident_closed_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_fds_incident_fss_reported CHECK ((fds_incident_fss_reported_yn = ANY (ARRAY['T'::bpchar, 'F'::bpchar])))
);

ALTER TABLE public.fds_incident ALTER COLUMN fds_incident_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.fds_incident_fds_incident_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.fds_rule (
    fds_rule_id bigint NOT NULL,
    fds_rule_code character varying(30) NOT NULL,
    fds_rule_name character varying(100) NOT NULL,
    fds_rule_category_code character varying(30) NOT NULL,
    fds_rule_target_event_code character varying(50) NOT NULL,
    fds_rule_condition_json json NOT NULL,
    fds_rule_risk_weight integer DEFAULT 50 NOT NULL,
    fds_rule_action_type_code character varying(20) NOT NULL,
    fds_rule_active_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    fds_rule_effective_date character(8) NOT NULL,
    fds_rule_expiry_date character(8),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_fds_rule_action_type CHECK (((fds_rule_action_type_code)::text = ANY ((ARRAY['BLOCK'::character varying, 'CHALLENGE'::character varying, 'MONITOR'::character varying])::text[]))),
    CONSTRAINT chk_fds_rule_active CHECK ((fds_rule_active_yn = ANY (ARRAY['T'::bpchar, 'F'::bpchar]))),
    CONSTRAINT chk_fds_rule_risk_weight CHECK (((fds_rule_risk_weight >= 0) AND (fds_rule_risk_weight <= 100)))
);

ALTER TABLE public.fds_rule ALTER COLUMN fds_rule_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.fds_rule_fds_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.foreigner_info (
    party_id bigint NOT NULL,
    foreigner_no_encrypted character varying(255),
    passport_no character varying(20),
    passport_country_code character(3),
    passport_expiry_date character(8),
    stay_qualification_code character varying(10),
    stay_expiry_date character(8),
    recent_entry_date character(8),
    stay_address character varying(500),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE TABLE public.identity_verification (
    identity_verification_id bigint NOT NULL,
    customer_id bigint,
    mobile_auth_id bigint NOT NULL,
    identity_verification_agency_code character varying(30) NOT NULL,
    identity_verification_purpose_code character varying(30) NOT NULL,
    identity_verification_ci_value character varying(88) NOT NULL,
    identity_verification_name character varying(50) NOT NULL,
    identity_verification_birth_date character(8) NOT NULL,
    identity_verification_gender_code character(1) NOT NULL,
    identity_verification_nationality_type_code character varying(20) NOT NULL,
    identity_verification_telecom_carrier_code character varying(20),
    identity_verification_phone_number character varying(20),
    identity_verified_at timestamp(3) with time zone NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    rrn_encrypted character varying(255),
    consumed_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    consumed_customer_id bigint,
    consumed_at timestamp(3) with time zone,
    CONSTRAINT chk_identity_verification_agency CHECK (((identity_verification_agency_code)::text = ANY ((ARRAY['NICE'::character varying, 'KCB'::character varying, 'SCI'::character varying, 'PASS'::character varying])::text[]))),
    CONSTRAINT chk_identity_verification_consumed CHECK ((consumed_yn = ANY (ARRAY['T'::bpchar, 'F'::bpchar])))
);

ALTER TABLE public.identity_verification ALTER COLUMN identity_verification_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.identity_verification_identity_verification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.login_attempt (
    login_attempt_id bigint NOT NULL,
    customer_id bigint,
    device_id bigint,
    attempted_login_id character varying(50) NOT NULL,
    login_attempt_channel_code character varying(20) NOT NULL,
    login_attempt_ip character varying(45) NOT NULL,
    login_attempt_ip_country_code character(3),
    login_attempt_user_agent text,
    login_attempt_device_fingerprint_hash character varying(255),
    login_attempt_success_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    login_attempt_failure_reason_code character varying(20),
    login_attempted_at timestamp(3) with time zone NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_login_attempt_success CHECK ((login_attempt_success_yn = ANY (ARRAY['T'::bpchar, 'F'::bpchar])))
);

ALTER TABLE public.login_attempt ALTER COLUMN login_attempt_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.login_attempt_login_attempt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.login_session (
    session_id character varying(64) NOT NULL,
    customer_id bigint NOT NULL,
    login_attempt_id bigint NOT NULL,
    device_id bigint,
    token_id bigint NOT NULL,
    session_issued_ip character varying(45) NOT NULL,
    session_channel_code character varying(20) NOT NULL,
    session_status_code character varying(20) NOT NULL,
    session_mfa_completed_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    session_expiry_at timestamp(3) with time zone NOT NULL,
    session_ended_at timestamp(3) with time zone,
    session_end_reason_code character varying(20),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_login_session_mfa CHECK ((session_mfa_completed_yn = ANY (ARRAY['T'::bpchar, 'F'::bpchar]))),
    CONSTRAINT chk_login_session_status CHECK (((session_status_code)::text = ANY ((ARRAY['ACTIVE'::character varying, 'EXPIRED'::character varying, 'LOGGED_OUT'::character varying, 'FORCED_OUT'::character varying])::text[])))
);

CREATE TABLE public.mobile_auth (
    mobile_auth_id bigint NOT NULL,
    customer_id bigint,
    mobile_auth_method_type_code character varying(20) NOT NULL,
    mobile_auth_telecom_carrier_code character varying(20) NOT NULL,
    mobile_auth_recipient_phone_number character varying(20) NOT NULL,
    mobile_auth_code_hash character varying(255) NOT NULL,
    mobile_auth_purpose_code character varying(30) NOT NULL,
    mobile_auth_request_ip character varying(45) NOT NULL,
    mobile_auth_request_channel_code character varying(20) NOT NULL,
    mobile_auth_sent_at timestamp(3) with time zone NOT NULL,
    mobile_auth_expiry_at timestamp(3) with time zone NOT NULL,
    mobile_auth_verified_at timestamp(3) with time zone,
    mobile_auth_verified_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    mobile_auth_attempt_count integer DEFAULT 0 NOT NULL,
    mobile_auth_failure_reason_code character varying(200),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_mobile_auth_verified CHECK ((mobile_auth_verified_yn = ANY (ARRAY['T'::bpchar, 'F'::bpchar])))
);

ALTER TABLE public.mobile_auth ALTER COLUMN mobile_auth_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.mobile_auth_mobile_auth_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.party (
    party_id bigint NOT NULL,
    party_type_code character varying(20) NOT NULL,
    party_name character varying(100) NOT NULL,
    party_english_name character varying(200),
    party_status_code character varying(20) NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE TABLE public.party_organization (
    party_id bigint NOT NULL,
    org_subtype_code character varying(20) NOT NULL,
    corp_reg_no character(14),
    corp_formal_name character varying(200),
    corp_formal_english_name character varying(400),
    hq_country_code character(3),
    foreign_corp_reg_no_encrypted character varying(255),
    corp_type_code character varying(20),
    non_corp_type_code character varying(10),
    ownership_type_code character varying(10),
    representative_type_code character varying(10),
    establishment_date character(8),
    dissolution_date character(8),
    capital_amount bigint,
    fiscal_month smallint,
    establishment_purpose character varying(500),
    member_count integer,
    charter_url character varying(500),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_party_org_foreign_corp CHECK ((((hq_country_code = 'KOR'::bpchar) AND (foreign_corp_reg_no_encrypted IS NULL)) OR ((hq_country_code <> 'KOR'::bpchar) AND (foreign_corp_reg_no_encrypted IS NOT NULL)) OR (hq_country_code IS NULL))),
    CONSTRAINT chk_party_org_subtype CHECK (((((org_subtype_code)::text = 'CORPORATION'::text) AND (corp_reg_no IS NOT NULL) AND (corp_type_code IS NOT NULL)) OR (((org_subtype_code)::text = 'NON_CORPORATION'::text) AND (non_corp_type_code IS NOT NULL))))
);

ALTER TABLE public.party ALTER COLUMN party_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.party_party_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.party_person (
    party_id bigint NOT NULL,
    rrn_encrypted character varying(255),
    ci_value character varying(88),
    nationality_type_code character varying(20),
    nationality_code character(3),
    birth_date character(8),
    gender_code character(1),
    marital_status_code character varying(10),
    dependent_count integer,
    occupation_code character varying(10),
    occupation_name character varying(100),
    workplace_name character varying(200),
    annual_income_amount bigint,
    income_proof_code character varying(10),
    capacity_limit_type_code character varying(20),
    is_pep_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    pep_type_code character varying(10),
    pep_country_code character(3),
    pep_position character varying(200),
    death_date character(8),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_party_person_pep CHECK ((((is_pep_yn = 'T'::bpchar) AND (pep_type_code IS NOT NULL)) OR ((is_pep_yn = 'F'::bpchar) AND (pep_type_code IS NULL) AND (pep_country_code IS NULL))))
);

CREATE TABLE public.party_relation (
    relation_id bigint NOT NULL,
    from_party_id bigint NOT NULL,
    to_party_id bigint NOT NULL,
    relation_type_code character varying(10) NOT NULL,
    relation_detail_code character varying(10),
    equity_ratio_bps integer,
    representation_scope character varying(200),
    proof_url character varying(500),
    relation_start_date character(8) NOT NULL,
    relation_end_date character(8),
    relation_end_reason_code character varying(20),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    relation_review_status_code character varying(20),
    CONSTRAINT chk_party_relation_no_self CHECK ((from_party_id <> to_party_id))
);

ALTER TABLE public.party_relation ALTER COLUMN relation_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.party_relation_relation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.party_role (
    role_id bigint NOT NULL,
    party_id bigint NOT NULL,
    role_type_code character varying(20) NOT NULL,
    role_status_code character varying(20) NOT NULL,
    role_start_date character(8) NOT NULL,
    role_end_date character(8),
    role_end_reason_code character varying(20),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_party_role_end CHECK (((((role_status_code)::text = 'CLOSED'::text) AND (role_end_date IS NOT NULL) AND (role_end_reason_code IS NOT NULL)) OR ((role_status_code)::text <> 'CLOSED'::text)))
);

ALTER TABLE public.party_role ALTER COLUMN role_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.party_role_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.password_history (
    password_history_id bigint NOT NULL,
    credential_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    password_hash character varying(255) NOT NULL,
    password_change_channel_code character varying(20) NOT NULL,
    password_change_reason_code character varying(200),
    password_change_ip character varying(45),
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

ALTER TABLE public.password_history ALTER COLUMN password_history_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.password_history_password_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.pin (
    pin_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    auth_method_id bigint NOT NULL,
    device_id bigint NOT NULL,
    pin_hash character varying(255) NOT NULL,
    pin_length integer NOT NULL,
    pin_login_failure_count integer DEFAULT 0 NOT NULL,
    max_pin_login_failure_count integer DEFAULT 5 NOT NULL,
    pin_login_locked_at timestamp(3) with time zone,
    pin_login_unlocked_at timestamp(3) with time zone,
    pin_last_login_at timestamp(3) with time zone,
    pin_status_code character varying(20) NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

ALTER TABLE public.pin ALTER COLUMN pin_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pin_pin_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.qr_login_token (
    qr_token_id bigint NOT NULL,
    qr_token_hash character varying(255) NOT NULL,
    customer_id bigint,
    session_id character varying(64),
    qr_status_code character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    request_ip character varying(45) NOT NULL,
    request_channel_code character varying(20) DEFAULT 'WEB'::character varying NOT NULL,
    issued_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    expiry_at timestamp(3) with time zone NOT NULL,
    scanned_at timestamp(3) with time zone,
    approved_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_qr_login_token_status CHECK (((qr_status_code)::text = ANY ((ARRAY['PENDING'::character varying, 'SCANNED'::character varying, 'APPROVED'::character varying, 'EXPIRED'::character varying, 'CANCELLED'::character varying])::text[])))
);

ALTER TABLE public.qr_login_token ALTER COLUMN qr_token_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.qr_login_token_qr_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.registered_device (
    device_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    device_name character varying(100),
    device_type_code character varying(20) NOT NULL,
    device_os_name character varying(50),
    device_os_version character varying(50),
    device_fingerprint_hash character varying(255) NOT NULL,
    trusted_device_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    designated_pc_yn character(1) DEFAULT 'F'::bpchar NOT NULL,
    device_registered_ip character varying(45) NOT NULL,
    device_last_used_at timestamp(3) with time zone,
    device_status_code character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_registered_device_designated_pc CHECK ((designated_pc_yn = ANY (ARRAY['T'::bpchar, 'F'::bpchar]))),
    CONSTRAINT chk_registered_device_status CHECK (((device_status_code)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REVOKED'::character varying])::text[]))),
    CONSTRAINT chk_registered_device_trusted CHECK ((trusted_device_yn = ANY (ARRAY['T'::bpchar, 'F'::bpchar]))),
    CONSTRAINT chk_registered_device_type CHECK (((device_type_code)::text = ANY ((ARRAY['MOBILE'::character varying, 'PC'::character varying, 'TABLET'::character varying])::text[])))
);

ALTER TABLE public.registered_device ALTER COLUMN device_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.registered_device_device_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.sanction_screening_hit (
    sanction_screening_hit_id bigint NOT NULL,
    party_id bigint NOT NULL,
    hit_type_code character varying(30) NOT NULL,
    match_rate integer NOT NULL,
    screening_status_code character varying(20) NOT NULL,
    reviewer_employee_id bigint,
    review_comment character varying(500),
    detected_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    reviewed_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_sanction_screening_hit_rate CHECK (((match_rate >= 0) AND (match_rate <= 100)))
);

ALTER TABLE public.sanction_screening_hit ALTER COLUMN sanction_screening_hit_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.sanction_screening_hit_sanction_screening_hit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.tax_residency_info (
    tax_residency_id bigint NOT NULL,
    party_id bigint NOT NULL,
    resident_type_code character varying(20) NOT NULL,
    tax_country_code character(3),
    foreign_tin character varying(50),
    withholding_rate_bps integer,
    tax_residency_confirm_date character(8) NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

ALTER TABLE public.tax_residency_info ALTER COLUMN tax_residency_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tax_residency_info_tax_residency_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.withdrawal_account (
    withdrawal_account_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    account_number character varying(50) NOT NULL,
    bank_code character varying(10) NOT NULL,
    bank_name character varying(50) NOT NULL,
    account_holder_name character varying(100),
    account_alias character varying(100),
    registration_type character varying(20) DEFAULT 'ONLINE'::character varying NOT NULL,
    priority_order smallint DEFAULT 0 NOT NULL,
    registered_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

ALTER TABLE public.withdrawal_account ALTER COLUMN withdrawal_account_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.withdrawal_account_withdrawal_account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE ONLY public.api_token
    ADD CONSTRAINT pk_api_token PRIMARY KEY (token_id);

ALTER TABLE ONLY public.auth_method
    ADD CONSTRAINT pk_auth_method PRIMARY KEY (auth_method_id);

ALTER TABLE ONLY public.business_info
    ADD CONSTRAINT pk_business_info PRIMARY KEY (business_info_id);

ALTER TABLE ONLY public.certificate
    ADD CONSTRAINT pk_certificate PRIMARY KEY (certificate_id);

ALTER TABLE ONLY public.certificate_use
    ADD CONSTRAINT pk_certificate_use PRIMARY KEY (certificate_use_id);

ALTER TABLE ONLY public.compliance_info
    ADD CONSTRAINT pk_compliance_info PRIMARY KEY (party_id);

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT pk_credential PRIMARY KEY (credential_id);

ALTER TABLE ONLY public.cust_code_master
    ADD CONSTRAINT pk_cust_code_master PRIMARY KEY (code_group_id, code_value);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT pk_customer PRIMARY KEY (customer_id);

ALTER TABLE ONLY public.customer_access_log
    ADD CONSTRAINT pk_customer_access_log PRIMARY KEY (customer_access_log_id);

ALTER TABLE ONLY public.customer_grade_history
    ADD CONSTRAINT pk_customer_grade_history PRIMARY KEY (customer_grade_history_id);

ALTER TABLE ONLY public.customer_status_history
    ADD CONSTRAINT pk_customer_status_history PRIMARY KEY (customer_status_history_id);

ALTER TABLE ONLY public.duplicate_review_case
    ADD CONSTRAINT pk_duplicate_review_case PRIMARY KEY (duplicate_review_case_id);

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT pk_employee PRIMARY KEY (employee_id);

ALTER TABLE ONLY public.fds_detection
    ADD CONSTRAINT pk_fds_detection PRIMARY KEY (fds_detection_id);

ALTER TABLE ONLY public.fds_incident
    ADD CONSTRAINT pk_fds_incident PRIMARY KEY (fds_incident_id);

ALTER TABLE ONLY public.fds_rule
    ADD CONSTRAINT pk_fds_rule PRIMARY KEY (fds_rule_id);

ALTER TABLE ONLY public.foreigner_info
    ADD CONSTRAINT pk_foreigner_info PRIMARY KEY (party_id);

ALTER TABLE ONLY public.identity_verification
    ADD CONSTRAINT pk_identity_verification PRIMARY KEY (identity_verification_id);

ALTER TABLE ONLY public.login_attempt
    ADD CONSTRAINT pk_login_attempt PRIMARY KEY (login_attempt_id);

ALTER TABLE ONLY public.login_session
    ADD CONSTRAINT pk_login_session PRIMARY KEY (session_id);

ALTER TABLE ONLY public.mobile_auth
    ADD CONSTRAINT pk_mobile_auth PRIMARY KEY (mobile_auth_id);

ALTER TABLE ONLY public.party
    ADD CONSTRAINT pk_party PRIMARY KEY (party_id);

ALTER TABLE ONLY public.party_organization
    ADD CONSTRAINT pk_party_organization PRIMARY KEY (party_id);

ALTER TABLE ONLY public.party_person
    ADD CONSTRAINT pk_party_person PRIMARY KEY (party_id);

ALTER TABLE ONLY public.party_relation
    ADD CONSTRAINT pk_party_relation PRIMARY KEY (relation_id);

ALTER TABLE ONLY public.party_role
    ADD CONSTRAINT pk_party_role PRIMARY KEY (role_id);

ALTER TABLE ONLY public.password_history
    ADD CONSTRAINT pk_password_history PRIMARY KEY (password_history_id);

ALTER TABLE ONLY public.pin
    ADD CONSTRAINT pk_pin PRIMARY KEY (pin_id);

ALTER TABLE ONLY public.qr_login_token
    ADD CONSTRAINT pk_qr_login_token PRIMARY KEY (qr_token_id);

ALTER TABLE ONLY public.registered_device
    ADD CONSTRAINT pk_registered_device PRIMARY KEY (device_id);

ALTER TABLE ONLY public.sanction_screening_hit
    ADD CONSTRAINT pk_sanction_screening_hit PRIMARY KEY (sanction_screening_hit_id);

ALTER TABLE ONLY public.tax_residency_info
    ADD CONSTRAINT pk_tax_residency_info PRIMARY KEY (tax_residency_id);

ALTER TABLE ONLY public.withdrawal_account
    ADD CONSTRAINT pk_withdrawal_account PRIMARY KEY (withdrawal_account_id);

ALTER TABLE ONLY public.business_info
    ADD CONSTRAINT uq_business_info_biz_reg_no UNIQUE (biz_reg_no);

ALTER TABLE ONLY public.certificate
    ADD CONSTRAINT uq_certificate_serial_number UNIQUE (certificate_serial_number);

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT uq_employee_party UNIQUE (party_id);

ALTER TABLE ONLY public.qr_login_token
    ADD CONSTRAINT uq_qr_login_token_hash UNIQUE (qr_token_hash);

CREATE INDEX idx_business_info_party ON public.business_info USING btree (party_id) WHERE (deleted_at IS NULL);

CREATE INDEX idx_certificate_use_cert_at ON public.certificate_use USING btree (certificate_id, certificate_used_at DESC);

CREATE INDEX idx_customer_access_log_accessor ON public.customer_access_log USING btree (accessor_employee_id);

CREATE INDEX idx_customer_access_log_at ON public.customer_access_log USING btree (accessed_at DESC);

CREATE INDEX idx_customer_access_log_target ON public.customer_access_log USING btree (target_customer_id);

CREATE INDEX idx_duplicate_review_case_status ON public.duplicate_review_case USING btree (review_status_code) WHERE (deleted_at IS NULL);

CREATE INDEX idx_employee_party ON public.employee USING btree (party_id);

CREATE INDEX idx_fds_detection_customer_at ON public.fds_detection USING btree (customer_id, fds_detected_at DESC);

CREATE INDEX idx_login_attempt_customer_at ON public.login_attempt USING btree (customer_id, login_attempted_at DESC);

CREATE INDEX idx_login_session_customer_expiry ON public.login_session USING btree (customer_id, session_expiry_at) WHERE (deleted_at IS NULL);

CREATE INDEX idx_party_relation_from ON public.party_relation USING btree (from_party_id, relation_type_code);

CREATE INDEX idx_party_relation_to ON public.party_relation USING btree (to_party_id, relation_type_code);

CREATE INDEX idx_party_role_active ON public.party_role USING btree (party_id, role_status_code) WHERE (((role_status_code)::text = 'ACTIVE'::text) AND (deleted_at IS NULL));

CREATE INDEX idx_password_history_credential ON public.password_history USING btree (credential_id, created_at DESC);

CREATE INDEX idx_qr_login_token_expiry ON public.qr_login_token USING btree (expiry_at) WHERE (((qr_status_code)::text = 'PENDING'::text) AND (deleted_at IS NULL));

CREATE INDEX idx_qr_login_token_hash_status ON public.qr_login_token USING btree (qr_token_hash, qr_status_code) WHERE (deleted_at IS NULL);

CREATE INDEX idx_registered_device_customer ON public.registered_device USING btree (customer_id) WHERE (deleted_at IS NULL);

CREATE INDEX idx_sanction_screening_hit_status ON public.sanction_screening_hit USING btree (screening_status_code) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_credential_active_login_id ON public.credential USING btree (login_id) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uq_customer_active_per_party ON public.customer USING btree (party_id) WHERE (((customer_status_code)::text <> 'CLOSED'::text) AND (deleted_at IS NULL));

CREATE UNIQUE INDEX uq_party_person_ci ON public.party_person USING btree (ci_value) WHERE ((ci_value IS NOT NULL) AND (deleted_at IS NULL));

CREATE UNIQUE INDEX uq_party_relation_active ON public.party_relation USING btree (from_party_id, to_party_id, relation_type_code) WHERE ((relation_end_date IS NULL) AND (deleted_at IS NULL));

CREATE UNIQUE INDEX uq_withdrawal_account_active ON public.withdrawal_account USING btree (customer_id, account_number) WHERE (deleted_at IS NULL);

ALTER TABLE ONLY public.api_token
    ADD CONSTRAINT fk_api_token_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.api_token
    ADD CONSTRAINT fk_api_token_session FOREIGN KEY (session_id) REFERENCES public.login_session(session_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.auth_method
    ADD CONSTRAINT fk_auth_method_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.business_info
    ADD CONSTRAINT fk_business_info_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.certificate
    ADD CONSTRAINT fk_certificate_auth_method FOREIGN KEY (auth_method_id) REFERENCES public.auth_method(auth_method_id);

ALTER TABLE ONLY public.certificate
    ADD CONSTRAINT fk_certificate_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.certificate_use
    ADD CONSTRAINT fk_certificate_use_certificate FOREIGN KEY (certificate_id) REFERENCES public.certificate(certificate_id);

ALTER TABLE ONLY public.certificate_use
    ADD CONSTRAINT fk_certificate_use_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.compliance_info
    ADD CONSTRAINT fk_compliance_info_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT fk_credential_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.customer_grade_history
    ADD CONSTRAINT fk_customer_grade_history_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.customer_grade_history
    ADD CONSTRAINT fk_customer_grade_history_self FOREIGN KEY (previous_customer_grade_history_id) REFERENCES public.customer_grade_history(customer_grade_history_id);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT fk_customer_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.customer_status_history
    ADD CONSTRAINT fk_customer_status_history_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.customer_status_history
    ADD CONSTRAINT fk_customer_status_history_self FOREIGN KEY (previous_customer_status_history_id) REFERENCES public.customer_status_history(customer_status_history_id);

ALTER TABLE ONLY public.duplicate_review_case
    ADD CONSTRAINT fk_duplicate_review_case_existing FOREIGN KEY (existing_party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.duplicate_review_case
    ADD CONSTRAINT fk_duplicate_review_case_new FOREIGN KEY (new_party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT fk_employee_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.fds_detection
    ADD CONSTRAINT fk_fds_detection_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.fds_detection
    ADD CONSTRAINT fk_fds_detection_rule FOREIGN KEY (fds_rule_id) REFERENCES public.fds_rule(fds_rule_id);

ALTER TABLE ONLY public.fds_incident
    ADD CONSTRAINT fk_fds_incident_detection FOREIGN KEY (fds_detection_id) REFERENCES public.fds_detection(fds_detection_id);

ALTER TABLE ONLY public.foreigner_info
    ADD CONSTRAINT fk_foreigner_info_party_person FOREIGN KEY (party_id) REFERENCES public.party_person(party_id);

ALTER TABLE ONLY public.identity_verification
    ADD CONSTRAINT fk_identity_verification_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.identity_verification
    ADD CONSTRAINT fk_identity_verification_mobile_auth FOREIGN KEY (mobile_auth_id) REFERENCES public.mobile_auth(mobile_auth_id);

ALTER TABLE ONLY public.login_attempt
    ADD CONSTRAINT fk_login_attempt_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.login_attempt
    ADD CONSTRAINT fk_login_attempt_device FOREIGN KEY (device_id) REFERENCES public.registered_device(device_id);

ALTER TABLE ONLY public.login_session
    ADD CONSTRAINT fk_login_session_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.login_session
    ADD CONSTRAINT fk_login_session_device FOREIGN KEY (device_id) REFERENCES public.registered_device(device_id);

ALTER TABLE ONLY public.login_session
    ADD CONSTRAINT fk_login_session_login_attempt FOREIGN KEY (login_attempt_id) REFERENCES public.login_attempt(login_attempt_id);

ALTER TABLE ONLY public.login_session
    ADD CONSTRAINT fk_login_session_token FOREIGN KEY (token_id) REFERENCES public.api_token(token_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.mobile_auth
    ADD CONSTRAINT fk_mobile_auth_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.party_organization
    ADD CONSTRAINT fk_party_organization_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.party_person
    ADD CONSTRAINT fk_party_person_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.party_relation
    ADD CONSTRAINT fk_party_relation_from FOREIGN KEY (from_party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.party_relation
    ADD CONSTRAINT fk_party_relation_to FOREIGN KEY (to_party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.party_role
    ADD CONSTRAINT fk_party_role_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.password_history
    ADD CONSTRAINT fk_password_history_credential FOREIGN KEY (credential_id) REFERENCES public.credential(credential_id);

ALTER TABLE ONLY public.password_history
    ADD CONSTRAINT fk_password_history_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.pin
    ADD CONSTRAINT fk_pin_auth_method FOREIGN KEY (auth_method_id) REFERENCES public.auth_method(auth_method_id);

ALTER TABLE ONLY public.pin
    ADD CONSTRAINT fk_pin_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.pin
    ADD CONSTRAINT fk_pin_device FOREIGN KEY (device_id) REFERENCES public.registered_device(device_id);

ALTER TABLE ONLY public.qr_login_token
    ADD CONSTRAINT fk_qr_login_token_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.qr_login_token
    ADD CONSTRAINT fk_qr_login_token_session FOREIGN KEY (session_id) REFERENCES public.login_session(session_id);

ALTER TABLE ONLY public.registered_device
    ADD CONSTRAINT fk_registered_device_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.sanction_screening_hit
    ADD CONSTRAINT fk_sanction_screening_hit_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.tax_residency_info
    ADD CONSTRAINT fk_tax_residency_info_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.withdrawal_account
    ADD CONSTRAINT fk_withdrawal_account_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);


-- ============================================================================
-- SERVICE: deposit-service + consultation-service
-- DATABASE: deposit_db
-- ============================================================================
\connect deposit_db

CREATE TABLE public.banking_deposit_product_interest_rates (
    rate_id bigint NOT NULL,
    banking_product_id bigint NOT NULL,
    rate_type character varying(30) NOT NULL,
    minimum_contract_period integer,
    maximum_contract_period integer,
    minimum_join_amount numeric(18,2),
    maximum_join_amount numeric(18,2),
    rate numeric(5,2) NOT NULL,
    condition_description text,
    effective_start_date character(8) NOT NULL,
    effective_end_date character(8),
    is_active boolean DEFAULT true NOT NULL,
    status character varying(20),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100),
    effective_date date,
    expiry_date date
);

CREATE SEQUENCE public.banking_deposit_product_interest_rates_rate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.banking_deposit_product_interest_rates_rate_id_seq OWNED BY public.banking_deposit_product_interest_rates.rate_id;

CREATE TABLE public.banking_deposit_product_join_channels (
    channel_id bigint NOT NULL,
    banking_product_id bigint NOT NULL,
    join_channel_code character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.banking_deposit_product_join_channels_channel_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.banking_deposit_product_join_channels_channel_id_seq OWNED BY public.banking_deposit_product_join_channels.channel_id;

CREATE TABLE public.banking_deposit_product_special_terms (
    deposit_product_special_term_id bigint NOT NULL,
    banking_product_id bigint NOT NULL,
    special_term_id bigint NOT NULL,
    is_required boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.banking_deposit_product_speci_deposit_product_special_term__seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.banking_deposit_product_speci_deposit_product_special_term__seq OWNED BY public.banking_deposit_product_special_terms.deposit_product_special_term_id;

CREATE TABLE public.banking_deposit_product_target_groups (
    banking_product_id bigint NOT NULL,
    target_group_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE TABLE public.banking_deposit_products (
    deposit_product_id bigint NOT NULL,
    banking_product_id bigint NOT NULL,
    deposit_type character varying(20) NOT NULL,
    is_compound_interest boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.banking_deposit_products_deposit_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.banking_deposit_products_deposit_product_id_seq OWNED BY public.banking_deposit_products.deposit_product_id;

CREATE TABLE public.chat_consultation (
    chat_consultation_id bigint NOT NULL,
    consultation_id bigint NOT NULL,
    chatbot_consultation_id bigint,
    employee_id bigint,
    agent_requested_at timestamp with time zone,
    agent_connected_at timestamp with time zone,
    waiting_seconds integer,
    waiting_abandoned_yn character varying(1) NOT NULL,
    waiting_abandoned_at timestamp with time zone,
    chat_started_at timestamp with time zone,
    chat_ended_at timestamp with time zone,
    chat_seconds integer,
    concurrent_chat_count integer,
    reassignment_count integer NOT NULL,
    total_turn_count integer NOT NULL,
    end_type_code_id bigint,
    agent_talk_seconds integer,
    satisfaction_score integer,
    active_yn character varying(1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE SEQUENCE public.chat_consultation_chat_consultation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.chat_consultation_chat_consultation_id_seq OWNED BY public.chat_consultation.chat_consultation_id;

CREATE TABLE public.chat_message_history (
    chat_message_history_id bigint NOT NULL,
    chat_consultation_id bigint,
    chatbot_consultation_id bigint,
    node_id bigint,
    sequence_no integer NOT NULL,
    sender_type_code_id bigint,
    message_type_code_id bigint,
    message_content text NOT NULL,
    button_value character varying(100),
    confidence_score integer,
    process_method_code_id bigint,
    response_time_ms integer,
    sentiment_result_code_id bigint,
    error_type_code_id bigint,
    read_yn character varying(1) NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE SEQUENCE public.chat_message_history_chat_message_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.chat_message_history_chat_message_history_id_seq OWNED BY public.chat_message_history.chat_message_history_id;

CREATE TABLE public.chatbot_consultation (
    chatbot_consultation_id bigint NOT NULL,
    consultation_id bigint NOT NULL,
    scenario_id bigint,
    intent_id bigint,
    process_method_code_id bigint,
    initial_intent character varying(100),
    entry_screen character varying(50),
    app_version character varying(20),
    session_started_at timestamp with time zone DEFAULT now(),
    session_ended_at timestamp with time zone,
    total_turn_count integer NOT NULL,
    resolved_yn character varying(1) NOT NULL,
    agent_connected_yn character varying(1) NOT NULL,
    end_type_code_id bigint,
    error_occurred_yn character varying(1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE SEQUENCE public.chatbot_consultation_chatbot_consultation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.chatbot_consultation_chatbot_consultation_id_seq OWNED BY public.chatbot_consultation.chatbot_consultation_id;

CREATE TABLE public.chatbot_intent (
    intent_id bigint NOT NULL,
    fallback_intent_id bigint,
    scenario_id bigint,
    intent_name character varying(100) NOT NULL,
    intent_desc character varying(500),
    process_method_code_id bigint,
    confidence_threshold integer,
    priority integer,
    test_yn character varying(1) NOT NULL,
    active_yn character varying(1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE SEQUENCE public.chatbot_intent_intent_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.chatbot_intent_intent_id_seq OWNED BY public.chatbot_intent.intent_id;

CREATE TABLE public.chatbot_node (
    node_id bigint NOT NULL,
    next_node_id bigint,
    scenario_id bigint NOT NULL,
    node_type_code_id bigint,
    node_name character varying(100) NOT NULL,
    response_message text NOT NULL,
    condition_expression text,
    error_move_node_id bigint,
    timeout_seconds integer,
    sort_order integer NOT NULL,
    exposure_count integer,
    active_yn character varying(1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE TABLE public.chatbot_node_button (
    id bigint NOT NULL,
    node_id bigint NOT NULL,
    button_text character varying(50) NOT NULL,
    button_value character varying(20) NOT NULL,
    sort_order integer NOT NULL,
    active_yn character varying(1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE SEQUENCE public.chatbot_node_button_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.chatbot_node_button_id_seq OWNED BY public.chatbot_node_button.id;

CREATE TABLE public.chatbot_node_flow (
    current_node_id bigint NOT NULL,
    next_node_id bigint NOT NULL,
    sort_order integer NOT NULL,
    chatbot_flow_type_cd character varying(20) NOT NULL,
    branch_criteria_cd character varying(20),
    branch_value character varying(50),
    active_yn character varying(1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE SEQUENCE public.chatbot_node_node_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.chatbot_node_node_id_seq OWNED BY public.chatbot_node.node_id;

CREATE TABLE public.chatbot_scenario (
    scenario_id bigint NOT NULL,
    scenario_name character varying(100) NOT NULL,
    scenario_desc character varying(500),
    scenario_type_code_id bigint,
    consultation_category_code_id bigint,
    reception_channel_code_id bigint,
    test_yn character varying(1) NOT NULL,
    active_yn character varying(1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE SEQUENCE public.chatbot_scenario_scenario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.chatbot_scenario_scenario_id_seq OWNED BY public.chatbot_scenario.scenario_id;

CREATE TABLE public.common_account (
    account_id bigint NOT NULL,
    account_no character varying(30),
    customer_id bigint NOT NULL,
    customer_no character varying(30),
    contract_id bigint,
    account_type_cd character varying(30),
    bank_cd character varying(10),
    account_nickname character varying(100),
    balance bigint,
    currency_cd character(3),
    account_password_hash character varying(255),
    daily_withdrawal_limit bigint,
    daily_withdrawal_count integer,
    suspic_account_yn character(1),
    account_status character varying(20),
    account_opened_at character(8),
    account_closed_at character(8),
    account_cancel_at timestamp(3) with time zone,
    last_transaction_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone,
    created_by bigint,
    updated_at timestamp(3) with time zone,
    updated_by bigint
);

CREATE SEQUENCE public.common_account_account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_account_account_id_seq OWNED BY public.common_account.account_id;

CREATE TABLE public.common_contract (
    contract_id bigint NOT NULL,
    contract_no character varying(50),
    customer_id bigint NOT NULL,
    customer_no character varying(30),
    product_id bigint NOT NULL,
    biz_div_cd character varying(10),
    contract_amount bigint,
    rate_type_cd character varying(10),
    base_rate_bps character varying(10),
    spread_bps integer,
    preferential_bps integer,
    total_rate_bps integer,
    interest_amount_at_maturity bigint,
    contract integer,
    contract_start_date character(8),
    contract_end_date character(8),
    contract_cancel_date character(8),
    contract_cancel_reason character varying(200),
    auto_transfer_yn character(1),
    auto_transfer_day integer,
    signed_at timestamp(3) with time zone,
    contract_channel_cd character varying(20),
    spot_id bigint,
    spot_name character varying(100),
    manager_id bigint,
    manager_name character varying(100),
    proxy_yn character(1),
    contract_status character varying(20),
    term_url character varying(500),
    term_hash character varying(64),
    contract_url character varying(500),
    contract_hash character varying(64),
    created_at timestamp(3) with time zone,
    created_by bigint,
    updated_at timestamp(3) with time zone,
    updated_by bigint
);

CREATE SEQUENCE public.common_contract_contract_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_contract_contract_id_seq OWNED BY public.common_contract.contract_id;

CREATE TABLE public.common_product (
    product_id bigint NOT NULL,
    product_cd character varying(30) NOT NULL,
    biz_div_cd character varying(10) NOT NULL,
    product_name character varying(200) NOT NULL,
    product_type_cd character varying(20),
    product_description text,
    target_type_cd character varying(50),
    channel_cd character varying(50),
    currency_cd character varying(50),
    policy_product_yn character(1),
    min_amount bigint,
    max_amount bigint,
    min_period_mo integer,
    max_period_mo integer,
    sale_yn character(1),
    sale_start_date character(8),
    sale_end_date character(8),
    product_brochure_url character varying(500),
    financial_consumer_act_yn character(1),
    product_status character varying(50),
    created_by bigint,
    created_at timestamp(3) with time zone,
    updated_by bigint,
    updated_at timestamp(3) with time zone
);

CREATE SEQUENCE public.common_product_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_product_product_id_seq OWNED BY public.common_product.product_id;

CREATE TABLE public.common_terms_consent (
    consent_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    terms_template_id bigint NOT NULL,
    biz_div_cd character varying(10) NOT NULL,
    consent_target_id bigint,
    consent_status_cd character varying(10) NOT NULL,
    agreed_yn character(1) NOT NULL,
    agreed_at character(8) NOT NULL,
    consent_method_cd character varying(10) NOT NULL,
    consent_tool character varying(500),
    signed_doc_url character varying(500),
    signed_doc_hash character varying(64),
    client_ip inet,
    withdrawn_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    withdrawn_at timestamp(3) with time zone,
    withdrawn_reason character varying(500),
    retention_until character varying(8),
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.common_terms_consent_consent_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_terms_consent_consent_id_seq OWNED BY public.common_terms_consent.consent_id;

CREATE TABLE public.common_terms_template (
    terms_template_id bigint NOT NULL,
    terms_no character varying(50) NOT NULL,
    terms_name character varying(200) NOT NULL,
    terms_category_cd character varying(10) NOT NULL,
    description text,
    required_yn character(1) DEFAULT 'Y'::bpchar NOT NULL,
    biz_div_cd character varying(50) NOT NULL,
    active_yn character(1) DEFAULT 'Y'::bpchar NOT NULL,
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.common_terms_template_terms_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_terms_template_terms_template_id_seq OWNED BY public.common_terms_template.terms_template_id;

CREATE TABLE public.common_transaction (
    transaction_id bigint NOT NULL,
    transaction_no character varying(50),
    account_id bigint,
    contract_id bigint,
    transaction_type_cd character varying(30),
    debit_credit_type character varying(10),
    transaction_amount bigint,
    balance_before bigint,
    balance_after bigint,
    fee_amount bigint,
    channel_cd character varying(30),
    counterparty_bank_cd character varying(10),
    counterparty_bank_name character varying(100),
    counterparty_account_no character varying(30),
    counterparty_name character varying(100),
    counterparty_customer_id bigint,
    counterparty_account_id bigint,
    counterparty_name_verified_yn character(1),
    original_transaction_id bigint NOT NULL,
    transaction_memo character varying(255),
    transaction_status character varying(20),
    transacted_at timestamp(3) with time zone,
    currency_cd character(3),
    available_balance bigint,
    transaction_summary character varying(100),
    transfer_type_cd character varying(30),
    transfer_requested_at timestamp(3) with time zone,
    transfer_completed_at timestamp(3) with time zone,
    transfer_failed_yn character(1),
    payment_method_code character varying(30),
    card_payment_yn character(1),
    payment_failed_yn character(1),
    merchant_no character varying(50),
    merchant_name character varying(100),
    failure_type_cd character varying(30),
    failure_reason_cd character varying(50),
    failure_cause_cd character varying(50),
    failed_at timestamp(3) with time zone,
    retry_count integer,
    approval_no character varying(50),
    external_transaction_no character varying(100),
    terminal_id character varying(50),
    client_ip character varying(45),
    transaction_location character varying(100),
    ledger_posted_at timestamp(3) with time zone,
    cancelled_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone,
    created_by bigint,
    updated_at timestamp(3) with time zone,
    updated_by bigint
);

CREATE SEQUENCE public.common_transaction_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_transaction_transaction_id_seq OWNED BY public.common_transaction.transaction_id;

CREATE TABLE public.consultation (
    consultation_id bigint NOT NULL,
    customer_no character varying(30) NOT NULL,
    reception_method_code_id bigint,
    inquiry_type_code_id bigint,
    reception_channel_code_id bigint,
    content_summary character varying(200),
    status_code_id bigint,
    answer_summary character varying(200),
    consulted_at timestamp with time zone DEFAULT now(),
    completed_at timestamp with time zone,
    previous_consultation_id bigint,
    active_yn character varying(1) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp with time zone DEFAULT now(),
    updated_by bigint
);

CREATE SEQUENCE public.consultation_consultation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.consultation_consultation_id_seq OWNED BY public.consultation.consultation_id;

CREATE TABLE public.customer (
    customer_id bigint NOT NULL,
    party_id bigint NOT NULL,
    customer_grade_code character varying(10),
    customer_status_code character varying(20) NOT NULL,
    main_customer_yn character(1),
    credit_rating_code character varying(10),
    credit_evaluation_date character(8),
    credit_agency_code character varying(10),
    preferred_language_code character(2),
    sms_receive_yn character(1),
    email_receive_yn character(1),
    postal_receive_yn character(1),
    notification_method_code character varying(10),
    email character varying(255),
    phone character varying(20),
    zip_code character varying(10),
    address character varying(255),
    address_detail character varying(255),
    join_channel_code character varying(20),
    first_join_date character(8),
    joined_at timestamp(3) with time zone NOT NULL,
    last_transaction_at timestamp(3) with time zone,
    dormant_datetime timestamp(3) with time zone,
    closed_at timestamp(3) with time zone,
    close_reason_code character varying(20),
    privacy_expiry_date character(8),
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.customer_customer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.customer_customer_id_seq OWNED BY public.customer.customer_id;

CREATE TABLE public.deposit_account (
    account_id bigint NOT NULL,
    saving_type character varying(20),
    balance numeric(18,2),
    total_paid_amount numeric(18,2),
    total_interest_amount numeric(18,2),
    last_transaction_at character(8),
    last_interest_paid_at character(8),
    is_withdrawable character(1),
    daily_withdraw_limit numeric(18,2),
    daily_withdraw_count_limit integer,
    atm_withdraw_limit numeric(18,2),
    is_online_banking_enabled character(1),
    is_mobile_banking_enabled character(1),
    is_phone_banking_enabled character(1),
    dormant_at character(8),
    dormant_released_at character(8),
    status_changed_at character(8)
);

CREATE SEQUENCE public.deposit_account_number_seq
    START WITH 100000000001
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.deposit_accounts (
    account_id bigint NOT NULL,
    account_number character varying(30) NOT NULL,
    customer_id character varying(30) NOT NULL,
    contract_id bigint NOT NULL,
    account_type character varying(30) NOT NULL,
    saving_type character varying(20),
    bank_code character varying(10) DEFAULT '001'::character varying NOT NULL,
    account_alias character varying(100),
    balance numeric(18,2) DEFAULT 0 NOT NULL,
    total_paid_amount numeric(18,2) DEFAULT 0 NOT NULL,
    total_interest_amount numeric(18,2) DEFAULT 0 NOT NULL,
    last_transaction_at timestamp with time zone,
    last_interest_paid_at timestamp with time zone,
    currency character(3) DEFAULT 'KRW'::bpchar NOT NULL,
    account_password character varying(255) NOT NULL,
    daily_withdraw_limit numeric(18,2),
    daily_withdraw_count_limit integer,
    atm_withdraw_limit numeric(18,2),
    is_withdrawable boolean DEFAULT true NOT NULL,
    is_online_banking_enabled boolean DEFAULT false NOT NULL,
    is_mobile_banking_enabled boolean DEFAULT false NOT NULL,
    is_phone_banking_enabled boolean DEFAULT false NOT NULL,
    account_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    opened_at date NOT NULL,
    maturity_at date,
    dormant_at date,
    dormant_released_at date,
    closed_at date,
    status_changed_at date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100),
    version bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.deposit_accounts_account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_accounts_account_id_seq OWNED BY public.deposit_accounts.account_id;

CREATE TABLE public.deposit_banking_products (
    banking_product_id bigint NOT NULL,
    deposit_product_type character varying(30) NOT NULL,
    deposit_product_name character varying(200) NOT NULL,
    description text,
    department_id bigint,
    base_interest_rate numeric(5,2) DEFAULT 0 NOT NULL,
    preferential_rate_condition text,
    min_join_amount numeric(18,2),
    max_join_amount numeric(18,2),
    min_period_month integer,
    max_period_month integer,
    is_early_termination_allowed boolean DEFAULT false NOT NULL,
    is_tax_benefit_available boolean DEFAULT false NOT NULL,
    is_auto_renewal_available boolean DEFAULT false NOT NULL,
    is_passbook_issued boolean DEFAULT false NOT NULL,
    released_at character(8),
    ended_at character(8),
    deposit_product_status character varying(20) DEFAULT 'SELLING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100),
    max_interest_rate numeric(5,2),
    promotion_end_date date
);

CREATE SEQUENCE public.deposit_banking_products_banking_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_banking_products_banking_product_id_seq OWNED BY public.deposit_banking_products.banking_product_id;

CREATE TABLE public.deposit_contract (
    contract_id bigint NOT NULL,
    is_monthly_payment character(1),
    payment_count_total integer,
    contract_interest_rate numeric(5,2),
    total_preferential_rate numeric(5,2),
    final_interest_rate numeric(5,2),
    tax_benefit_type character varying(30),
    applied_tax_rate numeric(5,2),
    expected_interest_amount numeric(18,2),
    is_auto_renewal character(1),
    status_changed_at character(8),
    "계약 지점 코드" character varying(20),
    is_power_of_attorney_verified character(1),
    power_of_attorney_file_url character varying(500)
);

CREATE TABLE public.deposit_contract_applied_rates (
    applied_rate_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    rate_id bigint,
    applied_rate numeric(5,2) NOT NULL,
    condition_verified_yn boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.deposit_contract_applied_rates_applied_rate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_contract_applied_rates_applied_rate_id_seq OWNED BY public.deposit_contract_applied_rates.applied_rate_id;

CREATE TABLE public.deposit_contract_special_term_agreements (
    special_agreement_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    special_term_id bigint NOT NULL,
    is_agreed boolean NOT NULL,
    agreed_at character(8),
    agreement_ip_address character varying(45),
    agreement_device_info character varying(255),
    is_electronic_signed boolean DEFAULT false NOT NULL,
    is_agreement_withdrawn boolean DEFAULT false NOT NULL,
    agreement_withdrawn_at character(8),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.deposit_contract_special_term_agreemen_special_agreement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_contract_special_term_agreemen_special_agreement_id_seq OWNED BY public.deposit_contract_special_term_agreements.special_agreement_id;

CREATE TABLE public.deposit_contracts (
    contract_id bigint NOT NULL,
    contract_number character varying(50) NOT NULL,
    customer_id character varying(30) NOT NULL,
    banking_product_id bigint NOT NULL,
    is_monthly_payment boolean DEFAULT false NOT NULL,
    payment_count_total integer,
    monthly_payment_day character varying(6),
    join_amount numeric(18,2) NOT NULL,
    contract_interest_rate numeric(5,2) NOT NULL,
    total_preferential_rate numeric(5,2) DEFAULT 0 NOT NULL,
    final_interest_rate numeric(5,2) NOT NULL,
    tax_benefit_type character varying(30) DEFAULT 'GENERAL'::character varying NOT NULL,
    applied_tax_rate numeric(5,2) DEFAULT 15.40 NOT NULL,
    expected_interest_amount numeric(18,2),
    contract_period_month integer NOT NULL,
    started_at date NOT NULL,
    maturity_at date,
    terminated_at date,
    termination_reason character varying(200),
    is_auto_renewal boolean DEFAULT false NOT NULL,
    auto_transfer_enabled boolean DEFAULT false NOT NULL,
    auto_transfer_day integer,
    contract_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    status_changed_at date,
    join_channel character varying(20) NOT NULL,
    branch_id bigint,
    branch_code character varying(20),
    branch_name character varying(100),
    manager_id bigint,
    manager_name character varying(100),
    is_proxy_joined boolean DEFAULT false NOT NULL,
    is_power_of_attorney_verified boolean DEFAULT false NOT NULL,
    power_of_attorney_file_url character varying(500),
    terms_file_url character varying(500),
    contract_file_url character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100),
    consecutive_miss_count integer DEFAULT 0 NOT NULL,
    source_account_id bigint
);

CREATE SEQUENCE public.deposit_contracts_contract_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_contracts_contract_id_seq OWNED BY public.deposit_contracts.contract_id;

CREATE TABLE public.deposit_departments (
    department_id bigint NOT NULL,
    department_code character varying(50) NOT NULL,
    department_name character varying(100) NOT NULL,
    parent_department_id bigint,
    department_type character varying(30),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.deposit_departments_department_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_departments_department_id_seq OWNED BY public.deposit_departments.department_id;

CREATE TABLE public.deposit_interest_history (
    interest_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    account_id bigint NOT NULL,
    applied_interest_rate numeric(5,2) NOT NULL,
    interest_calculation_start_date character(8),
    interest_calculation_end_date character(8),
    interest_occurred_at timestamp with time zone,
    interest_amount numeric(18,2) NOT NULL,
    tax_benefit_type character varying(30) NOT NULL,
    applied_tax_rate numeric(6,4) NOT NULL,
    interest_before_tax numeric(18,2) NOT NULL,
    interest_tax_amount numeric(18,2) DEFAULT 0 NOT NULL,
    local_income_tax_amount numeric(18,2) DEFAULT 0 NOT NULL,
    interest_after_tax numeric(18,2) NOT NULL,
    interest_reason character varying(30) NOT NULL,
    interest_paid_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.deposit_interest_history_interest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_interest_history_interest_id_seq OWNED BY public.deposit_interest_history.interest_id;

CREATE TABLE public.deposit_payment_schedules (
    schedule_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    account_id bigint NOT NULL,
    payment_round integer NOT NULL,
    scheduled_date date NOT NULL,
    scheduled_amount numeric(18,2) NOT NULL,
    is_auto_transfer boolean DEFAULT false NOT NULL,
    source_account_id bigint,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    paid_at timestamp(3) with time zone,
    actual_amount numeric(18,2),
    transaction_id bigint,
    failure_reason_code character varying(50),
    created_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    updated_at timestamp(3) with time zone
);

CREATE SEQUENCE public.deposit_payment_schedules_schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_payment_schedules_schedule_id_seq OWNED BY public.deposit_payment_schedules.schedule_id;

CREATE TABLE public.deposit_product (
    product_id bigint NOT NULL,
    doduct_tyeposit_prpe character varying(30),
    department_id bigint,
    base_interest_rate numeric(5,2),
    preferential_rate_condition text,
    is_early_termination_allowed character(1),
    is_tax_benefit_available character(1),
    is_auto_renewal_available character(1),
    is_passbook_issued character(1)
);

CREATE TABLE public.deposit_savings_products (
    savings_product_id bigint NOT NULL,
    banking_product_id bigint NOT NULL,
    saving_type character varying(20) NOT NULL,
    monthly_payment_min_amount numeric(18,2),
    monthly_payment_max_amount numeric(18,2),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.deposit_savings_products_savings_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_savings_products_savings_product_id_seq OWNED BY public.deposit_savings_products.savings_product_id;

CREATE TABLE public.deposit_special_terms (
    special_term_id bigint NOT NULL,
    special_term_name character varying(200) NOT NULL,
    special_term_content text NOT NULL,
    special_term_summary text,
    is_required boolean DEFAULT false NOT NULL,
    is_electronic_agreement_allowed boolean DEFAULT true NOT NULL,
    special_term_version character varying(20) NOT NULL,
    started_at character(8),
    ended_at character(8),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    status_changed_at character(8),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.deposit_special_terms_special_term_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_special_terms_special_term_id_seq OWNED BY public.deposit_special_terms.special_term_id;

CREATE TABLE public.deposit_subscription_payment_recognition_history (
    recognition_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    payment_amount numeric(18,2) NOT NULL,
    recognized_amount numeric(18,2) NOT NULL,
    payment_month character varying(6) NOT NULL,
    recognized_at timestamp with time zone,
    recognition_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE SEQUENCE public.deposit_subscription_payment_recognition_his_recognition_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_subscription_payment_recognition_his_recognition_id_seq OWNED BY public.deposit_subscription_payment_recognition_history.recognition_id;

CREATE TABLE public.deposit_subscription_products (
    banking_product_id bigint NOT NULL,
    monthly_payment_amount numeric(18,2) NOT NULL,
    min_monthly_payment numeric(18,2),
    max_monthly_payment numeric(18,2),
    max_recognized_payment_amount numeric(18,2),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

CREATE TABLE public.deposit_target_groups (
    target_group_id bigint NOT NULL,
    target_group_name character varying(100) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100),
    min_age integer,
    max_age integer
);

CREATE SEQUENCE public.deposit_target_groups_target_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_target_groups_target_group_id_seq OWNED BY public.deposit_target_groups.target_group_id;

CREATE TABLE public.deposit_term_application_management (
    term_application_id bigint NOT NULL,
    common_term_id bigint,
    term_target_id bigint,
    business_type_code character varying(10),
    is_required character(1) DEFAULT 'N'::bpchar NOT NULL,
    registered_at character(8),
    modified_at character(8),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100)
);

COMMENT ON TABLE public.deposit_term_application_management IS '약관 적용 관리';

COMMENT ON COLUMN public.deposit_term_application_management.common_term_id IS '공통 약관 ID (외부 서비스 참조)';

COMMENT ON COLUMN public.deposit_term_application_management.term_target_id IS '약관 적용 대상 ID';

COMMENT ON COLUMN public.deposit_term_application_management.business_type_code IS '업무 구분 코드 (DEPOSIT/SAVINGS/SUBSCRIPTION)';

COMMENT ON COLUMN public.deposit_term_application_management.is_required IS '필수 여부 Y/N';

COMMENT ON COLUMN public.deposit_term_application_management.registered_at IS '등록일 YYYYMMDD';

COMMENT ON COLUMN public.deposit_term_application_management.modified_at IS '수정일 YYYYMMDD';

CREATE SEQUENCE public.deposit_term_application_management_term_application_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_term_application_management_term_application_id_seq OWNED BY public.deposit_term_application_management.term_application_id;

CREATE TABLE public.deposit_transactions (
    transaction_id bigint NOT NULL,
    transaction_number character varying(50) NOT NULL,
    account_id bigint NOT NULL,
    contract_id bigint,
    transaction_type character varying(30) NOT NULL,
    direction_type character varying(10) NOT NULL,
    amount numeric(18,2) NOT NULL,
    balance_before numeric(18,2) NOT NULL,
    balance_after numeric(18,2) NOT NULL,
    available_balance_after numeric(18,2),
    fee_amount numeric(18,2) DEFAULT 0 NOT NULL,
    currency character(3) DEFAULT 'KRW'::bpchar NOT NULL,
    status character varying(20) DEFAULT 'SUCCESS'::character varying NOT NULL,
    channel_type character varying(30) NOT NULL,
    ip_address character varying(45),
    terminal_id character varying(50),
    transaction_location character varying(100),
    transaction_memo character varying(255),
    transaction_summary character varying(100),
    transaction_at timestamp with time zone NOT NULL,
    posted_at timestamp with time zone,
    canceled_at timestamp with time zone,
    depositor_customer_id character varying(30),
    depositor_name character varying(100),
    delegate_customer_id character varying(30),
    delegate_customer_name character varying(100),
    transfer_type character varying(30),
    counterparty_bank_code character varying(10),
    counterparty_bank_name character varying(100),
    counterparty_account_no character varying(30),
    counterparty_account_id bigint,
    counterparty_customer_id character varying(30),
    counterparty_name character varying(100),
    counterparty_name_verified_yn boolean,
    transfer_requested_at timestamp with time zone,
    transfer_completed_at timestamp with time zone,
    payment_method character varying(30),
    merchant_id character varying(50),
    merchant_name character varying(100),
    approval_number character varying(50),
    external_transaction_no character varying(100),
    payment_round integer,
    original_transaction_id bigint,
    failure_type character varying(30),
    failure_code character varying(50),
    failure_reason_code character varying(50),
    failure_at timestamp with time zone,
    retry_count integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100),
    idempotency_key character varying(64)
);

CREATE SEQUENCE public.deposit_transactions_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.deposit_transactions_transaction_id_seq OWNED BY public.deposit_transactions.transaction_id;

CREATE TABLE public.loan_account (
    account_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    product_id3 bigint NOT NULL,
    contract_id2 bigint NOT NULL,
    loan_account_type_cd character varying(20) NOT NULL,
    purpose_cd character varying(20),
    unpaid_interest bigint NOT NULL,
    principal_balance bigint NOT NULL,
    loan_balance bigint NOT NULL,
    valid_from character(8),
    valid_to character(8),
    loan_account_status character varying(20) NOT NULL,
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE TABLE public.loan_application (
    application_id bigint NOT NULL,
    application_no character varying(30) NOT NULL,
    customer_id bigint NOT NULL,
    product_id bigint NOT NULL,
    apply_channel_cd character varying(20) NOT NULL,
    application_branch_id bigint,
    application_charge_id bigint,
    request_amount bigint NOT NULL,
    request_period_mo integer NOT NULL,
    purpose_cd character varying(20) NOT NULL,
    repayment_method_cd character varying(20) NOT NULL,
    desired_disbursement_date character(8),
    reject_reason character varying(500),
    cancel_reason character varying(500),
    applied_at character(8) NOT NULL,
    completed_at character(8),
    apply_status character varying(20) NOT NULL,
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.loan_application_application_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_application_application_id_seq OWNED BY public.loan_application.application_id;

CREATE TABLE public.loan_contract (
    contract_id bigint NOT NULL,
    product_id bigint NOT NULL,
    loan_review_id bigint NOT NULL,
    loan_contract_no character varying(30) NOT NULL,
    loan_application_id bigint NOT NULL,
    loan_product_name character varying(30),
    contractor_id bigint NOT NULL,
    contractor_name character varying(30),
    facility_type_cd character varying(20) NOT NULL,
    repayment_method_cd character varying(20) NOT NULL,
    allocation_policy_cd character varying(30) NOT NULL,
    prepayment_recalc_mode_cd character varying(30) NOT NULL,
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint NOT NULL,
    updated_by timestamp(3) with time zone NOT NULL,
    updated_ bigint NOT NULL,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE TABLE public.loan_execution (
    execution_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    disbursement_account_id bigint NOT NULL,
    payment_tx_id character varying(50),
    disbursement_amount bigint NOT NULL,
    masked_account_no character varying(30),
    tranche_no integer NOT NULL,
    before_execution_id bigint NOT NULL,
    idempotency_key character varying(100) NOT NULL,
    loan_exection_status_cd character varying(20) NOT NULL,
    loan_exection_fail_reason_cd character varying(30),
    loan_exection_fail_reason_detail text,
    requested_at character(8) NOT NULL,
    executed_at character(8),
    reversal_yn character(1) NOT NULL,
    reverses_execution_id bigint NOT NULL,
    reversal_reason_cd character varying(30),
    reversal_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.loan_execution_execution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_execution_execution_id_seq OWNED BY public.loan_execution.execution_id;

CREATE TABLE public.loan_product (
    product_id bigint NOT NULL,
    loan_purpose_cd character varying(50),
    collateral_type_cd character varying(50),
    repayment_methods_cd character varying(500),
    guarantee_type_cd character varying(50),
    rate_type_cd character varying(50),
    display_min_rate_bps integer,
    display_max_rate_bps integer,
    collateral_required_yn character(1),
    guarantee_required_yn character(1),
    early_repay_fee_yn character(1),
    holiday_repay_target_yn character(1),
    loan_product_status_cd character varying(20),
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE TABLE public.loan_repayment (
    repayment_id bigint NOT NULL,
    party_id bigint NOT NULL,
    contract_id bigint NOT NULL,
    tx_type_cd character varying(20) NOT NULL,
    payment_channel_cd character varying(20) NOT NULL,
    idempotency_key character varying(100) NOT NULL,
    payment_tx_id character varying(50),
    paid_principal bigint NOT NULL,
    paid_interest bigint NOT NULL,
    paid_late_fee bigint NOT NULL,
    total_paid bigint NOT NULL,
    loan_account_id bigint NOT NULL,
    virtual_account_no character varying(30),
    sender_name character varying(100),
    third_party_yn character(1) NOT NULL,
    loan_repayment_status character varying(20) NOT NULL,
    loan_repayment_fail_reason_cd character varying(30),
    loan_repayment_fail_reason_detail text,
    previous_repayment_id bigint NOT NULL,
    reverses_repayment_id bigint NOT NULL,
    reversal_yn character(1) NOT NULL,
    reversal_reason_cd character varying(30),
    reversal_at timestamp(3) with time zone,
    requested_at character(8) NOT NULL,
    paid_at character(8),
    value_date character(8) NOT NULL,
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.loan_repayment_repayment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_repayment_repayment_id_seq OWNED BY public.loan_repayment.repayment_id;

CREATE TABLE public.loan_review (
    loan_review_id bigint NOT NULL,
    loan_review_no character varying(50) NOT NULL,
    application_id bigint NOT NULL,
    product_id bigint NOT NULL,
    review_target_cd character varying(30),
    review_round integer,
    approved_amount bigint,
    approved_rate_bps integer,
    review_method_cd character varying(30),
    reviewer_id bigint,
    approver_id bigint,
    loan_review_round_cd character varying(50),
    review_opinion_cd character varying(30),
    review_opinion_reason_cd character varying(30),
    approval_decision_cd character varying(30),
    approval_decision_reason_cd character varying(30),
    assigned_at character(8),
    started_at character(8),
    decided_at character(8),
    previous_review_id bigint NOT NULL,
    review_status_cd character varying(30),
    created_at timestamp(3) with time zone,
    created_by bigint,
    updated_at timestamp(3) with time zone,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.loan_review_loan_review_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_review_loan_review_id_seq OWNED BY public.loan_review.loan_review_id;

CREATE TABLE public.party (
    party_id bigint NOT NULL,
    party_type_code character varying(20) NOT NULL,
    party_name character varying(100) NOT NULL,
    party_english_name character varying(200),
    party_status_code character varying(20) NOT NULL,
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE TABLE public.party_organization (
    party_id bigint NOT NULL,
    org_subtype_code character varying(20) NOT NULL,
    corp_reg_no character(14) NOT NULL,
    corp_formal_name character varying(200) NOT NULL,
    corp_formal_english_name character varying(400),
    hq_country_code character(3),
    foreign_corp_reg_no_encrypted character varying(255) NOT NULL,
    corp_type_code character varying(20),
    non_corp_type_code character varying(10),
    ownership_type_code character varying(10),
    representative_type_code character varying(10),
    establishment_date character(8),
    dissolution_date character(8),
    capital_amount bigint,
    fiscal_month smallint,
    establishment_purpose character varying(500),
    member_count integer,
    charter_url character varying(500),
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.party_party_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.party_party_id_seq OWNED BY public.party.party_id;

CREATE TABLE public.party_person (
    party_id bigint NOT NULL,
    rrn_encrypted character varying(255),
    ci_value character varying(88),
    nationality_type_code character varying(10),
    nationality_code character(3),
    birth_date character(8),
    gender_code character(1),
    marital_status_code character varying(10),
    dependent_count integer,
    occupation_code character varying(10),
    occupation_name character varying(100),
    workplace_name character varying(200),
    annual_income_amount bigint,
    income_proof_code character varying(10),
    capacity_limit_type_code character varying(20),
    is_pep_yn character(1) NOT NULL,
    pep_type_code character varying(10),
    pep_country_code character varying(3),
    pep_position character varying(200),
    death_date character(8),
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE TABLE public.party_relation (
    relation_id bigint NOT NULL,
    from_party_id bigint NOT NULL,
    to_party_id bigint NOT NULL,
    relation_type_code character varying(10) NOT NULL,
    relation_detail_code character varying(10),
    equity_ratio_bps integer,
    representation_scope character varying(200),
    proof_url character varying(500),
    relation_start_date character(8) NOT NULL,
    relation_end_date character(8),
    relation_end_reason_code character varying(20),
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.party_relation_relation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.party_relation_relation_id_seq OWNED BY public.party_relation.relation_id;

CREATE TABLE public.party_role (
    role_id bigint NOT NULL,
    party_id bigint NOT NULL,
    role_type_code character varying(20) NOT NULL,
    role_status_code character varying(20),
    role_start_date character(8),
    role_end_date character(8) NOT NULL,
    role_end_reason_code character varying(20),
    created_at timestamp(3) with time zone NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.party_role_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.party_role_role_id_seq OWNED BY public.party_role.role_id;

CREATE TABLE public.terms_target_map (
    terms_target_map_id bigint NOT NULL,
    terms_template_id bigint NOT NULL,
    target_id bigint,
    biz_div_cd character varying(10),
    required_yn character(1),
    created_by bigint,
    created_at timestamp(3) with time zone,
    updated_by bigint,
    updated_at timestamp(3) with time zone
);

CREATE SEQUENCE public.terms_target_map_terms_target_map_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.terms_target_map_terms_target_map_id_seq OWNED BY public.terms_target_map.terms_target_map_id;

CREATE TABLE public."결제지시" (
    "결제지시번호" character varying(20) NOT NULL,
    "연결된멱등키" character varying(50) NOT NULL,
    "송신고객번호" character varying(30),
    "송신계좌번호" character varying(30),
    "인증토큰번호" character varying(50),
    "원거래참조번호" character varying(20),
    "거래번호" character varying(30) NOT NULL,
    "송신계좌번호_스냅샷" character varying(30) NOT NULL,
    "송신계좌별명_스냅샷" character varying(60),
    "수신은행코드" character(3) NOT NULL,
    "수신계좌번호" character varying(30) NOT NULL,
    "수신예금주명_스냅샷" character varying(60) NOT NULL,
    "예금주조회일시" timestamp without time zone NOT NULL,
    "자행이체여부" character(1) NOT NULL,
    "라우팅망종류" character varying(20) NOT NULL,
    "이체금액" numeric(15,0) NOT NULL,
    "결제수수료" numeric(15,0) NOT NULL,
    "수신통장_송신자표시명" character varying(60),
    "받는분통장메모" character varying(100),
    "내통장메모" character varying(100),
    "진행상태" character varying(20) NOT NULL,
    "실패분류" character varying(30),
    "결제지시채널" character varying(20) NOT NULL,
    "요청일시" timestamp without time zone NOT NULL,
    "완료일시" timestamp without time zone,
    "영업일자" character(8) NOT NULL,
    "다음재시도일시" timestamp without time zone,
    "다음타임아웃일시" timestamp without time zone,
    "트리거주체" character varying(20) NOT NULL,
    "예약여부" character(1) NOT NULL,
    "예약실행일시" timestamp without time zone,
    "최초등록일시" timestamp without time zone NOT NULL,
    "최초등록자ID" bigint,
    "최종수정일시" timestamp without time zone NOT NULL,
    "최종수정자ID" bigint
);

CREATE TABLE public."금융결제원청산거래" (
    "청산거래번호" character varying(20) NOT NULL,
    "우리결제지시번호" character varying(20) NOT NULL,
    "거래방향" character varying(5) NOT NULL,
    "상대은행결제지시번호" character varying(50),
    "청산번호" character varying(50) NOT NULL,
    "송신은행의청산ID" character varying(50),
    "수신은행의청산ID" character varying(50),
    "송신은행코드" character(3) NOT NULL,
    "송신계좌번호_스냅샷" character varying(30) NOT NULL,
    "송신예금주명_스냅샷" character varying(60) NOT NULL,
    "수신은행코드" character(3) NOT NULL,
    "수신계좌번호_스냅샷" character varying(30) NOT NULL,
    "수신예금주명_스냅샷" character varying(60) NOT NULL,
    "청산금액" numeric(15,0) NOT NULL,
    "통화" character(3) NOT NULL,
    "청산상태" character varying(20) NOT NULL,
    "거절코드" character varying(10),
    "거절메시지" character varying(200),
    "청산요청일시" timestamp without time zone NOT NULL,
    "ACK수신일시" timestamp without time zone,
    "정산완료일시" timestamp without time zone,
    "정산일자" character(8),
    "네트워크" character varying(30) NOT NULL,
    "조회횟수" integer NOT NULL,
    "마지막조회일시" timestamp without time zone,
    "최초등록일시" timestamp without time zone NOT NULL,
    "최초등록자ID" bigint,
    "최종수정일시" timestamp without time zone NOT NULL,
    "최종수정자ID" bigint
);

CREATE TABLE public."상태이력" (
    "상태이력번호" character varying(20) NOT NULL,
    "결제지시번호" character varying(20) NOT NULL,
    "결제지시내순번" integer NOT NULL,
    "이전상태" character varying(20),
    "다음상태" character varying(20) NOT NULL,
    "이벤트종류" character varying(30) NOT NULL,
    "사유코드" character varying(10),
    "사유메시지" character varying(200),
    "트리거주체" character varying(20) NOT NULL,
    "운영자ID" character varying(20),
    "이벤트발생일시" timestamp without time zone NOT NULL,
    "최초등록일시" timestamp without time zone NOT NULL,
    "최초등록자ID" bigint,
    "최종수정일시" timestamp without time zone NOT NULL,
    "최종수정자ID" bigint
);

CREATE TABLE public."원장" (
    "원장번호" character varying(20) NOT NULL,
    "결제지시번호" character varying(20),
    "계좌번호" character varying(30) NOT NULL,
    "원분개참조번호" character varying(20),
    "회계번호" character varying(20) NOT NULL,
    "계좌번호_스냅샷" character varying(30) NOT NULL,
    "예금주명_스냅샷" character varying(60) NOT NULL,
    "차변대변구분" character(6) NOT NULL,
    "분개종류" character varying(30) NOT NULL,
    "금액" numeric(15,0) NOT NULL,
    "통화" character(3) NOT NULL,
    "분개직전잔액" numeric(15,0) NOT NULL,
    "분개직후잔액" numeric(15,0) NOT NULL,
    "상대계좌번호_스냅샷" character varying(30),
    "상대은행코드_스냅샷" character(3),
    "상대예금주명_스냅샷" character varying(60),
    "거래일자" character(8) NOT NULL,
    "기장일자" character(8) NOT NULL,
    "자금가용일" character(8) NOT NULL,
    "기장일시" timestamp without time zone NOT NULL,
    "시스템적요" character varying(100) NOT NULL,
    "통장에찍히는메모_스냅샷" character varying(100),
    "역분개여부" character(1) NOT NULL,
    "역분개사유" character varying(20),
    "기장상태" character varying(20) NOT NULL,
    "최초등록일시" timestamp without time zone NOT NULL,
    "최초등록자ID" bigint,
    "최종수정일시" timestamp without time zone NOT NULL,
    "최종수정자ID" bigint
);

CREATE TABLE public."인증토큰" (
    "인증토큰번호" character varying(50) NOT NULL,
    "고객ID" bigint NOT NULL,
    "인증유형" character varying(20) NOT NULL,
    "최초등록일시" timestamp without time zone NOT NULL,
    "최초등록자ID" bigint,
    "최종수정일시" timestamp without time zone NOT NULL,
    "최종수정자ID" bigint
);

CREATE TABLE public."한국은행결제거래" (
    "결제거래번호" character varying(20) NOT NULL,
    "우리결제지시번호" character varying(20) NOT NULL,
    "거래방향" character varying(5) NOT NULL,
    "상대은행결제지시번호" character varying(50),
    "한은망참조번호" character varying(50) NOT NULL,
    "송신은행코드" character(3) NOT NULL,
    "송신은행의결제지시번호" character varying(50) NOT NULL,
    "송신계좌번호_스냅샷" character varying(30) NOT NULL,
    "송신예금주명_스냅샷" character varying(60) NOT NULL,
    "수신은행코드" character(3) NOT NULL,
    "수신은행의결제지시번호" character varying(50),
    "수신계좌번호_스냅샷" character varying(30) NOT NULL,
    "수신예금주명_스냅샷" character varying(60) NOT NULL,
    "결제금액" numeric(15,0) NOT NULL,
    "통화" character(3) NOT NULL,
    "결제상태" character varying(20) NOT NULL,
    "거절코드" character varying(10),
    "거절메시지" character varying(200),
    "결제요청일시" timestamp without time zone NOT NULL,
    "ACK수신일시" timestamp without time zone,
    "결제확정일시" timestamp without time zone,
    "영업일자" character(8) NOT NULL,
    "조회횟수" integer NOT NULL,
    "마지막조회일시" timestamp without time zone,
    "최초등록일시" timestamp without time zone NOT NULL,
    "최초등록자ID" bigint,
    "최종수정일시" timestamp without time zone NOT NULL,
    "최종수정자ID" bigint
);

ALTER TABLE ONLY public.banking_deposit_product_interest_rates ALTER COLUMN rate_id SET DEFAULT nextval('public.banking_deposit_product_interest_rates_rate_id_seq'::regclass);

ALTER TABLE ONLY public.banking_deposit_product_join_channels ALTER COLUMN channel_id SET DEFAULT nextval('public.banking_deposit_product_join_channels_channel_id_seq'::regclass);

ALTER TABLE ONLY public.banking_deposit_product_special_terms ALTER COLUMN deposit_product_special_term_id SET DEFAULT nextval('public.banking_deposit_product_speci_deposit_product_special_term__seq'::regclass);

ALTER TABLE ONLY public.banking_deposit_products ALTER COLUMN deposit_product_id SET DEFAULT nextval('public.banking_deposit_products_deposit_product_id_seq'::regclass);

ALTER TABLE ONLY public.chat_consultation ALTER COLUMN chat_consultation_id SET DEFAULT nextval('public.chat_consultation_chat_consultation_id_seq'::regclass);

ALTER TABLE ONLY public.chat_message_history ALTER COLUMN chat_message_history_id SET DEFAULT nextval('public.chat_message_history_chat_message_history_id_seq'::regclass);

ALTER TABLE ONLY public.chatbot_consultation ALTER COLUMN chatbot_consultation_id SET DEFAULT nextval('public.chatbot_consultation_chatbot_consultation_id_seq'::regclass);

ALTER TABLE ONLY public.chatbot_intent ALTER COLUMN intent_id SET DEFAULT nextval('public.chatbot_intent_intent_id_seq'::regclass);

ALTER TABLE ONLY public.chatbot_node ALTER COLUMN node_id SET DEFAULT nextval('public.chatbot_node_node_id_seq'::regclass);

ALTER TABLE ONLY public.chatbot_node_button ALTER COLUMN id SET DEFAULT nextval('public.chatbot_node_button_id_seq'::regclass);

ALTER TABLE ONLY public.chatbot_scenario ALTER COLUMN scenario_id SET DEFAULT nextval('public.chatbot_scenario_scenario_id_seq'::regclass);

ALTER TABLE ONLY public.common_account ALTER COLUMN account_id SET DEFAULT nextval('public.common_account_account_id_seq'::regclass);

ALTER TABLE ONLY public.common_contract ALTER COLUMN contract_id SET DEFAULT nextval('public.common_contract_contract_id_seq'::regclass);

ALTER TABLE ONLY public.common_product ALTER COLUMN product_id SET DEFAULT nextval('public.common_product_product_id_seq'::regclass);

ALTER TABLE ONLY public.common_terms_consent ALTER COLUMN consent_id SET DEFAULT nextval('public.common_terms_consent_consent_id_seq'::regclass);

ALTER TABLE ONLY public.common_terms_template ALTER COLUMN terms_template_id SET DEFAULT nextval('public.common_terms_template_terms_template_id_seq'::regclass);

ALTER TABLE ONLY public.common_transaction ALTER COLUMN transaction_id SET DEFAULT nextval('public.common_transaction_transaction_id_seq'::regclass);

ALTER TABLE ONLY public.consultation ALTER COLUMN consultation_id SET DEFAULT nextval('public.consultation_consultation_id_seq'::regclass);

ALTER TABLE ONLY public.customer ALTER COLUMN customer_id SET DEFAULT nextval('public.customer_customer_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_accounts ALTER COLUMN account_id SET DEFAULT nextval('public.deposit_accounts_account_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_banking_products ALTER COLUMN banking_product_id SET DEFAULT nextval('public.deposit_banking_products_banking_product_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_contract_applied_rates ALTER COLUMN applied_rate_id SET DEFAULT nextval('public.deposit_contract_applied_rates_applied_rate_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_contract_special_term_agreements ALTER COLUMN special_agreement_id SET DEFAULT nextval('public.deposit_contract_special_term_agreemen_special_agreement_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_contracts ALTER COLUMN contract_id SET DEFAULT nextval('public.deposit_contracts_contract_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_departments ALTER COLUMN department_id SET DEFAULT nextval('public.deposit_departments_department_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_interest_history ALTER COLUMN interest_id SET DEFAULT nextval('public.deposit_interest_history_interest_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_payment_schedules ALTER COLUMN schedule_id SET DEFAULT nextval('public.deposit_payment_schedules_schedule_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_savings_products ALTER COLUMN savings_product_id SET DEFAULT nextval('public.deposit_savings_products_savings_product_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_special_terms ALTER COLUMN special_term_id SET DEFAULT nextval('public.deposit_special_terms_special_term_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_subscription_payment_recognition_history ALTER COLUMN recognition_id SET DEFAULT nextval('public.deposit_subscription_payment_recognition_his_recognition_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_target_groups ALTER COLUMN target_group_id SET DEFAULT nextval('public.deposit_target_groups_target_group_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_term_application_management ALTER COLUMN term_application_id SET DEFAULT nextval('public.deposit_term_application_management_term_application_id_seq'::regclass);

ALTER TABLE ONLY public.deposit_transactions ALTER COLUMN transaction_id SET DEFAULT nextval('public.deposit_transactions_transaction_id_seq'::regclass);

ALTER TABLE ONLY public.loan_application ALTER COLUMN application_id SET DEFAULT nextval('public.loan_application_application_id_seq'::regclass);

ALTER TABLE ONLY public.loan_execution ALTER COLUMN execution_id SET DEFAULT nextval('public.loan_execution_execution_id_seq'::regclass);

ALTER TABLE ONLY public.loan_repayment ALTER COLUMN repayment_id SET DEFAULT nextval('public.loan_repayment_repayment_id_seq'::regclass);

ALTER TABLE ONLY public.loan_review ALTER COLUMN loan_review_id SET DEFAULT nextval('public.loan_review_loan_review_id_seq'::regclass);

ALTER TABLE ONLY public.party ALTER COLUMN party_id SET DEFAULT nextval('public.party_party_id_seq'::regclass);

ALTER TABLE ONLY public.party_relation ALTER COLUMN relation_id SET DEFAULT nextval('public.party_relation_relation_id_seq'::regclass);

ALTER TABLE ONLY public.party_role ALTER COLUMN role_id SET DEFAULT nextval('public.party_role_role_id_seq'::regclass);

ALTER TABLE ONLY public.terms_target_map ALTER COLUMN terms_target_map_id SET DEFAULT nextval('public.terms_target_map_terms_target_map_id_seq'::regclass);

ALTER TABLE ONLY public.banking_deposit_product_interest_rates
    ADD CONSTRAINT banking_deposit_product_interest_rates_pkey PRIMARY KEY (rate_id);

ALTER TABLE ONLY public.banking_deposit_product_join_channels
    ADD CONSTRAINT banking_deposit_product_join_channels_pkey PRIMARY KEY (channel_id);

ALTER TABLE ONLY public.banking_deposit_product_special_terms
    ADD CONSTRAINT banking_deposit_product_special_terms_pkey PRIMARY KEY (deposit_product_special_term_id);

ALTER TABLE ONLY public.banking_deposit_product_target_groups
    ADD CONSTRAINT banking_deposit_product_target_groups_pkey PRIMARY KEY (banking_product_id, target_group_id);

ALTER TABLE ONLY public.banking_deposit_products
    ADD CONSTRAINT banking_deposit_products_pkey PRIMARY KEY (deposit_product_id);

ALTER TABLE ONLY public.chat_consultation
    ADD CONSTRAINT chat_consultation_pkey PRIMARY KEY (chat_consultation_id);

ALTER TABLE ONLY public.chat_message_history
    ADD CONSTRAINT chat_message_history_pkey PRIMARY KEY (chat_message_history_id);

ALTER TABLE ONLY public.chatbot_consultation
    ADD CONSTRAINT chatbot_consultation_pkey PRIMARY KEY (chatbot_consultation_id);

ALTER TABLE ONLY public.chatbot_intent
    ADD CONSTRAINT chatbot_intent_pkey PRIMARY KEY (intent_id);

ALTER TABLE ONLY public.chatbot_node_button
    ADD CONSTRAINT chatbot_node_button_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.chatbot_node_flow
    ADD CONSTRAINT chatbot_node_flow_pkey PRIMARY KEY (current_node_id, next_node_id);

ALTER TABLE ONLY public.chatbot_node
    ADD CONSTRAINT chatbot_node_pkey PRIMARY KEY (node_id);

ALTER TABLE ONLY public.chatbot_scenario
    ADD CONSTRAINT chatbot_scenario_pkey PRIMARY KEY (scenario_id);

ALTER TABLE ONLY public.common_account
    ADD CONSTRAINT common_account_account_no_key UNIQUE (account_no);

ALTER TABLE ONLY public.common_account
    ADD CONSTRAINT common_account_pkey PRIMARY KEY (account_id);

ALTER TABLE ONLY public.common_contract
    ADD CONSTRAINT common_contract_pkey PRIMARY KEY (contract_id);

ALTER TABLE ONLY public.common_product
    ADD CONSTRAINT common_product_pkey PRIMARY KEY (product_id);

ALTER TABLE ONLY public.common_product
    ADD CONSTRAINT common_product_product_cd_key UNIQUE (product_cd);

ALTER TABLE ONLY public.common_terms_consent
    ADD CONSTRAINT common_terms_consent_pkey PRIMARY KEY (consent_id, customer_id);

ALTER TABLE ONLY public.common_terms_template
    ADD CONSTRAINT common_terms_template_pkey PRIMARY KEY (terms_template_id);

ALTER TABLE ONLY public.common_terms_template
    ADD CONSTRAINT common_terms_template_terms_no_key UNIQUE (terms_no);

ALTER TABLE ONLY public.common_transaction
    ADD CONSTRAINT common_transaction_pkey PRIMARY KEY (transaction_id);

ALTER TABLE ONLY public.consultation
    ADD CONSTRAINT consultation_pkey PRIMARY KEY (consultation_id);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (customer_id);

ALTER TABLE ONLY public.deposit_account
    ADD CONSTRAINT deposit_account_pkey PRIMARY KEY (account_id);

ALTER TABLE ONLY public.deposit_accounts
    ADD CONSTRAINT deposit_accounts_account_number_key UNIQUE (account_number);

ALTER TABLE ONLY public.deposit_accounts
    ADD CONSTRAINT deposit_accounts_contract_id_key UNIQUE (contract_id);

ALTER TABLE ONLY public.deposit_accounts
    ADD CONSTRAINT deposit_accounts_pkey PRIMARY KEY (account_id);

ALTER TABLE ONLY public.deposit_banking_products
    ADD CONSTRAINT deposit_banking_products_pkey PRIMARY KEY (banking_product_id);

ALTER TABLE ONLY public.deposit_contract_applied_rates
    ADD CONSTRAINT deposit_contract_applied_rates_pkey PRIMARY KEY (applied_rate_id);

ALTER TABLE ONLY public.deposit_contract
    ADD CONSTRAINT deposit_contract_pkey PRIMARY KEY (contract_id);

ALTER TABLE ONLY public.deposit_contract_special_term_agreements
    ADD CONSTRAINT deposit_contract_special_term_agreements_pkey PRIMARY KEY (special_agreement_id);

ALTER TABLE ONLY public.deposit_contracts
    ADD CONSTRAINT deposit_contracts_contract_number_key UNIQUE (contract_number);

ALTER TABLE ONLY public.deposit_contracts
    ADD CONSTRAINT deposit_contracts_pkey PRIMARY KEY (contract_id);

ALTER TABLE ONLY public.deposit_departments
    ADD CONSTRAINT deposit_departments_department_code_key UNIQUE (department_code);

ALTER TABLE ONLY public.deposit_departments
    ADD CONSTRAINT deposit_departments_pkey PRIMARY KEY (department_id);

ALTER TABLE ONLY public.deposit_interest_history
    ADD CONSTRAINT deposit_interest_history_pkey PRIMARY KEY (interest_id);

ALTER TABLE ONLY public.deposit_payment_schedules
    ADD CONSTRAINT deposit_payment_schedules_pkey PRIMARY KEY (schedule_id);

ALTER TABLE ONLY public.deposit_product
    ADD CONSTRAINT deposit_product_pkey PRIMARY KEY (product_id);

ALTER TABLE ONLY public.deposit_savings_products
    ADD CONSTRAINT deposit_savings_products_pkey PRIMARY KEY (savings_product_id);

ALTER TABLE ONLY public.deposit_special_terms
    ADD CONSTRAINT deposit_special_terms_pkey PRIMARY KEY (special_term_id);

ALTER TABLE ONLY public.deposit_subscription_payment_recognition_history
    ADD CONSTRAINT deposit_subscription_payment_recognition_history_pkey PRIMARY KEY (recognition_id);

ALTER TABLE ONLY public.deposit_subscription_products
    ADD CONSTRAINT deposit_subscription_products_pkey PRIMARY KEY (banking_product_id);

ALTER TABLE ONLY public.deposit_target_groups
    ADD CONSTRAINT deposit_target_groups_pkey PRIMARY KEY (target_group_id);

ALTER TABLE ONLY public.deposit_target_groups
    ADD CONSTRAINT deposit_target_groups_target_group_name_key UNIQUE (target_group_name);

ALTER TABLE ONLY public.deposit_term_application_management
    ADD CONSTRAINT deposit_term_application_management_pkey PRIMARY KEY (term_application_id);

ALTER TABLE ONLY public.deposit_transactions
    ADD CONSTRAINT deposit_transactions_pkey PRIMARY KEY (transaction_id);

ALTER TABLE ONLY public.deposit_transactions
    ADD CONSTRAINT deposit_transactions_transaction_number_key UNIQUE (transaction_number);

ALTER TABLE ONLY public.loan_account
    ADD CONSTRAINT loan_account_pkey PRIMARY KEY (account_id);

ALTER TABLE ONLY public.loan_application
    ADD CONSTRAINT loan_application_pkey PRIMARY KEY (application_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT loan_contract_pkey PRIMARY KEY (contract_id);

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT loan_execution_pkey PRIMARY KEY (execution_id);

ALTER TABLE ONLY public.loan_product
    ADD CONSTRAINT loan_product_pkey PRIMARY KEY (product_id);

ALTER TABLE ONLY public.loan_repayment
    ADD CONSTRAINT loan_repayment_pkey PRIMARY KEY (repayment_id);

ALTER TABLE ONLY public.loan_review
    ADD CONSTRAINT loan_review_pkey PRIMARY KEY (loan_review_id);

ALTER TABLE ONLY public.party_organization
    ADD CONSTRAINT party_organization_pkey PRIMARY KEY (party_id);

ALTER TABLE ONLY public.party_person
    ADD CONSTRAINT party_person_pkey PRIMARY KEY (party_id);

ALTER TABLE ONLY public.party
    ADD CONSTRAINT party_pkey PRIMARY KEY (party_id);

ALTER TABLE ONLY public.party_relation
    ADD CONSTRAINT party_relation_pkey PRIMARY KEY (relation_id);

ALTER TABLE ONLY public.party_role
    ADD CONSTRAINT party_role_pkey PRIMARY KEY (role_id);

ALTER TABLE ONLY public.terms_target_map
    ADD CONSTRAINT terms_target_map_pkey PRIMARY KEY (terms_target_map_id);

ALTER TABLE ONLY public.banking_deposit_product_join_channels
    ADD CONSTRAINT uq_bdp_join_channels_product_channel UNIQUE (banking_product_id, join_channel_code);

ALTER TABLE ONLY public.banking_deposit_product_special_terms
    ADD CONSTRAINT uq_bdp_special_terms_product_term UNIQUE (banking_product_id, special_term_id);

ALTER TABLE ONLY public.deposit_contract_special_term_agreements
    ADD CONSTRAINT uq_dcsta_contract_term UNIQUE (contract_id, special_term_id);

ALTER TABLE ONLY public.deposit_special_terms
    ADD CONSTRAINT uq_deposit_special_terms_name_ver UNIQUE (special_term_name, special_term_version);

ALTER TABLE ONLY public."결제지시"
    ADD CONSTRAINT "결제지시_pkey" PRIMARY KEY ("결제지시번호");

ALTER TABLE ONLY public."금융결제원청산거래"
    ADD CONSTRAINT "금융결제원청산거래_pkey" PRIMARY KEY ("청산거래번호");

ALTER TABLE ONLY public."상태이력"
    ADD CONSTRAINT "상태이력_pkey" PRIMARY KEY ("상태이력번호");

ALTER TABLE ONLY public."원장"
    ADD CONSTRAINT "원장_pkey" PRIMARY KEY ("원장번호");

ALTER TABLE ONLY public."인증토큰"
    ADD CONSTRAINT "인증토큰_pkey" PRIMARY KEY ("인증토큰번호");

ALTER TABLE ONLY public."한국은행결제거래"
    ADD CONSTRAINT "한국은행결제거래_pkey" PRIMARY KEY ("결제거래번호");

CREATE INDEX idx_bdp_ir_dates ON public.banking_deposit_product_interest_rates USING btree (banking_product_id, effective_start_date, effective_end_date);

CREATE INDEX idx_bdp_ir_product ON public.banking_deposit_product_interest_rates USING btree (banking_product_id, rate_type, is_active);

CREATE INDEX idx_bdp_product ON public.banking_deposit_products USING btree (banking_product_id);

CREATE INDEX idx_bdp_st_term ON public.banking_deposit_product_special_terms USING btree (special_term_id);

CREATE INDEX idx_bdp_tg_target ON public.banking_deposit_product_target_groups USING btree (target_group_id);

CREATE INDEX idx_common_account_cust ON public.common_account USING btree (customer_id, account_status);

CREATE INDEX idx_common_contract_cust ON public.common_contract USING btree (customer_id, contract_status);

CREATE INDEX idx_common_contract_prod ON public.common_contract USING btree (product_id);

CREATE INDEX idx_common_product_biz ON public.common_product USING btree (biz_div_cd, product_status);

CREATE INDEX idx_common_product_cd ON public.common_product USING btree (product_cd);

CREATE INDEX idx_common_terms_no ON public.common_terms_template USING btree (terms_no);

CREATE INDEX idx_common_tx_account ON public.common_transaction USING btree (account_id, transacted_at DESC);

CREATE INDEX idx_common_tx_contract ON public.common_transaction USING btree (contract_id, transacted_at DESC) WHERE (contract_id IS NOT NULL);

CREATE INDEX idx_customer_party ON public.customer USING btree (party_id);

CREATE INDEX idx_customer_status ON public.customer USING btree (customer_status_code);

CREATE INDEX idx_dbp_dept_status ON public.deposit_banking_products USING btree (department_id, deposit_product_status);

CREATE INDEX idx_dbp_type_status ON public.deposit_banking_products USING btree (deposit_product_type, deposit_product_status);

CREATE INDEX idx_deposit_accounts_customer ON public.deposit_accounts USING btree (customer_id, account_status);

CREATE INDEX idx_deposit_accounts_type ON public.deposit_accounts USING btree (account_type, account_status);

CREATE INDEX idx_deposit_car_contract ON public.deposit_contract_applied_rates USING btree (contract_id);

CREATE INDEX idx_deposit_contracts_customer ON public.deposit_contracts USING btree (customer_id, contract_status);

CREATE INDEX idx_deposit_contracts_maturity ON public.deposit_contracts USING btree (maturity_at) WHERE ((contract_status)::text = 'ACTIVE'::text);

CREATE INDEX idx_deposit_contracts_product ON public.deposit_contracts USING btree (banking_product_id, contract_status);

CREATE INDEX idx_deposit_contracts_started ON public.deposit_contracts USING btree (started_at);

CREATE INDEX idx_deposit_csta_contract ON public.deposit_contract_special_term_agreements USING btree (contract_id);

CREATE INDEX idx_deposit_ih_account ON public.deposit_interest_history USING btree (account_id, interest_paid_at DESC);

CREATE INDEX idx_deposit_ih_contract ON public.deposit_interest_history USING btree (contract_id, interest_paid_at DESC);

CREATE INDEX idx_deposit_products_status ON public.deposit_banking_products USING btree (deposit_product_status);

CREATE INDEX idx_deposit_transactions_account ON public.deposit_transactions USING btree (account_id, status);

CREATE INDEX idx_deposit_tx_account ON public.deposit_transactions USING btree (account_id, transaction_at DESC);

CREATE INDEX idx_deposit_tx_contract ON public.deposit_transactions USING btree (contract_id, transaction_at DESC) WHERE (contract_id IS NOT NULL);

CREATE INDEX idx_deposit_tx_type_status ON public.deposit_transactions USING btree (transaction_type, status, transaction_at DESC);

CREATE INDEX idx_dsp_product ON public.deposit_savings_products USING btree (banking_product_id);

CREATE INDEX idx_loan_app_customer ON public.loan_application USING btree (customer_id, apply_status);

CREATE INDEX idx_loan_exec_contract ON public.loan_execution USING btree (contract_id);

CREATE INDEX idx_loan_repay_account ON public.loan_repayment USING btree (loan_account_id);

CREATE INDEX idx_loan_repay_contract ON public.loan_repayment USING btree (contract_id);

CREATE INDEX idx_loan_review_app ON public.loan_review USING btree (application_id);

CREATE INDEX idx_party_relation_from ON public.party_relation USING btree (from_party_id);

CREATE INDEX idx_party_relation_to ON public.party_relation USING btree (to_party_id);

CREATE INDEX idx_party_role_party ON public.party_role USING btree (party_id);

CREATE INDEX idx_party_type_status ON public.party USING btree (party_type_code, party_status_code);

CREATE INDEX idx_payment_schedules_contract ON public.deposit_payment_schedules USING btree (contract_id);

CREATE INDEX idx_payment_schedules_scheduled_date ON public.deposit_payment_schedules USING btree (scheduled_date, status);

CREATE INDEX idx_terms_consent_customer ON public.common_terms_consent USING btree (customer_id);

CREATE INDEX idx_terms_consent_template ON public.common_terms_consent USING btree (terms_template_id);

CREATE INDEX idx_terms_target_template ON public.terms_target_map USING btree (terms_template_id);

CREATE UNIQUE INDEX uq_deposit_transactions_idempotency_key ON public.deposit_transactions USING btree (idempotency_key) WHERE (idempotency_key IS NOT NULL);

ALTER TABLE ONLY public.chat_consultation
    ADD CONSTRAINT chat_consultation_chatbot_consultation_id_fkey FOREIGN KEY (chatbot_consultation_id) REFERENCES public.chatbot_consultation(chatbot_consultation_id);

ALTER TABLE ONLY public.chat_consultation
    ADD CONSTRAINT chat_consultation_consultation_id_fkey FOREIGN KEY (consultation_id) REFERENCES public.consultation(consultation_id);

ALTER TABLE ONLY public.chat_message_history
    ADD CONSTRAINT chat_message_history_chat_consultation_id_fkey FOREIGN KEY (chat_consultation_id) REFERENCES public.chat_consultation(chat_consultation_id);

ALTER TABLE ONLY public.chat_message_history
    ADD CONSTRAINT chat_message_history_chatbot_consultation_id_fkey FOREIGN KEY (chatbot_consultation_id) REFERENCES public.chatbot_consultation(chatbot_consultation_id);

ALTER TABLE ONLY public.chat_message_history
    ADD CONSTRAINT chat_message_history_node_id_fkey FOREIGN KEY (node_id) REFERENCES public.chatbot_node(node_id);

ALTER TABLE ONLY public.chatbot_consultation
    ADD CONSTRAINT chatbot_consultation_consultation_id_fkey FOREIGN KEY (consultation_id) REFERENCES public.consultation(consultation_id);

ALTER TABLE ONLY public.chatbot_consultation
    ADD CONSTRAINT chatbot_consultation_intent_id_fkey FOREIGN KEY (intent_id) REFERENCES public.chatbot_intent(intent_id);

ALTER TABLE ONLY public.chatbot_consultation
    ADD CONSTRAINT chatbot_consultation_scenario_id_fkey FOREIGN KEY (scenario_id) REFERENCES public.chatbot_scenario(scenario_id);

ALTER TABLE ONLY public.chatbot_intent
    ADD CONSTRAINT chatbot_intent_fallback_intent_id_fkey FOREIGN KEY (fallback_intent_id) REFERENCES public.chatbot_intent(intent_id);

ALTER TABLE ONLY public.chatbot_intent
    ADD CONSTRAINT chatbot_intent_scenario_id_fkey FOREIGN KEY (scenario_id) REFERENCES public.chatbot_scenario(scenario_id);

ALTER TABLE ONLY public.chatbot_node_button
    ADD CONSTRAINT chatbot_node_button_node_id_fkey FOREIGN KEY (node_id) REFERENCES public.chatbot_node(node_id);

ALTER TABLE ONLY public.chatbot_node
    ADD CONSTRAINT chatbot_node_error_move_node_id_fkey FOREIGN KEY (error_move_node_id) REFERENCES public.chatbot_node(node_id);

ALTER TABLE ONLY public.chatbot_node_flow
    ADD CONSTRAINT chatbot_node_flow_current_node_id_fkey FOREIGN KEY (current_node_id) REFERENCES public.chatbot_node(node_id);

ALTER TABLE ONLY public.chatbot_node_flow
    ADD CONSTRAINT chatbot_node_flow_next_node_id_fkey FOREIGN KEY (next_node_id) REFERENCES public.chatbot_node(node_id);

ALTER TABLE ONLY public.chatbot_node
    ADD CONSTRAINT chatbot_node_next_node_id_fkey FOREIGN KEY (next_node_id) REFERENCES public.chatbot_node(node_id);

ALTER TABLE ONLY public.chatbot_node
    ADD CONSTRAINT chatbot_node_scenario_id_fkey FOREIGN KEY (scenario_id) REFERENCES public.chatbot_scenario(scenario_id);

ALTER TABLE ONLY public.consultation
    ADD CONSTRAINT consultation_previous_consultation_id_fkey FOREIGN KEY (previous_consultation_id) REFERENCES public.consultation(consultation_id);

ALTER TABLE ONLY public."인증토큰"
    ADD CONSTRAINT fk_auth_token_customer FOREIGN KEY ("고객ID") REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.banking_deposit_products
    ADD CONSTRAINT fk_banking_deposit_products_product FOREIGN KEY (banking_product_id) REFERENCES public.deposit_banking_products(banking_product_id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY public.banking_deposit_product_interest_rates
    ADD CONSTRAINT fk_bdp_interest_rates_product FOREIGN KEY (banking_product_id) REFERENCES public.deposit_banking_products(banking_product_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.banking_deposit_product_join_channels
    ADD CONSTRAINT fk_bdp_join_channels_product FOREIGN KEY (banking_product_id) REFERENCES public.deposit_banking_products(banking_product_id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY public.banking_deposit_product_special_terms
    ADD CONSTRAINT fk_bdp_special_terms_product FOREIGN KEY (banking_product_id) REFERENCES public.deposit_banking_products(banking_product_id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY public.banking_deposit_product_special_terms
    ADD CONSTRAINT fk_bdp_special_terms_term FOREIGN KEY (special_term_id) REFERENCES public.deposit_special_terms(special_term_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.banking_deposit_product_target_groups
    ADD CONSTRAINT fk_bdp_target_groups_product FOREIGN KEY (banking_product_id) REFERENCES public.deposit_banking_products(banking_product_id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY public.banking_deposit_product_target_groups
    ADD CONSTRAINT fk_bdp_target_groups_target FOREIGN KEY (target_group_id) REFERENCES public.deposit_target_groups(target_group_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public."한국은행결제거래"
    ADD CONSTRAINT fk_bok_payment_instr FOREIGN KEY ("우리결제지시번호") REFERENCES public."결제지시"("결제지시번호");

ALTER TABLE ONLY public."금융결제원청산거래"
    ADD CONSTRAINT fk_clearing_tx_payment FOREIGN KEY ("우리결제지시번호") REFERENCES public."결제지시"("결제지시번호");

ALTER TABLE ONLY public.common_account
    ADD CONSTRAINT fk_common_account_contract FOREIGN KEY (contract_id) REFERENCES public.common_contract(contract_id);

ALTER TABLE ONLY public.common_account
    ADD CONSTRAINT fk_common_account_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.common_contract
    ADD CONSTRAINT fk_common_contract_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.common_contract
    ADD CONSTRAINT fk_common_contract_product FOREIGN KEY (product_id) REFERENCES public.common_product(product_id);

ALTER TABLE ONLY public.common_transaction
    ADD CONSTRAINT fk_common_tx_account FOREIGN KEY (account_id) REFERENCES public.common_account(account_id);

ALTER TABLE ONLY public.common_transaction
    ADD CONSTRAINT fk_common_tx_contract FOREIGN KEY (contract_id) REFERENCES public.common_contract(contract_id);

ALTER TABLE ONLY public.common_transaction
    ADD CONSTRAINT fk_common_tx_original FOREIGN KEY (original_transaction_id) REFERENCES public.common_transaction(transaction_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT fk_customer_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.deposit_contract_special_term_agreements
    ADD CONSTRAINT fk_dcsta_contract FOREIGN KEY (contract_id) REFERENCES public.deposit_contracts(contract_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_contract_special_term_agreements
    ADD CONSTRAINT fk_dcsta_special_term FOREIGN KEY (special_term_id) REFERENCES public.deposit_special_terms(special_term_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_account
    ADD CONSTRAINT fk_deposit_account_common FOREIGN KEY (account_id) REFERENCES public.common_account(account_id);

ALTER TABLE ONLY public.deposit_accounts
    ADD CONSTRAINT fk_deposit_accounts_contract FOREIGN KEY (contract_id) REFERENCES public.deposit_contracts(contract_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_banking_products
    ADD CONSTRAINT fk_deposit_banking_products_dept FOREIGN KEY (department_id) REFERENCES public.deposit_departments(department_id) ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE ONLY public.deposit_contract_applied_rates
    ADD CONSTRAINT fk_deposit_contract_applied_rates_contract FOREIGN KEY (contract_id) REFERENCES public.deposit_contracts(contract_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_contract
    ADD CONSTRAINT fk_deposit_contract_common FOREIGN KEY (contract_id) REFERENCES public.common_contract(contract_id);

ALTER TABLE ONLY public.deposit_contracts
    ADD CONSTRAINT fk_deposit_contracts_product FOREIGN KEY (banking_product_id) REFERENCES public.deposit_banking_products(banking_product_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_departments
    ADD CONSTRAINT fk_deposit_departments_parent FOREIGN KEY (parent_department_id) REFERENCES public.deposit_departments(department_id) ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE ONLY public.deposit_interest_history
    ADD CONSTRAINT fk_deposit_interest_history_account FOREIGN KEY (account_id) REFERENCES public.deposit_accounts(account_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_interest_history
    ADD CONSTRAINT fk_deposit_interest_history_contract FOREIGN KEY (contract_id) REFERENCES public.deposit_contracts(contract_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_product
    ADD CONSTRAINT fk_deposit_product_common FOREIGN KEY (product_id) REFERENCES public.common_product(product_id);

ALTER TABLE ONLY public.deposit_savings_products
    ADD CONSTRAINT fk_deposit_savings_products_product FOREIGN KEY (banking_product_id) REFERENCES public.deposit_banking_products(banking_product_id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY public.deposit_subscription_products
    ADD CONSTRAINT fk_deposit_subscription_products_product FOREIGN KEY (banking_product_id) REFERENCES public.deposit_banking_products(banking_product_id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY public.deposit_transactions
    ADD CONSTRAINT fk_deposit_transactions_account FOREIGN KEY (account_id) REFERENCES public.deposit_accounts(account_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_transactions
    ADD CONSTRAINT fk_deposit_transactions_contract FOREIGN KEY (contract_id) REFERENCES public.deposit_contracts(contract_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_transactions
    ADD CONSTRAINT fk_deposit_transactions_counterparty FOREIGN KEY (counterparty_account_id) REFERENCES public.deposit_accounts(account_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_transactions
    ADD CONSTRAINT fk_deposit_transactions_original FOREIGN KEY (original_transaction_id) REFERENCES public.deposit_transactions(transaction_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public.deposit_subscription_payment_recognition_history
    ADD CONSTRAINT fk_dspr_history_contract FOREIGN KEY (contract_id) REFERENCES public.deposit_contracts(contract_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ONLY public."원장"
    ADD CONSTRAINT fk_ledger_payment FOREIGN KEY ("결제지시번호") REFERENCES public."결제지시"("결제지시번호");

ALTER TABLE ONLY public.loan_account
    ADD CONSTRAINT fk_loan_account_common FOREIGN KEY (account_id) REFERENCES public.common_account(account_id);

ALTER TABLE ONLY public.loan_account
    ADD CONSTRAINT fk_loan_account_contract FOREIGN KEY (contract_id2) REFERENCES public.common_contract(contract_id);

ALTER TABLE ONLY public.loan_account
    ADD CONSTRAINT fk_loan_account_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.loan_account
    ADD CONSTRAINT fk_loan_account_product FOREIGN KEY (product_id3) REFERENCES public.common_product(product_id);

ALTER TABLE ONLY public.loan_application
    ADD CONSTRAINT fk_loan_app_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.loan_application
    ADD CONSTRAINT fk_loan_app_product FOREIGN KEY (product_id) REFERENCES public.common_product(product_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT fk_loan_contract_application FOREIGN KEY (loan_application_id) REFERENCES public.loan_application(application_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT fk_loan_contract_common FOREIGN KEY (contract_id) REFERENCES public.common_contract(contract_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT fk_loan_contract_product FOREIGN KEY (product_id) REFERENCES public.loan_product(product_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT fk_loan_contract_review FOREIGN KEY (loan_review_id) REFERENCES public.loan_review(loan_review_id);

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT fk_loan_exec_account FOREIGN KEY (disbursement_account_id) REFERENCES public.common_account(account_id);

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT fk_loan_exec_before FOREIGN KEY (before_execution_id) REFERENCES public.loan_execution(execution_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT fk_loan_exec_contract FOREIGN KEY (contract_id) REFERENCES public.common_contract(contract_id);

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT fk_loan_exec_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT fk_loan_exec_reverses FOREIGN KEY (reverses_execution_id) REFERENCES public.loan_execution(execution_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.loan_product
    ADD CONSTRAINT fk_loan_product_common FOREIGN KEY (product_id) REFERENCES public.common_product(product_id);

ALTER TABLE ONLY public.loan_repayment
    ADD CONSTRAINT fk_loan_repay_account FOREIGN KEY (loan_account_id) REFERENCES public.loan_account(account_id);

ALTER TABLE ONLY public.loan_repayment
    ADD CONSTRAINT fk_loan_repay_contract FOREIGN KEY (contract_id) REFERENCES public.common_contract(contract_id);

ALTER TABLE ONLY public.loan_repayment
    ADD CONSTRAINT fk_loan_repay_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.loan_repayment
    ADD CONSTRAINT fk_loan_repay_previous FOREIGN KEY (previous_repayment_id) REFERENCES public.loan_repayment(repayment_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.loan_repayment
    ADD CONSTRAINT fk_loan_repay_reverses FOREIGN KEY (reverses_repayment_id) REFERENCES public.loan_repayment(repayment_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.loan_review
    ADD CONSTRAINT fk_loan_review_application FOREIGN KEY (application_id) REFERENCES public.loan_application(application_id);

ALTER TABLE ONLY public.loan_review
    ADD CONSTRAINT fk_loan_review_previous FOREIGN KEY (previous_review_id) REFERENCES public.loan_review(loan_review_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE ONLY public.loan_review
    ADD CONSTRAINT fk_loan_review_product FOREIGN KEY (product_id) REFERENCES public.common_product(product_id);

ALTER TABLE ONLY public.party_organization
    ADD CONSTRAINT fk_party_org_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.party_person
    ADD CONSTRAINT fk_party_person_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.party_relation
    ADD CONSTRAINT fk_party_relation_from FOREIGN KEY (from_party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.party_relation
    ADD CONSTRAINT fk_party_relation_to FOREIGN KEY (to_party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public.party_role
    ADD CONSTRAINT fk_party_role_party FOREIGN KEY (party_id) REFERENCES public.party(party_id);

ALTER TABLE ONLY public."결제지시"
    ADD CONSTRAINT fk_payment_instr_token FOREIGN KEY ("인증토큰번호") REFERENCES public."인증토큰"("인증토큰번호");

ALTER TABLE ONLY public.deposit_payment_schedules
    ADD CONSTRAINT fk_payment_schedule_account FOREIGN KEY (account_id) REFERENCES public.deposit_accounts(account_id);

ALTER TABLE ONLY public.deposit_payment_schedules
    ADD CONSTRAINT fk_payment_schedule_contract FOREIGN KEY (contract_id) REFERENCES public.deposit_contracts(contract_id);

ALTER TABLE ONLY public."상태이력"
    ADD CONSTRAINT fk_status_history_payment FOREIGN KEY ("결제지시번호") REFERENCES public."결제지시"("결제지시번호");

ALTER TABLE ONLY public.common_terms_consent
    ADD CONSTRAINT fk_terms_consent_customer FOREIGN KEY (customer_id) REFERENCES public.customer(customer_id);

ALTER TABLE ONLY public.common_terms_consent
    ADD CONSTRAINT fk_terms_consent_template FOREIGN KEY (terms_template_id) REFERENCES public.common_terms_template(terms_template_id);

ALTER TABLE ONLY public.terms_target_map
    ADD CONSTRAINT fk_terms_target_map_template FOREIGN KEY (terms_template_id) REFERENCES public.common_terms_template(terms_template_id);


-- ============================================================================
-- SERVICE: loan-service + advisory-service
-- DATABASE: loan_db
-- ============================================================================
\connect loan_db

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';

CREATE TABLE public.access_audit_log (
    log_id bigint NOT NULL,
    actor_id bigint NOT NULL,
    target_type character varying(50) NOT NULL,
    target_id bigint NOT NULL,
    action_cd character varying(30) NOT NULL,
    branch_id character varying(10),
    break_glass_reason text,
    logged_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    CONSTRAINT chk_aal_action_cd CHECK (((action_cd)::text = ANY ((ARRAY['VIEW'::character varying, 'UNMASK'::character varying, 'BREAK_GLASS'::character varying])::text[]))),
    CONSTRAINT chk_aal_target_type CHECK (((target_type)::text = ANY ((ARRAY['LOAN_APPLICATION'::character varying, 'LOAN_REVIEW'::character varying, 'DOCUMENT'::character varying])::text[])))
);

ALTER TABLE public.access_audit_log ALTER COLUMN log_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.access_audit_log_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.advisory_case_index (
    case_idx_id bigint NOT NULL,
    rev_id bigint NOT NULL,
    decision_cd character varying(50) NOT NULL,
    overturn_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    credit_score integer,
    credit_score_band_cd character varying(50),
    dsr_ratio_bps integer,
    ltv_ratio_bps integer,
    cohort_age_band_cd character varying(50),
    cohort_employment_type_cd character varying(50),
    cohort_loan_purpose_cd character varying(50),
    summary_text text,
    embedding_model_cd character varying(50) NOT NULL,
    embedding public.vector(1536),
    indexed_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.advisory_case_index_case_idx_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.advisory_case_index_case_idx_id_seq OWNED BY public.advisory_case_index.case_idx_id;

CREATE TABLE public.advisory_document (
    doc_id bigint NOT NULL,
    doc_cd character varying(50) NOT NULL,
    doc_title character varying(500) NOT NULL,
    doc_category_cd character varying(50) NOT NULL,
    doc_version character varying(50) NOT NULL,
    effective_start_date character varying(8),
    effective_end_date character varying(8),
    source_uri character varying(500),
    active_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    doc_desc character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE TABLE public.advisory_document_chunk (
    chunk_id bigint NOT NULL,
    doc_id bigint NOT NULL,
    chunk_seq integer NOT NULL,
    chunk_text text NOT NULL,
    section_path character varying(500),
    chunk_token_count integer,
    embedding_model_cd character varying(50) NOT NULL,
    embedding public.vector(1536),
    chunk_meta jsonb,
    indexed_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.advisory_document_chunk_chunk_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.advisory_document_chunk_chunk_id_seq OWNED BY public.advisory_document_chunk.chunk_id;

CREATE SEQUENCE public.advisory_document_doc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.advisory_document_doc_id_seq OWNED BY public.advisory_document.doc_id;

CREATE TABLE public.advisory_retrieval_log (
    retr_id bigint NOT NULL,
    advr_id bigint,
    retrieval_kind_cd character varying(50) NOT NULL,
    rule_cd character varying(50),
    query_text text,
    query_embedding_model_cd character varying(50) NOT NULL,
    result_count integer DEFAULT 0 NOT NULL,
    top_score numeric(10,6),
    result_detail jsonb,
    requested_by bigint,
    requested_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.advisory_retrieval_log_retr_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.advisory_retrieval_log_retr_id_seq OWNED BY public.advisory_retrieval_log.retr_id;

CREATE TABLE public.ai_audit_opinion (
    opinion_id bigint NOT NULL,
    advr_id bigint,
    rev_id bigint NOT NULL,
    reviewer_id bigint,
    analysis_type_cd character varying(50) NOT NULL,
    conclusion_cd character varying(50) NOT NULL,
    reasoning_summary character varying(2000),
    confidence_score numeric(5,4),
    input_tokens integer,
    output_tokens integer,
    generated_at timestamp with time zone DEFAULT now() NOT NULL,
    cited_chunk_ids text
);

CREATE SEQUENCE public.ai_audit_opinion_opinion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.ai_audit_opinion_opinion_id_seq OWNED BY public.ai_audit_opinion.opinion_id;

CREATE TABLE public.ai_review_advice (
    advice_id bigint NOT NULL,
    rev_id bigint NOT NULL,
    advice_type_cd character varying(40) NOT NULL,
    severity_cd character varying(20),
    advice_body text NOT NULL,
    model character varying(80),
    model_version character varying(40),
    prompt_hash character(64),
    input_token integer,
    output_token integer,
    latency_ms integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint
);

CREATE SEQUENCE public.ai_review_advice_advice_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.ai_review_advice_advice_id_seq OWNED BY public.ai_review_advice.advice_id;

CREATE TABLE public.auto_debit_clearing_pending (
    pending_id bigint NOT NULL,
    pi_id character varying(100) NOT NULL,
    cntr_id bigint NOT NULL,
    rsch_id bigint NOT NULL,
    installment_no integer NOT NULL,
    base_date character(8) NOT NULL,
    idempotency_key character varying(100) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    resolved_at timestamp with time zone,
    CONSTRAINT chk_adcp_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'DONE'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE SEQUENCE public.auto_debit_clearing_pending_pending_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.auto_debit_clearing_pending_pending_id_seq OWNED BY public.auto_debit_clearing_pending.pending_id;

CREATE TABLE public.batch_job_execution (
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);

CREATE TABLE public.batch_job_execution_context (
    job_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);

CREATE TABLE public.batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    parameter_name character varying(100) NOT NULL,
    parameter_type character varying(100) NOT NULL,
    parameter_value character varying(2500),
    identifying character(1) NOT NULL
);

CREATE SEQUENCE public.batch_job_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.batch_job_instance (
    job_instance_id bigint NOT NULL,
    version bigint,
    job_name character varying(100) NOT NULL,
    job_key character varying(32) NOT NULL
);

CREATE SEQUENCE public.batch_job_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.batch_step_execution (
    step_execution_id bigint NOT NULL,
    version bigint NOT NULL,
    step_name character varying(100) NOT NULL,
    job_execution_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);

CREATE TABLE public.batch_step_execution_context (
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);

CREATE SEQUENCE public.batch_step_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.branch (
    branch_id character varying(10) NOT NULL,
    branch_name character varying(100) NOT NULL,
    created_at timestamp(3) with time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL
);

CREATE TABLE public.business_calendar (
    cal_id bigint NOT NULL,
    cal_date character varying(8) NOT NULL,
    business_day_yn character(1) NOT NULL,
    holiday_type_cd character varying(50),
    holiday_name character varying(100),
    base_country_cd character varying(10),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.business_calendar_cal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.business_calendar_cal_id_seq OWNED BY public.business_calendar.cal_id;

CREATE TABLE public.collateral (
    col_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    col_type_cd character varying(50) NOT NULL,
    col_status_cd character varying(50) NOT NULL,
    col_no character varying(30) NOT NULL,
    col_name character varying(200),
    col_address character varying(500),
    col_registry_no character varying(100),
    declared_value bigint,
    currency_cd character varying(10) DEFAULT 'KRW'::character varying NOT NULL,
    ownership_type_cd character varying(50),
    senior_lien_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    senior_lien_amount bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.collateral_col_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.collateral_col_id_seq OWNED BY public.collateral.col_id;

CREATE TABLE public.collateral_evaluation (
    ceval_col_id bigint NOT NULL,
    col_id bigint NOT NULL,
    eval_method_cd character varying(50) NOT NULL,
    eval_agency_cd character varying(50),
    appraised_value bigint NOT NULL,
    applied_value bigint NOT NULL,
    eval_status_cd character varying(50) NOT NULL,
    eval_report_url character varying(500),
    eval_report_hash character varying(128),
    evaluated_at timestamp with time zone,
    applied_start_date character varying(8),
    applied_end_date character varying(8),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.collateral_evaluation_ceval_col_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.collateral_evaluation_ceval_col_id_seq OWNED BY public.collateral_evaluation.ceval_col_id;

CREATE TABLE public.common_sync_outbox (
    outbox_id bigint NOT NULL,
    target_type_cd character varying(20) NOT NULL,
    source_id bigint NOT NULL,
    source_no character varying(50),
    payload jsonb,
    common_id bigint,
    status character varying(50) NOT NULL,
    attempt_no integer DEFAULT 0 NOT NULL,
    max_attempt integer DEFAULT 5 NOT NULL,
    next_attempt_at timestamp with time zone NOT NULL,
    last_error character varying(500),
    synced_at timestamp with time zone,
    idempotency_key character varying(100) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_common_sync_outbox_target CHECK (((target_type_cd)::text = ANY ((ARRAY['PRODUCT'::character varying, 'CONTRACT'::character varying, 'TRANSACTION'::character varying])::text[])))
);

CREATE SEQUENCE public.common_sync_outbox_outbox_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_sync_outbox_outbox_id_seq OWNED BY public.common_sync_outbox.outbox_id;

CREATE TABLE public.credit_consent (
    csnt_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    consent_type_cd character varying(50) NOT NULL,
    consent_scope_cd character varying(50) NOT NULL,
    consent_target_cd character varying(50) NOT NULL,
    consent_yn character(1) NOT NULL,
    consented_at timestamp with time zone NOT NULL,
    consent_method_cd character varying(50),
    consent_token character varying(100),
    signed_doc_url character varying(500),
    signed_doc_hash character varying(128),
    client_ip character varying(64),
    device character varying(200),
    retention_until character varying(8),
    withdrawn_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    withdrawn_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.credit_consent_csnt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.credit_consent_csnt_id_seq OWNED BY public.credit_consent.csnt_id;

CREATE TABLE public.credit_evaluation (
    ceval_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    ceval_engine character varying(50) NOT NULL,
    ceval_engine_version character varying(50),
    ceval_grade character varying(10),
    ceval_score integer,
    pd_bps integer,
    ceval_decision_cd character varying(50) NOT NULL,
    eval_limit_amount bigint,
    eval_rate_bps integer,
    ceval_status_cd character varying(50) NOT NULL,
    ceval_factors jsonb,
    evaluated_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.credit_evaluation_ceval_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.credit_evaluation_ceval_id_seq OWNED BY public.credit_evaluation.ceval_id;

CREATE TABLE public.credit_info_report (
    crpt_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    crpt_type_cd character varying(50) NOT NULL,
    crpt_agency_cd character varying(50) NOT NULL,
    crpt_status_cd character varying(50) NOT NULL,
    report_target_cd character varying(50) NOT NULL,
    report_reason_cd character varying(50),
    report_payload jsonb,
    external_tx_no character varying(100),
    reported_at timestamp with time zone,
    ack_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    dlq_id bigint,
    external_ack_no character varying(100)
);

CREATE SEQUENCE public.credit_info_report_crpt_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.credit_info_report_crpt_id_seq OWNED BY public.credit_info_report.crpt_id;

CREATE TABLE public.credit_info_report_outbox (
    outbox_id bigint NOT NULL,
    crpt_id bigint NOT NULL,
    status character varying(50) NOT NULL,
    attempt_no integer DEFAULT 0 NOT NULL,
    max_attempt integer DEFAULT 5 NOT NULL,
    next_attempt_at timestamp with time zone NOT NULL,
    last_error character varying(500),
    sent_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.credit_info_report_outbox_outbox_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.credit_info_report_outbox_outbox_id_seq OWNED BY public.credit_info_report_outbox.outbox_id;

CREATE TABLE public.daily_accounting_summary (
    das_id bigint NOT NULL,
    summary_date character varying(8) NOT NULL,
    interest_revenue bigint DEFAULT 0 NOT NULL,
    overdue_interest_revenue bigint DEFAULT 0 NOT NULL,
    auto_debit_principal bigint DEFAULT 0 NOT NULL,
    auto_debit_interest bigint DEFAULT 0 NOT NULL,
    auto_debit_overdue_interest bigint DEFAULT 0 NOT NULL,
    auto_debit_count integer DEFAULT 0 NOT NULL,
    disbursed_amount bigint DEFAULT 0 NOT NULL,
    disbursed_count integer DEFAULT 0 NOT NULL,
    active_contract_count integer DEFAULT 0 NOT NULL,
    active_delinquency_count integer DEFAULT 0 NOT NULL,
    summarized_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.daily_accounting_summary_das_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.daily_accounting_summary_das_id_seq OWNED BY public.daily_accounting_summary.das_id;

CREATE TABLE public.delinquency (
    dlq_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    dlq_status_cd character varying(50) NOT NULL,
    dlq_start_date character varying(8) NOT NULL,
    dlq_end_date character varying(8),
    dlq_days integer DEFAULT 0 NOT NULL,
    dlq_principal_amt bigint DEFAULT 0 NOT NULL,
    dlq_interest_amt bigint DEFAULT 0 NOT NULL,
    dlq_total_amt bigint DEFAULT 0 NOT NULL,
    overdue_rate_bps integer DEFAULT 0 NOT NULL,
    dlq_stage_cd character varying(50) NOT NULL,
    resolved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE TABLE public.delinquency_daily_snapshot (
    dlqs_id bigint NOT NULL,
    dlq_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    snapshot_date character varying(8) NOT NULL,
    dlq_days integer NOT NULL,
    dlq_principal_amt bigint DEFAULT 0 NOT NULL,
    dlq_interest_amt bigint DEFAULT 0 NOT NULL,
    dlq_total_amt bigint DEFAULT 0 NOT NULL,
    overdue_rate_bps integer DEFAULT 0 NOT NULL,
    dlq_stage_cd character varying(50) NOT NULL,
    snapshotted_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.delinquency_daily_snapshot_dlqs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.delinquency_daily_snapshot_dlqs_id_seq OWNED BY public.delinquency_daily_snapshot.dlqs_id;

CREATE SEQUENCE public.delinquency_dlq_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.delinquency_dlq_id_seq OWNED BY public.delinquency.dlq_id;

CREATE TABLE public.dsr_calculation (
    dsr_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    annual_income_amt bigint NOT NULL,
    existing_principal_total bigint DEFAULT 0 NOT NULL,
    existing_annual_repay_amt bigint DEFAULT 0 NOT NULL,
    new_annual_repay_amt bigint NOT NULL,
    total_annual_repay_amt bigint NOT NULL,
    dsr_ratio_bps integer NOT NULL,
    dsr_limit_bps integer NOT NULL,
    dsr_status_cd character varying(50) NOT NULL,
    dsr_reg_type_cd character varying(50),
    calculated_at timestamp with time zone NOT NULL,
    calc_engine_version character varying(50),
    dsr_detail jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.dsr_calculation_dsr_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.dsr_calculation_dsr_id_seq OWNED BY public.dsr_calculation.dsr_id;

CREATE TABLE public.guarantee_insurance (
    gins_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    gins_agency_cd character varying(50) NOT NULL,
    gins_policy_no character varying(50) NOT NULL,
    guarantee_amount bigint NOT NULL,
    guarantee_ratio_bps integer NOT NULL,
    premium_amount bigint NOT NULL,
    gins_status_cd character varying(50) NOT NULL,
    gins_start_date character varying(8) NOT NULL,
    gins_end_date character varying(8) NOT NULL,
    gins_doc_url character varying(500),
    gins_doc_hash character varying(128),
    issued_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.guarantee_insurance_gins_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.guarantee_insurance_gins_id_seq OWNED BY public.guarantee_insurance.gins_id;

CREATE TABLE public.guarantor_agreement (
    gagr_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    gmst_id bigint NOT NULL,
    gagr_type_cd character varying(50) NOT NULL,
    guarantee_amount bigint NOT NULL,
    guarantee_ratio_bps integer,
    gagr_status_cd character varying(50) NOT NULL,
    consented_at timestamp with time zone,
    signed_doc_url character varying(500),
    signed_doc_hash character varying(128),
    client_ip character varying(64),
    device character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.guarantor_agreement_gagr_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.guarantor_agreement_gagr_id_seq OWNED BY public.guarantor_agreement.gagr_id;

CREATE TABLE public.guarantor_master (
    gmst_id bigint NOT NULL,
    guarantor_name_enc bytea NOT NULL,
    guarantor_name_masked character varying(50),
    guarantor_ci_hash character varying(128) NOT NULL,
    relation_type_cd character varying(50),
    mobile_no_enc bytea,
    mobile_no_masked character varying(20),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.guarantor_master_gmst_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.guarantor_master_gmst_id_seq OWNED BY public.guarantor_master.gmst_id;

CREATE TABLE public.interest_accrual (
    iacc_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    accrual_date character varying(8) NOT NULL,
    principal_balance bigint NOT NULL,
    applied_rate_bps integer NOT NULL,
    day_count_basis_cd character varying(50) NOT NULL,
    daily_interest_amt bigint NOT NULL,
    cumulative_interest_amt bigint NOT NULL,
    iacc_status_cd character varying(50) NOT NULL,
    accrued_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.interest_accrual_iacc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.interest_accrual_iacc_id_seq OWNED BY public.interest_accrual.iacc_id;

CREATE TABLE public.loan_application (
    appl_id bigint NOT NULL,
    appl_no character varying(30) NOT NULL,
    customer_id bigint NOT NULL,
    prod_id bigint NOT NULL,
    channel_cd character varying(50) NOT NULL,
    requested_amount bigint NOT NULL,
    requested_period_mo integer NOT NULL,
    loan_purpose_cd character varying(50),
    repayment_method_cd character varying(50) NOT NULL,
    estimated_income_amt bigint,
    employment_type_cd character varying(50),
    appl_status_cd character varying(50) NOT NULL,
    applied_at timestamp with time zone NOT NULL,
    client_ip character varying(64),
    device character varying(200),
    idempotency_key character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    branch_id character varying(10)
);

CREATE SEQUENCE public.loan_application_appl_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_application_appl_id_seq OWNED BY public.loan_application.appl_id;

CREATE TABLE public.loan_certificate (
    cert_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    cert_type_cd character varying(50) NOT NULL,
    cert_no character varying(50) NOT NULL,
    cert_status_cd character varying(50) NOT NULL,
    cert_purpose_cd character varying(50),
    cert_doc_url character varying(500),
    cert_doc_hash character varying(128),
    issue_channel_cd character varying(50),
    issued_at timestamp with time zone,
    retention_until character varying(8),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.loan_certificate_cert_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_certificate_cert_id_seq OWNED BY public.loan_certificate.cert_id;

CREATE TABLE public.loan_closure (
    clos_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    clos_type_cd character varying(50) NOT NULL,
    clos_reason_cd character varying(50),
    clos_status_cd character varying(50) NOT NULL,
    final_principal_amt bigint DEFAULT 0 NOT NULL,
    final_interest_amt bigint DEFAULT 0 NOT NULL,
    final_fee_amt bigint DEFAULT 0 NOT NULL,
    prepayment_fee_amt bigint DEFAULT 0 NOT NULL,
    total_settled_amt bigint DEFAULT 0 NOT NULL,
    clos_date character varying(8) NOT NULL,
    closed_at timestamp with time zone,
    clos_doc_url character varying(500),
    clos_doc_hash character varying(128),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    write_off_amount bigint,
    subrogation_amount bigint,
    subrogation_party_ref character varying(200),
    write_off_reason_cd character varying(50)
);

CREATE SEQUENCE public.loan_closure_clos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_closure_clos_id_seq OWNED BY public.loan_closure.clos_id;

CREATE TABLE public.loan_contract (
    cntr_id bigint NOT NULL,
    cntr_no character varying(30) NOT NULL,
    contract_id bigint,
    appl_id bigint NOT NULL,
    rev_id bigint,
    customer_id bigint NOT NULL,
    prod_id bigint NOT NULL,
    contracted_amount bigint NOT NULL,
    currency_cd character varying(10) DEFAULT 'KRW'::character varying NOT NULL,
    contracted_period_mo integer NOT NULL,
    total_rate_bps integer NOT NULL,
    base_rate_bps integer NOT NULL,
    spread_bps integer DEFAULT 0 NOT NULL,
    preferential_rate_bps integer DEFAULT 0 NOT NULL,
    rate_type_cd character varying(50) NOT NULL,
    repayment_method_cd character varying(50) NOT NULL,
    cntr_status_cd character varying(50) NOT NULL,
    cntr_start_date character varying(8) NOT NULL,
    cntr_end_date character varying(8) NOT NULL,
    cntr_doc_url character varying(500),
    cntr_doc_hash character varying(128),
    signed_at timestamp with time zone,
    client_ip character varying(64),
    device character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.loan_contract_cntr_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_contract_cntr_id_seq OWNED BY public.loan_contract.cntr_id;

CREATE TABLE public.loan_document (
    doc_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    doc_type_cd character varying(50) NOT NULL,
    doc_status_cd character varying(50) NOT NULL,
    doc_source_cd character varying(50),
    doc_name character varying(200),
    doc_url character varying(500),
    doc_hash character varying(128),
    mime_type character varying(100),
    file_size_bytes bigint,
    submitted_at timestamp with time zone,
    verified_at timestamp with time zone,
    verify_result_cd character varying(50),
    retention_until character varying(8),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.loan_document_doc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_document_doc_id_seq OWNED BY public.loan_document.doc_id;

CREATE TABLE public.loan_document_submission (
    submission_id character varying(36) NOT NULL,
    doc_id bigint,
    application_id character varying(30) NOT NULL,
    doc_code character varying(50) NOT NULL,
    verify_status character varying(50),
    confidence_score numeric(5,4),
    occurred_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.loan_ecl_summary (
    ecl_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    summary_month character varying(6) NOT NULL,
    ifrs_stage_cd character varying(50) NOT NULL,
    pd_bps integer NOT NULL,
    lgd_bps integer NOT NULL,
    ead bigint NOT NULL,
    ecl bigint NOT NULL,
    engine_version character varying(50) NOT NULL,
    calculated_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.loan_ecl_summary_ecl_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_ecl_summary_ecl_id_seq OWNED BY public.loan_ecl_summary.ecl_id;

CREATE TABLE public.loan_execution (
    exec_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    transaction_id bigint,
    executed_amount bigint NOT NULL,
    currency_cd character varying(10) DEFAULT 'KRW'::character varying NOT NULL,
    exec_status_cd character varying(50) NOT NULL,
    disbursement_bank_cd character varying(10),
    disbursement_account_enc bytea,
    disbursement_account_masked character varying(50),
    executed_at timestamp with time zone,
    value_date character varying(8),
    fee_amount bigint DEFAULT 0 NOT NULL,
    idempotency_key character varying(100),
    journal_entry_no character varying(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    pi_id character varying(100)
);

CREATE SEQUENCE public.loan_execution_exec_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_execution_exec_id_seq OWNED BY public.loan_execution.exec_id;

CREATE TABLE public.loan_identity_verification (
    idv_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    idv_method_cd character varying(50) NOT NULL,
    idv_status_cd character varying(50) NOT NULL,
    idv_result_cd character varying(50),
    idv_target_cd character varying(50) NOT NULL,
    ci_hash character varying(128),
    di_hash character varying(128),
    mobile_no_enc bytea,
    mobile_no_masked character varying(20),
    verified_at timestamp with time zone,
    client_ip character varying(64),
    device character varying(200),
    external_tx_no character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.loan_identity_verification_idv_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_identity_verification_idv_id_seq OWNED BY public.loan_identity_verification.idv_id;

CREATE TABLE public.loan_prescreening (
    presc_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    presc_result_cd character varying(50) NOT NULL,
    estimated_limit_amt bigint,
    estimated_rate_bps integer,
    estimated_grade character varying(10),
    estimated_score integer,
    reject_reason_cd character varying(50),
    presc_remark character varying(500),
    prescreened_at timestamp with time zone NOT NULL,
    presc_engine_version character varying(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    ai_track_cd character varying(20)
);

COMMENT ON COLUMN public.loan_prescreening.ai_track_cd IS 'AI 트랙 분기 결과 (TRACK_1/2/3) — PASS 건만 저장';

CREATE SEQUENCE public.loan_prescreening_presc_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_prescreening_presc_id_seq OWNED BY public.loan_prescreening.presc_id;

CREATE TABLE public.loan_product (
    prod_id bigint NOT NULL,
    product_id bigint,
    prod_cd character varying(30) NOT NULL,
    prod_name character varying(200) NOT NULL,
    loan_type_cd character varying(50) NOT NULL,
    target_customer_cd character varying(50),
    repayment_method_cd character varying(50) NOT NULL,
    rate_type_cd character varying(50) NOT NULL,
    base_rate_bps integer NOT NULL,
    min_rate_bps integer,
    max_rate_bps integer,
    min_amount bigint NOT NULL,
    max_amount bigint NOT NULL,
    min_period_mo integer NOT NULL,
    max_period_mo integer NOT NULL,
    collateral_required_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    guarantor_required_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    sale_start_date character varying(8),
    sale_end_date character varying(8),
    prod_status_cd character varying(50) NOT NULL,
    prod_terms_url character varying(500),
    prod_terms_hash character varying(128),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    min_guarantor_count integer DEFAULT 0 NOT NULL,
    application_validity_days integer
);

CREATE SEQUENCE public.loan_product_prod_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_product_prod_id_seq OWNED BY public.loan_product.prod_id;

CREATE TABLE public.loan_review (
    rev_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    rev_type_cd character varying(50) NOT NULL,
    rev_status_cd character varying(50) NOT NULL,
    rev_decision_cd character varying(50),
    approved_amount bigint,
    approved_rate_bps integer,
    approved_period_mo integer,
    reject_reason_cd character varying(50),
    rev_remark character varying(500),
    reviewer_id bigint,
    reviewed_at timestamp with time zone,
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    agent_opinion_json jsonb,
    approver_id bigint,
    approved_decision_cd character varying(50),
    override_reason_cd character varying(50),
    override_remark character varying(500),
    bias_severity_cd character varying(20),
    bias_override_by bigint,
    bias_override_reason character varying(500),
    bias_overridden_at timestamp with time zone,
    pending_approver_since timestamp with time zone,
    rev_ai_track_cd character varying(20),
    rev_ai_pd numeric(10,6),
    rev_ai_rationale text,
    owner_id bigint,
    escalated_at timestamp(3) with time zone,
    CONSTRAINT chk_agent_opinion_json_size CHECK ((pg_column_size(agent_opinion_json) < 65536))
);

COMMENT ON COLUMN public.loan_review.agent_opinion_json IS 'Pre-Review Agent 의견 JSON (schema_version v1). NULL = 에이전트 미실행 또는 fallback.';

COMMENT ON COLUMN public.loan_review.rev_ai_track_cd IS 'AI 트랙 분기 결과 (TRACK_1/2/3)';

COMMENT ON COLUMN public.loan_review.rev_ai_pd IS 'AI PD 스코어 (0~1)';

COMMENT ON COLUMN public.loan_review.rev_ai_rationale IS 'AI 결정 근거 한 줄 요약';

CREATE TABLE public.loan_review_outbox (
    outbox_id bigint NOT NULL,
    aggregate_id bigint NOT NULL,
    event_type_cd character varying(50) NOT NULL,
    payload jsonb NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    attempt_no integer DEFAULT 0 NOT NULL,
    max_attempt integer DEFAULT 5 NOT NULL,
    next_attempt_at timestamp with time zone DEFAULT now() NOT NULL,
    last_error character varying(500),
    sent_at timestamp with time zone,
    idempotency_key character varying(200) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE public.loan_review_outbox_outbox_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_review_outbox_outbox_id_seq OWNED BY public.loan_review_outbox.outbox_id;

CREATE SEQUENCE public.loan_review_rev_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_review_rev_id_seq OWNED BY public.loan_review.rev_id;

CREATE TABLE public.ltv_calculation (
    ltv_id bigint NOT NULL,
    appl_id bigint NOT NULL,
    col_id bigint NOT NULL,
    applied_col_value bigint NOT NULL,
    senior_lien_amount bigint,
    requested_amount bigint NOT NULL,
    ltv_ratio_bps integer NOT NULL,
    ltv_limit_bps integer NOT NULL,
    max_loan_amount bigint NOT NULL,
    ltv_status_cd character varying(50) NOT NULL,
    calculated_at timestamp with time zone NOT NULL,
    calc_engine_version character varying(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.ltv_calculation_ltv_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.ltv_calculation_ltv_id_seq OWNED BY public.ltv_calculation.ltv_id;

CREATE TABLE public.maturity (
    mat_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    original_maturity_date character varying(8) NOT NULL,
    current_maturity_date character varying(8) NOT NULL,
    mat_status_cd character varying(50) NOT NULL,
    extension_type_cd character varying(50),
    extension_count integer DEFAULT 0 NOT NULL,
    last_extended_date character varying(8),
    extended_period_mo integer,
    notice_status_cd character varying(50),
    last_notice_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.maturity_mat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.maturity_mat_id_seq OWNED BY public.maturity.mat_id;

CREATE TABLE public.monthly_accounting_summary (
    mas_id bigint NOT NULL,
    summary_month character varying(6) NOT NULL,
    base_month_start_date character varying(8) NOT NULL,
    base_month_end_date character varying(8) NOT NULL,
    interest_revenue bigint DEFAULT 0 NOT NULL,
    overdue_interest_revenue bigint DEFAULT 0 NOT NULL,
    auto_debit_principal bigint DEFAULT 0 NOT NULL,
    auto_debit_interest bigint DEFAULT 0 NOT NULL,
    auto_debit_overdue_interest bigint DEFAULT 0 NOT NULL,
    auto_debit_count integer DEFAULT 0 NOT NULL,
    new_disbursed_amount bigint DEFAULT 0 NOT NULL,
    new_disbursed_count integer DEFAULT 0 NOT NULL,
    month_end_active_contracts integer DEFAULT 0 NOT NULL,
    month_end_active_delinquencies integer DEFAULT 0 NOT NULL,
    month_end_npl_count integer DEFAULT 0 NOT NULL,
    month_end_npl_principal bigint DEFAULT 0 NOT NULL,
    summarized_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.monthly_accounting_summary_mas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.monthly_accounting_summary_mas_id_seq OWNED BY public.monthly_accounting_summary.mas_id;

CREATE TABLE public.notification_outbox (
    outbox_id bigint NOT NULL,
    event_type_cd character varying(50) NOT NULL,
    reference_id bigint NOT NULL,
    channel_cd character varying(50) NOT NULL,
    payload jsonb,
    status character varying(50) NOT NULL,
    attempt_no integer DEFAULT 0 NOT NULL,
    max_attempt integer DEFAULT 5 NOT NULL,
    next_attempt_at timestamp with time zone NOT NULL,
    last_error character varying(500),
    sent_at timestamp with time zone,
    idempotency_key character varying(200) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.notification_outbox_outbox_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.notification_outbox_outbox_id_seq OWNED BY public.notification_outbox.outbox_id;

CREATE TABLE public.overdue_accrual (
    oa_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    dlq_id bigint NOT NULL,
    accrual_date character varying(8) NOT NULL,
    overdue_principal bigint NOT NULL,
    overdue_rate_bps integer NOT NULL,
    dlq_days integer NOT NULL,
    daily_overdue_interest bigint NOT NULL,
    cumulative_overdue_interest bigint NOT NULL,
    oa_status_cd character varying(50) NOT NULL,
    accrued_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.overdue_accrual_oa_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.overdue_accrual_oa_id_seq OWNED BY public.overdue_accrual.oa_id;

CREATE TABLE public.preferential_rate_policy (
    policy_id bigint NOT NULL,
    prod_id bigint NOT NULL,
    policy_name character varying(200) NOT NULL,
    condition_cd character varying(50) NOT NULL,
    preferential_rate_bps integer NOT NULL,
    max_stack_bps integer,
    active_yn character(1) DEFAULT 'Y'::bpchar NOT NULL,
    effective_start_date character varying(8),
    effective_end_date character varying(8),
    policy_remark character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.preferential_rate_policy_policy_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.preferential_rate_policy_policy_id_seq OWNED BY public.preferential_rate_policy.policy_id;

CREATE TABLE public.rate_change_history (
    rchg_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    rate_change_reason_cd character varying(50) NOT NULL,
    previous_rate_bps integer NOT NULL,
    new_rate_bps integer NOT NULL,
    base_rate_bps integer NOT NULL,
    spread_bps integer DEFAULT 0 NOT NULL,
    preferential_rate_bps integer DEFAULT 0 NOT NULL,
    applied_start_date character varying(8) NOT NULL,
    applied_end_date character varying(8),
    changed_at timestamp with time zone NOT NULL,
    changed_by bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.rate_change_history_rchg_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.rate_change_history_rchg_id_seq OWNED BY public.rate_change_history.rchg_id;

CREATE TABLE public.repayment_account (
    racct_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    account_id bigint,
    account_no_masked character varying(50),
    account_no_enc bytea,
    bank_cd character varying(10) NOT NULL,
    holder_name_masked character varying(50),
    racct_status_cd character varying(50) NOT NULL,
    auto_debit_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    debit_day integer,
    verified_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    holder_name_enc bytea
);

CREATE SEQUENCE public.repayment_account_racct_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.repayment_account_racct_id_seq OWNED BY public.repayment_account.racct_id;

CREATE TABLE public.repayment_schedule (
    rsch_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    installment_no integer NOT NULL,
    due_date character varying(8) NOT NULL,
    scheduled_principal bigint NOT NULL,
    scheduled_interest bigint NOT NULL,
    scheduled_total bigint NOT NULL,
    remaining_balance bigint NOT NULL,
    applied_rate_bps integer NOT NULL,
    rsch_status_cd character varying(50) NOT NULL,
    rsch_version_cd character varying(50) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    holiday_adjusted_yn character(1) DEFAULT 'N'::bpchar NOT NULL
);

CREATE SEQUENCE public.repayment_schedule_rsch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.repayment_schedule_rsch_id_seq OWNED BY public.repayment_schedule.rsch_id;

CREATE TABLE public.repayment_transaction (
    rtx_id bigint NOT NULL,
    cntr_id bigint NOT NULL,
    rsch_id bigint,
    transaction_id bigint,
    rtx_type_cd character varying(50) NOT NULL,
    total_amount bigint NOT NULL,
    principal_amount bigint DEFAULT 0 NOT NULL,
    interest_amount bigint DEFAULT 0 NOT NULL,
    overdue_interest_amount bigint DEFAULT 0 NOT NULL,
    fee_amount bigint DEFAULT 0 NOT NULL,
    currency_cd character varying(10) DEFAULT 'KRW'::character varying NOT NULL,
    channel_cd character varying(50) NOT NULL,
    rtx_status_cd character varying(50) NOT NULL,
    paid_at timestamp with time zone,
    value_date character varying(8),
    balance_after bigint,
    idempotency_key character varying(100),
    reversal_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    reversal_target_rtx_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    pi_id character varying(100)
);

CREATE SEQUENCE public.repayment_transaction_rtx_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.repayment_transaction_rtx_id_seq OWNED BY public.repayment_transaction.rtx_id;

CREATE TABLE public.review_advisory_ack (
    advk_id bigint NOT NULL,
    advr_id bigint NOT NULL,
    ack_reviewer_id bigint NOT NULL,
    ack_response_cd character varying(50) NOT NULL,
    decision_change_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    ack_reason_cd character varying(50),
    ack_remark character varying(500),
    before_decision_cd character varying(50),
    after_decision_cd character varying(50),
    acked_at timestamp with time zone NOT NULL,
    client_ip character varying(64),
    device character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.review_advisory_ack_advk_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.review_advisory_ack_advk_id_seq OWNED BY public.review_advisory_ack.advk_id;

CREATE TABLE public.review_advisory_report (
    advr_id bigint NOT NULL,
    rev_id bigint NOT NULL,
    rule_id bigint NOT NULL,
    advisory_type_cd character varying(50) NOT NULL,
    severity_cd character varying(50) NOT NULL,
    advr_status_cd character varying(50) NOT NULL,
    advr_title character varying(200) NOT NULL,
    advr_summary text,
    advr_payload jsonb,
    target_reviewer_id bigint,
    generated_at timestamp with time zone NOT NULL,
    first_viewed_at timestamp with time zone,
    resolved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL,
    quarantined_at timestamp with time zone
);

CREATE SEQUENCE public.review_advisory_report_advr_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.review_advisory_report_advr_id_seq OWNED BY public.review_advisory_report.advr_id;

CREATE TABLE public.review_advisory_rule (
    rule_id bigint NOT NULL,
    rule_cd character varying(50) NOT NULL,
    rule_name character varying(200) NOT NULL,
    advisory_type_cd character varying(50) NOT NULL,
    rule_category_cd character varying(50) NOT NULL,
    severity_cd character varying(50) NOT NULL,
    rule_params jsonb,
    rule_version character varying(50) NOT NULL,
    active_yn character(1) DEFAULT 'Y'::bpchar NOT NULL,
    effective_start_date character varying(8),
    effective_end_date character varying(8),
    rule_desc character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by bigint NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by bigint,
    version integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.review_advisory_rule_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.review_advisory_rule_rule_id_seq OWNED BY public.review_advisory_rule.rule_id;

CREATE TABLE public.review_advisory_signal (
    advs_id bigint NOT NULL,
    advr_id bigint NOT NULL,
    signal_kind_cd character varying(50) NOT NULL,
    signal_metric character varying(100) NOT NULL,
    observed_value numeric(20,6),
    threshold_value numeric(20,6),
    peer_baseline_value numeric(20,6),
    sample_size integer,
    signal_detail jsonb,
    observed_window_start character varying(8),
    observed_window_end character varying(8),
    observed_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.review_advisory_signal_advs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.review_advisory_signal_advs_id_seq OWNED BY public.review_advisory_signal.advs_id;

CREATE TABLE public.review_check_log (
    rchk_id bigint NOT NULL,
    rev_id bigint NOT NULL,
    check_item_cd character varying(50) NOT NULL,
    check_result_cd character varying(50) NOT NULL,
    check_remark character varying(500),
    checker_id bigint,
    checked_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.review_check_log_rchk_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.review_check_log_rchk_id_seq OWNED BY public.review_check_log.rchk_id;

CREATE TABLE public.reviewer_decision_snapshot (
    rds_id bigint NOT NULL,
    reviewer_id bigint NOT NULL,
    snapshot_date character varying(8) NOT NULL,
    aggregation_window_cd character varying(50) NOT NULL,
    cohort_dimension_cd character varying(50) NOT NULL,
    cohort_value character varying(100) NOT NULL,
    total_review_count integer DEFAULT 0 NOT NULL,
    approve_count integer DEFAULT 0 NOT NULL,
    reject_count integer DEFAULT 0 NOT NULL,
    pending_count integer DEFAULT 0 NOT NULL,
    approve_rate_bps integer DEFAULT 0 NOT NULL,
    reject_rate_bps integer DEFAULT 0 NOT NULL,
    peer_avg_reject_rate_bps integer,
    deviation_sigma numeric(10,4),
    snapshotted_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.reviewer_decision_snapshot_rds_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.reviewer_decision_snapshot_rds_id_seq OWNED BY public.reviewer_decision_snapshot.rds_id;

CREATE TABLE public.reviewer_risk_score (
    score_id bigint NOT NULL,
    reviewer_id bigint NOT NULL,
    bias_score numeric(5,2) DEFAULT 0 NOT NULL,
    compliance_score numeric(5,2) DEFAULT 0 NOT NULL,
    evaluation_count integer DEFAULT 0 NOT NULL,
    last_evaluated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE public.reviewer_risk_score_score_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.reviewer_risk_score_score_id_seq OWNED BY public.reviewer_risk_score.score_id;

CREATE TABLE public.status_history (
    sthist_id bigint NOT NULL,
    target_domain_cd character varying(30) NOT NULL,
    target_table_cd character varying(50) NOT NULL,
    target_id bigint NOT NULL,
    before_status_cd character varying(50),
    after_status_cd character varying(50) NOT NULL,
    change_reason_cd character varying(50),
    change_remark character varying(500),
    changed_at timestamp with time zone NOT NULL,
    changed_by bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.status_history_sthist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.status_history_sthist_id_seq OWNED BY public.status_history.sthist_id;

ALTER TABLE ONLY public.advisory_case_index ALTER COLUMN case_idx_id SET DEFAULT nextval('public.advisory_case_index_case_idx_id_seq'::regclass);

ALTER TABLE ONLY public.advisory_document ALTER COLUMN doc_id SET DEFAULT nextval('public.advisory_document_doc_id_seq'::regclass);

ALTER TABLE ONLY public.advisory_document_chunk ALTER COLUMN chunk_id SET DEFAULT nextval('public.advisory_document_chunk_chunk_id_seq'::regclass);

ALTER TABLE ONLY public.advisory_retrieval_log ALTER COLUMN retr_id SET DEFAULT nextval('public.advisory_retrieval_log_retr_id_seq'::regclass);

ALTER TABLE ONLY public.ai_audit_opinion ALTER COLUMN opinion_id SET DEFAULT nextval('public.ai_audit_opinion_opinion_id_seq'::regclass);

ALTER TABLE ONLY public.ai_review_advice ALTER COLUMN advice_id SET DEFAULT nextval('public.ai_review_advice_advice_id_seq'::regclass);

ALTER TABLE ONLY public.auto_debit_clearing_pending ALTER COLUMN pending_id SET DEFAULT nextval('public.auto_debit_clearing_pending_pending_id_seq'::regclass);

ALTER TABLE ONLY public.business_calendar ALTER COLUMN cal_id SET DEFAULT nextval('public.business_calendar_cal_id_seq'::regclass);

ALTER TABLE ONLY public.collateral ALTER COLUMN col_id SET DEFAULT nextval('public.collateral_col_id_seq'::regclass);

ALTER TABLE ONLY public.collateral_evaluation ALTER COLUMN ceval_col_id SET DEFAULT nextval('public.collateral_evaluation_ceval_col_id_seq'::regclass);

ALTER TABLE ONLY public.common_sync_outbox ALTER COLUMN outbox_id SET DEFAULT nextval('public.common_sync_outbox_outbox_id_seq'::regclass);

ALTER TABLE ONLY public.credit_consent ALTER COLUMN csnt_id SET DEFAULT nextval('public.credit_consent_csnt_id_seq'::regclass);

ALTER TABLE ONLY public.credit_evaluation ALTER COLUMN ceval_id SET DEFAULT nextval('public.credit_evaluation_ceval_id_seq'::regclass);

ALTER TABLE ONLY public.credit_info_report ALTER COLUMN crpt_id SET DEFAULT nextval('public.credit_info_report_crpt_id_seq'::regclass);

ALTER TABLE ONLY public.credit_info_report_outbox ALTER COLUMN outbox_id SET DEFAULT nextval('public.credit_info_report_outbox_outbox_id_seq'::regclass);

ALTER TABLE ONLY public.daily_accounting_summary ALTER COLUMN das_id SET DEFAULT nextval('public.daily_accounting_summary_das_id_seq'::regclass);

ALTER TABLE ONLY public.delinquency ALTER COLUMN dlq_id SET DEFAULT nextval('public.delinquency_dlq_id_seq'::regclass);

ALTER TABLE ONLY public.delinquency_daily_snapshot ALTER COLUMN dlqs_id SET DEFAULT nextval('public.delinquency_daily_snapshot_dlqs_id_seq'::regclass);

ALTER TABLE ONLY public.dsr_calculation ALTER COLUMN dsr_id SET DEFAULT nextval('public.dsr_calculation_dsr_id_seq'::regclass);

ALTER TABLE ONLY public.guarantee_insurance ALTER COLUMN gins_id SET DEFAULT nextval('public.guarantee_insurance_gins_id_seq'::regclass);

ALTER TABLE ONLY public.guarantor_agreement ALTER COLUMN gagr_id SET DEFAULT nextval('public.guarantor_agreement_gagr_id_seq'::regclass);

ALTER TABLE ONLY public.guarantor_master ALTER COLUMN gmst_id SET DEFAULT nextval('public.guarantor_master_gmst_id_seq'::regclass);

ALTER TABLE ONLY public.interest_accrual ALTER COLUMN iacc_id SET DEFAULT nextval('public.interest_accrual_iacc_id_seq'::regclass);

ALTER TABLE ONLY public.loan_application ALTER COLUMN appl_id SET DEFAULT nextval('public.loan_application_appl_id_seq'::regclass);

ALTER TABLE ONLY public.loan_certificate ALTER COLUMN cert_id SET DEFAULT nextval('public.loan_certificate_cert_id_seq'::regclass);

ALTER TABLE ONLY public.loan_closure ALTER COLUMN clos_id SET DEFAULT nextval('public.loan_closure_clos_id_seq'::regclass);

ALTER TABLE ONLY public.loan_contract ALTER COLUMN cntr_id SET DEFAULT nextval('public.loan_contract_cntr_id_seq'::regclass);

ALTER TABLE ONLY public.loan_document ALTER COLUMN doc_id SET DEFAULT nextval('public.loan_document_doc_id_seq'::regclass);

ALTER TABLE ONLY public.loan_ecl_summary ALTER COLUMN ecl_id SET DEFAULT nextval('public.loan_ecl_summary_ecl_id_seq'::regclass);

ALTER TABLE ONLY public.loan_execution ALTER COLUMN exec_id SET DEFAULT nextval('public.loan_execution_exec_id_seq'::regclass);

ALTER TABLE ONLY public.loan_identity_verification ALTER COLUMN idv_id SET DEFAULT nextval('public.loan_identity_verification_idv_id_seq'::regclass);

ALTER TABLE ONLY public.loan_prescreening ALTER COLUMN presc_id SET DEFAULT nextval('public.loan_prescreening_presc_id_seq'::regclass);

ALTER TABLE ONLY public.loan_product ALTER COLUMN prod_id SET DEFAULT nextval('public.loan_product_prod_id_seq'::regclass);

ALTER TABLE ONLY public.loan_review ALTER COLUMN rev_id SET DEFAULT nextval('public.loan_review_rev_id_seq'::regclass);

ALTER TABLE ONLY public.loan_review_outbox ALTER COLUMN outbox_id SET DEFAULT nextval('public.loan_review_outbox_outbox_id_seq'::regclass);

ALTER TABLE ONLY public.ltv_calculation ALTER COLUMN ltv_id SET DEFAULT nextval('public.ltv_calculation_ltv_id_seq'::regclass);

ALTER TABLE ONLY public.maturity ALTER COLUMN mat_id SET DEFAULT nextval('public.maturity_mat_id_seq'::regclass);

ALTER TABLE ONLY public.monthly_accounting_summary ALTER COLUMN mas_id SET DEFAULT nextval('public.monthly_accounting_summary_mas_id_seq'::regclass);

ALTER TABLE ONLY public.notification_outbox ALTER COLUMN outbox_id SET DEFAULT nextval('public.notification_outbox_outbox_id_seq'::regclass);

ALTER TABLE ONLY public.overdue_accrual ALTER COLUMN oa_id SET DEFAULT nextval('public.overdue_accrual_oa_id_seq'::regclass);

ALTER TABLE ONLY public.preferential_rate_policy ALTER COLUMN policy_id SET DEFAULT nextval('public.preferential_rate_policy_policy_id_seq'::regclass);

ALTER TABLE ONLY public.rate_change_history ALTER COLUMN rchg_id SET DEFAULT nextval('public.rate_change_history_rchg_id_seq'::regclass);

ALTER TABLE ONLY public.repayment_account ALTER COLUMN racct_id SET DEFAULT nextval('public.repayment_account_racct_id_seq'::regclass);

ALTER TABLE ONLY public.repayment_schedule ALTER COLUMN rsch_id SET DEFAULT nextval('public.repayment_schedule_rsch_id_seq'::regclass);

ALTER TABLE ONLY public.repayment_transaction ALTER COLUMN rtx_id SET DEFAULT nextval('public.repayment_transaction_rtx_id_seq'::regclass);

ALTER TABLE ONLY public.review_advisory_ack ALTER COLUMN advk_id SET DEFAULT nextval('public.review_advisory_ack_advk_id_seq'::regclass);

ALTER TABLE ONLY public.review_advisory_report ALTER COLUMN advr_id SET DEFAULT nextval('public.review_advisory_report_advr_id_seq'::regclass);

ALTER TABLE ONLY public.review_advisory_rule ALTER COLUMN rule_id SET DEFAULT nextval('public.review_advisory_rule_rule_id_seq'::regclass);

ALTER TABLE ONLY public.review_advisory_signal ALTER COLUMN advs_id SET DEFAULT nextval('public.review_advisory_signal_advs_id_seq'::regclass);

ALTER TABLE ONLY public.review_check_log ALTER COLUMN rchk_id SET DEFAULT nextval('public.review_check_log_rchk_id_seq'::regclass);

ALTER TABLE ONLY public.reviewer_decision_snapshot ALTER COLUMN rds_id SET DEFAULT nextval('public.reviewer_decision_snapshot_rds_id_seq'::regclass);

ALTER TABLE ONLY public.reviewer_risk_score ALTER COLUMN score_id SET DEFAULT nextval('public.reviewer_risk_score_score_id_seq'::regclass);

ALTER TABLE ONLY public.status_history ALTER COLUMN sthist_id SET DEFAULT nextval('public.status_history_sthist_id_seq'::regclass);

ALTER TABLE ONLY public.advisory_case_index
    ADD CONSTRAINT advisory_case_index_pkey PRIMARY KEY (case_idx_id);

ALTER TABLE ONLY public.advisory_document_chunk
    ADD CONSTRAINT advisory_document_chunk_pkey PRIMARY KEY (chunk_id);

ALTER TABLE ONLY public.advisory_document
    ADD CONSTRAINT advisory_document_pkey PRIMARY KEY (doc_id);

ALTER TABLE ONLY public.advisory_retrieval_log
    ADD CONSTRAINT advisory_retrieval_log_pkey PRIMARY KEY (retr_id);

ALTER TABLE ONLY public.ai_audit_opinion
    ADD CONSTRAINT ai_audit_opinion_pkey PRIMARY KEY (opinion_id);

ALTER TABLE ONLY public.ai_review_advice
    ADD CONSTRAINT ai_review_advice_pkey PRIMARY KEY (advice_id);

ALTER TABLE ONLY public.auto_debit_clearing_pending
    ADD CONSTRAINT auto_debit_clearing_pending_pi_id_key UNIQUE (pi_id);

ALTER TABLE ONLY public.auto_debit_clearing_pending
    ADD CONSTRAINT auto_debit_clearing_pending_pkey PRIMARY KEY (pending_id);

ALTER TABLE ONLY public.batch_job_execution_context
    ADD CONSTRAINT batch_job_execution_context_pkey PRIMARY KEY (job_execution_id);

ALTER TABLE ONLY public.batch_job_execution
    ADD CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id);

ALTER TABLE ONLY public.batch_job_instance
    ADD CONSTRAINT batch_job_instance_pkey PRIMARY KEY (job_instance_id);

ALTER TABLE ONLY public.batch_step_execution_context
    ADD CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id);

ALTER TABLE ONLY public.batch_step_execution
    ADD CONSTRAINT batch_step_execution_pkey PRIMARY KEY (step_execution_id);

ALTER TABLE ONLY public.business_calendar
    ADD CONSTRAINT business_calendar_cal_date_key UNIQUE (cal_date);

ALTER TABLE ONLY public.business_calendar
    ADD CONSTRAINT business_calendar_pkey PRIMARY KEY (cal_id);

ALTER TABLE ONLY public.collateral
    ADD CONSTRAINT collateral_col_no_key UNIQUE (col_no);

ALTER TABLE ONLY public.collateral_evaluation
    ADD CONSTRAINT collateral_evaluation_pkey PRIMARY KEY (ceval_col_id);

ALTER TABLE ONLY public.collateral
    ADD CONSTRAINT collateral_pkey PRIMARY KEY (col_id);

ALTER TABLE ONLY public.common_sync_outbox
    ADD CONSTRAINT common_sync_outbox_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY public.common_sync_outbox
    ADD CONSTRAINT common_sync_outbox_pkey PRIMARY KEY (outbox_id);

ALTER TABLE ONLY public.credit_consent
    ADD CONSTRAINT credit_consent_pkey PRIMARY KEY (csnt_id);

ALTER TABLE ONLY public.credit_evaluation
    ADD CONSTRAINT credit_evaluation_appl_id_key UNIQUE (appl_id);

ALTER TABLE ONLY public.credit_evaluation
    ADD CONSTRAINT credit_evaluation_pkey PRIMARY KEY (ceval_id);

ALTER TABLE ONLY public.credit_info_report_outbox
    ADD CONSTRAINT credit_info_report_outbox_pkey PRIMARY KEY (outbox_id);

ALTER TABLE ONLY public.credit_info_report
    ADD CONSTRAINT credit_info_report_pkey PRIMARY KEY (crpt_id);

ALTER TABLE ONLY public.daily_accounting_summary
    ADD CONSTRAINT daily_accounting_summary_pkey PRIMARY KEY (das_id);

ALTER TABLE ONLY public.daily_accounting_summary
    ADD CONSTRAINT daily_accounting_summary_summary_date_key UNIQUE (summary_date);

ALTER TABLE ONLY public.delinquency_daily_snapshot
    ADD CONSTRAINT delinquency_daily_snapshot_pkey PRIMARY KEY (dlqs_id);

ALTER TABLE ONLY public.delinquency
    ADD CONSTRAINT delinquency_pkey PRIMARY KEY (dlq_id);

ALTER TABLE ONLY public.dsr_calculation
    ADD CONSTRAINT dsr_calculation_appl_id_key UNIQUE (appl_id);

ALTER TABLE ONLY public.dsr_calculation
    ADD CONSTRAINT dsr_calculation_pkey PRIMARY KEY (dsr_id);

ALTER TABLE ONLY public.guarantee_insurance
    ADD CONSTRAINT guarantee_insurance_gins_policy_no_key UNIQUE (gins_policy_no);

ALTER TABLE ONLY public.guarantee_insurance
    ADD CONSTRAINT guarantee_insurance_pkey PRIMARY KEY (gins_id);

ALTER TABLE ONLY public.guarantor_agreement
    ADD CONSTRAINT guarantor_agreement_pkey PRIMARY KEY (gagr_id);

ALTER TABLE ONLY public.guarantor_master
    ADD CONSTRAINT guarantor_master_pkey PRIMARY KEY (gmst_id);

ALTER TABLE ONLY public.interest_accrual
    ADD CONSTRAINT interest_accrual_pkey PRIMARY KEY (iacc_id);

ALTER TABLE ONLY public.batch_job_instance
    ADD CONSTRAINT job_inst_un UNIQUE (job_name, job_key);

ALTER TABLE ONLY public.loan_application
    ADD CONSTRAINT loan_application_appl_no_key UNIQUE (appl_no);

ALTER TABLE ONLY public.loan_application
    ADD CONSTRAINT loan_application_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY public.loan_application
    ADD CONSTRAINT loan_application_pkey PRIMARY KEY (appl_id);

ALTER TABLE ONLY public.loan_certificate
    ADD CONSTRAINT loan_certificate_cert_no_key UNIQUE (cert_no);

ALTER TABLE ONLY public.loan_certificate
    ADD CONSTRAINT loan_certificate_pkey PRIMARY KEY (cert_id);

ALTER TABLE ONLY public.loan_closure
    ADD CONSTRAINT loan_closure_cntr_id_key UNIQUE (cntr_id);

ALTER TABLE ONLY public.loan_closure
    ADD CONSTRAINT loan_closure_pkey PRIMARY KEY (clos_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT loan_contract_cntr_no_key UNIQUE (cntr_no);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT loan_contract_pkey PRIMARY KEY (cntr_id);

ALTER TABLE ONLY public.loan_document
    ADD CONSTRAINT loan_document_pkey PRIMARY KEY (doc_id);

ALTER TABLE ONLY public.loan_ecl_summary
    ADD CONSTRAINT loan_ecl_summary_pkey PRIMARY KEY (ecl_id);

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT loan_execution_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT loan_execution_pkey PRIMARY KEY (exec_id);

ALTER TABLE ONLY public.loan_identity_verification
    ADD CONSTRAINT loan_identity_verification_pkey PRIMARY KEY (idv_id);

ALTER TABLE ONLY public.loan_prescreening
    ADD CONSTRAINT loan_prescreening_appl_id_key UNIQUE (appl_id);

ALTER TABLE ONLY public.loan_prescreening
    ADD CONSTRAINT loan_prescreening_pkey PRIMARY KEY (presc_id);

ALTER TABLE ONLY public.loan_product
    ADD CONSTRAINT loan_product_pkey PRIMARY KEY (prod_id);

ALTER TABLE ONLY public.loan_product
    ADD CONSTRAINT loan_product_prod_cd_key UNIQUE (prod_cd);

ALTER TABLE ONLY public.loan_review
    ADD CONSTRAINT loan_review_appl_id_key UNIQUE (appl_id);

ALTER TABLE ONLY public.loan_review_outbox
    ADD CONSTRAINT loan_review_outbox_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY public.loan_review_outbox
    ADD CONSTRAINT loan_review_outbox_pkey PRIMARY KEY (outbox_id);

ALTER TABLE ONLY public.loan_review
    ADD CONSTRAINT loan_review_pkey PRIMARY KEY (rev_id);

ALTER TABLE ONLY public.ltv_calculation
    ADD CONSTRAINT ltv_calculation_pkey PRIMARY KEY (ltv_id);

ALTER TABLE ONLY public.maturity
    ADD CONSTRAINT maturity_cntr_id_key UNIQUE (cntr_id);

ALTER TABLE ONLY public.maturity
    ADD CONSTRAINT maturity_pkey PRIMARY KEY (mat_id);

ALTER TABLE ONLY public.monthly_accounting_summary
    ADD CONSTRAINT monthly_accounting_summary_pkey PRIMARY KEY (mas_id);

ALTER TABLE ONLY public.monthly_accounting_summary
    ADD CONSTRAINT monthly_accounting_summary_summary_month_key UNIQUE (summary_month);

ALTER TABLE ONLY public.notification_outbox
    ADD CONSTRAINT notification_outbox_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY public.notification_outbox
    ADD CONSTRAINT notification_outbox_pkey PRIMARY KEY (outbox_id);

ALTER TABLE ONLY public.overdue_accrual
    ADD CONSTRAINT overdue_accrual_pkey PRIMARY KEY (oa_id);

ALTER TABLE ONLY public.access_audit_log
    ADD CONSTRAINT pk_access_audit_log PRIMARY KEY (log_id);

ALTER TABLE ONLY public.branch
    ADD CONSTRAINT pk_branch PRIMARY KEY (branch_id);

ALTER TABLE ONLY public.loan_document_submission
    ADD CONSTRAINT pk_loan_document_submission PRIMARY KEY (submission_id);

ALTER TABLE ONLY public.preferential_rate_policy
    ADD CONSTRAINT preferential_rate_policy_pkey PRIMARY KEY (policy_id);

ALTER TABLE ONLY public.rate_change_history
    ADD CONSTRAINT rate_change_history_pkey PRIMARY KEY (rchg_id);

ALTER TABLE ONLY public.repayment_account
    ADD CONSTRAINT repayment_account_cntr_id_key UNIQUE (cntr_id);

ALTER TABLE ONLY public.repayment_account
    ADD CONSTRAINT repayment_account_pkey PRIMARY KEY (racct_id);

ALTER TABLE ONLY public.repayment_schedule
    ADD CONSTRAINT repayment_schedule_pkey PRIMARY KEY (rsch_id);

ALTER TABLE ONLY public.repayment_transaction
    ADD CONSTRAINT repayment_transaction_idempotency_key_key UNIQUE (idempotency_key);

ALTER TABLE ONLY public.repayment_transaction
    ADD CONSTRAINT repayment_transaction_pkey PRIMARY KEY (rtx_id);

ALTER TABLE ONLY public.review_advisory_ack
    ADD CONSTRAINT review_advisory_ack_pkey PRIMARY KEY (advk_id);

ALTER TABLE ONLY public.review_advisory_report
    ADD CONSTRAINT review_advisory_report_pkey PRIMARY KEY (advr_id);

ALTER TABLE ONLY public.review_advisory_rule
    ADD CONSTRAINT review_advisory_rule_pkey PRIMARY KEY (rule_id);

ALTER TABLE ONLY public.review_advisory_signal
    ADD CONSTRAINT review_advisory_signal_pkey PRIMARY KEY (advs_id);

ALTER TABLE ONLY public.review_check_log
    ADD CONSTRAINT review_check_log_pkey PRIMARY KEY (rchk_id);

ALTER TABLE ONLY public.reviewer_decision_snapshot
    ADD CONSTRAINT reviewer_decision_snapshot_pkey PRIMARY KEY (rds_id);

ALTER TABLE ONLY public.reviewer_risk_score
    ADD CONSTRAINT reviewer_risk_score_pkey PRIMARY KEY (score_id);

ALTER TABLE ONLY public.reviewer_risk_score
    ADD CONSTRAINT reviewer_risk_score_reviewer_id_key UNIQUE (reviewer_id);

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT status_history_pkey PRIMARY KEY (sthist_id);

CREATE INDEX idx_aal_actor ON public.access_audit_log USING btree (actor_id, logged_at DESC);

CREATE INDEX idx_aal_target ON public.access_audit_log USING btree (target_type, target_id, logged_at DESC);

CREATE INDEX idx_adcp_pending ON public.auto_debit_clearing_pending USING btree (status) WHERE ((status)::text = 'PENDING'::text);

CREATE INDEX idx_advisory_case_index_decision_overturn ON public.advisory_case_index USING btree (decision_cd, overturn_yn);

CREATE INDEX idx_advisory_case_index_embedding ON public.advisory_case_index USING ivfflat (embedding public.vector_cosine_ops) WITH (lists='10');

CREATE INDEX idx_advisory_case_index_rev ON public.advisory_case_index USING btree (rev_id);

CREATE INDEX idx_advisory_document_active_effective ON public.advisory_document USING btree (active_yn, effective_start_date, effective_end_date) WHERE (deleted_at IS NULL);

CREATE INDEX idx_advisory_document_chunk_doc ON public.advisory_document_chunk USING btree (doc_id, chunk_seq);

CREATE INDEX idx_advisory_document_chunk_embedding ON public.advisory_document_chunk USING ivfflat (embedding public.vector_cosine_ops) WITH (lists='10');

CREATE INDEX idx_advisory_document_chunk_model ON public.advisory_document_chunk USING btree (embedding_model_cd);

CREATE INDEX idx_advisory_retrieval_log_advr ON public.advisory_retrieval_log USING btree (advr_id, requested_at);

CREATE INDEX idx_advisory_retrieval_log_kind ON public.advisory_retrieval_log USING btree (retrieval_kind_cd, requested_at);

CREATE INDEX idx_ai_audit_opinion_advr ON public.ai_audit_opinion USING btree (advr_id);

CREATE INDEX idx_ai_audit_opinion_reviewer ON public.ai_audit_opinion USING btree (reviewer_id, generated_at DESC);

CREATE INDEX idx_ai_audit_opinion_type ON public.ai_audit_opinion USING btree (analysis_type_cd, conclusion_cd);

CREATE INDEX idx_common_sync_outbox_dispatch ON public.common_sync_outbox USING btree (status, next_attempt_at) WHERE (deleted_at IS NULL);

CREATE INDEX idx_common_sync_outbox_source ON public.common_sync_outbox USING btree (target_type_cd, source_id) WHERE (deleted_at IS NULL);

CREATE INDEX idx_credit_info_report_dlq_id ON public.credit_info_report USING btree (dlq_id) WHERE (dlq_id IS NOT NULL);

CREATE INDEX idx_credit_info_report_outbox_crpt_id ON public.credit_info_report_outbox USING btree (crpt_id) WHERE (deleted_at IS NULL);

CREATE INDEX idx_credit_info_report_outbox_dispatch ON public.credit_info_report_outbox USING btree (status, next_attempt_at) WHERE (deleted_at IS NULL);

CREATE INDEX idx_lds_application_id ON public.loan_document_submission USING btree (application_id);

CREATE INDEX idx_lds_doc_id ON public.loan_document_submission USING btree (doc_id);

CREATE INDEX idx_loan_application_customer ON public.loan_application USING btree (customer_id);

CREATE INDEX idx_loan_contract_customer ON public.loan_contract USING btree (customer_id);

CREATE INDEX idx_loan_ecl_summary_month_stage ON public.loan_ecl_summary USING btree (summary_month, ifrs_stage_cd);

CREATE INDEX idx_loan_execution_pi_id ON public.loan_execution USING btree (pi_id) WHERE (pi_id IS NOT NULL);

CREATE INDEX idx_loan_review_escalated ON public.loan_review USING btree (escalated_at) WHERE (escalated_at IS NOT NULL);

CREATE INDEX idx_loan_review_outbox_aggregate ON public.loan_review_outbox USING btree (aggregate_id);

CREATE INDEX idx_loan_review_outbox_dispatch ON public.loan_review_outbox USING btree (status, next_attempt_at);

CREATE INDEX idx_notification_outbox_dispatch ON public.notification_outbox USING btree (status, next_attempt_at) WHERE (deleted_at IS NULL);

CREATE INDEX idx_notification_outbox_event_ref ON public.notification_outbox USING btree (event_type_cd, reference_id) WHERE (deleted_at IS NULL);

CREATE INDEX idx_overdue_accrual_dlq ON public.overdue_accrual USING btree (dlq_id, accrual_date);

CREATE INDEX idx_pref_rate_policy_prod ON public.preferential_rate_policy USING btree (prod_id, active_yn);

CREATE INDEX idx_repayment_transaction_pi_id ON public.repayment_transaction USING btree (pi_id) WHERE (pi_id IS NOT NULL);

CREATE INDEX idx_review_advisory_ack_advr ON public.review_advisory_ack USING btree (advr_id, acked_at);

CREATE INDEX idx_review_advisory_report_quarantine ON public.review_advisory_report USING btree (quarantined_at DESC) WHERE (((advr_status_cd)::text = 'QUARANTINE'::text) AND (deleted_at IS NULL));

CREATE INDEX idx_review_advisory_report_rev ON public.review_advisory_report USING btree (rev_id);

CREATE INDEX idx_review_advisory_report_reviewer_status ON public.review_advisory_report USING btree (target_reviewer_id, advr_status_cd) WHERE (deleted_at IS NULL);

CREATE INDEX idx_review_advisory_report_unresolved_critical ON public.review_advisory_report USING btree (rev_id, severity_cd, advr_status_cd) WHERE (deleted_at IS NULL);

CREATE INDEX idx_review_advisory_rule_active ON public.review_advisory_rule USING btree (active_yn, advisory_type_cd) WHERE (deleted_at IS NULL);

CREATE INDEX idx_review_advisory_signal_advr ON public.review_advisory_signal USING btree (advr_id);

CREATE INDEX idx_reviewer_decision_snapshot_reviewer_date ON public.reviewer_decision_snapshot USING btree (reviewer_id, snapshot_date);

CREATE INDEX idx_reviewer_risk_score_bias ON public.reviewer_risk_score USING btree (bias_score DESC);

CREATE INDEX idx_reviewer_risk_score_compliance ON public.reviewer_risk_score USING btree (compliance_score DESC);

CREATE INDEX idx_status_history_target ON public.status_history USING btree (target_domain_cd, target_table_cd, target_id, changed_at);

CREATE INDEX ix_ai_review_advice_rev_type_created ON public.ai_review_advice USING btree (rev_id, advice_type_cd, created_at DESC);

CREATE INDEX ix_loan_review_status_bias ON public.loan_review USING btree (rev_status_cd) WHERE ((rev_status_cd)::text = ANY ((ARRAY['BIAS_REVIEWING'::character varying, 'PENDING_APPROVER'::character varying])::text[]));

CREATE UNIQUE INDEX uk_advisory_document_cd_version ON public.advisory_document USING btree (doc_cd, doc_version) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uk_credit_info_report_dlq_idem ON public.credit_info_report USING btree (cntr_id, dlq_id, crpt_type_cd, report_reason_cd) WHERE ((dlq_id IS NOT NULL) AND ((crpt_status_cd)::text = ANY ((ARRAY['REQUESTED'::character varying, 'SENT'::character varying, 'ACKED'::character varying])::text[])));

CREATE UNIQUE INDEX uk_dlq_snapshot_dlq_date ON public.delinquency_daily_snapshot USING btree (dlq_id, snapshot_date);

CREATE UNIQUE INDEX uk_interest_accrual_cntr_date ON public.interest_accrual USING btree (cntr_id, accrual_date);

CREATE UNIQUE INDEX uk_loan_ecl_summary_cntr_month ON public.loan_ecl_summary USING btree (cntr_id, summary_month);

CREATE UNIQUE INDEX uk_overdue_accrual_cntr_date ON public.overdue_accrual USING btree (cntr_id, accrual_date);

CREATE UNIQUE INDEX uk_pref_rate_policy_prod_condition_active ON public.preferential_rate_policy USING btree (prod_id, condition_cd) WHERE ((deleted_at IS NULL) AND (active_yn = 'Y'::bpchar));

CREATE UNIQUE INDEX uk_repayment_schedule_cntr_inst_ver ON public.repayment_schedule USING btree (cntr_id, installment_no, rsch_version_cd);

CREATE UNIQUE INDEX uk_review_advisory_rule_cd ON public.review_advisory_rule USING btree (rule_cd) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX uk_reviewer_decision_snapshot_unit ON public.reviewer_decision_snapshot USING btree (reviewer_id, snapshot_date, aggregation_window_cd, cohort_dimension_cd, cohort_value);

CREATE UNIQUE INDEX ux_rtx_active_reversal_target ON public.repayment_transaction USING btree (reversal_target_rtx_id) WHERE ((reversal_yn = 'Y'::bpchar) AND ((rtx_status_cd)::text = 'SUCCESS'::text) AND (deleted_at IS NULL));

ALTER TABLE ONLY public.advisory_case_index
    ADD CONSTRAINT advisory_case_index_rev_id_fkey FOREIGN KEY (rev_id) REFERENCES public.loan_review(rev_id);

ALTER TABLE ONLY public.advisory_document_chunk
    ADD CONSTRAINT advisory_document_chunk_doc_id_fkey FOREIGN KEY (doc_id) REFERENCES public.advisory_document(doc_id);

ALTER TABLE ONLY public.advisory_retrieval_log
    ADD CONSTRAINT advisory_retrieval_log_advr_id_fkey FOREIGN KEY (advr_id) REFERENCES public.review_advisory_report(advr_id);

ALTER TABLE ONLY public.ai_review_advice
    ADD CONSTRAINT ai_review_advice_rev_id_fkey FOREIGN KEY (rev_id) REFERENCES public.loan_review(rev_id);

ALTER TABLE ONLY public.collateral
    ADD CONSTRAINT collateral_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.collateral_evaluation
    ADD CONSTRAINT collateral_evaluation_col_id_fkey FOREIGN KEY (col_id) REFERENCES public.collateral(col_id);

ALTER TABLE ONLY public.credit_consent
    ADD CONSTRAINT credit_consent_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.credit_evaluation
    ADD CONSTRAINT credit_evaluation_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.credit_info_report
    ADD CONSTRAINT credit_info_report_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.credit_info_report_outbox
    ADD CONSTRAINT credit_info_report_outbox_crpt_id_fkey FOREIGN KEY (crpt_id) REFERENCES public.credit_info_report(crpt_id);

ALTER TABLE ONLY public.delinquency
    ADD CONSTRAINT delinquency_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.delinquency_daily_snapshot
    ADD CONSTRAINT delinquency_daily_snapshot_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.delinquency_daily_snapshot
    ADD CONSTRAINT delinquency_daily_snapshot_dlq_id_fkey FOREIGN KEY (dlq_id) REFERENCES public.delinquency(dlq_id);

ALTER TABLE ONLY public.dsr_calculation
    ADD CONSTRAINT dsr_calculation_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.repayment_transaction
    ADD CONSTRAINT fk_rtx_reversal_target FOREIGN KEY (reversal_target_rtx_id) REFERENCES public.repayment_transaction(rtx_id);

ALTER TABLE ONLY public.guarantee_insurance
    ADD CONSTRAINT guarantee_insurance_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.guarantor_agreement
    ADD CONSTRAINT guarantor_agreement_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.guarantor_agreement
    ADD CONSTRAINT guarantor_agreement_gmst_id_fkey FOREIGN KEY (gmst_id) REFERENCES public.guarantor_master(gmst_id);

ALTER TABLE ONLY public.interest_accrual
    ADD CONSTRAINT interest_accrual_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.batch_job_execution_context
    ADD CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);

ALTER TABLE ONLY public.batch_job_execution_params
    ADD CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);

ALTER TABLE ONLY public.batch_step_execution
    ADD CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);

ALTER TABLE ONLY public.batch_job_execution
    ADD CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES public.batch_job_instance(job_instance_id);

ALTER TABLE ONLY public.loan_application
    ADD CONSTRAINT loan_application_branch_id_fkey FOREIGN KEY (branch_id) REFERENCES public.branch(branch_id);

ALTER TABLE ONLY public.loan_application
    ADD CONSTRAINT loan_application_prod_id_fkey FOREIGN KEY (prod_id) REFERENCES public.loan_product(prod_id);

ALTER TABLE ONLY public.loan_certificate
    ADD CONSTRAINT loan_certificate_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.loan_closure
    ADD CONSTRAINT loan_closure_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT loan_contract_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT loan_contract_prod_id_fkey FOREIGN KEY (prod_id) REFERENCES public.loan_product(prod_id);

ALTER TABLE ONLY public.loan_contract
    ADD CONSTRAINT loan_contract_rev_id_fkey FOREIGN KEY (rev_id) REFERENCES public.loan_review(rev_id);

ALTER TABLE ONLY public.loan_document
    ADD CONSTRAINT loan_document_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.loan_document_submission
    ADD CONSTRAINT loan_document_submission_doc_id_fkey FOREIGN KEY (doc_id) REFERENCES public.loan_document(doc_id);

ALTER TABLE ONLY public.loan_ecl_summary
    ADD CONSTRAINT loan_ecl_summary_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.loan_execution
    ADD CONSTRAINT loan_execution_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.loan_identity_verification
    ADD CONSTRAINT loan_identity_verification_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.loan_prescreening
    ADD CONSTRAINT loan_prescreening_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.loan_review
    ADD CONSTRAINT loan_review_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.ltv_calculation
    ADD CONSTRAINT ltv_calculation_appl_id_fkey FOREIGN KEY (appl_id) REFERENCES public.loan_application(appl_id);

ALTER TABLE ONLY public.ltv_calculation
    ADD CONSTRAINT ltv_calculation_col_id_fkey FOREIGN KEY (col_id) REFERENCES public.collateral(col_id);

ALTER TABLE ONLY public.maturity
    ADD CONSTRAINT maturity_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.overdue_accrual
    ADD CONSTRAINT overdue_accrual_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.overdue_accrual
    ADD CONSTRAINT overdue_accrual_dlq_id_fkey FOREIGN KEY (dlq_id) REFERENCES public.delinquency(dlq_id);

ALTER TABLE ONLY public.preferential_rate_policy
    ADD CONSTRAINT preferential_rate_policy_prod_id_fkey FOREIGN KEY (prod_id) REFERENCES public.loan_product(prod_id);

ALTER TABLE ONLY public.rate_change_history
    ADD CONSTRAINT rate_change_history_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.repayment_account
    ADD CONSTRAINT repayment_account_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.repayment_schedule
    ADD CONSTRAINT repayment_schedule_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.repayment_transaction
    ADD CONSTRAINT repayment_transaction_cntr_id_fkey FOREIGN KEY (cntr_id) REFERENCES public.loan_contract(cntr_id);

ALTER TABLE ONLY public.repayment_transaction
    ADD CONSTRAINT repayment_transaction_rsch_id_fkey FOREIGN KEY (rsch_id) REFERENCES public.repayment_schedule(rsch_id);

ALTER TABLE ONLY public.review_advisory_ack
    ADD CONSTRAINT review_advisory_ack_advr_id_fkey FOREIGN KEY (advr_id) REFERENCES public.review_advisory_report(advr_id);

ALTER TABLE ONLY public.review_advisory_report
    ADD CONSTRAINT review_advisory_report_rev_id_fkey FOREIGN KEY (rev_id) REFERENCES public.loan_review(rev_id);

ALTER TABLE ONLY public.review_advisory_report
    ADD CONSTRAINT review_advisory_report_rule_id_fkey FOREIGN KEY (rule_id) REFERENCES public.review_advisory_rule(rule_id);

ALTER TABLE ONLY public.review_advisory_signal
    ADD CONSTRAINT review_advisory_signal_advr_id_fkey FOREIGN KEY (advr_id) REFERENCES public.review_advisory_report(advr_id);

ALTER TABLE ONLY public.review_check_log
    ADD CONSTRAINT review_check_log_rev_id_fkey FOREIGN KEY (rev_id) REFERENCES public.loan_review(rev_id);

ALTER TABLE ONLY public.batch_step_execution_context
    ADD CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES public.batch_step_execution(step_execution_id);


-- ============================================================================
-- SERVICE: payment-service
-- DATABASE: payment_db
-- ============================================================================
\connect payment_db

CREATE TABLE public.bok_settlement_transaction (
    settlement_transaction_id character varying(20) NOT NULL,
    our_payment_instruction_id character varying(20) NOT NULL,
    direction character varying(5) NOT NULL,
    counterparty_payment_id character varying(50),
    bok_reference_no character varying(50) NOT NULL,
    sender_bank_clearing_id character varying(50),
    receiver_bank_clearing_id character varying(50),
    sender_bank_code character(3) NOT NULL,
    sender_account_no_snap character varying(30) NOT NULL,
    sender_holder_name_snap character varying(60) NOT NULL,
    receiver_bank_code character(3) NOT NULL,
    receiver_account_no_snap character varying(30) NOT NULL,
    receiver_holder_name_snap character varying(60) NOT NULL,
    settlement_amount numeric(15,0) NOT NULL,
    currency character(3) DEFAULT 'KRW'::bpchar NOT NULL,
    settlement_status character varying(20) NOT NULL,
    reject_code character varying(30),
    reject_message character varying(200),
    settlement_requested_at character varying(14) NOT NULL,
    ack_received_at character varying(14),
    settled_at character varying(14),
    settlement_date character varying(8),
    network character varying(30) NOT NULL,
    last_inquiry_at timestamp(3) without time zone,
    inquiry_count integer DEFAULT 0 NOT NULL,
    first_registered_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registrant_id character varying(20),
    last_modified_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_modifier_id character varying(20),
    CONSTRAINT chk_bst_direction CHECK (((direction)::text = ANY ((ARRAY['OUT'::character varying, 'IN'::character varying])::text[]))),
    CONSTRAINT chk_bst_network CHECK (((network)::text = 'BOK_CLEARING'::text)),
    CONSTRAINT chk_bst_settlement_amount CHECK ((settlement_amount >= (0)::numeric)),
    CONSTRAINT chk_bst_settlement_status CHECK (((settlement_status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'ACK_RECEIVED'::character varying, 'SETTLED'::character varying, 'REJECTED'::character varying, 'TIMEOUT'::character varying])::text[])))
);

COMMENT ON TABLE public.bok_settlement_transaction IS 'BOK한은망정산거래';

COMMENT ON COLUMN public.bok_settlement_transaction.settlement_transaction_id IS '정산거래번호';

COMMENT ON COLUMN public.bok_settlement_transaction.our_payment_instruction_id IS '결제지시번호(자행)';

COMMENT ON COLUMN public.bok_settlement_transaction.direction IS '방향(OUT=타행송신/IN=타행수신)';

COMMENT ON COLUMN public.bok_settlement_transaction.counterparty_payment_id IS '상대방거래참조';

COMMENT ON COLUMN public.bok_settlement_transaction.bok_reference_no IS 'BOK정산식별번호';

COMMENT ON COLUMN public.bok_settlement_transaction.sender_bank_clearing_id IS '송신은행청산ID';

COMMENT ON COLUMN public.bok_settlement_transaction.receiver_bank_clearing_id IS '수신은행청산ID';

COMMENT ON COLUMN public.bok_settlement_transaction.sender_bank_code IS '송신은행코드';

COMMENT ON COLUMN public.bok_settlement_transaction.sender_account_no_snap IS '송신계좌번호_스냅샷';

COMMENT ON COLUMN public.bok_settlement_transaction.sender_holder_name_snap IS '송신예금주명_스냅샷';

COMMENT ON COLUMN public.bok_settlement_transaction.receiver_bank_code IS '수신은행코드';

COMMENT ON COLUMN public.bok_settlement_transaction.receiver_account_no_snap IS '수신계좌번호_스냅샷';

COMMENT ON COLUMN public.bok_settlement_transaction.receiver_holder_name_snap IS '수신예금주명_스냅샷';

COMMENT ON COLUMN public.bok_settlement_transaction.settlement_amount IS '정산금액';

COMMENT ON COLUMN public.bok_settlement_transaction.currency IS '통화';

COMMENT ON COLUMN public.bok_settlement_transaction.settlement_status IS '정산상태';

COMMENT ON COLUMN public.bok_settlement_transaction.reject_code IS '거절코드';

COMMENT ON COLUMN public.bok_settlement_transaction.reject_message IS '거절메시지';

COMMENT ON COLUMN public.bok_settlement_transaction.settlement_requested_at IS '정산요청시각(yyyyMMddHHmmss)';

COMMENT ON COLUMN public.bok_settlement_transaction.ack_received_at IS 'ACK수신시각(yyyyMMddHHmmss)';

COMMENT ON COLUMN public.bok_settlement_transaction.settled_at IS '정산완료시각(yyyyMMddHHmmss)';

COMMENT ON COLUMN public.bok_settlement_transaction.settlement_date IS '정산일자(yyyyMMdd)';

COMMENT ON COLUMN public.bok_settlement_transaction.network IS '청산망종류';

COMMENT ON COLUMN public.bok_settlement_transaction.last_inquiry_at IS '마지막조회시각';

COMMENT ON COLUMN public.bok_settlement_transaction.inquiry_count IS '조회횟수';

COMMENT ON COLUMN public.bok_settlement_transaction.first_registered_at IS '최초등록일시';

COMMENT ON COLUMN public.bok_settlement_transaction.first_registrant_id IS '최초등록자식별번호';

COMMENT ON COLUMN public.bok_settlement_transaction.last_modified_at IS '최종수정일시';

COMMENT ON COLUMN public.bok_settlement_transaction.last_modifier_id IS '최종수정자식별번호';

CREATE TABLE public.external_call (
    call_id character varying(20) NOT NULL,
    call_idempotency_key character varying(150) NOT NULL,
    compensation_type character varying(20) DEFAULT 'ORIGINAL'::character varying NOT NULL,
    compensation_target_call_id character varying(20),
    payment_instruction_id character varying(20),
    parent_call_id character varying(20),
    session_id character varying(50),
    user_id character varying(20),
    call_type character varying(30) NOT NULL,
    target_system character varying(50) NOT NULL,
    endpoint_url character varying(500) NOT NULL,
    http_method character varying(10) NOT NULL,
    request_id character varying(50) NOT NULL,
    request_header jsonb,
    request_body jsonb,
    request_body_hash character varying(100),
    response_status_code integer,
    response_header jsonb,
    response_body jsonb,
    business_response_code character varying(50),
    response_message character varying(500),
    result character varying(20) NOT NULL,
    attempt_no integer DEFAULT 1 NOT NULL,
    requested_at timestamp(3) without time zone NOT NULL,
    responded_at timestamp(3) without time zone,
    response_time_ms integer,
    timeout_ms integer NOT NULL,
    first_registered_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registrant_id character varying(20),
    last_modified_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_modifier_id character varying(20),
    CONSTRAINT chk_external_call_attempt_no CHECK ((attempt_no >= 1)),
    CONSTRAINT chk_external_call_call_type CHECK (((call_type)::text = ANY ((ARRAY['ACCOUNT_OWNER_INQUIRY'::character varying, 'BALANCE_INQUIRY'::character varying, 'LIMIT_CHECK'::character varying, 'AUTH_VERIFY'::character varying, 'FRAUD_CHECK'::character varying, 'KFTC_GATEWAY'::character varying, 'BOK_GATEWAY'::character varying, 'INBOUND_RESPONSE'::character varying, 'BALANCE_WITHDRAW_CANCEL'::character varying, 'BALANCE_DEPOSIT_CANCEL'::character varying, 'LIMIT_CONSUME_CANCEL'::character varying, 'AUTH_REVOKE'::character varying, 'ACCOUNT_INQUIRY'::character varying, 'BALANCE_WITHDRAW'::character varying, 'BALANCE_DEPOSIT'::character varying])::text[]))),
    CONSTRAINT chk_external_call_compensation_consistency CHECK ((((compensation_type)::text <> 'COMPENSATION'::text) OR (compensation_target_call_id IS NOT NULL))),
    CONSTRAINT chk_external_call_compensation_type CHECK (((compensation_type)::text = ANY ((ARRAY['ORIGINAL'::character varying, 'RETRY'::character varying, 'COMPENSATION'::character varying])::text[]))),
    CONSTRAINT chk_external_call_http_method CHECK (((http_method)::text = ANY ((ARRAY['GET'::character varying, 'POST'::character varying, 'PUT'::character varying, 'DELETE'::character varying, 'PATCH'::character varying])::text[]))),
    CONSTRAINT chk_external_call_response_time_ms CHECK (((response_time_ms IS NULL) OR (response_time_ms >= 0))),
    CONSTRAINT chk_external_call_result CHECK (((result)::text = ANY ((ARRAY['SUCCESS'::character varying, 'FAIL'::character varying, 'TIMEOUT'::character varying, 'NETWORK_ERROR'::character varying])::text[]))),
    CONSTRAINT chk_external_call_retry_consistency CHECK ((((compensation_type)::text <> 'RETRY'::text) OR (parent_call_id IS NOT NULL))),
    CONSTRAINT chk_external_call_timeout_ms CHECK ((timeout_ms >= 0))
);

COMMENT ON TABLE public.external_call IS '외부호출';

COMMENT ON COLUMN public.external_call.call_id IS '외부호출번호';

COMMENT ON COLUMN public.external_call.call_idempotency_key IS '호출멱등키';

COMMENT ON COLUMN public.external_call.compensation_type IS '보상유형';

COMMENT ON COLUMN public.external_call.compensation_target_call_id IS '보상대상호출번호';

COMMENT ON COLUMN public.external_call.payment_instruction_id IS '결제지시번호';

COMMENT ON COLUMN public.external_call.parent_call_id IS '부모호출번호';

COMMENT ON COLUMN public.external_call.session_id IS '세션ID';

COMMENT ON COLUMN public.external_call.user_id IS '고객번호';

COMMENT ON COLUMN public.external_call.call_type IS '호출종류';

COMMENT ON COLUMN public.external_call.target_system IS '대상시스템';

COMMENT ON COLUMN public.external_call.endpoint_url IS '엔드포인트URL';

COMMENT ON COLUMN public.external_call.http_method IS 'HTTP메서드';

COMMENT ON COLUMN public.external_call.request_id IS '요청ID';

COMMENT ON COLUMN public.external_call.request_header IS '요청헤더';

COMMENT ON COLUMN public.external_call.request_body IS '요청본문';

COMMENT ON COLUMN public.external_call.request_body_hash IS '요청본문해시';

COMMENT ON COLUMN public.external_call.response_status_code IS '응답상태코드';

COMMENT ON COLUMN public.external_call.response_header IS '응답헤더';

COMMENT ON COLUMN public.external_call.response_body IS '응답본문';

COMMENT ON COLUMN public.external_call.business_response_code IS '비즈니스응답코드 — 외부시스템(deposit ErrorCode.name(), KFTC/BOK 코드 등) 원문 박제. V20: VARCHAR(10)→(50)';

COMMENT ON COLUMN public.external_call.response_message IS '응답메시지';

COMMENT ON COLUMN public.external_call.result IS '결과';

COMMENT ON COLUMN public.external_call.attempt_no IS '시도번호';

COMMENT ON COLUMN public.external_call.requested_at IS '요청시각';

COMMENT ON COLUMN public.external_call.responded_at IS '응답시각';

COMMENT ON COLUMN public.external_call.response_time_ms IS '응답시간_ms';

COMMENT ON COLUMN public.external_call.timeout_ms IS '타임아웃설정값';

COMMENT ON COLUMN public.external_call.first_registered_at IS '최초등록일시';

COMMENT ON COLUMN public.external_call.first_registrant_id IS '최초등록자식별번호';

COMMENT ON COLUMN public.external_call.last_modified_at IS '최종수정일시';

COMMENT ON COLUMN public.external_call.last_modifier_id IS '최종수정자식별번호';

CREATE TABLE public.idempotency_key (
    idempotency_key character varying(50) NOT NULL,
    client_id character varying(30) NOT NULL,
    request_hash character varying(100) NOT NULL,
    idempotency_status character varying(20) DEFAULT 'PROCESSING'::character varying NOT NULL,
    first_response_snap jsonb,
    retry_count integer DEFAULT 0 NOT NULL,
    first_received_at timestamp(3) without time zone NOT NULL,
    last_received_at timestamp(3) without time zone NOT NULL,
    expires_at timestamp(3) without time zone NOT NULL,
    first_registered_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registrant_id character varying(20),
    last_modified_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_modifier_id character varying(20),
    CONSTRAINT chk_idempotency_key_retry_count CHECK ((retry_count >= 0)),
    CONSTRAINT chk_idempotency_key_status CHECK (((idempotency_status)::text = ANY ((ARRAY['PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);

COMMENT ON TABLE public.idempotency_key IS '멱등키';

COMMENT ON COLUMN public.idempotency_key.idempotency_key IS '멱등키값';

COMMENT ON COLUMN public.idempotency_key.client_id IS '클라이언트식별자';

COMMENT ON COLUMN public.idempotency_key.request_hash IS '요청내용해시';

COMMENT ON COLUMN public.idempotency_key.idempotency_status IS '멱등키상태';

COMMENT ON COLUMN public.idempotency_key.first_response_snap IS '첫응답스냅샷';

COMMENT ON COLUMN public.idempotency_key.retry_count IS '재시도횟수';

COMMENT ON COLUMN public.idempotency_key.first_received_at IS '최초수신시각';

COMMENT ON COLUMN public.idempotency_key.last_received_at IS '마지막수신시각';

COMMENT ON COLUMN public.idempotency_key.expires_at IS '만료시각';

COMMENT ON COLUMN public.idempotency_key.first_registered_at IS '최초등록일시';

COMMENT ON COLUMN public.idempotency_key.first_registrant_id IS '최초등록자식별번호';

COMMENT ON COLUMN public.idempotency_key.last_modified_at IS '최종수정일시';

COMMENT ON COLUMN public.idempotency_key.last_modifier_id IS '최종수정자식별번호';

CREATE TABLE public.kftc_clearing_transaction (
    clearing_transaction_id character varying(20) NOT NULL,
    our_payment_instruction_id character varying(20) NOT NULL,
    direction character varying(5) NOT NULL,
    counterparty_payment_id character varying(50),
    clearing_no character varying(50) NOT NULL,
    sender_bank_clearing_id character varying(50),
    receiver_bank_clearing_id character varying(50),
    sender_bank_code character(3) NOT NULL,
    sender_account_no_snap character varying(30) NOT NULL,
    sender_holder_name_snap character varying(60) NOT NULL,
    receiver_bank_code character(3) NOT NULL,
    receiver_account_no_snap character varying(30) NOT NULL,
    receiver_holder_name_snap character varying(60) NOT NULL,
    clearing_amount numeric(15,0) NOT NULL,
    currency character(3) DEFAULT 'KRW'::bpchar NOT NULL,
    clearing_status character varying(20) NOT NULL,
    reject_code character varying(30),
    reject_message character varying(200),
    clearing_requested_at character varying(14) NOT NULL,
    ack_received_at character varying(14),
    settled_at character varying(14),
    settlement_date character varying(8),
    network character varying(30) NOT NULL,
    last_inquiry_at timestamp(3) without time zone,
    inquiry_count integer DEFAULT 0 NOT NULL,
    first_registered_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registrant_id character varying(20),
    last_modified_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_modifier_id character varying(20),
    CONSTRAINT chk_kct_clearing_amount CHECK ((clearing_amount >= (0)::numeric)),
    CONSTRAINT chk_kct_clearing_status CHECK (((clearing_status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'ACK'::character varying, 'SETTLED'::character varying, 'REJECTED'::character varying, 'TIMEOUT'::character varying])::text[]))),
    CONSTRAINT chk_kct_direction CHECK (((direction)::text = ANY ((ARRAY['OUT'::character varying, 'IN'::character varying])::text[]))),
    CONSTRAINT chk_kct_network CHECK (((network)::text = ANY ((ARRAY['KFTC_CLEARING'::character varying, 'INTERBANK'::character varying, 'EBANKING'::character varying, 'BOK_CLEARING'::character varying])::text[])))
);

COMMENT ON TABLE public.kftc_clearing_transaction IS 'KFTC청산거래';

COMMENT ON COLUMN public.kftc_clearing_transaction.clearing_transaction_id IS '청산거래번호';

COMMENT ON COLUMN public.kftc_clearing_transaction.our_payment_instruction_id IS '결제지시번호(자행)';

COMMENT ON COLUMN public.kftc_clearing_transaction.direction IS '방향(OUT=타행송신/IN=타행수신)';

COMMENT ON COLUMN public.kftc_clearing_transaction.counterparty_payment_id IS '상대방거래참조';

COMMENT ON COLUMN public.kftc_clearing_transaction.clearing_no IS 'KFTC청산식별번호';

COMMENT ON COLUMN public.kftc_clearing_transaction.sender_bank_clearing_id IS '송신은행청산ID';

COMMENT ON COLUMN public.kftc_clearing_transaction.receiver_bank_clearing_id IS '수신은행청산ID';

COMMENT ON COLUMN public.kftc_clearing_transaction.sender_bank_code IS '송신은행코드';

COMMENT ON COLUMN public.kftc_clearing_transaction.sender_account_no_snap IS '송신계좌번호_스냅샷';

COMMENT ON COLUMN public.kftc_clearing_transaction.sender_holder_name_snap IS '송신예금주명_스냅샷';

COMMENT ON COLUMN public.kftc_clearing_transaction.receiver_bank_code IS '수신은행코드';

COMMENT ON COLUMN public.kftc_clearing_transaction.receiver_account_no_snap IS '수신계좌번호_스냅샷';

COMMENT ON COLUMN public.kftc_clearing_transaction.receiver_holder_name_snap IS '수신예금주명_스냅샷';

COMMENT ON COLUMN public.kftc_clearing_transaction.clearing_amount IS '청산금액';

COMMENT ON COLUMN public.kftc_clearing_transaction.currency IS '통화';

COMMENT ON COLUMN public.kftc_clearing_transaction.clearing_status IS '청산상태';

COMMENT ON COLUMN public.kftc_clearing_transaction.reject_code IS '거절코드';

COMMENT ON COLUMN public.kftc_clearing_transaction.reject_message IS '거절메시지';

COMMENT ON COLUMN public.kftc_clearing_transaction.clearing_requested_at IS '청산요청시각(yyyyMMddHHmmss)';

COMMENT ON COLUMN public.kftc_clearing_transaction.ack_received_at IS 'ACK수신시각(yyyyMMddHHmmss)';

COMMENT ON COLUMN public.kftc_clearing_transaction.settled_at IS '정산완료시각(yyyyMMddHHmmss)';

COMMENT ON COLUMN public.kftc_clearing_transaction.settlement_date IS '정산일자(yyyyMMdd)';

COMMENT ON COLUMN public.kftc_clearing_transaction.network IS '청산망종류';

COMMENT ON COLUMN public.kftc_clearing_transaction.last_inquiry_at IS '마지막조회시각';

COMMENT ON COLUMN public.kftc_clearing_transaction.inquiry_count IS '조회횟수';

COMMENT ON COLUMN public.kftc_clearing_transaction.first_registered_at IS '최초등록일시';

COMMENT ON COLUMN public.kftc_clearing_transaction.first_registrant_id IS '최초등록자식별번호';

COMMENT ON COLUMN public.kftc_clearing_transaction.last_modified_at IS '최종수정일시';

COMMENT ON COLUMN public.kftc_clearing_transaction.last_modifier_id IS '최종수정자식별번호';

CREATE TABLE public.ledger (
    ledger_id character varying(20) NOT NULL,
    payment_instruction_id character varying(20),
    account_id character varying(20) NOT NULL,
    original_ledger_id character varying(20),
    journal_no character varying(20) NOT NULL,
    account_no_snap character varying(30) NOT NULL,
    holder_name_snap character varying(60) NOT NULL,
    debit_credit character varying(20) NOT NULL,
    journal_type character varying(30) NOT NULL,
    amount numeric(15,0) NOT NULL,
    currency character(3) DEFAULT 'KRW'::bpchar NOT NULL,
    balance_before numeric(15,0) NOT NULL,
    balance_after numeric(15,0) NOT NULL,
    counterparty_account_no_snap character varying(30),
    counterparty_bank_code_snap character(3),
    counterparty_holder_name_snap character varying(60),
    transaction_date character varying(8) NOT NULL,
    posting_date character varying(8) NOT NULL,
    value_date character varying(8) NOT NULL,
    posted_at timestamp(3) without time zone NOT NULL,
    system_description character varying(100) NOT NULL,
    passbook_memo_snap character varying(100),
    is_reversal boolean DEFAULT false NOT NULL,
    reversal_reason character varying(20),
    posting_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    first_registered_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registrant_id character varying(20),
    last_modified_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_modifier_id character varying(20),
    CONSTRAINT chk_ledger_amount CHECK ((amount >= (0)::numeric)),
    CONSTRAINT chk_ledger_balance_after CHECK ((balance_after >= (0)::numeric)),
    CONSTRAINT chk_ledger_balance_before CHECK ((balance_before >= (0)::numeric)),
    CONSTRAINT chk_ledger_debit_credit CHECK (((debit_credit)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[]))),
    CONSTRAINT chk_ledger_journal_type CHECK (((journal_type)::text = ANY ((ARRAY['TRANSFER_OUT'::character varying, 'TRANSFER_IN'::character varying, 'CLEARING_PENDING'::character varying, 'FEE'::character varying, 'FEE_INCOME'::character varying, 'REVERSAL_TRANSFER_OUT'::character varying, 'REVERSAL_CLEARING_PENDING'::character varying, 'REVERSAL_FEE'::character varying, 'REVERSAL_FEE_INCOME'::character varying, 'CLEARING_PENDING_UNWIND'::character varying, 'INTERBANK_SETTLEMENT'::character varying])::text[]))),
    CONSTRAINT chk_ledger_posting_status CHECK (((posting_status)::text = ANY ((ARRAY['PENDING'::character varying, 'POSTED'::character varying, 'CANCELED'::character varying])::text[]))),
    CONSTRAINT chk_ledger_reversal_original_consistency CHECK (((is_reversal = false) OR (original_ledger_id IS NOT NULL))),
    CONSTRAINT chk_ledger_reversal_reason CHECK (((reversal_reason IS NULL) OR ((reversal_reason)::text = ANY ((ARRAY['PUBLISH_FAILURE'::character varying, 'SYSTEM_FAILURE'::character varying, 'COMPENSATION'::character varying, 'KFTC_REJECTION'::character varying, 'BOK_REJECTION'::character varying, 'SETTLEMENT_FAILURE'::character varying, 'OPERATOR'::character varying])::text[])))),
    CONSTRAINT chk_ledger_reversal_reason_consistency CHECK (((is_reversal = false) OR (reversal_reason IS NOT NULL)))
);

COMMENT ON TABLE public.ledger IS '계좌원장';

COMMENT ON COLUMN public.ledger.ledger_id IS '분개번호';

COMMENT ON COLUMN public.ledger.payment_instruction_id IS '결제지시번호';

COMMENT ON COLUMN public.ledger.account_id IS '계좌번호';

COMMENT ON COLUMN public.ledger.original_ledger_id IS '원분개참조';

COMMENT ON COLUMN public.ledger.journal_no IS '회계번호';

COMMENT ON COLUMN public.ledger.account_no_snap IS '계좌번호_스냅샷';

COMMENT ON COLUMN public.ledger.holder_name_snap IS '예금주명_스냅샷';

COMMENT ON COLUMN public.ledger.debit_credit IS '차변대변구분';

COMMENT ON COLUMN public.ledger.journal_type IS '분개종류';

COMMENT ON COLUMN public.ledger.amount IS '금액';

COMMENT ON COLUMN public.ledger.currency IS '통화';

COMMENT ON COLUMN public.ledger.balance_before IS '분개직전잔액';

COMMENT ON COLUMN public.ledger.balance_after IS '분개직후잔액';

COMMENT ON COLUMN public.ledger.counterparty_account_no_snap IS '상대계좌번호_스냅샷';

COMMENT ON COLUMN public.ledger.counterparty_bank_code_snap IS '상대은행코드_스냅샷';

COMMENT ON COLUMN public.ledger.counterparty_holder_name_snap IS '상대예금주명_스냅샷';

COMMENT ON COLUMN public.ledger.transaction_date IS '거래일자';

COMMENT ON COLUMN public.ledger.posting_date IS '기장일자';

COMMENT ON COLUMN public.ledger.value_date IS '자금가용일';

COMMENT ON COLUMN public.ledger.posted_at IS '기장시각';

COMMENT ON COLUMN public.ledger.system_description IS '시스템적요';

COMMENT ON COLUMN public.ledger.passbook_memo_snap IS '통장에찍히는메모_스냅샷';

COMMENT ON COLUMN public.ledger.is_reversal IS '역분개여부';

COMMENT ON COLUMN public.ledger.reversal_reason IS '역분개사유';

COMMENT ON COLUMN public.ledger.posting_status IS '기장상태';

COMMENT ON COLUMN public.ledger.first_registered_at IS '최초등록일시';

COMMENT ON COLUMN public.ledger.first_registrant_id IS '최초등록자식별번호';

COMMENT ON COLUMN public.ledger.last_modified_at IS '최종수정일시';

COMMENT ON COLUMN public.ledger.last_modifier_id IS '최종수정자식별번호';

CREATE TABLE public.outbox_message (
    message_id character varying(20) NOT NULL,
    payment_instruction_id character varying(20) NOT NULL,
    event_type character varying(30) NOT NULL,
    event_schema_version character varying(10) NOT NULL,
    payload jsonb NOT NULL,
    publish_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    available_at timestamp(3) without time zone NOT NULL,
    last_error character varying(500),
    published_at timestamp(3) without time zone,
    first_registered_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registrant_id character varying(20),
    last_modified_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_modifier_id character varying(20),
    CONSTRAINT chk_outbox_message_attempt_count CHECK ((attempt_count >= 0)),
    CONSTRAINT chk_outbox_message_event_type CHECK (((event_type)::text = ANY ((ARRAY['PAYMENT_REQUESTED'::character varying, 'PAYMENT_SCHEDULED'::character varying, 'PAYMENT_SCHEDULE_CANCELED'::character varying, 'KFTC_REQUEST_SENT'::character varying, 'KFTC_REJECTED'::character varying, 'KFTC_SETTLED'::character varying, 'BOK_REQUEST_SENT'::character varying, 'BOK_REJECTED'::character varying, 'BOK_CONFIRMED'::character varying, 'PAYMENT_REVERSED'::character varying, 'PAYMENT_COMPLETED'::character varying, 'PAYMENT_FAILED'::character varying, 'PAYMENT_CANCELED'::character varying, 'INBOUND_RECEIVED'::character varying, 'KFTC_ACK_SENT'::character varying, 'BOK_ACK_SENT'::character varying, 'KFTC_SETTLEMENT_SENT'::character varying, 'BOK_CONFIRM_SENT'::character varying, 'KFTC_REJECT_SENT'::character varying, 'BOK_REJECT_SENT'::character varying])::text[]))),
    CONSTRAINT chk_outbox_message_publish_status CHECK (((publish_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PUBLISHING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[])))
);

COMMENT ON TABLE public.outbox_message IS 'Outbox메시지';

COMMENT ON COLUMN public.outbox_message.message_id IS '메시지번호';

COMMENT ON COLUMN public.outbox_message.payment_instruction_id IS '결제지시번호';

COMMENT ON COLUMN public.outbox_message.event_type IS '이벤트종류';

COMMENT ON COLUMN public.outbox_message.event_schema_version IS '이벤트스키마버전';

COMMENT ON COLUMN public.outbox_message.payload IS '페이로드';

COMMENT ON COLUMN public.outbox_message.publish_status IS '발행상태';

COMMENT ON COLUMN public.outbox_message.attempt_count IS '시도횟수';

COMMENT ON COLUMN public.outbox_message.available_at IS '처리가능시각';

COMMENT ON COLUMN public.outbox_message.last_error IS '마지막오류';

COMMENT ON COLUMN public.outbox_message.published_at IS '발행시각';

COMMENT ON COLUMN public.outbox_message.first_registered_at IS '최초등록일시';

COMMENT ON COLUMN public.outbox_message.first_registrant_id IS '최초등록자식별번호';

COMMENT ON COLUMN public.outbox_message.last_modified_at IS '최종수정일시';

COMMENT ON COLUMN public.outbox_message.last_modifier_id IS '최종수정자식별번호';

CREATE TABLE public.payment_instruction (
    payment_instruction_id character varying(20) NOT NULL,
    idempotency_key character varying(50) NOT NULL,
    sender_user_id character varying(20),
    sender_account_id character varying(20),
    auth_token_id character varying(20),
    original_payment_id character varying(20),
    transaction_no character varying(30) NOT NULL,
    sender_account_no_snap character varying(30) NOT NULL,
    sender_account_alias_snap character varying(60),
    receiver_bank_code character(3) NOT NULL,
    receiver_account_no character varying(30) NOT NULL,
    receiver_holder_name_snap character varying(60),
    holder_inquiry_at timestamp(3) without time zone,
    is_intra_bank boolean NOT NULL,
    routing_network_type character varying(20) NOT NULL,
    transfer_amount numeric(15,0) NOT NULL,
    fee_amount numeric(15,0) DEFAULT 0 NOT NULL,
    receiver_passbook_sender_display character varying(60),
    receiver_memo character varying(100),
    sender_memo character varying(100),
    status character varying(20) NOT NULL,
    failure_category character varying(30),
    channel character varying(20) NOT NULL,
    requested_at timestamp(3) without time zone NOT NULL,
    completed_at timestamp(3) without time zone,
    business_date character varying(8) NOT NULL,
    next_retry_at timestamp(3) without time zone,
    next_timeout_at timestamp(3) without time zone,
    version integer DEFAULT 0 NOT NULL,
    trigger_source character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    is_scheduled boolean DEFAULT false NOT NULL,
    scheduled_execution_at timestamp(3) without time zone,
    first_registered_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registrant_id character varying(20),
    last_modified_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_modifier_id character varying(20),
    CONSTRAINT chk_payment_instruction_channel CHECK (((channel)::text = ANY ((ARRAY['WEB'::character varying, 'MOBILE'::character varying, 'BRANCH'::character varying, 'ATM'::character varying, 'OPEN_BANKING'::character varying, 'INBOUND'::character varying])::text[]))),
    CONSTRAINT chk_payment_instruction_failure_category CHECK (((failure_category IS NULL) OR ((failure_category)::text = ANY ((ARRAY['INSUFFICIENT_BALANCE'::character varying, 'LIMIT_EXCEEDED'::character varying, 'AUTH_FAILED'::character varying, 'OWNER_INQUIRY_FAILED'::character varying, 'KFTC_REJECTED'::character varying, 'KFTC_TIMEOUT'::character varying, 'BOK_REJECTED'::character varying, 'BOK_TIMEOUT'::character varying, 'INVALID_ACCOUNT'::character varying, 'FRAUD_DETECTED'::character varying, 'SYSTEM_ERROR'::character varying, 'ACCOUNT_RESTRICTED'::character varying, 'ACCOUNT_NOT_FOUND'::character varying, 'ACCOUNT_CLOSED'::character varying])::text[])))),
    CONSTRAINT chk_payment_instruction_fee_amount CHECK ((fee_amount >= (0)::numeric)),
    CONSTRAINT chk_payment_instruction_routing_network_type CHECK (((routing_network_type)::text = ANY ((ARRAY['INTERNAL'::character varying, 'KFTC'::character varying, 'BOK'::character varying])::text[]))),
    CONSTRAINT chk_payment_instruction_scheduled_consistency CHECK (((is_scheduled = false) OR (scheduled_execution_at IS NOT NULL))),
    CONSTRAINT chk_payment_instruction_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'AUTHORIZED'::character varying, 'SCHEDULED'::character varying, 'PROCESSING'::character varying, 'CLEARING'::character varying, 'REVERSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'CANCELED'::character varying])::text[]))),
    CONSTRAINT chk_payment_instruction_transfer_amount CHECK ((transfer_amount >= (0)::numeric)),
    CONSTRAINT chk_payment_instruction_trigger_source CHECK (((trigger_source)::text = ANY ((ARRAY['USER'::character varying, 'AUTO_TRANSFER'::character varying, 'SCHEDULER'::character varying, 'OPERATOR'::character varying, 'COUNTERPARTY_BANK'::character varying])::text[]))),
    CONSTRAINT chk_payment_instruction_version CHECK ((version >= 0))
);

COMMENT ON TABLE public.payment_instruction IS '결제지시';

COMMENT ON COLUMN public.payment_instruction.payment_instruction_id IS '결제지시번호';

COMMENT ON COLUMN public.payment_instruction.idempotency_key IS '연결된멱등키값';

COMMENT ON COLUMN public.payment_instruction.sender_user_id IS '송신고객번호';

COMMENT ON COLUMN public.payment_instruction.sender_account_id IS '송신계좌번호';

COMMENT ON COLUMN public.payment_instruction.auth_token_id IS '인증토큰번호';

COMMENT ON COLUMN public.payment_instruction.original_payment_id IS '원거래참조';

COMMENT ON COLUMN public.payment_instruction.transaction_no IS '거래번호';

COMMENT ON COLUMN public.payment_instruction.sender_account_no_snap IS '송신계좌번호_스냅샷';

COMMENT ON COLUMN public.payment_instruction.sender_account_alias_snap IS '송신계좌별명_스냅샷';

COMMENT ON COLUMN public.payment_instruction.receiver_bank_code IS '수신은행코드';

COMMENT ON COLUMN public.payment_instruction.receiver_account_no IS '수신계좌번호';

COMMENT ON COLUMN public.payment_instruction.receiver_holder_name_snap IS '수신예금주명_스냅샷';

COMMENT ON COLUMN public.payment_instruction.holder_inquiry_at IS '예금주조회시각';

COMMENT ON COLUMN public.payment_instruction.is_intra_bank IS '자행이체여부';

COMMENT ON COLUMN public.payment_instruction.routing_network_type IS '라우팅망종류';

COMMENT ON COLUMN public.payment_instruction.transfer_amount IS '이체금액';

COMMENT ON COLUMN public.payment_instruction.fee_amount IS '수수료';

COMMENT ON COLUMN public.payment_instruction.receiver_passbook_sender_display IS '수신통장_송신자표시명';

COMMENT ON COLUMN public.payment_instruction.receiver_memo IS '받는분통장메모';

COMMENT ON COLUMN public.payment_instruction.sender_memo IS '내통장메모';

COMMENT ON COLUMN public.payment_instruction.status IS '진행상태';

COMMENT ON COLUMN public.payment_instruction.failure_category IS '실패분류';

COMMENT ON COLUMN public.payment_instruction.channel IS '채널';

COMMENT ON COLUMN public.payment_instruction.requested_at IS '요청시각';

COMMENT ON COLUMN public.payment_instruction.completed_at IS '완료시각';

COMMENT ON COLUMN public.payment_instruction.business_date IS '영업일자';

COMMENT ON COLUMN public.payment_instruction.next_retry_at IS '다음재시도시각';

COMMENT ON COLUMN public.payment_instruction.next_timeout_at IS '다음타임아웃시각';

COMMENT ON COLUMN public.payment_instruction.version IS '낙관적락버전';

COMMENT ON COLUMN public.payment_instruction.trigger_source IS '트리거주체';

COMMENT ON COLUMN public.payment_instruction.is_scheduled IS '예약여부';

COMMENT ON COLUMN public.payment_instruction.scheduled_execution_at IS '예약실행시각';

COMMENT ON COLUMN public.payment_instruction.first_registered_at IS '최초등록일시';

COMMENT ON COLUMN public.payment_instruction.first_registrant_id IS '최초등록자식별번호';

COMMENT ON COLUMN public.payment_instruction.last_modified_at IS '최종수정일시';

COMMENT ON COLUMN public.payment_instruction.last_modifier_id IS '최종수정자식별번호';

CREATE TABLE public.status_history (
    history_id character varying(20) NOT NULL,
    payment_instruction_id character varying(20) NOT NULL,
    related_external_call_id character varying(20),
    sequence_in_payment integer NOT NULL,
    previous_status character varying(20),
    next_status character varying(20) NOT NULL,
    event_type character varying(30) NOT NULL,
    reason_code character varying(30),
    reason_message character varying(200),
    triggered_by character varying(20) NOT NULL,
    operator_id character varying(20),
    payload_snapshot jsonb,
    event_occurred_at timestamp(3) without time zone NOT NULL,
    db_recorded_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registered_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    first_registrant_id character varying(20),
    last_modified_at timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    last_modifier_id character varying(20),
    CONSTRAINT chk_status_history_event_type CHECK (((event_type)::text = ANY ((ARRAY['INSTRUCTION_CREATED'::character varying, 'OWNER_INQUIRY_DONE'::character varying, 'OWNER_INQUIRY_FAILED'::character varying, 'AUTH_PASSED'::character varying, 'AUTH_FAILED'::character varying, 'SCHEDULED_REGISTERED'::character varying, 'SCHEDULED_TRIGGERED'::character varying, 'SCHEDULED_CANCELED'::character varying, 'PROCESSING_STARTED'::character varying, 'BALANCE_CHECK_FAILED'::character varying, 'LIMIT_CHECK_FAILED'::character varying, 'KFTC_REQUEST_SENT'::character varying, 'KFTC_ACK_RECEIVED'::character varying, 'KFTC_REJECT_RECEIVED'::character varying, 'KFTC_SETTLED'::character varying, 'BOK_REQUEST_SENT'::character varying, 'BOK_ACK_RECEIVED'::character varying, 'BOK_REJECT_RECEIVED'::character varying, 'BOK_CONFIRMED'::character varying, 'REVERSAL_STARTED'::character varying, 'REVERSAL_COMPLETED'::character varying, 'PAYMENT_COMPLETED'::character varying, 'PAYMENT_FAILED'::character varying, 'PAYMENT_CANCELED'::character varying, 'INBOUND_REJECTED'::character varying, 'INBOUND_RECEIVED'::character varying, 'SYSTEM_FAILURE_DETECTED'::character varying, 'COMPENSATION_STARTED'::character varying, 'COMPENSATION_COMPLETED'::character varying, 'COMPENSATION_FAILED'::character varying, 'KFTC_ACK_SENT'::character varying, 'BOK_ACK_SENT'::character varying, 'KFTC_SETTLEMENT_SENT'::character varying, 'BOK_CONFIRM_SENT'::character varying, 'KFTC_REJECT_SENT'::character varying, 'BOK_REJECT_SENT'::character varying, 'INBOUND_VALIDATION_PASSED'::character varying, 'INBOUND_VALIDATION_FAILED'::character varying, 'KFTC_REQUEST_FAILED'::character varying, 'KFTC_TIMEOUT_DETECTED'::character varying, 'KFTC_SETTLEMENT_FAILED'::character varying, 'OPERATOR_CANCEL_DECIDED'::character varying, 'BOK_REQUEST_FAILED'::character varying, 'BOK_TIMEOUT_DETECTED'::character varying, 'BOK_SETTLEMENT_FAILED'::character varying, 'ACCOUNT_CHECK_FAILED'::character varying])::text[]))),
    CONSTRAINT chk_status_history_next_status CHECK (((next_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'AUTHORIZED'::character varying, 'SCHEDULED'::character varying, 'PROCESSING'::character varying, 'CLEARING'::character varying, 'REVERSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'CANCELED'::character varying])::text[]))),
    CONSTRAINT chk_status_history_operator_consistency CHECK ((((triggered_by)::text <> 'OPERATOR'::text) OR (operator_id IS NOT NULL))),
    CONSTRAINT chk_status_history_previous_status CHECK (((previous_status IS NULL) OR ((previous_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'AUTHORIZED'::character varying, 'SCHEDULED'::character varying, 'PROCESSING'::character varying, 'CLEARING'::character varying, 'REVERSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'CANCELED'::character varying])::text[])))),
    CONSTRAINT chk_status_history_sequence_in_payment CHECK ((sequence_in_payment >= 1)),
    CONSTRAINT chk_status_history_triggered_by CHECK (((triggered_by)::text = ANY ((ARRAY['USER'::character varying, 'SYSTEM'::character varying, 'KFTC'::character varying, 'BOK'::character varying, 'OPERATOR'::character varying, 'SCHEDULER'::character varying, 'COUNTERPARTY_BANK'::character varying])::text[])))
);

COMMENT ON TABLE public.status_history IS '상태이력';

COMMENT ON COLUMN public.status_history.history_id IS '상태이력번호';

COMMENT ON COLUMN public.status_history.payment_instruction_id IS '결제지시번호';

COMMENT ON COLUMN public.status_history.related_external_call_id IS '관련외부호출번호';

COMMENT ON COLUMN public.status_history.sequence_in_payment IS '결제지시내순번';

COMMENT ON COLUMN public.status_history.previous_status IS '이전상태';

COMMENT ON COLUMN public.status_history.next_status IS '다음상태';

COMMENT ON COLUMN public.status_history.event_type IS '이벤트종류';

COMMENT ON COLUMN public.status_history.reason_code IS '사유코드';

COMMENT ON COLUMN public.status_history.reason_message IS '사유메시지';

COMMENT ON COLUMN public.status_history.triggered_by IS '트리거주체';

COMMENT ON COLUMN public.status_history.operator_id IS '운영자ID';

COMMENT ON COLUMN public.status_history.payload_snapshot IS '페이로드스냅샷';

COMMENT ON COLUMN public.status_history.event_occurred_at IS '이벤트발생시각';

COMMENT ON COLUMN public.status_history.db_recorded_at IS 'DB기록시각';

COMMENT ON COLUMN public.status_history.first_registered_at IS '최초등록일시';

COMMENT ON COLUMN public.status_history.first_registrant_id IS '최초등록자식별번호';

COMMENT ON COLUMN public.status_history.last_modified_at IS '최종수정일시';

COMMENT ON COLUMN public.status_history.last_modifier_id IS '최종수정자식별번호';

ALTER TABLE ONLY public.bok_settlement_transaction
    ADD CONSTRAINT pk_bok_settlement_transaction PRIMARY KEY (settlement_transaction_id);

ALTER TABLE ONLY public.external_call
    ADD CONSTRAINT pk_external_call PRIMARY KEY (call_id);

ALTER TABLE ONLY public.idempotency_key
    ADD CONSTRAINT pk_idempotency_key PRIMARY KEY (idempotency_key);

ALTER TABLE ONLY public.kftc_clearing_transaction
    ADD CONSTRAINT pk_kftc_clearing_transaction PRIMARY KEY (clearing_transaction_id);

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT pk_ledger PRIMARY KEY (ledger_id);

ALTER TABLE ONLY public.outbox_message
    ADD CONSTRAINT pk_outbox_message PRIMARY KEY (message_id);

ALTER TABLE ONLY public.payment_instruction
    ADD CONSTRAINT pk_payment_instruction PRIMARY KEY (payment_instruction_id);

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT pk_status_history PRIMARY KEY (history_id);

ALTER TABLE ONLY public.bok_settlement_transaction
    ADD CONSTRAINT uq_bst_bok_reference_no UNIQUE (bok_reference_no);

ALTER TABLE ONLY public.bok_settlement_transaction
    ADD CONSTRAINT uq_bst_payment_instruction UNIQUE (our_payment_instruction_id);

ALTER TABLE ONLY public.external_call
    ADD CONSTRAINT uq_external_call_idempotency_key UNIQUE (call_idempotency_key);

ALTER TABLE ONLY public.external_call
    ADD CONSTRAINT uq_external_call_request_id UNIQUE (request_id);

ALTER TABLE ONLY public.idempotency_key
    ADD CONSTRAINT uq_idempotency_key_client_combo UNIQUE (client_id, idempotency_key);

ALTER TABLE ONLY public.kftc_clearing_transaction
    ADD CONSTRAINT uq_kct_clearing_no UNIQUE (clearing_no);

ALTER TABLE ONLY public.kftc_clearing_transaction
    ADD CONSTRAINT uq_kct_payment_instruction UNIQUE (our_payment_instruction_id);

ALTER TABLE ONLY public.payment_instruction
    ADD CONSTRAINT uq_payment_instruction_auth_token_id UNIQUE (auth_token_id);

ALTER TABLE ONLY public.payment_instruction
    ADD CONSTRAINT uq_payment_instruction_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE ONLY public.payment_instruction
    ADD CONSTRAINT uq_payment_instruction_transaction_no UNIQUE (transaction_no);

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT uq_status_history_payment_sequence UNIQUE (payment_instruction_id, sequence_in_payment);

CREATE INDEX idx_pi_next_timeout_at ON public.payment_instruction USING btree (next_timeout_at) WHERE (next_timeout_at IS NOT NULL);

ALTER TABLE ONLY public.bok_settlement_transaction
    ADD CONSTRAINT fk_bst_pi FOREIGN KEY (our_payment_instruction_id) REFERENCES public.payment_instruction(payment_instruction_id);

ALTER TABLE ONLY public.external_call
    ADD CONSTRAINT fk_external_call_compensation_target FOREIGN KEY (compensation_target_call_id) REFERENCES public.external_call(call_id);

ALTER TABLE ONLY public.external_call
    ADD CONSTRAINT fk_external_call_parent FOREIGN KEY (parent_call_id) REFERENCES public.external_call(call_id);

ALTER TABLE ONLY public.external_call
    ADD CONSTRAINT fk_external_call_payment_instruction FOREIGN KEY (payment_instruction_id) REFERENCES public.payment_instruction(payment_instruction_id);

ALTER TABLE ONLY public.kftc_clearing_transaction
    ADD CONSTRAINT fk_kct_pi FOREIGN KEY (our_payment_instruction_id) REFERENCES public.payment_instruction(payment_instruction_id);

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT fk_ledger_original FOREIGN KEY (original_ledger_id) REFERENCES public.ledger(ledger_id);

ALTER TABLE ONLY public.ledger
    ADD CONSTRAINT fk_ledger_payment_instruction FOREIGN KEY (payment_instruction_id) REFERENCES public.payment_instruction(payment_instruction_id);

ALTER TABLE ONLY public.outbox_message
    ADD CONSTRAINT fk_outbox_message_payment_instruction FOREIGN KEY (payment_instruction_id) REFERENCES public.payment_instruction(payment_instruction_id);

ALTER TABLE ONLY public.payment_instruction
    ADD CONSTRAINT fk_payment_instruction_idempotency FOREIGN KEY (idempotency_key) REFERENCES public.idempotency_key(idempotency_key);

ALTER TABLE ONLY public.payment_instruction
    ADD CONSTRAINT fk_payment_instruction_original FOREIGN KEY (original_payment_id) REFERENCES public.payment_instruction(payment_instruction_id);

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT fk_status_history_external_call FOREIGN KEY (related_external_call_id) REFERENCES public.external_call(call_id);

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT fk_status_history_payment_instruction FOREIGN KEY (payment_instruction_id) REFERENCES public.payment_instruction(payment_instruction_id);


-- ============================================================================
-- SERVICE: common database
-- DATABASE: common_db
-- ============================================================================
\connect common_db

CREATE TABLE public.common_account (
    account_id bigint NOT NULL,
    account_no character varying(30),
    customer_id bigint NOT NULL,
    customer_no character varying(30),
    contract_id bigint,
    account_type_cd character varying(30),
    bank_cd character varying(10),
    account_nickname character varying(100),
    balance bigint,
    currency_cd character(3),
    account_password_hash character varying(255),
    daily_withdrawal_limit bigint,
    daily_withdrawal_count integer,
    suspic_account_yn character(1),
    account_status character varying(20),
    account_opened_at character(8),
    account_closed_at character(8),
    account_cancel_at timestamp(3) with time zone,
    last_transaction_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone,
    updated_by bigint
);

CREATE SEQUENCE public.common_account_account_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_account_account_id_seq OWNED BY public.common_account.account_id;

CREATE TABLE public.common_contract (
    contract_id bigint NOT NULL,
    contract_no character varying(50),
    customer_id bigint NOT NULL,
    customer_no character varying(30),
    product_id bigint NOT NULL,
    biz_div_cd character varying(10),
    contract_amount bigint,
    rate_type_cd character varying(10),
    base_rate_bps integer,
    spread_bps integer,
    preferential_bps integer,
    total_rate_bps integer,
    interest_amount_at_maturity bigint,
    contract_start_date character(8),
    contract_end_date character(8),
    contract_cancel_date character(8),
    contract_cancel_reason character varying(200),
    auto_transfer_yn character(1),
    auto_transfer_day integer,
    signed_at timestamp(3) with time zone,
    contract_channel_cd character varying(20),
    spot_id bigint,
    spot_name character varying(100),
    manager_id bigint,
    manager_name character varying(100),
    proxy_yn character(1),
    contract_status character varying(20),
    term_url character varying(500),
    term_hash character varying(64),
    contract_url character varying(500),
    contract_hash character varying(64),
    created_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    updated_by bigint
);

CREATE SEQUENCE public.common_contract_contract_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_contract_contract_id_seq OWNED BY public.common_contract.contract_id;

CREATE TABLE public.common_product (
    product_id bigint NOT NULL,
    product_cd character varying(30) NOT NULL,
    biz_div_cd character varying(10) NOT NULL,
    product_name character varying(200) NOT NULL,
    product_type_cd character varying(20),
    product_description text,
    target_type_cd character varying(50),
    channel_cd character varying(50),
    currency_cd character(3),
    policy_product_yn character(1),
    min_amount bigint,
    max_amount bigint,
    min_period_mo integer,
    max_period_mo integer,
    sale_yn character(1),
    sale_start_date character(8),
    sale_end_date character(8),
    product_brochure_url character varying(500),
    financial_consumer_act_yn character(1),
    product_status character varying(50),
    created_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    updated_by bigint
);

CREATE SEQUENCE public.common_product_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_product_product_id_seq OWNED BY public.common_product.product_id;

CREATE TABLE public.common_terms_consent (
    consent_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    terms_template_id bigint NOT NULL,
    biz_div_cd character varying(10) NOT NULL,
    consent_target_id bigint,
    consent_status_cd character varying(10) NOT NULL,
    agreed_yn character(1) NOT NULL,
    agreed_at character(8) NOT NULL,
    consent_method_cd character varying(10) NOT NULL,
    consent_tool character varying(500),
    signed_doc_url character varying(500),
    signed_doc_hash character varying(64),
    client_ip inet,
    withdrawn_yn character(1) DEFAULT 'N'::bpchar NOT NULL,
    withdrawn_at timestamp(3) with time zone,
    withdrawn_reason character varying(500),
    retention_until character(8),
    created_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.common_terms_consent_consent_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_terms_consent_consent_id_seq OWNED BY public.common_terms_consent.consent_id;

CREATE TABLE public.common_terms_template (
    terms_template_id bigint NOT NULL,
    terms_no character varying(50) NOT NULL,
    terms_name character varying(200) NOT NULL,
    terms_category_cd character varying(10) NOT NULL,
    description text,
    required_yn character(1) DEFAULT 'Y'::bpchar NOT NULL,
    biz_div_cd character varying(50) NOT NULL,
    active_yn character(1) DEFAULT 'Y'::bpchar NOT NULL,
    created_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    updated_by bigint,
    deleted_at timestamp(3) with time zone,
    deleted_by bigint
);

CREATE SEQUENCE public.common_terms_template_terms_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_terms_template_terms_template_id_seq OWNED BY public.common_terms_template.terms_template_id;

CREATE TABLE public.common_transaction (
    transaction_id bigint NOT NULL,
    transaction_no character varying(50),
    account_id bigint,
    contract_id bigint,
    transaction_type_cd character varying(30),
    debit_credit_type character varying(10),
    transaction_amount bigint,
    balance_before bigint,
    balance_after bigint,
    fee_amount bigint,
    channel_cd character varying(30),
    counterparty_bank_cd character varying(10),
    counterparty_bank_name character varying(100),
    counterparty_account_no character varying(30),
    counterparty_name character varying(100),
    counterparty_customer_id bigint,
    counterparty_account_id bigint,
    counterparty_name_verified_yn character(1),
    original_transaction_id bigint,
    transaction_memo character varying(255),
    transaction_status character varying(20),
    transacted_at timestamp(3) with time zone,
    currency_cd character(3),
    available_balance bigint,
    transaction_summary character varying(100),
    transfer_type_cd character varying(30),
    transfer_requested_at timestamp(3) with time zone,
    transfer_completed_at timestamp(3) with time zone,
    transfer_failed_yn character(1),
    payment_method_code character varying(30),
    card_payment_yn character(1),
    payment_failed_yn character(1),
    merchant_no character varying(50),
    merchant_name character varying(100),
    failure_type_cd character varying(30),
    failure_reason_cd character varying(50),
    failure_cause_cd character varying(50),
    failed_at timestamp(3) with time zone,
    retry_count integer,
    approval_no character varying(50),
    external_transaction_no character varying(100),
    terminal_id character varying(50),
    client_ip character varying(45),
    transaction_location character varying(100),
    ledger_posted_at timestamp(3) with time zone,
    cancelled_at timestamp(3) with time zone,
    created_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    created_by bigint,
    updated_at timestamp(3) with time zone DEFAULT now() NOT NULL,
    updated_by bigint
);

CREATE SEQUENCE public.common_transaction_transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.common_transaction_transaction_id_seq OWNED BY public.common_transaction.transaction_id;

ALTER TABLE ONLY public.common_account ALTER COLUMN account_id SET DEFAULT nextval('public.common_account_account_id_seq'::regclass);

ALTER TABLE ONLY public.common_contract ALTER COLUMN contract_id SET DEFAULT nextval('public.common_contract_contract_id_seq'::regclass);

ALTER TABLE ONLY public.common_product ALTER COLUMN product_id SET DEFAULT nextval('public.common_product_product_id_seq'::regclass);

ALTER TABLE ONLY public.common_terms_consent ALTER COLUMN consent_id SET DEFAULT nextval('public.common_terms_consent_consent_id_seq'::regclass);

ALTER TABLE ONLY public.common_terms_template ALTER COLUMN terms_template_id SET DEFAULT nextval('public.common_terms_template_terms_template_id_seq'::regclass);

ALTER TABLE ONLY public.common_transaction ALTER COLUMN transaction_id SET DEFAULT nextval('public.common_transaction_transaction_id_seq'::regclass);

ALTER TABLE ONLY public.common_account
    ADD CONSTRAINT common_account_account_no_key UNIQUE (account_no);

ALTER TABLE ONLY public.common_account
    ADD CONSTRAINT common_account_pkey PRIMARY KEY (account_id);

ALTER TABLE ONLY public.common_contract
    ADD CONSTRAINT common_contract_pkey PRIMARY KEY (contract_id);

ALTER TABLE ONLY public.common_product
    ADD CONSTRAINT common_product_pkey PRIMARY KEY (product_id);

ALTER TABLE ONLY public.common_product
    ADD CONSTRAINT common_product_product_cd_key UNIQUE (product_cd);

ALTER TABLE ONLY public.common_terms_consent
    ADD CONSTRAINT common_terms_consent_pkey PRIMARY KEY (consent_id, customer_id);

ALTER TABLE ONLY public.common_terms_template
    ADD CONSTRAINT common_terms_template_pkey PRIMARY KEY (terms_template_id);

ALTER TABLE ONLY public.common_terms_template
    ADD CONSTRAINT common_terms_template_terms_no_key UNIQUE (terms_no);

ALTER TABLE ONLY public.common_transaction
    ADD CONSTRAINT common_transaction_pkey PRIMARY KEY (transaction_id);

CREATE INDEX idx_common_account_no ON public.common_account USING btree (account_no);

CREATE INDEX idx_common_account_type ON public.common_account USING btree (account_type_cd);

CREATE INDEX idx_common_contract_cust ON public.common_contract USING btree (customer_id, contract_status);

CREATE INDEX idx_common_contract_no ON public.common_contract USING btree (contract_no) WHERE (contract_no IS NOT NULL);

CREATE INDEX idx_common_contract_prod ON public.common_contract USING btree (product_id);

CREATE INDEX idx_common_product_biz ON public.common_product USING btree (biz_div_cd, product_status);

CREATE INDEX idx_common_terms_consent_customer ON public.common_terms_consent USING btree (customer_id);

CREATE INDEX idx_common_terms_consent_template ON public.common_terms_consent USING btree (terms_template_id);

CREATE INDEX idx_common_terms_template_biz ON public.common_terms_template USING btree (biz_div_cd, active_yn);

CREATE INDEX idx_common_tx_account ON public.common_transaction USING btree (account_id, transacted_at DESC);

CREATE INDEX idx_common_tx_contract ON public.common_transaction USING btree (contract_id, transacted_at DESC) WHERE (contract_id IS NOT NULL);

CREATE INDEX idx_common_tx_no ON public.common_transaction USING btree (transaction_no) WHERE (transaction_no IS NOT NULL);

ALTER TABLE ONLY public.common_contract
    ADD CONSTRAINT fk_common_contract_product FOREIGN KEY (product_id) REFERENCES public.common_product(product_id);

ALTER TABLE ONLY public.common_terms_consent
    ADD CONSTRAINT fk_common_terms_consent_template FOREIGN KEY (terms_template_id) REFERENCES public.common_terms_template(terms_template_id);

ALTER TABLE ONLY public.common_transaction
    ADD CONSTRAINT fk_common_tx_account FOREIGN KEY (account_id) REFERENCES public.common_account(account_id);

ALTER TABLE ONLY public.common_transaction
    ADD CONSTRAINT fk_common_tx_contract FOREIGN KEY (contract_id) REFERENCES public.common_contract(contract_id);

ALTER TABLE ONLY public.common_transaction
    ADD CONSTRAINT fk_common_tx_original FOREIGN KEY (original_transaction_id) REFERENCES public.common_transaction(transaction_id) DEFERRABLE INITIALLY DEFERRED;


-- ============================================================================
-- SERVICE: master-service
-- DATABASE: master_db
-- ============================================================================
\connect master_db

CREATE TABLE public.code_master (
    code_id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by bigint NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by bigint,
    updated_at timestamp(6) with time zone NOT NULL,
    updated_by bigint NOT NULL,
    version integer NOT NULL,
    active_yn character varying(1) NOT NULL,
    code_cd character varying(50) NOT NULL,
    code_desc character varying(500),
    code_group_cd character varying(50) NOT NULL,
    code_name character varying(200),
    sort_no integer
);

ALTER TABLE public.code_master ALTER COLUMN code_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.code_master_code_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE public.status_history (
    sthist_id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by bigint NOT NULL,
    after_status_cd character varying(50) NOT NULL,
    before_status_cd character varying(50),
    change_reason_cd character varying(50),
    change_remark character varying(500),
    changed_at timestamp(6) with time zone NOT NULL,
    changed_by bigint NOT NULL,
    target_domain_cd character varying(30) NOT NULL,
    target_id bigint NOT NULL,
    target_table_cd character varying(50) NOT NULL
);

ALTER TABLE public.status_history ALTER COLUMN sthist_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.status_history_sthist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE ONLY public.code_master
    ADD CONSTRAINT code_master_pkey PRIMARY KEY (code_id);

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT status_history_pkey PRIMARY KEY (sthist_id);

ALTER TABLE ONLY public.code_master
    ADD CONSTRAINT uk_code_master_group_code UNIQUE (code_group_cd, code_cd);

CREATE INDEX idx_status_history_target ON public.status_history USING btree (target_domain_cd, target_table_cd, target_id, changed_at);


-- ============================================================================
-- SERVICE: auto-loan-review
-- DATABASE: ai_db
-- ============================================================================
\connect ai_db

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';

CREATE FUNCTION public.ai_embedding_set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE FUNCTION public.fn_aal_block_mutate() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'agent_audit_log is INSERT-ONLY. UPDATE/DELETE is forbidden (여신전문금융업법 §52의2).';
END;
$$;

CREATE TABLE public.agent_audit_log (
    id bigint NOT NULL,
    rev_id bigint NOT NULL,
    schema_version character varying(10) DEFAULT 'v1'::character varying NOT NULL,
    track character varying(16) NOT NULL,
    request_snapshot jsonb NOT NULL,
    opinion_json jsonb NOT NULL,
    tool_calls_json jsonb DEFAULT '[]'::jsonb NOT NULL,
    raw_llm_response text,
    pii_masked boolean DEFAULT true NOT NULL,
    fallback_reason character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    retention_until date GENERATED ALWAYS AS (((timezone('UTC'::text, created_at))::date + '5 years'::interval)) STORED NOT NULL,
    CONSTRAINT chk_aal_opinion_size CHECK ((pg_column_size(opinion_json) < 65536)),
    CONSTRAINT chk_aal_request_size CHECK ((pg_column_size(request_snapshot) < 131072)),
    CONSTRAINT chk_aal_schema_version CHECK (((schema_version)::text = 'v1'::text)),
    CONSTRAINT chk_aal_track CHECK (((track)::text = ANY ((ARRAY['TRACK_1'::character varying, 'TRACK_2'::character varying, 'TRACK_3'::character varying])::text[])))
);

CREATE SEQUENCE public.agent_audit_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.agent_audit_log_id_seq OWNED BY public.agent_audit_log.id;

CREATE TABLE public.ai_embedding (
    id bigint NOT NULL,
    corpus text NOT NULL,
    source_id text NOT NULL,
    chunk_seq smallint DEFAULT 0 NOT NULL,
    chunk_text text NOT NULL,
    chunk_summary text,
    embedding public.vector(1024) NOT NULL,
    embedding_model text DEFAULT 'text-embedding-005'::text NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    fts_tokens tsvector,
    effective_date date,
    expiry_date date,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE public.ai_embedding_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.ai_embedding_id_seq OWNED BY public.ai_embedding.id;

CREATE TABLE public.fairness_report (
    id bigint NOT NULL,
    report_month date NOT NULL,
    group_key character varying(64) NOT NULL,
    approval_rate numeric(5,4) NOT NULL,
    sample_count integer NOT NULL,
    overall_rate numeric(5,4) NOT NULL,
    rate_gap numeric(5,4) NOT NULL,
    flagged boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE public.fairness_report_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.fairness_report_id_seq OWNED BY public.fairness_report.id;

CREATE TABLE public.psi_baseline (
    id bigint NOT NULL,
    feature_name character varying(128) NOT NULL,
    bucket_index smallint NOT NULL,
    bucket_low numeric(18,6),
    bucket_high numeric(18,6),
    baseline_ratio numeric(8,6) NOT NULL,
    baseline_date date NOT NULL,
    model_version character varying(64) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE public.psi_baseline_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.psi_baseline_id_seq OWNED BY public.psi_baseline.id;

CREATE TABLE public.psi_drift_result (
    id bigint NOT NULL,
    feature_name character varying(128) NOT NULL,
    calc_week date NOT NULL,
    psi_value numeric(8,6) NOT NULL,
    status character varying(16) NOT NULL,
    sample_count integer NOT NULL,
    model_version character varying(64) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_psi_status CHECK (((status)::text = ANY ((ARRAY['STABLE'::character varying, 'WARNING'::character varying, 'CRITICAL'::character varying])::text[])))
);

CREATE SEQUENCE public.psi_drift_result_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.psi_drift_result_id_seq OWNED BY public.psi_drift_result.id;

CREATE TABLE public.shadow_run_result (
    id bigint NOT NULL,
    rev_id bigint NOT NULL,
    prod_opinion_json jsonb NOT NULL,
    shadow_opinion_json jsonb NOT NULL,
    diverged boolean DEFAULT false NOT NULL,
    diverge_reasons text DEFAULT '[]'::text NOT NULL,
    prod_track character varying(16) NOT NULL,
    shadow_track character varying(16) NOT NULL,
    prod_decision_score numeric(6,4),
    shadow_decision_score numeric(6,4),
    shadow_model character varying(64) NOT NULL,
    shadow_prompt_version character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    rag_enabled boolean DEFAULT false NOT NULL,
    rag_backend character varying(16) DEFAULT 'inline'::character varying NOT NULL,
    CONSTRAINT chk_prod_track CHECK (((prod_track)::text = ANY ((ARRAY['TRACK_1'::character varying, 'TRACK_2'::character varying, 'TRACK_3'::character varying])::text[]))),
    CONSTRAINT chk_shadow_track CHECK (((shadow_track)::text = ANY ((ARRAY['TRACK_1'::character varying, 'TRACK_2'::character varying, 'TRACK_3'::character varying])::text[])))
);

CREATE SEQUENCE public.shadow_run_result_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.shadow_run_result_id_seq OWNED BY public.shadow_run_result.id;

ALTER TABLE ONLY public.agent_audit_log ALTER COLUMN id SET DEFAULT nextval('public.agent_audit_log_id_seq'::regclass);

ALTER TABLE ONLY public.ai_embedding ALTER COLUMN id SET DEFAULT nextval('public.ai_embedding_id_seq'::regclass);

ALTER TABLE ONLY public.fairness_report ALTER COLUMN id SET DEFAULT nextval('public.fairness_report_id_seq'::regclass);

ALTER TABLE ONLY public.psi_baseline ALTER COLUMN id SET DEFAULT nextval('public.psi_baseline_id_seq'::regclass);

ALTER TABLE ONLY public.psi_drift_result ALTER COLUMN id SET DEFAULT nextval('public.psi_drift_result_id_seq'::regclass);

ALTER TABLE ONLY public.shadow_run_result ALTER COLUMN id SET DEFAULT nextval('public.shadow_run_result_id_seq'::regclass);

ALTER TABLE ONLY public.agent_audit_log
    ADD CONSTRAINT agent_audit_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_embedding
    ADD CONSTRAINT ai_embedding_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.fairness_report
    ADD CONSTRAINT fairness_report_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.psi_baseline
    ADD CONSTRAINT psi_baseline_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.psi_drift_result
    ADD CONSTRAINT psi_drift_result_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.shadow_run_result
    ADD CONSTRAINT shadow_run_result_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ai_embedding
    ADD CONSTRAINT uq_ai_embedding UNIQUE (corpus, source_id, chunk_seq, embedding_model);

ALTER TABLE ONLY public.fairness_report
    ADD CONSTRAINT uq_fairness UNIQUE (report_month, group_key);

ALTER TABLE ONLY public.psi_baseline
    ADD CONSTRAINT uq_psi_baseline UNIQUE (feature_name, bucket_index, model_version);

ALTER TABLE ONLY public.psi_drift_result
    ADD CONSTRAINT uq_psi_week UNIQUE (feature_name, calc_week, model_version);

CREATE INDEX ai_embedding_corpus_active_idx ON public.ai_embedding USING btree (corpus) WHERE is_active;

CREATE INDEX ai_embedding_effective_idx ON public.ai_embedding USING btree (corpus, effective_date DESC) WHERE is_active;

CREATE INDEX ai_embedding_fts_gin_idx ON public.ai_embedding USING gin (fts_tokens);

CREATE INDEX ai_embedding_meta_gin_idx ON public.ai_embedding USING gin (metadata);

CREATE INDEX ai_embedding_vec_idx ON public.ai_embedding USING ivfflat (embedding public.vector_cosine_ops) WITH (lists='10');

CREATE INDEX idx_aal_created_at ON public.agent_audit_log USING btree (created_at DESC);

CREATE INDEX idx_aal_rev_id ON public.agent_audit_log USING btree (rev_id);

CREATE INDEX idx_fr_month ON public.fairness_report USING btree (report_month DESC);

CREATE INDEX idx_pdr_feature_week ON public.psi_drift_result USING btree (feature_name, calc_week DESC);

CREATE INDEX idx_srr_created_at ON public.shadow_run_result USING btree (created_at DESC);

CREATE INDEX idx_srr_diverged ON public.shadow_run_result USING btree (diverged) WHERE (diverged = true);

CREATE INDEX idx_srr_rag_backend ON public.shadow_run_result USING btree (rag_backend);

CREATE INDEX idx_srr_rag_enabled ON public.shadow_run_result USING btree (rag_enabled);

CREATE INDEX idx_srr_rev_id ON public.shadow_run_result USING btree (rev_id);

CREATE TRIGGER trg_aal_no_delete BEFORE DELETE ON public.agent_audit_log FOR EACH ROW EXECUTE FUNCTION public.fn_aal_block_mutate();

CREATE TRIGGER trg_aal_no_update BEFORE UPDATE ON public.agent_audit_log FOR EACH ROW EXECUTE FUNCTION public.fn_aal_block_mutate();

CREATE TRIGGER trg_ai_embedding_updated_at BEFORE UPDATE ON public.ai_embedding FOR EACH ROW EXECUTE FUNCTION public.ai_embedding_set_updated_at();


-- ============================================================================
-- SERVICE: doc-agent
-- DATABASE: doc_agent_db
-- ============================================================================
\connect doc_agent_db

CREATE TABLE public.identity_verify_cache (
    cache_key character varying(200) NOT NULL,
    result character varying(20) NOT NULL,
    verified_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone NOT NULL
);

CREATE TABLE public.loan_document_submission (
    submission_id uuid DEFAULT gen_random_uuid() NOT NULL,
    application_id character varying(50) NOT NULL,
    doc_code character varying(10) NOT NULL,
    raw_object_key character varying(500),
    masked_object_key character varying(500),
    forgery_score numeric(3,2),
    verify_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    reviewer_id character varying(50),
    human_review_status character varying(20) DEFAULT 'NOT_REQUIRED'::character varying,
    retention_until date,
    legal_hold boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.loan_forgery_signal (
    signal_id bigint NOT NULL,
    submission_id uuid NOT NULL,
    category character varying(20) NOT NULL,
    signal_type character varying(50) NOT NULL,
    score double precision NOT NULL,
    evidence jsonb,
    detected_at timestamp without time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE public.loan_forgery_signal_signal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.loan_forgery_signal_signal_id_seq OWNED BY public.loan_forgery_signal.signal_id;

CREATE TABLE public.loan_product_documents (
    product_id character varying(10) NOT NULL,
    product_name character varying(100) NOT NULL,
    req_doc_code character varying(10) NOT NULL,
    req_doc_name character varying(100) NOT NULL,
    is_essential boolean DEFAULT true NOT NULL,
    valid_days integer,
    accepted_formats character varying(100) DEFAULT 'pdf,jpg,png'::character varying NOT NULL,
    min_dpi integer DEFAULT 200 NOT NULL,
    issuer_type character varying(20) NOT NULL,
    auto_verify_enabled boolean DEFAULT true NOT NULL,
    retention_days integer
);

CREATE TABLE public.status_history (
    sthist_id bigint NOT NULL,
    target_domain_cd character varying(30) NOT NULL,
    target_table_cd character varying(50) NOT NULL,
    target_id bigint NOT NULL,
    before_status_cd character varying(50),
    after_status_cd character varying(50) NOT NULL,
    change_reason_cd character varying(50),
    change_remark character varying(500),
    changed_at timestamp with time zone NOT NULL,
    changed_by bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by bigint NOT NULL
);

CREATE SEQUENCE public.status_history_sthist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.status_history_sthist_id_seq OWNED BY public.status_history.sthist_id;

ALTER TABLE ONLY public.loan_forgery_signal ALTER COLUMN signal_id SET DEFAULT nextval('public.loan_forgery_signal_signal_id_seq'::regclass);

ALTER TABLE ONLY public.status_history ALTER COLUMN sthist_id SET DEFAULT nextval('public.status_history_sthist_id_seq'::regclass);

ALTER TABLE ONLY public.identity_verify_cache
    ADD CONSTRAINT identity_verify_cache_pkey PRIMARY KEY (cache_key);

ALTER TABLE ONLY public.loan_document_submission
    ADD CONSTRAINT loan_document_submission_pkey PRIMARY KEY (submission_id);

ALTER TABLE ONLY public.loan_forgery_signal
    ADD CONSTRAINT loan_forgery_signal_pkey PRIMARY KEY (signal_id);

ALTER TABLE ONLY public.loan_product_documents
    ADD CONSTRAINT loan_product_documents_pkey PRIMARY KEY (product_id, req_doc_code);

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT status_history_pkey PRIMARY KEY (sthist_id);

CREATE INDEX idx_doc_submission_application ON public.loan_document_submission USING btree (application_id);

CREATE INDEX idx_doc_submission_retention ON public.loan_document_submission USING btree (retention_until) WHERE (legal_hold = false);

CREATE INDEX idx_doc_submission_status ON public.loan_document_submission USING btree (verify_status);

CREATE INDEX idx_forgery_signal_category ON public.loan_forgery_signal USING btree (category, signal_type);

CREATE INDEX idx_forgery_signal_submission ON public.loan_forgery_signal USING btree (submission_id);

CREATE INDEX idx_identity_verify_expires ON public.identity_verify_cache USING btree (expires_at);

CREATE INDEX idx_lpd_product_essential ON public.loan_product_documents USING btree (product_id, is_essential);

CREATE INDEX idx_status_history_target ON public.status_history USING btree (target_domain_cd, target_table_cd, target_id, changed_at);

ALTER TABLE ONLY public.loan_forgery_signal
    ADD CONSTRAINT loan_forgery_signal_submission_id_fkey FOREIGN KEY (submission_id) REFERENCES public.loan_document_submission(submission_id);
