package pccafe.dto;

public class Payment {
    public static final String TIME = "TIME";
    public static final String FOOD = "FOOD";

    private String memberId;
    private String type;
    private int amount;

    public Payment(String memberId, String type, int amount) {
        this.memberId = memberId;
        this.type = type;
        this.amount = amount;
    }

    public String getMemberId() { return memberId; }
    public String getType() { return type; }
    public int getAmount() { return amount; }
}
