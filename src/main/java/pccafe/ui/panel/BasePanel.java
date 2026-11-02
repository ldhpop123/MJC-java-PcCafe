package pccafe.ui.panel;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

// 관리자 창의 탭(메뉴, 주문, 회원, 매출)이 공통으로 상속하는 부모
public abstract class BasePanel extends JPanel {

    protected BasePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    // 탭이 선택될 때마다 AdminFrame이 호출해서 DB에서 다시 읽어온다
    public abstract void refresh();

    // 셀을 직접 고칠 수 없는 표 (수정은 아래 입력칸으로)
    protected static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // 숫자 입력칸을 읽는다. 숫자가 아니면 안내 후 null
    protected Integer readInt(JTextField field, String label) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value < 0) {
                showInfo(label + "은(는) 0 이상이어야 합니다.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            showInfo(label + "은(는) 숫자로 입력하세요.");
            return null;
        }
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "확인", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    protected void showError(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
    }
}
