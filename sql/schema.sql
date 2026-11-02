CREATE DATABASE IF NOT EXISTS pccafe DEFAULT CHARACTER SET utf8mb4;
USE pccafe;

DROP TABLE IF EXISTS payment, orders, menu, time_product, seat, member;

CREATE TABLE member (
    id             VARCHAR(20) PRIMARY KEY,
    password       VARCHAR(64) NOT NULL,
    name           VARCHAR(20) NOT NULL,
    phone          VARCHAR(20),
    remain_seconds INT NOT NULL DEFAULT 0,
    is_admin       BOOLEAN NOT NULL DEFAULT FALSE,
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

CREATE TABLE orders (
    order_id   INT AUTO_INCREMENT PRIMARY KEY,
    member_id  VARCHAR(20),
    seat_no    INT,
    menu_id    INT,
    qty        INT NOT NULL DEFAULT 1,
    status     VARCHAR(10) NOT NULL DEFAULT 'WAIT',   -- WAIT / DONE
    ordered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE SET NULL,
    FOREIGN KEY (menu_id)   REFERENCES menu(menu_id) ON DELETE SET NULL
);

CREATE TABLE payment (
    pay_id    INT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(20),
    type      VARCHAR(10) NOT NULL,                   -- TIME / FOOD
    amount    INT NOT NULL,
    paid_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
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
