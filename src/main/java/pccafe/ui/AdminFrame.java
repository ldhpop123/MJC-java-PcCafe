package pccafe.ui;

import pccafe.dao.SeatDAO;
import pccafe.dto.Seat;
import pccafe.util.TimeFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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

    private final List<GuestFrame> guests = new ArrayList<>();
    private final Map<Integer, JLabel> seatLabels = new LinkedHashMap<>();
    private final SeatDAO seatDAO = new SeatDAO();

    private int selectedSeat = -1;
    private JLabel selectedLabel;
    private JTextArea chatLog;
    private JTextField chatInput;

    public AdminFrame() {
        super("PC방 관리자", 1000, 650);
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

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("좌석", createSeatTab());
        tabs.addTab("회원 관리", placeholder("TODO: 회원 조회 / 검색 / 수정 / 삭제 (MemberDAO)"));
        tabs.addTab("메뉴 관리", placeholder("TODO: 메뉴 추가 / 수정 / 삭제 (MenuDAO)"));
        tabs.addTab("주문", placeholder("TODO: 주문 목록, 상태 변경 (OrderDAO)"));
        tabs.addTab("매출", placeholder("TODO: 오늘 시간 / 음식 매출 (PaymentDAO.sumToday)"));

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
        JPanel board = new JPanel(new GridLayout(0, 4, 8, 8));
        board.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        try {
            for (Seat seat : seatDAO.findAll()) {
                JLabel label = createSeatLabel(seat.getSeatNo());
                seatLabels.put(seat.getSeatNo(), label);
                board.add(label);
            }
        } catch (SQLException e) {
            showError(e);
        }

        JPanel tab = new JPanel(new BorderLayout());
        tab.add(board, BorderLayout.CENTER);
        tab.add(createSidePanel(), BorderLayout.EAST);
        return tab;
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
    }

    private JPanel createSidePanel() {
        selectedLabel = new JLabel("선택된 좌석: 없음");

        JButton plus = new JButton("시간 +10분");
        JButton minus = new JButton("시간 -10분");
        JButton kick = new JButton("강제 종료");
        plus.addActionListener(e -> withSelectedGuest(g -> g.addSeconds(600)));
        minus.addActionListener(e -> withSelectedGuest(g -> g.addSeconds(-600)));
        kick.addActionListener(e -> withSelectedGuest(g -> g.forceLogout("관리자가 사용을 종료했습니다.")));

        JPanel controls = new JPanel(new GridLayout(0, 1, 4, 4));
        controls.add(selectedLabel);
        controls.add(plus);
        controls.add(minus);
        controls.add(kick);

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

    private JPanel placeholder(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(text, SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
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

    public void removeGuest(GuestFrame guest) {
        guests.remove(guest);
    }
}
