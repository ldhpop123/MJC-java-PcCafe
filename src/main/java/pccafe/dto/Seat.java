package pccafe.dto;

public class Seat {
    public static final String EMPTY = "EMPTY";
    public static final String USING = "USING";

    private int seatNo;
    private String status;
    private String memberId;

    public Seat(int seatNo, String status, String memberId) {
        this.seatNo = seatNo;
        this.status = status;
        this.memberId = memberId;
    }

    public int getSeatNo() { return seatNo; }
    public String getStatus() { return status; }
    public String getMemberId() { return memberId; }
}
