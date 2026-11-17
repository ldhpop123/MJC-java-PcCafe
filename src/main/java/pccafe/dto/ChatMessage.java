package pccafe.dto;

import java.time.LocalDateTime;

public class ChatMessage {
    public static final String ADMIN = "ADMIN";
    public static final String GUEST = "GUEST";

    private int msgId;
    private String memberId;   // 대화 상대 손님 (관리자가 보낸 메시지도 받는 손님 아이디)
    private int seatNo;
    private String sender;     // ADMIN / GUEST
    private String content;
    private LocalDateTime sentAt;

    public ChatMessage(String memberId, int seatNo, String sender, String content) {
        this.memberId = memberId;
        this.seatNo = seatNo;
        this.sender = sender;
        this.content = content;
    }

    public int getMsgId() { return msgId; }
    public void setMsgId(int msgId) { this.msgId = msgId; }

    public String getMemberId() { return memberId; }
    public int getSeatNo() { return seatNo; }
    public String getSender() { return sender; }
    public String getContent() { return content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
