CREATE DATABASE IF NOT EXISTS pccafe DEFAULT CHARACTER SET utf8mb4;
USE pccafe;

-- 다른 테이블이 참조하는 테이블은 나중에 지운다
DROP TABLE IF EXISTS chat_message, orders, payment_detail, payment, menu, time_product, seat, member;

CREATE TABLE member (
    id             VARCHAR(20) PRIMARY KEY,
    password       VARCHAR(64) NOT NULL,
    name           VARCHAR(20) NOT NULL,
    phone          VARCHAR(20),
    remain_seconds INT NOT NULL DEFAULT 0,
    is_admin       BOOLEAN NOT NULL DEFAULT FALSE,
    point          INT NOT NULL DEFAULT 0,               -- 보유 포인트 (카드·현금 결제액의 5% 적립)
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seat (
    seat_no   INT PRIMARY KEY,
    status    VARCHAR(10) NOT NULL DEFAULT 'EMPTY',   -- EMPTY / USING
    member_id VARCHAR(20) NULL,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE SET NULL
);

CREATE TABLE time_product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    minutes    INT NOT NULL,
    price      INT NOT NULL
);

CREATE TABLE menu (
    menu_id  INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(30) NOT NULL,
    price    INT NOT NULL,
    category VARCHAR(20),
    stock    INT NOT NULL DEFAULT 0
);

-- 돈의 흐름. 결제 한 번에 한 행. 환불은 원래 결제를 음수로 복사한 행
CREATE TABLE payment (
    pay_id       INT AUTO_INCREMENT PRIMARY KEY,
    member_id    VARCHAR(20),
    staff_id     VARCHAR(20) NULL,                      -- 처리한 관리자 (손님이 직접 결제하면 NULL)
    type         VARCHAR(10) NOT NULL,                  -- TIME / FOOD
    amount       INT NOT NULL,                          -- 결제 합계 = card + cash + point
    card_amount  INT NOT NULL DEFAULT 0,
    cash_amount  INT NOT NULL DEFAULT 0,
    point_amount INT NOT NULL DEFAULT 0,
    paid_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE SET NULL,
    FOREIGN KEY (staff_id)  REFERENCES member(id) ON DELETE SET NULL
);

-- 영수증 품목. 음식이면 menu_id, 시간이면 product_id 중 하나만 채운다.
-- 이름과 단가는 결제 당시 값을 저장해서, 나중에 메뉴 가격이 바뀌어도 과거 결제는 그대로 남는다
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

-- 주방 처리 상태. 음식 주문(장바구니 결제) 한 번에 한 행. 품목은 pay_id로 payment_detail에서 찾는다
CREATE TABLE orders (
    order_id   INT AUTO_INCREMENT PRIMARY KEY,
    pay_id     INT NULL,
    member_id  VARCHAR(20),
    seat_no    INT,
    status     VARCHAR(10) NOT NULL DEFAULT 'WAIT',   -- WAIT / DONE / CANCEL
    ordered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pay_id)    REFERENCES payment(pay_id) ON DELETE SET NULL,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE SET NULL
);

-- 관리자 ↔ 손님 채팅. member_id는 대화 상대 손님 (관리자가 보낸 메시지도 받는 손님 아이디)
CREATE TABLE chat_message (
    msg_id    INT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(20),
    seat_no   INT,
    sender    VARCHAR(10) NOT NULL,                   -- ADMIN / GUEST
    content   VARCHAR(500) NOT NULL,
    sent_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE SET NULL
);

-- 초기 데이터
INSERT INTO member (id, password, name, is_admin) VALUES ('admin', 'admin', '관리자', TRUE);
INSERT INTO member (id, password, name, remain_seconds) VALUES ('test', '1234', '테스트', 3600);

INSERT INTO seat (seat_no) VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12);

INSERT INTO time_product (minutes, price) VALUES (30, 500), (60, 1000), (180, 3000), (300, 5000);

INSERT INTO menu (name, price, category, stock) VALUES
    ('라면', 3000, '식사', 50),
    ('볶음밥', 5000, '식사', 30),
    ('콜라', 1500, '음료', 100),
    ('아메리카노', 2000, '음료', 100),
    ('과자', 1500, '간식', 80);
