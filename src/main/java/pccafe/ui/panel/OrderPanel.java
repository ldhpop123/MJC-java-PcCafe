package pccafe.ui.panel;

import pccafe.dao.MenuDAO;
import pccafe.dao.OrderDAO;
import pccafe.dao.PaymentDAO;
import pccafe.dto.Order;
import pccafe.dto.Payment;

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

    private final OrderDAO orderDAO = new OrderDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final DefaultTableModel model =
            readOnlyModel("주문번호", "시간", "좌석", "회원", "메뉴", "수량", "금액", "상태");
    private final JTable table = new JTable(model);

    private List<Order> orders = new ArrayList<>();

    public OrderPanel() {
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
                    o.getMenuName() == null ? "(삭제된 메뉴)" : o.getMenuName(),
                    o.getQty(),
                    String.format("%,d", o.getTotal()),
                    Order.WAIT.equals(o.getStatus()) ? "대기" : "완료"
            });
        }
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
        if (Order.DONE.equals(o.getStatus())) {
            showInfo("이미 완료된 주문입니다.");
            return;
        }
        try {
            orderDAO.updateStatus(o.getOrderId(), Order.DONE);
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }

    // 대기 중인 주문만 취소: 주문 삭제 + 재고 되돌리기 + 환불(음수 결제) 기록
    private void cancelOrder() {
        Order o = selected();
        if (o == null) {
            showInfo("주문을 선택하세요.");
            return;
        }
        if (Order.DONE.equals(o.getStatus())) {
            showInfo("완료된 주문은 취소할 수 없습니다.");
            return;
        }
        if (!confirm(o.getOrderId() + "번 주문을 취소할까요?")) {
            return;
        }
        try {
            orderDAO.delete(o.getOrderId());
            if (o.getMenuId() > 0) {
                menuDAO.increaseStock(o.getMenuId(), o.getQty());
            }
            if (o.getTotal() > 0) {
                paymentDAO.insert(new Payment(o.getMemberId(), Payment.FOOD, -o.getTotal()));
            }
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }
}
