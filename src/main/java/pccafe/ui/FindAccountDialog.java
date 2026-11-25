package pccafe.ui;

import pccafe.dao.MemberDAO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;

// 아이디 찾기 / 비밀번호 찾기 창. 게스트 로그인 화면의 두 버튼이 각각 해당 탭을 연 채로 띄운다
public class FindAccountDialog extends JDialog {
    public static final int FIND_ID = 0;
    public static final int FIND_PW = 1;

    private final MemberDAO memberDAO = new MemberDAO();

    // 아이디 찾기 탭
    private final JTextField nameField = new JTextField(12);
    private final JTextField phoneField = new JTextField(12);
    private final JLabel idResultLabel = new JLabel(" ");

    // 비밀번호 찾기 탭
    private final JTextField idField = new JTextField(12);
    private final JLabel questionLabel = new JLabel("아이디 입력 후 [질문 확인]을 누르세요.");
    private final JTextField answerField = new JTextField(12);
    private final JPasswordField newPwField = new JPasswordField(12);
    private final JPasswordField confirmPwField = new JPasswordField(12);
    private JButton resetButton;
    private String checkedId;   // 질문을 확인한 아이디. 확인 전에는 null

    public FindAccountDialog(JFrame owner, int tab) {
        super(owner, "아이디 · 비밀번호 찾기", true);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("아이디 찾기", createFindIdPanel());
        tabs.addTab("비밀번호 찾기", createFindPwPanel());
        tabs.setSelectedIndex(tab);
        add(tabs);

        pack();
        setLocationRelativeTo(owner);
    }

    // ---------- 아이디 찾기: 이름 + 전화번호 ----------

    private JPanel createFindIdPanel() {
        JPanel form = Forms.labeled("이름", nameField, "전화번호", phoneField);
        JButton find = new JButton("아이디 찾기");
        find.addActionListener(e -> findId());
        phoneField.addActionListener(e -> findId());

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.add(find, BorderLayout.NORTH);
        bottom.add(idResultLabel, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(new JLabel("가입할 때 입력한 이름과 전화번호를 입력하세요."), BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void findId() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름과 전화번호를 입력하세요.");
            return;
        }
        try {
            List<String> ids = memberDAO.findIds(name, phone);
            idResultLabel.setText(ids.isEmpty() ? "일치하는 회원이 없습니다."
                    : "회원님의 아이디: " + String.join(", ", ids));
        } catch (SQLException e) {
            showError(e);
        }
    }

    // ---------- 비밀번호 찾기: 아이디 → 질문 확인 → 답변 + 새 비밀번호 ----------

    private JPanel createFindPwPanel() {
        JButton check = new JButton("질문 확인");
        check.addActionListener(e -> checkQuestion());
        idField.addActionListener(e -> checkQuestion());
        JPanel idRow = new JPanel(new BorderLayout(6, 0));
        idRow.add(idField, BorderLayout.CENTER);
        idRow.add(check, BorderLayout.EAST);

        JPanel form = Forms.labeled("아이디", idRow, "질문", questionLabel, "답변", answerField,
                "새 비밀번호", newPwField, "새 비밀번호 확인", confirmPwField);

        resetButton = new JButton("비밀번호 변경");
        resetButton.addActionListener(e -> resetPassword());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(resetButton);
        setResetEnabled(false);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(form, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void checkQuestion() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "아이디를 입력하세요.");
            return;
        }
        try {
            String question = memberDAO.findPwQuestion(id);
            if (question == null) {
                checkedId = null;
                setResetEnabled(false);
                questionLabel.setText("아이디 입력 후 [질문 확인]을 누르세요.");
                // 없는 아이디와 질문 미등록 회원을 구분하지 않는다 (아이디 존재 여부를 알려 주지 않기 위해)
                JOptionPane.showMessageDialog(this,
                        "비밀번호 찾기를 할 수 없는 아이디입니다.\n질문을 등록하지 않은 회원은 관리자에게 문의하세요.");
                return;
            }
            checkedId = id;
            questionLabel.setText(question);
            setResetEnabled(true);
            answerField.requestFocus();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void resetPassword() {
        // 질문을 확인한 뒤 아이디 칸을 고쳤으면 다시 확인하게 한다
        if (checkedId == null || !checkedId.equals(idField.getText().trim())) {
            JOptionPane.showMessageDialog(this, "[질문 확인]을 다시 눌러 주세요.");
            setResetEnabled(false);
            return;
        }
        String answer = answerField.getText().trim();
        String newPw = new String(newPwField.getPassword());
        String confirmPw = new String(confirmPwField.getPassword());
        if (answer.isEmpty() || newPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "답변과 새 비밀번호를 입력하세요.");
            return;
        }
        if (!newPw.equals(confirmPw)) {
            JOptionPane.showMessageDialog(this, "새 비밀번호가 서로 다릅니다.");
            return;
        }
        try {
            if (!memberDAO.resetPassword(checkedId, answer, newPw)) {
                JOptionPane.showMessageDialog(this, "답변이 틀렸습니다.");
                return;
            }
            JOptionPane.showMessageDialog(this, "비밀번호를 변경했습니다. 새 비밀번호로 로그인하세요.");
            dispose();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void setResetEnabled(boolean enabled) {
        answerField.setEnabled(enabled);
        newPwField.setEnabled(enabled);
        confirmPwField.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    private void showError(SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
    }
}
