package pccafe.ui.panel;

import pccafe.dao.MemberDAO;
import pccafe.dto.Member;
import pccafe.ui.AdminFrame;
import pccafe.util.TimeFormat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
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

public class MemberPanel extends BasePanel {
    private final AdminFrame admin;
    private final MemberDAO memberDAO = new MemberDAO();
    private final DefaultTableModel model =
            readOnlyModel("아이디", "이름", "전화번호", "남은 시간", "구분", "상태");
    private final JTable table = new JTable(model);

    private final JTextField searchField = new JTextField(12);
    private final JTextField nameField = new JTextField(8);
    private final JTextField phoneField = new JTextField(10);
    private final JPasswordField pwField = new JPasswordField(8);
    private final JTextField minutesField = new JTextField(5);

    private List<Member> members = new ArrayList<>();

    public MemberPanel(AdminFrame admin) {
        this.admin = admin;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillForm();
            }
        });

        JButton search = new JButton("검색");
        search.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("아이디 / 이름"));
        top.add(searchField);
        top.add(search);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("이름"));
        form.add(nameField);
        form.add(new JLabel("전화번호"));
        form.add(phoneField);
        form.add(new JLabel("새 비밀번호"));
        form.add(pwField);
        form.add(new JLabel("남은 시간(분)"));
        form.add(minutesField);

        JButton update = new JButton("수정");
        JButton delete = new JButton("삭제");
        update.addActionListener(e -> updateMember());
        delete.addActionListener(e -> deleteMember());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(update);
        buttons.add(delete);
        buttons.add(new JLabel("비밀번호는 비워두면 그대로 유지"));

        JPanel south = new JPanel(new BorderLayout());
        south.add(form, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        try {
            members = memberDAO.search(searchField.getText().trim());
        } catch (SQLException e) {
            showError(e);
            return;
        }
        model.setRowCount(0);
        for (Member m : members) {
            model.addRow(new Object[]{
                    m.getId(),
                    m.getName(),
                    m.getPhone(),
                    TimeFormat.format(m.getRemainSeconds()),
                    m.isAdmin() ? "관리자" : "회원",
                    admin.isLoggedIn(m.getId()) ? "사용 중" : ""
            });
        }
        clearForm();
    }

    private Member selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : members.get(row);
    }

    private void fillForm() {
        Member m = selected();
        if (m == null) {
            return;
        }
        nameField.setText(m.getName());
        phoneField.setText(m.getPhone());
        pwField.setText("");
        minutesField.setText(String.valueOf(m.getRemainSeconds() / 60));
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        phoneField.setText("");
        pwField.setText("");
        minutesField.setText("");
    }

    private void updateMember() {
        Member m = selected();
        if (m == null) {
            showInfo("수정할 회원을 선택하세요.");
            return;
        }
        // 사용 중인 회원은 게스트 창이 남은 시간을 갖고 있어서, 여기서 바꾸면 종료할 때 덮어써진다
        if (admin.isLoggedIn(m.getId())) {
            showInfo("사용 중인 회원은 수정할 수 없습니다. 시간은 좌석 탭에서 조정하세요.");
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showInfo("이름을 입력하세요.");
            return;
        }
        Integer minutes = readInt(minutesField, "남은 시간");
        if (minutes == null) {
            return;
        }

        m.setName(name);
        m.setPhone(phoneField.getText().trim());
        String pw = new String(pwField.getPassword());
        if (!pw.isEmpty()) {
            m.setPassword(pw);
        }
        // 분 단위로 보여주므로, 분이 바뀌었을 때만 덮어써서 남은 초를 잃지 않게 한다
        if (minutes != m.getRemainSeconds() / 60) {
            m.setRemainSeconds(minutes * 60);
        }

        try {
            memberDAO.update(m);
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteMember() {
        Member m = selected();
        if (m == null) {
            showInfo("삭제할 회원을 선택하세요.");
            return;
        }
        if (m.isAdmin()) {
            showInfo("관리자 계정은 삭제할 수 없습니다.");
            return;
        }
        if (admin.isLoggedIn(m.getId())) {
            showInfo("사용 중인 회원은 삭제할 수 없습니다. 먼저 강제 종료하세요.");
            return;
        }
        if (!confirm(m.getId() + " 회원을 삭제할까요?")) {
            return;
        }
        try {
            memberDAO.delete(m.getId());
            refresh();
        } catch (SQLException e) {
            showError(e);
        }
    }
}
