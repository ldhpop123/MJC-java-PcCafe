package pccafe.dto;

import java.time.LocalDateTime;

// 주방에서 처리할 음식 주문. 장바구니 결제 한 번 = 주문 한 건.
// 무엇을 몇 개 샀는지는 payId로 payment_detail에서 찾는다
public class Order {
    public static final String WAIT = "WAIT";
    public static final String DONE = "DONE";
    public static final String CANCEL = "CANCEL";

    private int orderId;
    private int payId;          // 0 = 결제와 연결되지 않은 예전 주문
    private String memberId;
    private int seatNo;
    private String status;
    private LocalDateTime orderedAt;

    // 조회할 때 payment / payment_detail에서 JOIN으로 채움
    private String items;       // "라면 2개, 콜라 1개"
    private int amount;

    public Order(int payId, String memberId, int seatNo) {
        this.payId = payId;
        this.memberId = memberId;
        this.seatNo = seatNo;
        this.status = WAIT;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getPayId() { return payId; }
    public String getMemberId() { return memberId; }
    public int getSeatNo() { return seatNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOrderedAt() { return orderedAt; }
    public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }

    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}
