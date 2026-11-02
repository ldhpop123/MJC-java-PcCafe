# PC방 관리 프로그램 기획서

Java Swing + MySQL로 만드는 PC방 관리 프로그램입니다.
한 JVM 안에서 **관리자 창 1개**와 **게스트 창 N개**를 띄우고, 게스트 창은 관리자 화면의 버튼으로 계속 생성할 수 있습니다.

## 1. 개요

| 항목 | 내용 |
| --- | --- |
| 언어 / UI | Java, Swing (JFrame 상속 구조) |
| DB | MySQL, JDBC (MySQL Connector/J) |
| 실행 형태 | 한 JVM, 관리자 창 1 + 게스트 창 N |
| 핵심 기술 | 상속, 스레드, 이벤트 처리, DB CRUD |
| 채팅 | 객체 참조로 메시지 전달 (DB 저장 안 함) |

## 2. 시스템 구조

```
                 BaseFrame (abstract, extends JFrame)
                 ├─ 공통 레이아웃 / 폰트 / 창 크기
                 └─ initUI() 추상 메서드
                        ▲                    ▲
                        │ 상속               │ 상속
                  AdminFrame             GuestFrame  ×N (new 로 계속 생성)
                  - guests 목록 관리      - 로그인 / 회원가입
                  - 좌석판 표시           - 좌석 선택
                  - "게스트 추가" 버튼 ──▶ - TimerThread 1개씩 보유
                        │   ▲                    │
                        │   └── 채팅 / 시간 갱신 콜백 ┘
                        ▼
                    DAO 계층 ──▶ MySQL
```

- **상속**: `BaseFrame`을 `AdminFrame`, `GuestFrame`이 상속한다. (`LoginDialog` 등도 공통 부모를 두면 좋음)
- **게스트 창 생성**: 관리자 화면의 "게스트 추가" 버튼을 누를 때마다 `new GuestFrame(this)` 실행 → 인스턴스 다중 생성
- **창 닫기 설정**
  - `AdminFrame`: `EXIT_ON_CLOSE` (프로그램 전체 종료)
  - `GuestFrame`: `DISPOSE_ON_CLOSE` (자기 창만 닫힘)
- 관리자는 `List<GuestFrame>`으로 모든 게스트 창을 알고, 게스트는 생성자로 받은 `AdminFrame` 참조를 가진다.

## 3. 기능 명세

### 3-1. 게스트 창

| 기능 | 설명 | 우선순위 |
| --- | --- | --- |
| 회원가입 | 아이디 중복 체크 후 DB 저장 | 필수 |
| 로그인 | DB 조회, 같은 계정 중복 로그인 차단 | 필수 |
| 좌석 선택 | 빈 좌석만 선택 가능 | 필수 |
| 남은 시간 표시 | 1초마다 감소, 화면 갱신 | 필수 |
| 시간 추가 구매 | 요금표(1시간 1,000원 등)에서 선택 → 시간 추가 + 결제 내역 저장 | 필수 |
| 사용 종료 / 창 닫기 | 남은 시간 DB 저장, 좌석 비우기, 스레드 종료 | 필수 |
| 관리자와 채팅 | 메시지 송수신 | 중요 |
| 음식 주문 | 메뉴 목록에서 주문 → 관리자에게 표시 | 중요 |
| 5분 남음 경고 | 팝업 알림 | 선택 |
| 시간 0 → 자동 종료 | 자동 로그아웃 | 필수 |

### 3-2. 관리자 창

| 기능 | 설명 | 우선순위 |
| --- | --- | --- |
| 게스트 추가 버튼 | 새 `GuestFrame` 인스턴스 생성 | 필수 |
| 좌석판 | 좌석 그리드, 빈자리/사용중 색 구분, 사용자·남은 시간 실시간 표시 | 필수 |
| 손님 시간 추가 / 차감 | 선택한 좌석의 남은 시간 변경 | 필수 |
| 강제 종료 | 선택한 좌석 사용 종료 (시간 저장 후 로그아웃) | 필수 |
| 회원 관리 | 회원 조회 / 검색 / 수정 / 삭제 (CRUD) | 필수 |
| 메뉴 관리 | 메뉴 추가 / 수정 / 삭제 (CRUD) | 중요 |
| 주문 확인 | 주문 목록, 상태 변경 (접수 → 완료) | 중요 |
| 게스트와 채팅 | 좌석별 채팅 | 중요 |
| 매출 확인 | 오늘 시간 판매 / 음식 매출 합계 | 선택 |
| 자리 이동 | 사용 중인 손님 좌석 변경 | 선택 |

