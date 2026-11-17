package pccafe.ui;

import pccafe.dao.ChatDAO;
import pccafe.dao.MemberDAO;
import pccafe.dao.MenuDAO;
import pccafe.dao.SeatDAO;
import pccafe.dao.TimeProductDAO;
import pccafe.dto.ChatMessage;
import pccafe.dto.Member;
import pccafe.dto.Menu;
import pccafe.dto.Payment;
import pccafe.dto.PaymentDetail;
import pccafe.dto.TimeProduct;
import pccafe.service.PaymentException;
import pccafe.service.PaymentService;
import pccafe.thread.TimerThread;
import pccafe.util.TimeFormat;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GuestFrame extends BaseFrame {
    private static final String LOGIN_CARD = "login";
    private static final String USING_CARD = "using";
    private static final int WARNING_SECONDS = 300;
    private static final int CHAT_HISTORY_SIZE = 50;

    private final AdminFrame admin;
    private final MemberDAO memberDAO = new MemberDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final PaymentService paymentService = new PaymentService();
    private final TimeProductDAO timeProductDAO = new TimeProductDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final ChatDAO chatDAO = new ChatDAO();

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
        // 다른 창에서 좌석을 차지하거나 비울 수 있으므로, 목록을 펼칠 때마다 빈 좌석을 다시 읽는다
        seatCombo.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refreshSeats();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

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

    // 빈 좌석 목록을 다시 읽는다. 고르던 좌석이 아직 비어 있으면 그대로 선택해 둔다
    private void refreshSeats() {
        Object selected = seatCombo.getSelectedItem();
        seatCombo.removeAllItems();
        try {
            for (int no : seatDAO.findEmptySeatNos()) {
                seatCombo.addItem(no);
            }
        } catch (SQLException e) {
            showError(e);
        }
        if (selected != null) {
            seatCombo.setSelectedItem(selected);
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
        loadChatHistory();
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
            Payment pay = new PaymentDialog(this, "시간 구매 결제", p.getPrice(), currentPoint(m))
                    .showDialog(m.getId(), Payment.TIME);
            if (pay == null) {
                return;
            }
            paymentService.buyTime(p, pay);   // 결제 + 결제 상세 + 포인트 (트랜잭션)
            addSeconds(p.getMinutes() * 60);
            saveRemainTime();
            showInfo(String.format("%s 충전되었습니다. (적립 %,dP)", p.getName(), PaymentService.earnedPoint(pay)));
        } catch (PaymentException e) {
            showInfo(e.getMessage());
        } catch (SQLException e) {
            showError(e);
        }
    }

    // 장바구니에 메뉴를 담고 → 결제 수단 입력 → 재고 차감·결제·주문을 한 번에 저장 → 관리자에게 알림
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
            List<PaymentDetail> cart = showCartDialog(menus);
            if (cart == null) {
                return;
            }
            int total = 0;
            for (PaymentDetail item : cart) {
                total += item.getSubtotal();
            }
            Payment pay = new PaymentDialog(this, "음식 결제", total, currentPoint(m))
                    .showDialog(m.getId(), Payment.FOOD);
            if (pay == null) {
                return;
            }
            int orderId = paymentService.orderFood(seatNo, cart, pay);

            StringBuilder summary = new StringBuilder();
            for (PaymentDetail item : cart) {
                if (summary.length() > 0) {
                    summary.append(", ");
                }
                summary.append(item.getItemName()).append(' ').append(item.getQty()).append("개");
            }
            admin.onNewOrder(seatNo, m.getName(), summary.toString());
            chatLog.append(String.format("[주문 %d번] %s (%,d원)%n", orderId, summary, total));
            showInfo(String.format("주문되었습니다. (적립 %,dP)", PaymentService.earnedPoint(pay)));
        } catch (PaymentException e) {
            showInfo(e.getMessage());   // 재고·포인트 부족: 아무것도 저장되지 않았다 (rollback)
        } catch (SQLException e) {
            showError(e);
        }
    }

    // 장바구니 창. 메뉴와 수량을 골라 "담기", 목록에서 골라 "빼기". 확인을 누르면 담은 목록, 취소하면 null
    private List<PaymentDetail> showCartDialog(List<Menu> menus) {
        JComboBox<Menu> menuCombo = new JComboBox<>(menus.toArray(new Menu[0]));
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        JButton addButton = new JButton("담기");
        JButton removeButton = new JButton("빼기");

        List<PaymentDetail> cart = new ArrayList<>();
        DefaultListModel<PaymentDetail> cartModel = new DefaultListModel<>();
        JList<PaymentDetail> cartList = new JList<>(cartModel);
        JLabel totalLabel = new JLabel("합계: 0원");

        Runnable redraw = () -> {
            cartModel.clear();
            int total = 0;
            for (PaymentDetail item : cart) {
                cartModel.addElement(item);
                total += item.getSubtotal();
            }
            totalLabel.setText(String.format("합계: %,d원", total));
        };
        addButton.addActionListener(e -> {
            Menu menu = (Menu) menuCombo.getSelectedItem();
            int qty = (Integer) qtySpinner.getValue();
            // 이미 담은 메뉴면 수량만 늘린다
            for (PaymentDetail item : cart) {
                if (item.getMenuId() == menu.getMenuId()) {
                    item.setQty(item.getQty() + qty);
                    redraw.run();
                    return;
                }
            }
            cart.add(PaymentDetail.ofMenu(menu, qty));
            redraw.run();
        });
        removeButton.addActionListener(e -> {
            int index = cartList.getSelectedIndex();
            if (index >= 0) {
                cart.remove(index);
                redraw.run();
            }
        });

        JPanel pick = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pick.add(menuCombo);
        pick.add(qtySpinner);
        pick.add(addButton);

        JScrollPane listScroll = new JScrollPane(cartList);
        listScroll.setPreferredSize(new Dimension(320, 140));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(removeButton, BorderLayout.WEST);
        bottom.add(totalLabel, BorderLayout.EAST);

        JPanel form = new JPanel(new BorderLayout(6, 6));
        form.add(pick, BorderLayout.NORTH);
        form.add(listScroll, BorderLayout.CENTER);
        form.add(bottom, BorderLayout.SOUTH);

        while (true) {
            int answer = JOptionPane.showConfirmDialog(this, form, "음식 주문 (장바구니)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return null;
            }
            if (!cart.isEmpty()) {
                return cart;
            }
            showInfo("메뉴를 담아 주세요.");
        }
    }

    // 결제 창에 보여 줄 현재 포인트. 다른 결제로 바뀌었을 수 있으므로 DB에서 다시 읽는다
    private int currentPoint(Member m) throws SQLException {
        Member fresh = memberDAO.findById(m.getId());
        return fresh == null ? 0 : fresh.getPoint();
    }

    // 로그인하면 이 회원이 관리자와 나눈 이전 대화를 DB에서 불러온다
    private void loadChatHistory() {
        chatLog.setText("");
        try {
            for (ChatMessage m : chatDAO.findRecent(member.getId(), CHAT_HISTORY_SIZE)) {
                String who = ChatMessage.ADMIN.equals(m.getSender()) ? "관리자" : "나";
                chatLog.append(who + ": " + m.getContent() + "\n");
            }
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
        try {
            chatDAO.insert(new ChatMessage(m.getId(), seatNo, ChatMessage.GUEST, text));
        } catch (SQLException e) {
            showError(e);
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
            // == 300으로 비교하면 관리자가 -10분을 눌러 300초를 건너뛸 때 경고가 안 뜬다.
            // 5분 이하로 처음 내려갔을 때 한 번만 띄우고, 시간을 추가해 5분을 넘기면 다시 띄울 수 있게 한다
            if (remain > WARNING_SECONDS) {
                warned = false;
            } else if (remain > 0 && !warned) {
                warned = true;
                showInfo("남은 시간이 " + TimeFormat.format(remain) + " 남았습니다.");
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

    // 관리자가 좌석판을 새로 그린 뒤 호출: 내 좌석에 이름과 남은 시간을 다시 표시
    public void redrawSeat() {
        refreshDisplay();
    }

    public String getMemberId() {
        Member m = member;
        return m == null ? null : m.getId();
    }

    public int getSeatNo() {
        return seatNo;
    }
}
