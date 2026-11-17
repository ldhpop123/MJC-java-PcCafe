package pccafe.dto;

// 결제 상세 (영수증 한 줄). 음식이면 menuId, 시간이면 productId 중 하나만 채운다.
// 이름과 단가는 결제 당시 값을 그대로 저장한다
public class PaymentDetail {
    private int detailId;
    private int payId;
    private Integer menuId;
    private Integer productId;
    private String itemName;
    private int qty;
    private int unitPrice;

    public PaymentDetail(Integer menuId, Integer productId, String itemName, int qty, int unitPrice) {
        this.menuId = menuId;
        this.productId = productId;
        this.itemName = itemName;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    public static PaymentDetail ofMenu(Menu menu, int qty) {
        return new PaymentDetail(menu.getMenuId(), null, menu.getName(), qty, menu.getPrice());
    }

    public static PaymentDetail ofProduct(TimeProduct product) {
        return new PaymentDetail(null, product.getProductId(), product.getName(), 1, product.getPrice());
    }

    // 환불용: 수량을 음수로 (소계도 음수가 된다)
    public PaymentDetail toRefund() {
        return new PaymentDetail(menuId, productId, itemName, -qty, unitPrice);
    }

    public int getDetailId() { return detailId; }
    public void setDetailId(int detailId) { this.detailId = detailId; }

    public int getPayId() { return payId; }
    public void setPayId(int payId) { this.payId = payId; }

    public Integer getMenuId() { return menuId; }
    public Integer getProductId() { return productId; }
    public String getItemName() { return itemName; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public int getUnitPrice() { return unitPrice; }
    public int getSubtotal() { return unitPrice * qty; }

    // 장바구니 목록에 표시되는 문자열
    @Override
    public String toString() {
        return String.format("%s × %d = %,d원", itemName, qty, getSubtotal());
    }
}