## 4. 스레드 설계

- 게스트 1명 = `TimerThread` 1개. 로그인하고 좌석을 선택하면 시작한다.
- 1초마다 `remainSeconds--` → 게스트 화면과 관리자 좌석판 갱신
- Swing 화면 갱신은 반드시 `SwingUtilities.invokeLater(...)` 안에서 처리
- 관리자의 시간 추가/차감과 타이머 감소가 동시에 일어나므로 시간 변경 메서드는 `synchronized`
- **DB 저장 시점**
  1. 게스트 창 닫기 (`windowClosed`)
  2. 사용 종료 / 강제 종료
  3. 1분마다 자동 저장 (비정상 종료 대비)
- 종료할 때는 `interrupt()`로 스레드를 멈추고, `InterruptedException`에서 루프를 빠져나온다.
- 남은 시간이 0이 되면 자동 종료, 300초(5분)가 되면 경고 팝업

```java
public class TimerThread extends Thread {
    private final GuestFrame guest;

    @Override
    public void run() {
        int tick = 0;
        try {
            while (guest.getRemainSeconds() > 0) {
                Thread.sleep(1000);
                guest.decreaseOneSecond();          // synchronized
                if (++tick % 60 == 0) guest.saveRemainTime();
            }
            SwingUtilities.invokeLater(guest::forceLogout);
        } catch (InterruptedException e) {
            // 정상 종료
        }
    }
}
```

## 5. 채팅 설계

- 한 JVM이라 소켓 없이 **객체 참조로 메서드를 직접 호출**한다.
- 게스트 → 관리자: `admin.receiveMessage(seatNo, msg)`
- 관리자 → 게스트: `guestFrame.receiveMessage(msg)` (`guests` 목록에서 좌석 번호로 찾음)
- 관리자 창에는 좌석별 채팅 탭(또는 좌석 선택 후 채팅창)을 둔다.
- 채팅 내용은 DB에 저장하지 않는다.

## 6. DB 설계

```sql
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
```

### CRUD 매핑

| 테이블 | Create | Read | Update | Delete |
| --- | --- | --- | --- | --- |
| member | 회원가입 | 로그인, 회원 목록 | 정보 수정, 남은 시간 저장 | 회원 삭제 |
| seat | 초기 데이터 | 좌석판 | 사용 시작 / 종료, 자리 이동 | - |
| menu | 메뉴 추가 | 메뉴 목록 | 메뉴 수정, 재고 | 메뉴 삭제 |
| orders | 음식 주문 | 주문 목록 | 상태 변경 | 주문 취소 |
| payment | 시간 구매, 주문 결제 | 매출 확인 | - | - |
| time_product | 초기 데이터 | 요금표 | - | - |

관리자 계정은 `member.is_admin = TRUE`로 구분한다.

## 7. 패키지 / 클래스 구조

```
src/
├─ Main.java                 // AdminFrame 실행
├─ db/
│  └─ DBUtil.java            // Connection 관리
├─ dto/
│  ├─ Member.java
│  ├─ Seat.java
│  ├─ Menu.java
│  ├─ Order.java
│  └─ Payment.java
├─ dao/
│  ├─ MemberDAO.java
│  ├─ SeatDAO.java
│  ├─ MenuDAO.java
│  ├─ OrderDAO.java
│  └─ PaymentDAO.java
├─ ui/
│  ├─ BaseFrame.java         // abstract, extends JFrame
│  ├─ AdminFrame.java        // extends BaseFrame
│  ├─ GuestFrame.java        // extends BaseFrame
│  ├─ SignUpDialog.java
│  └─ panel/                 // 좌석판, 채팅, 메뉴관리 등 패널
└─ thread/
   └─ TimerThread.java
sql/
└─ schema.sql                // 테이블 생성 + 초기 데이터
```

## 8. 개발 순서

1. DB 스키마(`schema.sql`) 작성, `DBUtil` 연결 확인
2. `BaseFrame` / `AdminFrame` / `GuestFrame` 뼈대, 게스트 추가 버튼
3. 회원가입 / 로그인 (`MemberDAO`)
4. 좌석 선택 + `TimerThread`, 창 닫을 때 시간 저장
5. 관리자 좌석판, 시간 추가 / 차감, 강제 종료
6. 시간 추가 구매 (`time_product`, `payment`)
7. 채팅
8. 메뉴 관리 / 음식 주문
9. 회원 관리 CRUD, 매출 확인, 경고 팝업 등 마무리
