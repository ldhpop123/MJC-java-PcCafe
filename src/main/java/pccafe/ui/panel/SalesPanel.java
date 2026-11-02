package pccafe.ui.panel;

import pccafe.dao.PaymentDAO;
import pccafe.dto.Payment;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;

public class SalesPanel extends BasePanel {
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private final JLabel timeLabel = new JLabel();
    private final JLabel foodLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();

    public SalesPanel() {
        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.add(new JLabel("시간 판매"));
        grid.add(timeLabel);
        grid.add(new JLabel("음식 판매"));
        grid.add(foodLabel);
        grid.add(new JLabel("합계"));
        grid.add(totalLabel);
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 18f));

        JButton reload = new JButton("새로고침");
        reload.addActionListener(e -> refresh());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("오늘 매출"));
        top.add(reload);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrap.add(grid);

        add(top, BorderLayout.NORTH);
        add(wrap, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            int time = paymentDAO.sumToday(Payment.TIME);
            int food = paymentDAO.sumToday(Payment.FOOD);
            timeLabel.setText(String.format("%,d원", time));
            foodLabel.setText(String.format("%,d원", food));
            totalLabel.setText(String.format("%,d원", time + food));
        } catch (SQLException e) {
            showError(e);
        }
    }
}
