package pccafe.ui.panel;

import pccafe.dao.OrderDAO;
import pccafe.dto.Order;
import pccafe.service.PaymentException;
import pccafe.service.PaymentService;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrderPanel extends BasePanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final String staffId;   // 로그인한 관리자. 환불 결제의 staff_id로 저장
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentService paymentService = new PaymentService();
    private final DefaultTableModel model =
            readOnlyModel("주문번호", "시간", "좌석", "회원", "품목", "결제 금액", "상태");
    private final JTable table = new JTable(model);

    private List<Order> orders = new ArrayList<>();

    public OrderPanel(String staffId) {
        this.staffId = staffId;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton done = new JButton("완료 처리");
        JButton cancel = new JButton("주문 취소");
        JButton reload = new JButton("새로고침");
        done.addActionListener(e -> completeOrder());
        cancel.addActionListener(e -> cancelOrder());
        reload.addActionListener(e -> refresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(done);
        buttons.add(cancel);
        buttons.add(reload);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        try {
            orders = orderDAO.findAll();
        } catch (SQLException e) {
            showError(e);
            return;
        }
        model.setRowCount(0);
        for (Order o : orders) {
            model.addRow(new Object[]{
                    o.getOrderId(),
                    o.getOrderedAt().format(TIME),
                    o.getSeatNo() + "번",
                    o.getMemberId(),
                    o.getItems() == null ? "(결제 정보 없음)" : o.getItems(),
                    String.format("%,d", o.getAmount()),
                    statusText(o.getStatus())
            });
        }
    }

    private static String statusText(String status) {
        if (Order.WAIT.equals(status)) {
            return "대기";
        }
        return Order.DONE.equals(status) ? "완료" : "취소";
    }

    private Order selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : orders.get(row);
    }

    private void completeOrder() {
        Order o = selected();
        if (o == null) {
            showInfo("주문을 선택하세요.");
            return;
        }
        try {
            if (!orderDAO.updateStatusIfWaiting(o.getOrderId(), Order.DONE)) {
                showInfo("대기 중인 주문만 완료 처리할 수 있습니다.");
            }
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }

    // 대기 중인 주문만 취소: 주문 상태 CANCEL + 환불 결제 + 재고 복구 + 포인트 되돌리기 (트랜잭션)
    private void cancelOrder() {
        Order o = selected();
        if (o == null) {
            showInfo("주문을 선택하세요.");
            return;
        }
        if (!Order.WAIT.equals(o.getStatus())) {
            showInfo("대기 중인 주문만 취소할 수 있습니다.");
            return;
        }
        if (!confirm(String.format("%d번 주문을 취소하고 %,d원을 환불할까요?", o.getOrderId(), o.getAmount()))) {
            return;
        }
        try {
            paymentService.cancelOrder(o.getOrderId(), staffId);
        } catch (PaymentException e) {
            showInfo(e.getMessage());
        } catch (SQLException e) {
            showError(e);
        }
        refresh();
    }
}
