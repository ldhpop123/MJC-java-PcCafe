package pccafe.ui;

import pccafe.dao.MemberDAO;
import pccafe.dto.Member;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;

public class SignUpDialog extends JDialog {
    private final MemberDAO memberDAO = new MemberDAO();

    private final JTextField idField = new JTextField(12);
    private final JPasswordField pwField = new JPasswordField(12);
    private final JTextField nameField = new JTextField(12);
    private final JTextField phoneField = new JTextField(12);

    public SignUpDialog(JFrame owner) {
        super(owner, "회원가입", true);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("아이디"));
        form.add(idField);
        form.add(new JLabel("비밀번호"));
        form.add(pwField);
        form.add(new JLabel("이름"));
        form.add(nameField);
        form.add(new JLabel("전화번호"));
        form.add(phoneField);

        JButton submit = new JButton("가입");
        submit.addActionListener(e -> signUp());

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(form, BorderLayout.CENTER);
        panel.add(submit, BorderLayout.SOUTH);
        add(panel);

        pack();
        setLocationRelativeTo(owner);
    }

    private void signUp() {
        String id = idField.getText().trim();
        String pw = new String(pwField.getPassword());
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (id.isEmpty() || pw.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "아이디, 비밀번호, 이름은 필수입니다.");
            return;
        }
        try {
            if (memberDAO.existsById(id)) {
                JOptionPane.showMessageDialog(this, "이미 사용 중인 아이디입니다.");
                return;
            }
            memberDAO.insert(new Member(id, pw, name, phone));
            JOptionPane.showMessageDialog(this, "가입되었습니다.");
            dispose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}
