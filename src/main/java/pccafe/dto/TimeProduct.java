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

    // "30분", "1시간", "1시간 30분"
    public String getName() {
        if (minutes < 60) {
            return minutes + "분";
        }
        return minutes % 60 == 0 ? (minutes / 60) + "시간" : (minutes / 60) + "시간 " + (minutes % 60) + "분";
    }

    // 콤보박스 / 선택창에 표시되는 문자열
    @Override
    public String toString() {
        return String.format("%s - %,d원", getName(), price);
    }
}
