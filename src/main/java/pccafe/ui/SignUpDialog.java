package pccafe.ui;

import pccafe.dao.MemberDAO;
import pccafe.dto.Member;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.sql.SQLException;

public class SignUpDialog extends JDialog {
    private final MemberDAO memberDAO = new MemberDAO();

    private final JTextField idField = new JTextField(12);
    private final JPasswordField pwField = new JPasswordField(12);
    private final JTextField nameField = new JTextField(12);
    private final JTextField phoneField = new JTextField(12);
    private final JComboBox<String> questionCombo = new JComboBox<>(Member.PW_QUESTIONS);
    private final JTextField answerField = new JTextField(12);

    public SignUpDialog(JFrame owner) {
        super(owner, "회원가입", true);

        JPanel form = Forms.labeled("아이디", idField, "비밀번호", pwField, "이름", nameField, "전화번호", phoneField,
                "비밀번호 찾기 질문", questionCombo, "답변", answerField);

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
        String answer = answerField.getText().trim();

        // 질문 답변은 비밀번호를 잊었을 때 다시 설정하는 유일한 방법이라 필수로 받는다
        if (id.isEmpty() || pw.isEmpty() || name.isEmpty() || answer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "아이디, 비밀번호, 이름, 질문 답변은 필수입니다.");
            return;
        }
        try {
            if (memberDAO.existsById(id)) {
                JOptionPane.showMessageDialog(this, "이미 사용 중인 아이디입니다.");
                return;
            }
            Member m = new Member(id, pw, name, phone);
            m.setPwQuestion((String) questionCombo.getSelectedItem());
            m.setPwAnswer(answer);
            memberDAO.insert(m);
            JOptionPane.showMessageDialog(this, "가입되었습니다.");
            dispose();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}
