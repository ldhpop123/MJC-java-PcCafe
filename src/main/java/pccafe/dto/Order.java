package pccafe.dto;

import java.time.LocalDateTime;

public class Order {
    public static final String WAIT = "WAIT";
    public static final String DONE = "DONE";

    private int orderId;
    private String memberId;
    private int seatNo;
    private int menuId;
    private int qty;
    private String status;
    private LocalDateTime orderedAt;

    // 조회할 때 menu 테이블에서 JOIN으로 채움 (메뉴가 삭제됐으면 null / 0)
    private String menuName;
    private int price;

    public Order() {
    }

    public Order(String memberId, int seatNo, int menuId, int qty) {
        this.memberId = memberId;
        this.seatNo = seatNo;
        this.menuId = menuId;
        this.qty = qty;
        this.status = WAIT;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public int getSeatNo() { return seatNo; }
    public void setSeatNo(int seatNo) { this.seatNo = seatNo; }

    public int getMenuId() { return menuId; }
    public void setMenuId(int menuId) { this.menuId = menuId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOrderedAt() { return orderedAt; }
    public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }

    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getTotal() { return price * qty; }
}
