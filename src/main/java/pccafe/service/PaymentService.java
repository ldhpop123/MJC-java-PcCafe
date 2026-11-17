package pccafe.service;

import pccafe.dao.MemberDAO;
import pccafe.dao.MenuDAO;
import pccafe.dao.OrderDAO;
import pccafe.dao.PaymentDAO;
import pccafe.dao.PaymentDetailDAO;
import pccafe.db.DBUtil;
import pccafe.dto.Order;
import pccafe.dto.Payment;
import pccafe.dto.PaymentDetail;
import pccafe.dto.TimeProduct;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// 여러 테이블을 함께 바꾸는 결제 작업을 트랜잭션 하나로 묶는다.
// 중간에 하나라도 실패하면 rollback 해서, 재고만 줄고 결제는 안 남는 것 같은 반쪽 상태가 생기지 않는다
public class PaymentService {
    // 카드·현금으로 낸 금액의 5%를 포인트로 적립 (포인트로 낸 금액에는 적립하지 않음)
    public static final int POINT_RATE_PERCENT = 5;

    // 게스트 창마다 PaymentService를 따로 만들기 때문에, 모든 창이 같은 자물쇠를 쓰도록 static으로 둔다.
    // 결제 트랜잭션은 한 번에 하나만 실행되어, 마지막 남은 재고를 두 창이 동시에 주문해도 한 명만 성공한다
    private static final Object LOCK = new Object();

    private final MemberDAO memberDAO = new MemberDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final PaymentDetailDAO detailDAO = new PaymentDetailDAO();

    public static int earnedPoint(Payment p) {
        return (p.getCardAmount() + p.getCashAmount()) * POINT_RATE_PERCENT / 100;
    }

    // 음식 주문: 재고 차감 → 결제 → 결제 상세 → 주문 → 포인트. 성공하면 주문 번호
    public int orderFood(int seatNo, List<PaymentDetail> cart, Payment pay) throws SQLException, PaymentException {
        return inTransaction(conn -> {
            for (PaymentDetail item : cart) {
                if (!menuDAO.decreaseStock(conn, item.getMenuId(), item.getQty())) {
                    throw new PaymentException(item.getItemName() + " 재고가 부족합니다.");
                }
            }
            int payId = savePayment(conn, pay, cart);
            return orderDAO.insert(conn, new Order(payId, pay.getMemberId(), seatNo));
        });
    }

    // 시간 구매: 결제 → 결제 상세 → 포인트. 남은 시간은 게스트 창(타이머)이 가지고 있으므로 호출한 쪽에서 더한다
    public void buyTime(TimeProduct product, Payment pay) throws SQLException, PaymentException {
        inTransaction(conn -> savePayment(conn, pay, List.of(PaymentDetail.ofProduct(product))));
    }

    // 주문 취소: 주문 상태 CANCEL → 환불 결제(음수) → 환불 상세 → 재고 복구 → 포인트 되돌리기
    public void cancelOrder(int orderId, String staffId) throws SQLException, PaymentException {
        inTransaction(conn -> {
            Order order = orderDAO.findForUpdate(conn, orderId);
            if (order == null || !Order.WAIT.equals(order.getStatus())) {
                throw new PaymentException("대기 중인 주문만 취소할 수 있습니다.");
            }
            if (order.getPayId() == 0) {
                throw new PaymentException("결제 정보가 없는 예전 주문은 취소할 수 없습니다.");
            }
            orderDAO.updateStatusIfWaiting(conn, orderId, Order.CANCEL);

            Payment original = paymentDAO.findById(conn, order.getPayId());
            Payment refund = original.toRefund(staffId);
            int refundId = paymentDAO.insert(conn, refund);
            for (PaymentDetail item : detailDAO.findByPayId(conn, original.getPayId())) {
                detailDAO.insert(conn, refundId, item.toRefund());
                if (item.getMenuId() != null) {
                    menuDAO.increaseStock(conn, item.getMenuId(), item.getQty());
                }
            }
            // 쓴 포인트는 돌려주고, 이 결제로 적립된 포인트는 회수한다
            if (original.getMemberId() != null) {
                memberDAO.addPoint(conn, original.getMemberId(),
                        original.getPointAmount() - earnedPoint(original));
            }
            return null;
        });
    }

    // 결제 + 결제 상세 + 포인트 사용/적립을 저장하고 pay_id를 돌려준다
    private int savePayment(Connection conn, Payment pay, List<PaymentDetail> items)
            throws SQLException, PaymentException {
        int total = 0;
        for (PaymentDetail item : items) {
            total += item.getSubtotal();
        }
        if (pay.getAmount() != total) {
            throw new PaymentException(String.format("결제 금액(%,d원)이 합계(%,d원)와 다릅니다.", pay.getAmount(), total));
        }
        if (!memberDAO.usePoint(conn, pay.getMemberId(), pay.getPointAmount(), earnedPoint(pay))) {
            throw new PaymentException("포인트가 부족합니다.");
        }
        int payId = paymentDAO.insert(conn, pay);
        for (PaymentDetail item : items) {
            detailDAO.insert(conn, payId, item);
        }
        return payId;
    }

    @FunctionalInterface
    private interface Work<T> {
        T run(Connection conn) throws SQLException, PaymentException;
    }

    // setAutoCommit(false)로 시작해서, 모두 성공하면 commit, 예외가 나면 rollback
    private <T> T inTransaction(Work<T> work) throws SQLException, PaymentException {
        synchronized (LOCK) {
            return runInTransaction(work);
        }
    }

    private <T> T runInTransaction(Work<T> work) throws SQLException, PaymentException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | PaymentException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
