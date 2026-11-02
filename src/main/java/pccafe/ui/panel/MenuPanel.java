package pccafe.ui.panel;

import pccafe.dao.MenuDAO;
import pccafe.dto.Menu;

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

public class MenuPanel extends BasePanel {
    private final MenuDAO menuDAO = new MenuDAO();
    private final DefaultTableModel model = readOnlyModel("번호", "이름", "가격", "분류", "재고");
    private final JTable table = new JTable(model);

    private final JTextField nameField = new JTextField(10);
    private final JTextField priceField = new JTextField(6);
    private final JTextField categoryField = new JTextField(6);
    private final JTextField stockField = new JTextField(4);

    private List<Menu> menus = new ArrayList<>();

    public MenuPanel() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillForm();
            }
        });

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("이름"));
        form.add(nameField);
        form.add(new JLabel("가격"));
        form.add(priceField);
        form.add(new JLabel("분류"));
        form.add(categoryField);
        form.add(new JLabel("재고"));
        form.add(stockField);

        JButton add = new JButton("추가");
        JButton update = new JButton("수정");
        JButton delete = new JButton("삭제");
        JButton clear = new JButton("새로 입력");
        add.addActionListener(e -> addMenu());
        update.addActionListener(e -> updateMenu());
        delete.addActionListener(e -> deleteMenu());
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
            menus = menuDAO.findAll();
        } catch (SQLException e) {
            showError(e);
            return;
        }
        model.setRowCount(0);
        for (Menu m : menus) {
            model.addRow(new Object[]{m.getMenuId(), m.getName(), String.format("%,d", m.getPrice()),
                    m.getCategory(), m.getStock()});
        }
        clearForm();
    }

    private Menu selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : menus.get(row);
    }

    private void fillForm() {
        Menu m = selected();
        if (m == null) {
            return;
        }
        nameField.setText(m.getName());
        priceField.setText(String.valueOf(m.getPrice()));
        categoryField.setText(m.getCategory());
        stockField.setText(String.valueOf(m.getStock()));
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        priceField.setText("");
        categoryField.setText("");
        stockField.setText("");
    }

    // 입력칸 → Menu. 잘못 입력했으면 null
    private Menu readForm() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showInfo("이름을 입력하세요.");
            return null;
        }
        Integer price = readInt(priceField, "가격");
        if (price == null) {
            return null;
        }
        Integer stock = readInt(stockField, "재고");
        if (stock == null) {
            return null;
        }
        return new Menu(0, name, price, categoryField.getText().trim(), stock);
    }

    private void addMenu() {
        Menu m = readForm();
        if (m == null) {
            return;
        }
        try {
            menuDAO.insert(m);
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void updateMenu() {
        Menu target = selected();
        if (target == null) {
            showInfo("수정할 메뉴를 선택하세요.");
            return;
        }
        Menu m = readForm();
        if (m == null) {
            return;
        }
        m.setMenuId(target.getMenuId());
        try {
            menuDAO.update(m);
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteMenu() {
        Menu target = selected();
        if (target == null) {
            showInfo("삭제할 메뉴를 선택하세요.");
            return;
        }
        if (!confirm(target.getName() + " 메뉴를 삭제할까요?")) {
            return;
        }
        try {
            menuDAO.delete(target.getMenuId());
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }
}
