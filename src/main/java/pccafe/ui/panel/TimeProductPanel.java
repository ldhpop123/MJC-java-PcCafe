package pccafe.ui.panel;

import pccafe.dao.TimeProductDAO;
import pccafe.dto.TimeProduct;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// 시간 요금제 관리 탭. 게스트의 "시간 구매" 목록이 여기서 정한 요금제로 나온다
public class TimeProductPanel extends BasePanel {
    private final TimeProductDAO timeProductDAO = new TimeProductDAO();
    private final DefaultTableModel model = readOnlyModel("번호", "요금제", "시간(분)", "가격");
    private final JTable table = new JTable(model);

    private final JTextField minutesField = new JTextField(5);
    private final JTextField priceField = new JTextField(7);

    private List<TimeProduct> products = new ArrayList<>();

    public TimeProductPanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillForm();
            }
        });

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("시간(분)"));
        form.add(minutesField);
        form.add(new JLabel("가격(원)"));
        form.add(priceField);

        JButton add = new JButton("신규 요금제 등록");
        JButton update = new JButton("정보 수정");
        JButton delete = new JButton("삭제");
        JButton clear = new JButton("입력칸 비우기");
        add.addActionListener(e -> addProduct());
        update.addActionListener(e -> updateProduct());
        delete.addActionListener(e -> deleteProduct());
        clear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        try {
            products = timeProductDAO.findAll();
        } catch (SQLException e) {
            showError(e);
            return;
        }
        model.setRowCount(0);
        for (TimeProduct p : products) {
            model.addRow(new Object[]{p.getProductId(), p.toString(), p.getMinutes(),
                    String.format("%,d", p.getPrice())});
        }
        clearForm();
    }

    private TimeProduct selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : products.get(row);
    }

    private void fillForm() {
        TimeProduct p = selected();
        if (p == null) {
            return;
        }
        minutesField.setText(String.valueOf(p.getMinutes()));
        priceField.setText(String.valueOf(p.getPrice()));
    }

    private void clearForm() {
        table.clearSelection();
        minutesField.setText("");
        priceField.setText("");
    }

    // 입력칸 → TimeProduct. 잘못 입력했으면 null
    private TimeProduct readForm(int productId) {
        Integer minutes = readInt(minutesField, "시간");
        if (minutes == null) {
            return null;
        }
        if (minutes == 0) {
            showInfo("시간은 1분 이상이어야 합니다.");
            return null;
        }
        Integer price = readInt(priceField, "가격");
        if (price == null) {
            return null;
        }
        return new TimeProduct(productId, minutes, price);
    }

    private void addProduct() {
        TimeProduct p = readForm(0);
        if (p == null) {
            return;
        }
        try {
            timeProductDAO.insert(p);
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void updateProduct() {
        TimeProduct target = selected();
        if (target == null) {
            showInfo("수정할 요금제를 선택하세요.");
            return;
        }
        TimeProduct p = readForm(target.getProductId());
        if (p == null) {
            return;
        }
        try {
            timeProductDAO.update(p);
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteProduct() {
        TimeProduct target = selected();
        if (target == null) {
            showInfo("삭제할 요금제를 선택하세요.");
            return;
        }
        if (!confirm(target + " 요금제를 삭제할까요?")) {
            return;
        }
        try {
            timeProductDAO.delete(target.getProductId());
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }
}
