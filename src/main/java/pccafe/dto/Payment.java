package pccafe.dto;

import java.time.LocalDateTime;

// 결제 한 번. amount(합계) = 카드 + 현금 + 포인트. 환불은 금액이 모두 음수인 결제
public class Payment {
    public static final String TIME = "TIME";
    public static final String FOOD = "FOOD";

    private int payId;
    private String memberId;
    private String staffId;   // 처리한 관리자. 손님이 직접 결제하면 null
    private String type;
    private int amount;
    private int cardAmount;
    private int cashAmount;
    private int pointAmount;
    private LocalDateTime paidAt;

    public Payment(String memberId, String type, int cardAmount, int cashAmount, int pointAmount) {
        this.memberId = memberId;
        this.type = type;
        this.cardAmount = cardAmount;
        this.cashAmount = cashAmount;
        this.pointAmount = pointAmount;
        this.amount = cardAmount + cashAmount + pointAmount;
    }

    // 이 결제를 되돌리는 환불 결제 (모든 금액을 음수로)
    public Payment toRefund(String staffId) {
        Payment refund = new Payment(memberId, type, -cardAmount, -cashAmount, -pointAmount);
        refund.staffId = staffId;
        return refund;
    }

    public int getPayId() { return payId; }
    public void setPayId(int payId) { this.payId = payId; }

    public String getMemberId() { return memberId; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getType() { return type; }
    public int getAmount() { return amount; }
    public int getCardAmount() { return cardAmount; }
    public int getCashAmount() { return cashAmount; }
    public int getPointAmount() { return pointAmount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
