-- 기존 pccafe DB를 새 구조(결제 상세, 결제 수단, 포인트, 채팅 저장)로 바꾸는 스크립트. 데이터는 유지한다.
-- schema.sql은 테이블을 지우고 다시 만들기 때문에, 이미 쓰던 DB에는 이 파일을 한 번만 실행한다.
--
--   1) 백업:  mysqldump -u root pccafe > ~/pccafe_backup.sql
--   2) 실행:  mysql -u root pccafe < sql/migrate_v2.sql
--
-- 이미 새 구조인 DB에 다시 실행하면 "Duplicate column" 오류로 멈춘다.

-- 1. 회원 포인트
ALTER TABLE member ADD COLUMN point INT NOT NULL DEFAULT 0 AFTER is_admin;

-- 2. 결제 수단과 처리한 관리자. 예전 결제는 수단을 기록하지 않았으므로 0으로 둔다 (수단 미기록)
ALTER TABLE payment
    ADD COLUMN staff_id     VARCHAR(20) NULL AFTER member_id,
    ADD COLUMN card_amount  INT NOT NULL DEFAULT 0 AFTER amount,
    ADD COLUMN cash_amount  INT NOT NULL DEFAULT 0 AFTER card_amount,
    ADD COLUMN point_amount INT NOT NULL DEFAULT 0 AFTER cash_amount,
    ADD FOREIGN KEY (staff_id) REFERENCES member(id) ON DELETE SET NULL;

-- 3. 결제 상세 (영수증 품목)
CREATE TABLE payment_detail (
    detail_id  INT AUTO_INCREMENT PRIMARY KEY,
    pay_id     INT NOT NULL,
    menu_id    INT NULL,
    product_id INT NULL,
    item_name  VARCHAR(30) NOT NULL,
    qty        INT NOT NULL,
    unit_price INT NOT NULL,
    subtotal   INT NOT NULL,
    FOREIGN KEY (pay_id)     REFERENCES payment(pay_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id)    REFERENCES menu(menu_id) ON DELETE SET NULL,
    FOREIGN KEY (product_id) REFERENCES time_product(product_id) ON DELETE SET NULL
);

-- 4. 주문 ↔ 결제 연결
ALTER TABLE orders
    ADD COLUMN pay_id INT NULL AFTER order_id,
    ADD FOREIGN KEY (pay_id) REFERENCES payment(pay_id) ON DELETE SET NULL;

-- 예전 프로그램은 주문을 저장한 직후 같은 회원의 음식 결제를 저장했다.
-- 같은 회원이 5초 안에 한 음식 결제를 그 주문의 결제로 연결한다
UPDATE orders o
JOIN payment p
  ON p.member_id = o.member_id
 AND p.type = 'FOOD'
 AND p.amount > 0
 AND ABS(TIMESTAMPDIFF(SECOND, o.ordered_at, p.paid_at)) <= 5
SET o.pay_id = p.pay_id
WHERE o.pay_id IS NULL;

-- 연결된 주문의 품목을 결제 상세로 옮긴다. 단가는 결제 금액 / 수량 (주문 당시 가격)
INSERT INTO payment_detail (pay_id, menu_id, item_name, qty, unit_price, subtotal)
SELECT o.pay_id, o.menu_id, COALESCE(m.name, '(삭제된 메뉴)'), o.qty, p.amount DIV o.qty, p.amount
FROM orders o
JOIN payment p ON p.pay_id = o.pay_id
LEFT JOIN menu m ON m.menu_id = o.menu_id;

-- 예전 시간 구매는 금액이 같은 요금제를 품목으로 기록한다 (같은 가격 요금제가 여럿이면 번호가 작은 것)
INSERT INTO payment_detail (pay_id, product_id, item_name, qty, unit_price, subtotal)
SELECT p.pay_id, t.product_id,
       IF(t.minutes >= 60, CONCAT(t.minutes DIV 60, '시간'), CONCAT(t.minutes, '분')),
       1, p.amount, p.amount
FROM payment p
JOIN (SELECT price, MIN(product_id) AS product_id FROM time_product GROUP BY price) one ON one.price = p.amount
JOIN time_product t ON t.product_id = one.product_id
WHERE p.type = 'TIME' AND p.amount > 0;

-- 품목을 옮겼으므로 orders의 메뉴/수량 컬럼은 지운다. 연결하지 못한 주문은 pay_id가 NULL로 남는다
ALTER TABLE orders DROP FOREIGN KEY orders_ibfk_2;   -- menu_id 외래 키 (schema.sql로 만든 DB에서의 이름)
ALTER TABLE orders DROP COLUMN menu_id, DROP COLUMN qty;

-- 5. 채팅 저장
CREATE TABLE chat_message (
    msg_id    INT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(20),
    seat_no   INT,
    sender    VARCHAR(10) NOT NULL,
    content   VARCHAR(500) NOT NULL,
    sent_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE SET NULL
);
