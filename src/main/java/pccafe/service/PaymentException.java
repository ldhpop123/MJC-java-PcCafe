package pccafe.service;

// 재고 부족, 포인트 부족처럼 결제를 진행할 수 없는 경우. 메시지를 그대로 사용자에게 보여 준다
public class PaymentException extends Exception {
    public PaymentException(String message) {
        super(message);
    }
}
