package pccafe.ui;

import pccafe.dto.Payment;
import pccafe.service.PaymentService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

// 결제 수단 입력 창. 카드 + 현금 + 포인트 = 결제 금액이 되어야 결제할 수 있다
public class PaymentDialog extends JDialog {
    private final int total;
    private final int pointBalance;

    private final JTextField cardField = new JTextField(8);
    private final JTextField cashField = new JTextField(8);
    private final JTextField pointField = new JTextField(8);
    private final JLabel remainLabel = new JLabel();

    private int[] amounts;   // {카드, 현금, 포인트}. 취소하면 null

    public PaymentDialog(JFrame owner, String title, int total, int pointBalance) {
        super(owner, title, true);
        this.total = total;
        this.pointBalance = pointBalance;

        cardField.setText(String.valueOf(total));   // 기본은 전액 카드
        cashField.setText("0");
        pointField.setText("0");
        DocumentListener update = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateRemain(); }
            public void removeUpdate(DocumentEvent e) { updateRemain(); }
            public void changedUpdate(DocumentEvent e) { updateRemain(); }
        };
        cardField.getDocument().addDocumentListener(update);
        cashField.getDocument().addDocumentListener(update);
        pointField.getDocument().addDocumentListener(update);

        JPanel info = new JPanel(new GridLayout(0, 1, 4, 4));
        info.add(new JLabel(String.format("결제 금액: %,d원", total)));
        info.add(new JLabel(String.format("보유 포인트: %,dP  (카드·현금 결제액의 %d%% 적립)",
                pointBalance, PaymentService.POINT_RATE_PERCENT)));
        info.add(remainLabel);

        JPanel form = Forms.labeled("카드", cardField, "현금", cashField, "포인트", pointField);

        JButton pay = new JButton("결제");
        JButton cancel = new JButton("취소");
        pay.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(pay);
        buttons.add(cancel);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(info, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        add(panel);

        updateRemain();
        pack();
        setLocationRelativeTo(owner);
    }

    // 창을 띄우고, 결제를 누르면 Payment를, 취소하면 null을 돌려준다
    public Payment showDialog(String memberId, String type) {
        setVisible(true);   // 모달이라 창이 닫힐 때까지 여기서 기다린다
        if (amounts == null) {
            return null;
        }
        return new Payment(memberId, type, amounts[0], amounts[1], amounts[2]);
    }

    private void updateRemain() {
        int sum = parse(cardField) + parse(cashField) + parse(pointField);
        int remain = total - sum;
        remainLabel.setText(remain == 0 ? "금액이 맞습니다."
                : String.format("남은 금액: %,d원", remain));
    }

    private void submit() {
        int card = parse(cardField);
        int cash = parse(cashField);
        int point = parse(pointField);
        if (card < 0 || cash < 0 || point < 0) {
            JOptionPane.showMessageDialog(this, "금액은 0 이상의 숫자로 입력하세요.");
            return;
        }
        if (card + cash + point != total) {
            JOptionPane.showMessageDialog(this, String.format("카드 + 현금 + 포인트가 %,d원이 되어야 합니다.", total));
            return;
        }
        if (point > pointBalance) {
            JOptionPane.showMessageDialog(this, "보유 포인트보다 많이 쓸 수 없습니다.");
            return;
        }
        amounts = new int[]{card, cash, point};
        dispose();
    }

    // 빈칸은 0, 숫자가 아니면 -1
    private static int parse(JTextField field) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
