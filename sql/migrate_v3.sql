-- 2차 피드백: 비밀번호 찾기 질문·답변 컬럼을 추가한다. 데이터는 유지한다.
-- migrate_v2.sql을 적용한 DB에 한 번만 실행한다.
--
--   1) 백업:  mysqldump -u root pccafe > ~/pccafe_backup.sql
--   2) 실행:  mysql -u root pccafe < sql/migrate_v3.sql
--
-- 이미 적용한 DB에 다시 실행하면 "Duplicate column" 오류로 멈춘다.
-- 기존 회원과 관리자는 질문이 NULL이라 비밀번호 찾기를 쓸 수 없다 (관리자에게 문의).

ALTER TABLE member
    ADD COLUMN pw_question VARCHAR(50) NULL AFTER point,
    ADD COLUMN pw_answer   VARCHAR(50) NULL AFTER pw_question,
    ADD CONSTRAINT chk_member_pw_qa CHECK ((pw_question IS NULL) = (pw_answer IS NULL));
