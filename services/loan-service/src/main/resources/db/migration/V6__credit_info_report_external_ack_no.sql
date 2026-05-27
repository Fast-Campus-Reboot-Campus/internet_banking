-- ACK callback 으로 들어오는 외부 기관 추적 번호.
-- nullable — dispatch SENT 단계에서는 아직 ACK 안 옴.

ALTER TABLE credit_info_report
    ADD COLUMN external_ack_no VARCHAR(100);
