---
tags: [pc방, 발표]
담당: 담당 2
---
# 2. DB와 DAO

**한 줄 요약**: 회원, 좌석, 메뉴, 주문, 결제 정보를 MySQL 테이블 6개에 저장한다. 자바 코드에서 SQL은 DAO 클래스에만 모아 두었다.

**맡은 파일**
- `sql/schema.sql`
- `src/main/resources/db.properties.example`
- `src/main/java/pccafe/db/DBUtil.java`
- `src/main/java/pccafe/dto/*` (Member, Seat, Menu, Order, Payment, TimeProduct)
- `src/main/java/pccafe/dao/*` (MemberDAO, SeatDAO, MenuDAO, OrderDAO, PaymentDAO, TimeProductDAO)
- `src/main/java/pccafe/ui/panel/*` (관리자 탭: MenuPanel, OrderPanel, MemberPanel, SalesPanel)

---

## 2-1. 테이블 6개

| 테이블 | 무엇을 저장? | 주요 칸 |
| --- | --- | --- |
| `member` | 회원 (관리자 포함) | 아이디, 비밀번호, 이름, **남은 시간(초)**, 관리자 여부 |
| `seat` | 좌석 12개 | 좌석 번호, 상태(EMPTY / USING), 앉은 회원 |
| `time_product` | 요금표 | 30분 500원, 1시간 1,000원 … |
| `menu` | 음식 메뉴 | 이름, 가격, 분류, 재고 |
| `orders` | 음식 주문 | 누가, 몇 번 좌석에서, 무엇을, 몇 개, 상태 |
| `payment` | 결제 내역 | 누가, 시간/음식, 금액, 날짜 |

```
member ──┬── seat      (어떤 회원이 앉아 있나)
         ├── orders ── menu   (무엇을 주문했나)
         └── payment   (얼마를 냈나)
```

