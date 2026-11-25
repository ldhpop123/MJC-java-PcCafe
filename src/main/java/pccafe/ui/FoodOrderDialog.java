package pccafe.ui;

import pccafe.dto.Menu;
import pccafe.dto.PaymentDetail;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// 음식 주문 창. 위쪽 메뉴 표에서 고르고 [담기] (또는 더블클릭) → 아래쪽 주문 목록에 쌓인다
public class FoodOrderDialog extends JDialog {
    private final List<Menu> menus;
    private final List<PaymentDetail> cart = new ArrayList<>();

    private final DefaultTableModel menuModel = readOnlyModel("메뉴", "분류", "가격", "재고");
    private final JTable menuTable = new JTable(menuModel);
    private final DefaultTableModel cartModel = readOnlyModel("메뉴", "수량", "금액");
    private final JTable cartTable = new JTable(cartModel);
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private final JLabel totalLabel = new JLabel("합계: 0원");

    private boolean ordered;   // [주문하기]로 닫았으면 true

    public FoodOrderDialog(JFrame owner, List<Menu> menus) {
        super(owner, "음식 주문", true);
        this.menus = menus;

        for (Menu m : menus) {
            menuModel.addRow(new Object[]{m.getName(), m.getCategory(), String.format("%,d원", m.getPrice()),
                    m.getStock() > 0 ? m.getStock() + "개" : "품절"});
        }
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuTable.setRowHeight(24);
        menuTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addToCart(1);
                }
            }
        });
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setRowHeight(24);

        JButton add = new JButton("담기");
        add.addActionListener(e -> addToCart((Integer) qtySpinner.getValue()));
        JPanel pick = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pick.add(new JLabel("메뉴를 고르고 수량을 정한 뒤 [담기] (더블클릭하면 1개)"));
        pick.add(qtySpinner);
        pick.add(add);

        JPanel menuPanel = new JPanel(new BorderLayout(4, 4));
        menuPanel.setBorder(BorderFactory.createTitledBorder("전체 메뉴"));
        menuPanel.add(new JScrollPane(menuTable), BorderLayout.CENTER);
        menuPanel.add(pick, BorderLayout.SOUTH);

        JButton remove = new JButton("빼기");
        remove.addActionListener(e -> removeFromCart());
        JPanel cartBottom = new JPanel(new BorderLayout());
        cartBottom.add(remove, BorderLayout.WEST);
        cartBottom.add(totalLabel, BorderLayout.EAST);

        JPanel cartPanel = new JPanel(new BorderLayout(4, 4));
        cartPanel.setBorder(BorderFactory.createTitledBorder("주문 목록"));
        cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);
        cartPanel.add(cartBottom, BorderLayout.SOUTH);

        // 메뉴 표가 남는 높이를 모두 쓰고, 주문 목록은 아래에 고정 높이로 둔다
        cartPanel.setPreferredSize(new Dimension(0, 190));
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(menuPanel, BorderLayout.CENTER);
        center.add(cartPanel, BorderLayout.SOUTH);

        JButton order = new JButton("주문하기");
        JButton cancel = new JButton("취소");
        order.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(order);
        buttons.add(cancel);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(center, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        add(panel);

        setSize(new Dimension(560, 640));
        setLocationRelativeTo(owner);
    }

    // 창을 띄우고, [주문하기]를 누르면 담은 목록을, 취소하면 null을 돌려준다
    public List<PaymentDetail> showDialog() {
        setVisible(true);   // 모달이라 창이 닫힐 때까지 여기서 기다린다
        return ordered ? cart : null;
    }

    private void addToCart(int qty) {
        int row = menuTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "메뉴 표에서 메뉴를 고르세요.");
            return;
        }
        Menu menu = menus.get(row);
        PaymentDetail item = findInCart(menu);
        int already = item == null ? 0 : item.getQty();
        // 표에 보이는 재고보다 많이 담지 못하게 한다 (실제 재고는 결제할 때 DB에서 다시 확인)
        if (already + qty > menu.getStock()) {
            JOptionPane.showMessageDialog(this, menu.getStock() == 0 ? menu.getName() + "은(는) 품절입니다."
                    : String.format("%s 재고는 %d개입니다. (이미 %d개 담음)", menu.getName(), menu.getStock(), already));
            return;
        }
        if (item == null) {
            cart.add(PaymentDetail.ofMenu(menu, qty));
        } else {
            item.setQty(already + qty);   // 이미 담은 메뉴면 수량만 늘린다
        }
        redrawCart();
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "주문 목록에서 뺄 메뉴를 고르세요.");
            return;
        }
        cart.remove(row);
        redrawCart();
    }

    private PaymentDetail findInCart(Menu menu) {
        for (PaymentDetail item : cart) {
            if (item.getMenuId() == menu.getMenuId()) {
                return item;
            }
        }
        return null;
    }

    private void redrawCart() {
        cartModel.setRowCount(0);
        int total = 0;
        for (PaymentDetail item : cart) {
            cartModel.addRow(new Object[]{item.getItemName(), item.getQty() + "개",
                    String.format("%,d원", item.getSubtotal())});
            total += item.getSubtotal();
        }
        totalLabel.setText(String.format("합계: %,d원", total));
    }

    private void submit() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "메뉴를 담아 주세요.");
            return;
        }
        ordered = true;
        dispose();
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
