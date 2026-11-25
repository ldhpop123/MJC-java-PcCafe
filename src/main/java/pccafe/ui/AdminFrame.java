package pccafe.ui;

import pccafe.dao.ChatDAO;
import pccafe.dao.SeatDAO;
import pccafe.dto.ChatMessage;
import pccafe.dto.Member;
import pccafe.dto.Seat;
import pccafe.ui.panel.BasePanel;
import pccafe.ui.panel.MemberPanel;
import pccafe.ui.panel.MenuPanel;
import pccafe.ui.panel.OrderPanel;
import pccafe.ui.panel.SalesPanel;
import pccafe.ui.panel.TimeProductPanel;
import pccafe.util.TimeFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AdminFrame extends BaseFrame {
    private static final Color EMPTY_COLOR = new Color(230, 230, 230);
    private static final Color USING_COLOR = new Color(170, 215, 255);
    private static final Color SELECTED_BORDER = new Color(30, 110, 220);
    private static final int CHAT_HISTORY_SIZE = 50;
    private static final int MAX_SEATS = 100;

    private final List<GuestFrame> guests = new ArrayList<>();
    private final Map<Integer, JLabel> seatLabels = new LinkedHashMap<>();
    private final SeatDAO seatDAO = new SeatDAO();
    private final ChatDAO chatDAO = new ChatDAO();

    private int selectedSeat = -1;
    private JPanel board;
    private final JSpinner seatTotalSpinner = new JSpinner(new SpinnerNumberModel(12, 1, MAX_SEATS, 1));
    private JLabel selectedLabel;
    private JTextArea chatLog;
    private JTextField chatInput;
    private OrderPanel orderPanel;
    private final Member loginAdmin;

    public AdminFrame(Member loginAdmin) {
        super("PC방 관리자 - " + loginAdmin.getName(), 1000, 650);
        this.loginAdmin = loginAdmin;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    @Override
    protected void initUI() {
        try {
            seatDAO.resetAll();
        } catch (SQLException e) {
            showError(e);
        }

        JButton addGuest = new JButton("게스트 추가");
        addGuest.addActionListener(e -> openGuest());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(addGuest);

        orderPanel = new OrderPanel(loginAdmin.getId());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("좌석", createSeatTab());
        tabs.addTab("회원 관리", new MemberPanel(this));
        tabs.addTab("메뉴 관리", new MenuPanel());
        tabs.addTab("요금제 관리", new TimeProductPanel());
        tabs.addTab("주문", orderPanel);
        tabs.addTab("매출", new SalesPanel());
        // 탭을 열 때마다 DB에서 다시 읽기
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() instanceof BasePanel panel) {
                panel.refresh();
            }
        });

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private void openGuest() {
        GuestFrame guest = new GuestFrame(this);
        guests.add(guest);
        guest.setLocation(getX() + 40 * guests.size(), getY() + 40 * guests.size());
        guest.display();
    }

    // ---------- 좌석 탭 ----------

    private JPanel createSeatTab() {
        board = new JPanel(new GridLayout(0, 4, 8, 8));
        board.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        rebuildBoard();

        JPanel tab = new JPanel(new BorderLayout());
        // 좌석이 많아지면 스크롤
        tab.add(new JScrollPane(board), BorderLayout.CENTER);
        tab.add(createSidePanel(), BorderLayout.EAST);
        return tab;
    }

    // seat 테이블을 다시 읽어 좌석판을 새로 그린다 (좌석 추가 / 삭제 후 호출)
    private void rebuildBoard() {
        board.removeAll();
        seatLabels.clear();
        try {
            for (Seat seat : seatDAO.findAll()) {
                JLabel label = createSeatLabel(seat.getSeatNo());
                seatLabels.put(seat.getSeatNo(), label);
                board.add(label);
            }
        } catch (SQLException e) {
            showError(e);
        }
        // 사용 중인 좌석은 게스트 창이 이름과 남은 시간을 다시 그려 준다
        for (GuestFrame guest : guests) {
            guest.redrawSeat();
        }
        if (!seatLabels.containsKey(selectedSeat)) {
            selectedSeat = -1;
            if (selectedLabel != null) {
                selectedLabel.setText("선택된 좌석: 없음");
            }
        } else {
            seatLabels.get(selectedSeat).setBorder(BorderFactory.createLineBorder(SELECTED_BORDER, 3));
        }
        seatTotalSpinner.setValue(seatLabels.size());
        board.revalidate();
        board.repaint();
    }

    // 총 좌석 수 입력 → 그 수에 맞게 좌석을 만들거나 지운다. 사용 중인 좌석은 지우지 않는다
    private void resizeSeats() {
        int total = (Integer) seatTotalSpinner.getValue();
        int current = seatLabels.size();
        if (total == current) {
            showInfo("이미 " + total + "석입니다.");
            return;
        }
        if (total < current && !confirm(String.format("좌석을 %d석에서 %d석으로 줄일까요?%n"
                + "번호가 큰 빈 좌석부터 지우고, 사용 중인 좌석은 지우지 않습니다.", current, total))) {
            seatTotalSpinner.setValue(current);
            return;
        }
        try {
            int result = seatDAO.resizeTo(total);
            rebuildBoard();
            showInfo(result == total ? "총 좌석 수를 " + total + "석으로 맞췄습니다."
                    : String.format("사용 중인 좌석이 있어 %d석까지만 줄였습니다.", result));
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void addSeat() {
        try {
            int no = seatDAO.insertNext();
            rebuildBoard();
            showInfo(no + "번 좌석을 추가했습니다.");
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void deleteSeat() {
        if (selectedSeat == -1) {
            showInfo("삭제할 좌석을 선택하세요.");
            return;
        }
        int no = selectedSeat;
        if (!confirm(no + "번 좌석을 삭제할까요?")) {
            return;
        }
        try {
            if (!seatDAO.deleteIfEmpty(no)) {
                showInfo("사용 중인 좌석은 삭제할 수 없습니다.");
                return;
            }
            rebuildBoard();
        } catch (SQLException e) {
            showError(e);
        }
    }

    private JLabel createSeatLabel(int seatNo) {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setOpaque(true);
        label.setPreferredSize(new Dimension(120, 90));
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectSeat(seatNo);
            }
        });
        renderSeat(label, seatNo, null, 0);
        return label;
    }

    private void renderSeat(JLabel label, int seatNo, String name, int remainSeconds) {
        if (name == null) {
            label.setText("<html><center>" + seatNo + "번<br>빈자리</center></html>");
            label.setBackground(EMPTY_COLOR);
        } else {
            label.setText("<html><center>" + seatNo + "번<br>" + name + "<br>"
                    + TimeFormat.format(remainSeconds) + "</center></html>");
            label.setBackground(USING_COLOR);
        }
    }

    private void selectSeat(int seatNo) {
        if (selectedSeat != -1) {
            seatLabels.get(selectedSeat).setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
        selectedSeat = seatNo;
        seatLabels.get(seatNo).setBorder(BorderFactory.createLineBorder(SELECTED_BORDER, 3));
        selectedLabel.setText("선택된 좌석: " + seatNo + "번");
        showChatHistory(seatNo);
    }

    // 사용 중인 좌석을 고르면 그 손님과 나눈 이전 대화를 DB에서 불러온다
    private void showChatHistory(int seatNo) {
        GuestFrame guest = findGuestBySeat(seatNo);
        if (guest == null) {
            return;
        }
        try {
            chatLog.setText("── " + seatNo + "번 좌석 " + guest.getMemberId() + " 님과의 대화 ──\n");
            for (ChatMessage m : chatDAO.findRecent(guest.getMemberId(), CHAT_HISTORY_SIZE)) {
                String who = ChatMessage.ADMIN.equals(m.getSender()) ? "관리자" : m.getMemberId();
                chatLog.append("[" + who + "] " + m.getContent() + "\n");
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private JPanel createSidePanel() {
        selectedLabel = new JLabel("선택된 좌석: 없음");

        JButton plus = new JButton("시간 +10분");
        JButton minus = new JButton("시간 -10분");
        JButton kick = new JButton("강제 종료");
        plus.addActionListener(e -> withSelectedGuest(g -> g.addSeconds(600)));
        minus.addActionListener(e -> withSelectedGuest(g -> g.addSeconds(-600)));
        kick.addActionListener(e -> withSelectedGuest(g -> g.forceLogout("관리자가 사용을 종료했습니다.")));

        JButton addSeat = new JButton("좌석 추가");
        JButton deleteSeat = new JButton("좌석 삭제");
        addSeat.addActionListener(e -> addSeat());
        deleteSeat.addActionListener(e -> deleteSeat());
        JPanel seatButtons = new JPanel(new GridLayout(1, 2, 4, 4));
        seatButtons.add(addSeat);
        seatButtons.add(deleteSeat);

        JButton applyTotal = new JButton("적용");
        applyTotal.addActionListener(e -> resizeSeats());
        JPanel totalRow = new JPanel(new BorderLayout(6, 0));
        totalRow.add(new JLabel("총 좌석 수"), BorderLayout.WEST);
        totalRow.add(seatTotalSpinner, BorderLayout.CENTER);
        totalRow.add(applyTotal, BorderLayout.EAST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 4, 4));
        controls.add(selectedLabel);
        controls.add(plus);
        controls.add(minus);
        controls.add(kick);
        controls.add(totalRow);
        controls.add(seatButtons);

        chatLog = new JTextArea();
        chatLog.setEditable(false);
        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendChat());

        JPanel chat = new JPanel(new BorderLayout(4, 4));
        chat.setBorder(BorderFactory.createTitledBorder("채팅 (선택된 좌석에게 전송)"));
        chat.add(new JScrollPane(chatLog), BorderLayout.CENTER);
        chat.add(chatInput, BorderLayout.SOUTH);

        JPanel side = new JPanel(new BorderLayout(8, 8));
        side.setPreferredSize(new Dimension(280, 0));
        side.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));
        side.add(controls, BorderLayout.NORTH);
        side.add(chat, BorderLayout.CENTER);
        return side;
    }

    private void withSelectedGuest(Consumer<GuestFrame> action) {
        GuestFrame guest = findGuestBySeat(selectedSeat);
        if (guest == null) {
            showInfo("사용 중인 좌석을 선택하세요.");
            return;
        }
        action.accept(guest);
    }

    private void sendChat() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        GuestFrame guest = findGuestBySeat(selectedSeat);
        if (guest == null) {
            showInfo("사용 중인 좌석을 선택하세요.");
            return;
        }
        try {
            chatDAO.insert(new ChatMessage(guest.getMemberId(), selectedSeat, ChatMessage.ADMIN, text));
        } catch (SQLException e) {
            showError(e);
            return;
        }
        guest.receiveMessage(text);
        chatLog.append("[관리자 → " + selectedSeat + "번] " + text + "\n");
        chatInput.setText("");
    }

    private GuestFrame findGuestBySeat(int seatNo) {
        for (GuestFrame guest : guests) {
            if (guest.getSeatNo() == seatNo) {
                return guest;
            }
        }
        return null;
    }

    // ---------- GuestFrame에서 호출 (모두 EDT에서 호출됨) ----------

    public void updateSeat(int seatNo, String name, int remainSeconds) {
        JLabel label = seatLabels.get(seatNo);
        if (label != null) {
            renderSeat(label, seatNo, name, remainSeconds);
        }
    }

    public void clearSeat(int seatNo) {
        updateSeat(seatNo, null, 0);
    }

    public boolean isLoggedIn(String memberId) {
        for (GuestFrame guest : guests) {
            if (memberId.equals(guest.getMemberId())) {
                return true;
            }
        }
        return false;
    }

    public void receiveMessage(int seatNo, String name, String message) {
        chatLog.append("[" + seatNo + "번 " + name + "] " + message + "\n");
    }

    public void onNewOrder(int seatNo, String name, String summary) {
        chatLog.append("[주문] " + seatNo + "번 " + name + ": " + summary + "\n");
        orderPanel.refresh();
        Toolkit.getDefaultToolkit().beep();
    }

    public void removeGuest(GuestFrame guest) {
        guests.remove(guest);
    }
}
