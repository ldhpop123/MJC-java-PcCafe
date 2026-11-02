package pccafe.ui;

import pccafe.dao.MemberDAO;
import pccafe.dao.MenuDAO;
import pccafe.dao.OrderDAO;
import pccafe.dao.PaymentDAO;
import pccafe.dao.SeatDAO;
import pccafe.dao.TimeProductDAO;
import pccafe.dto.Member;
import pccafe.dto.Menu;
import pccafe.dto.Order;
import pccafe.dto.Payment;
import pccafe.dto.TimeProduct;
import pccafe.thread.TimerThread;
import pccafe.util.TimeFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;

public class GuestFrame extends BaseFrame {
    private static final String LOGIN_CARD = "login";
    private static final String USING_CARD = "using";
    private static final int WARNING_SECONDS = 300;

    private final AdminFrame admin;
    private final MemberDAO memberDAO = new MemberDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final TimeProductDAO timeProductDAO = new TimeProductDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    // 타이머 스레드에서도 읽으므로 volatile
    private volatile Member member;   // null = 로그인 전
    private volatile int seatNo = -1;
    private int remainSeconds;        // synchronized 메서드로만 접근
    private TimerThread timer;
    private boolean warned;

    private CardLayout cards;
    private JPanel root;
    private JTextField idField;
    private JPasswordField pwField;
    private JComboBox<Integer> seatCombo;
    private JLabel infoLabel;
    private JLabel remainLabel;
    private JTextArea chatLog;
    private JTextField chatInput;

