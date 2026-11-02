package pccafe.ui;

import pccafe.dao.MemberDAO;
import pccafe.dto.Member;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.sql.SQLException;

// 프로그램 시작 화면. is_admin 계정으로 로그인해야 관리자 창이 열린다
public class AdminLoginFrame extends BaseFrame {
    private final MemberDAO memberDAO = new MemberDAO();

    private JTextField idField;
    private JPasswordField pwField;

    public AdminLoginFrame() {
        super("관리자 로그인", 320, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    @Override
    protected void initUI() {
        idField = new JTextField(12);
        pwField = new JPasswordField(12);
        pwField.addActionListener(e -> login());

        JPanel form = Forms.labeled("아이디", idField, "비밀번호", pwField);

        JButton login = new JButton("로그인");
        login.addActionListener(e -> login());

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(form, BorderLayout.NORTH);
        panel.add(login, BorderLayout.SOUTH);
        add(panel);
    }

    private void login() {
        try {
            Member m = memberDAO.login(idField.getText().trim(), new String(pwField.getPassword()));
            if (m == null) {
                showInfo("아이디 또는 비밀번호가 틀렸습니다.");
                return;
            }
            if (!m.isAdmin()) {
                showInfo("관리자 계정이 아닙니다.");
                return;
            }
            dispose();
            new AdminFrame(m).display();
        } catch (SQLException e) {
            showError(e);
        }
    }
}
