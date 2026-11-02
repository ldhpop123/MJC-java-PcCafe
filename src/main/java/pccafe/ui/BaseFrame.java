package pccafe.ui;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public abstract class BaseFrame extends JFrame {
    private boolean initialized;

    protected BaseFrame(String title, int width, int height) {
        super(title);
        setSize(width, height);
        setLocationRelativeTo(null);
    }

    // 생성자 안에서 initUI()를 부르면 자식 클래스의 필드가 초기화되기 전에 실행되므로,
    // 객체를 다 만든 뒤 display()에서 화면을 구성한다.
    public final void display() {
        if (!initialized) {
            initUI();
            initialized = true;
        }
        setVisible(true);
    }

    protected abstract void initUI();

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    protected void showError(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
    }
}