정식 E-R 다이어그램과 관계 설명은 [[DB 설계#E-R 다이어그램]]에 있다. 발표 화면에는 그 다이어그램을 띄운다.

- **남은 시간은 초 단위 정수**(`remain_seconds`)로 저장한다. 1시간 = 3600. 계산이 쉽고, 화면에 보여줄 때만 `01:00:00`으로 바꾼다 (`TimeFormat.format`).
- 관리자 테이블을 따로 안 만들고 `member.is_admin`으로 구분했다.
- `schema.sql`을 실행하면 테이블을 지우고 다시 만든 뒤 테스트 데이터까지 넣는다. 시연 전 초기화용.

## 2-2. 3층 구조: 화면 → DAO → DB

```
 화면 (GuestFrame, AdminFrame)
     │  memberDAO.login("test", "1234")
     ▼
 DAO (MemberDAO)          ← SQL은 여기에만
     │  SELECT * FROM member WHERE id=? AND password=?
     ▼
 MySQL (pccafe DB)
     │  결과 한 줄
     ▼
 DTO (Member 객체)        ← 결과를 담아서 화면으로 돌려줌
```

| 이름 | 뜻 | 비유 | 예시 |
| --- | --- | --- | --- |
| **DTO** | Data Transfer Object, 데이터 담는 그릇 | 택배 상자 | `Member`: id, name, remainSeconds … getter/setter만 있음 |
| **DAO** | Data Access Object, DB 담당 직원 | 창고 직원 | `MemberDAO.login()`, `insert()`, `delete()` |

**왜 나눴나?** 화면 코드에 SQL이 섞여 있으면 화면을 고치다 SQL을 망가뜨리기 쉽다. 나눠 두면 화면 담당은 `memberDAO.login(...)`만 부르면 되고, SQL이 어떻게 생겼는지 몰라도 된다.

## 2-3. DB 연결: DBUtil

```java
public class DBUtil {
    static {
        // 프로그램 시작 시 db.properties 파일에서 주소, 아이디, 비밀번호를 읽어둠
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

- 접속 정보를 코드에 직접 안 쓰고 `db.properties` 파일로 뺐다.
- 이유: GitHub 저장소가 public이라 **비밀번호가 올라가면 안 된다**. 그래서 `db.properties`는 `.gitignore`로 제외하고, 예시 파일(`db.properties.example`)만 올렸다.
- MySQL 드라이버(`mysql-connector-j`)는 Maven(`pom.xml`)이 자동으로 받아준다.

## 2-4. DAO 코드 읽는 법 (MemberDAO.login)

```java
public Member login(String id, String password) throws SQLException {
    String sql = "SELECT * FROM member WHERE id = ? AND password = ?";
    try (Connection conn = DBUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, id);          // 첫 번째 ?에 id
        ps.setString(2, password);    // 두 번째 ?에 password
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? map(rs) : null;   // 찾으면 Member, 없으면 null
        }
    }
}
```

| 부분 | 설명 |
| --- | --- |
| `?` + `PreparedStatement` | 값을 SQL 문자열에 직접 붙이지 않고 따로 넣는다. **SQL 인젝션 방지**. |
| `try ( ... )` | try-with-resources. 끝나면 연결을 **자동으로 닫아준다**. 안 닫으면 연결이 쌓여서 DB가 멈춘다. |
| `executeQuery()` | SELECT용, 결과(ResultSet)를 돌려줌 |
| `executeUpdate()` | INSERT / UPDATE / DELETE용, 바뀐 줄 수를 돌려줌 |
| `map(rs)` | DB 결과 한 줄 → `Member` 객체로 옮겨 담기 |

> [!example] SQL 인젝션이 뭐예요?
> 문자열을 그냥 붙이면 `"... WHERE id='" + id + "'"`에 아이디로 `' OR '1'='1`을 넣었을 때 비밀번호 없이 로그인된다. `?`를 쓰면 입력값은 그냥 "글자"로만 취급돼서 이런 공격이 안 된다.

## 2-5. CRUD 정리

| 테이블 | Create (추가) | Read (조회) | Update (수정) | Delete (삭제) |
| --- | --- | --- | --- | --- |
| member | 회원가입 | 로그인, 회원 목록 | 정보 수정, **남은 시간 저장** | 회원 삭제 |
| seat | (초기 데이터) | 좌석판, 빈 좌석 목록 | 사용 시작 / 종료 | - |
| menu | 메뉴 추가 | 메뉴 목록 | 메뉴 수정 | 메뉴 삭제 |
| orders | 음식 주문 | 주문 목록 | 상태 변경 | 주문 취소 |
| payment | 시간 구매 결제 | 오늘 매출 합계 | - | - |
| time_product | (초기 데이터) | 요금표 | - | - |

## 2-6. 좌석 두 명이 동시에 앉으면? (SeatDAO.startUse)

```sql
UPDATE seat SET status = 'USING', member_id = ?
WHERE seat_no = ? AND status = 'EMPTY'
```

- `AND status = 'EMPTY'` 조건 덕분에 **빈 좌석일 때만** 바뀐다.
- 게스트 두 명이 같은 좌석을 고르면, 먼저 처리된 쪽만 1줄이 바뀌고 나머지는 0줄이 바뀐다.
- `executeUpdate() == 1`이면 성공, 아니면 "이미 사용 중인 좌석입니다"를 띄운다.

또 프로그램을 켤 때 `SeatDAO.resetAll()`로 모든 좌석을 비운다. 지난번에 프로그램이 갑자기 꺼져서 "사용 중"으로 남은 좌석을 정리하기 위해서다.

재고도 같은 방식이다 (`MenuDAO.decreaseStock`).

```sql
UPDATE menu SET stock = stock - ? WHERE menu_id = ? AND stock >= ?
```

재고가 주문 수량 이상일 때만 줄어든다. 재고가 모자라면 0줄이 바뀌고 "재고가 부족합니다."가 뜬다. 재고가 마이너스가 될 일이 없다.

## 2-7. 관리자 탭 4개 = CRUD 화면

모든 탭은 `BasePanel`을 상속하고, 탭을 누를 때마다 `refresh()`로 DB를 다시 읽는다.

| 탭 | 화면 | C | R | U | D |
| --- | --- | --- | --- | --- | --- |
| 메뉴 관리 | 표 + 입력칸 (이름, 가격, 분류, 재고) | 추가 | 목록 | 수정 | 삭제 |
| 주문 | 표 (대기 주문이 위로) | (게스트가 주문) | 목록 | 완료 처리 | 주문 취소 |
| 회원 관리 | 검색창 + 표 + 입력칸 | (게스트 회원가입) | 검색 | 수정 | 삭제 |
| 매출 | 오늘 시간 / 음식 / 합계 | - | 합계 | - | - |

**표 쓰는 법 (공통)**
1. `refresh()`: DAO로 목록을 가져와서 `List`에 저장하고, 표(`JTable`)에 한 줄씩 넣는다.
2. 표에서 한 줄을 클릭하면 그 줄의 값이 아래 입력칸에 채워진다.
3. 입력칸을 고치고 "수정"을 누르면 DAO의 `update`가 실행되고, 다시 `refresh()`.

표의 셀은 직접 못 고치게 막았다(`readOnlyModel`). 수정은 반드시 입력칸으로 해서, 숫자 확인 같은 검사를 한 곳에서 한다.

**주문 탭: JOIN**
`orders` 테이블에는 메뉴 번호만 있다. 화면에 메뉴 이름과 금액을 보여주려고 `menu` 테이블과 JOIN했다.

```sql
SELECT o.*, m.name AS menu_name, m.price
FROM orders o LEFT JOIN menu m ON o.menu_id = m.menu_id
ORDER BY o.status = 'DONE', o.ordered_at DESC
```

- `LEFT JOIN`: 메뉴가 삭제돼도 주문 기록은 남는다 ("(삭제된 메뉴)"로 표시).
- `ORDER BY o.status = 'DONE'`: 대기(false = 0)가 완료(true = 1)보다 먼저 나온다.

**주문 취소** 하면 3가지를 한다: 주문 삭제 → 재고 되돌리기 → `payment`에 **음수 금액(환불)** 기록. 그래서 매출 합계가 저절로 맞는다.

**회원 관리에서 막아둔 것**
- 관리자 계정은 삭제 불가
- 사용 중인 회원은 수정 / 삭제 불가. 게스트 창이 남은 시간을 들고 있어서, 여기서 고쳐도 종료할 때 게스트 창 값으로 덮어써지기 때문이다. 사용 중인 회원의 시간은 좌석 탭에서 ±로 조정한다.
- 비밀번호 칸을 비워두면 기존 비밀번호를 유지한다.

---

## 시연 담당
- 주문 탭: 게스트가 주문한 것이 "대기"로 보임 → 완료 처리
- 메뉴 관리 탭: 재고가 줄어든 것 확인, 새 메뉴 추가 → 게스트 주문창에 바로 나오는 것
- 매출 탭: 시간 / 음식 매출 합계
- 회원 관리 탭: 검색, 종료 후 남은 시간이 저장된 것
- (보조) TablePlus로 `seat` 테이블에서 로그인하면 `USING`, 종료하면 `EMPTY`로 바뀌는 것

## 예상 질문
- **Q. 비밀번호를 그대로 저장하나요?** → 지금은 그렇다. 해시(SHA-256 또는 BCrypt)로 바꿀 계획이다.
- **Q. 주문, 재고 차감, 결제가 따로 실행되는데 중간에 오류 나면요?** → 지금은 트랜잭션으로 묶지 않아서, 중간에 실패하면 일부만 저장될 수 있다. `conn.setAutoCommit(false)` → `commit()` / `rollback()`으로 묶는 게 개선 방향이다.
- **Q. 채팅은 왜 DB에 안 넣었나요?** → 채팅은 그때그때 주고받는 메시지라 저장할 필요가 없다고 판단했다.

이전: [[1 구조와 상속]] · 다음: [[3 게스트 화면]]
