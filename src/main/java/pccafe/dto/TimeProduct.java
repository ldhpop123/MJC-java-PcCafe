package pccafe.dto;

public class TimeProduct {
    private int productId;
    private int minutes;
    private int price;

    public TimeProduct(int productId, int minutes, int price) {
        this.productId = productId;
        this.minutes = minutes;
        this.price = price;
    }

    public int getProductId() { return productId; }
    public int getMinutes() { return minutes; }
    public int getPrice() { return price; }

    // 콤보박스 / 선택창에 표시되는 문자열
    @Override
    public String toString() {
        String time = minutes >= 60 ? (minutes / 60) + "시간" : minutes + "분";
        return String.format("%s - %,d원", time, price);
    }
}