    public GuestFrame(AdminFrame admin) {
        super("PC방 - 게스트", 420, 520);
        this.admin = admin;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                endSession();
                admin.removeGuest(GuestFrame.this);
            }
        });
    }

    @Override
    protected void initUI() {
        cards = new CardLayout();
        root = new JPanel(cards);
        root.add(createLoginPanel(), LOGIN_CARD);
        root.add(createUsingPanel(), USING_CARD);
        add(root);
        refreshSeats();
    }

    // ---------- 로그인 화면 ----------

    private JPanel createLoginPanel() {
        idField = new JTextField(12);
        pwField = new JPasswordField(12);
        seatCombo = new JComboBox<>();

        JPanel form = Forms.labeled("아이디", idField, "비밀번호", pwField, "좌석", seatCombo);

        JButton login = new JButton("로그인");
        JButton signUp = new JButton("회원가입");
        login.addActionListener(e -> login());
        pwField.addActionListener(e -> login());
        signUp.addActionListener(e -> new SignUpDialog(this).setVisible(true));

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(login);
        buttons.add(signUp);

        JLabel title = new JLabel("PC방 로그인", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        // 제목 → 폼 → 버튼을 위에서부터 붙여 쌓고, 남는 공간은 아래로 보낸다
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.add(form, BorderLayout.NORTH);
        body.add(buttons, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(0, 24));
        panel.setBorder(BorderFactory.createEmptyBorder(80, 40, 40, 40));
        panel.add(title, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private void refreshSeats() {
        seatCombo.removeAllItems();
        try {
            for (int no : seatDAO.findEmptySeatNos()) {
                seatCombo.addItem(no);
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void login() {
        String id = idField.getText().trim();
        String pw = new String(pwField.getPassword());
        Integer seat = (Integer) seatCombo.getSelectedItem();
        if (seat == null) {
            showInfo("빈 좌석이 없습니다.");
            return;
        }
        if (admin.isLoggedIn(id)) {
            showInfo("이미 다른 좌석에서 사용 중인 계정입니다.");
            return;
        }

        try {
            Member m = memberDAO.login(id, pw);
            if (m == null) {
                showInfo("아이디 또는 비밀번호가 틀렸습니다.");
                return;
            }
            if (m.isAdmin()) {
                showInfo("관리자 계정은 게스트로 로그인할 수 없습니다.");
                return;
            }
            if (!seatDAO.startUse(seat, id)) {
                showInfo("이미 사용 중인 좌석입니다.");
                refreshSeats();
                return;
            }
            setRemainSeconds(m.getRemainSeconds());
            member = m;
            seatNo = seat;
        } catch (SQLException e) {
            showError(e);
            return;
        }

        warned = false;
        chatLog.setText("");
        infoLabel.setText(seatNo + "번 좌석 · " + member.getName() + "님");
        cards.show(root, USING_CARD);
        refreshDisplay();

        if (getRemainSeconds() <= 0) {
            showInfo("남은 시간이 없습니다. 시간을 구매해주세요.");
            buyTime();
            if (getRemainSeconds() <= 0) {
                endSession();
                return;
            }
        }
        timer = new TimerThread(this);
        timer.start();
    }

    // ---------- 사용 중 화면 ----------

    private JPanel createUsingPanel() {
        infoLabel = new JLabel("", SwingConstants.CENTER);
        remainLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        remainLabel.setFont(remainLabel.getFont().deriveFont(Font.BOLD, 36f));

        JPanel info = new JPanel(new GridLayout(0, 1));
        info.add(infoLabel);
        info.add(new JLabel("남은 시간", SwingConstants.CENTER));
        info.add(remainLabel);

        chatLog = new JTextArea();
        chatLog.setEditable(false);
        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendChat());

        JPanel chat = new JPanel(new BorderLayout(4, 4));
        chat.setBorder(BorderFactory.createTitledBorder("관리자 채팅"));
        chat.add(new JScrollPane(chatLog), BorderLayout.CENTER);
        chat.add(chatInput, BorderLayout.SOUTH);

        JButton buy = new JButton("시간 구매");
        JButton order = new JButton("음식 주문");
        JButton logout = new JButton("사용 종료");
        buy.addActionListener(e -> buyTime());
        order.addActionListener(e -> orderFood());
        logout.addActionListener(e -> endSession());

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(buy);
        buttons.add(order);
        buttons.add(logout);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(info, BorderLayout.NORTH);
        panel.add(chat, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void buyTime() {
        Member m = member;
        if (m == null) {
            return;
        }
        try {
            List<TimeProduct> products = timeProductDAO.findAll();
            if (products.isEmpty()) {
                showInfo("등록된 요금제가 없습니다.");
                return;
            }
            TimeProduct p = (TimeProduct) JOptionPane.showInputDialog(this, "구매할 시간을 선택하세요.", "시간 구매",
                    JOptionPane.QUESTION_MESSAGE, null, products.toArray(), products.get(0));
            if (p == null) {
                return;
            }
            paymentDAO.insert(new Payment(m.getId(), Payment.TIME, p.getPrice()));
            addSeconds(p.getMinutes() * 60);
            saveRemainTime();
        } catch (SQLException e) {
            showError(e);
        }
    }

    // 재고 차감 → 주문 저장 → 결제 기록 → 관리자에게 알림
    private void orderFood() {
        Member m = member;
        if (m == null) {
            return;
        }
        try {
            List<Menu> menus = menuDAO.findAll();
            if (menus.isEmpty()) {
                showInfo("등록된 메뉴가 없습니다.");
                return;
            }
            JComboBox<Menu> menuCombo = new JComboBox<>(menus.toArray(new Menu[0]));
            JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
            JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
            form.add(new JLabel("메뉴"));
            form.add(menuCombo);
            form.add(new JLabel("수량"));
            form.add(qtySpinner);

            int answer = JOptionPane.showConfirmDialog(this, form, "음식 주문",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }

            Menu menu = (Menu) menuCombo.getSelectedItem();
            int qty = (Integer) qtySpinner.getValue();
            if (!menuDAO.decreaseStock(menu.getMenuId(), qty)) {
                showInfo("재고가 부족합니다.");
                return;
            }
            int total = menu.getPrice() * qty;
            orderDAO.insert(new Order(m.getId(), seatNo, menu.getMenuId(), qty));
            paymentDAO.insert(new Payment(m.getId(), Payment.FOOD, total));

            admin.onNewOrder(seatNo, m.getName(), menu.getName() + " " + qty + "개");
            chatLog.append(String.format("[주문] %s %d개 (%,d원)%n", menu.getName(), qty, total));
            showInfo("주문되었습니다.");
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void sendChat() {
        String text = chatInput.getText().trim();
        Member m = member;
        if (text.isEmpty() || m == null) {
            return;
        }
        admin.receiveMessage(seatNo, m.getName(), text);
        chatLog.append("나: " + text + "\n");
        chatInput.setText("");
    }

    private void refreshDisplay() {
        Member m = member;
        if (m == null) {
            return;
        }
        int remain = getRemainSeconds();
        remainLabel.setText(TimeFormat.format(remain));
        admin.updateSeat(seatNo, m.getName(), remain);
    }

    // 사용 종료: 타이머 정지 → 시간 저장 → 좌석 비우기 → 로그인 화면
    private void endSession() {
        if (member == null) {
            return;
        }
        if (timer != null) {
            timer.interrupt();
            timer = null;
        }
        saveRemainTime();
        try {
            seatDAO.endUse(seatNo);
        } catch (SQLException e) {
            showError(e);
        }
        admin.clearSeat(seatNo);
        member = null;
        seatNo = -1;

        pwField.setText("");
        refreshSeats();
        cards.show(root, LOGIN_CARD);
    }

    // ---------- 남은 시간 (타이머 스레드와 EDT가 같이 접근) ----------

    public synchronized int getRemainSeconds() {
        return remainSeconds;
    }

    private synchronized void setRemainSeconds(int seconds) {
        remainSeconds = seconds;
    }

    public synchronized int decreaseOneSecond() {
        if (remainSeconds > 0) {
            remainSeconds--;
        }
        return remainSeconds;
    }

    // 관리자의 시간 추가(+) / 차감(-)에도 사용
    public void addSeconds(int seconds) {
        synchronized (this) {
            remainSeconds = Math.max(0, remainSeconds + seconds);
        }
        refreshDisplay();
    }

    // ---------- TimerThread에서 호출 ----------

    public void onTick(int remain) {
        SwingUtilities.invokeLater(() -> {
            refreshDisplay();
            if (remain == WARNING_SECONDS && !warned) {
                warned = true;
                showInfo("남은 시간이 5분입니다.");
            }
        });
    }

    public void saveRemainTime() {
        Member m = member;
        if (m == null) {
            return;
        }
        try {
            memberDAO.updateRemainSeconds(m.getId(), getRemainSeconds());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- AdminFrame에서 호출 ----------

    public void forceLogout(String reason) {
        if (member == null) {
            return;
        }
        endSession();
        showInfo(reason);
    }

    public void receiveMessage(String message) {
        chatLog.append("관리자: " + message + "\n");
    }

    public String getMemberId() {
        Member m = member;
        return m == null ? null : m.getId();
    }

    public int getSeatNo() {
        return seatNo;
    }
}
