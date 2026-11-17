package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PaymentDAO {

    // 트랜잭션 안에서 호출한다. 새로 만든 pay_id를 돌려준다
    public int insert(Connection conn, Payment p) throws SQLException {
        String sql = "INSERT INTO payment (member_id, staff_id, type, amount, card_amount, cash_amount, point_amount) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getMemberId());
            ps.setString(2, p.getStaffId());
            ps.setString(3, p.getType());
            ps.setInt(4, p.getAmount());
            ps.setInt(5, p.getCardAmount());
            ps.setInt(6, p.getCashAmount());
            ps.setInt(7, p.getPointAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int payId = keys.getInt(1);
                p.setPayId(payId);
                return payId;
            }
        }
    }

    public Payment findById(Connection conn, int payId) throws SQLException {
        String sql = "SELECT * FROM payment WHERE pay_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Payment p = new Payment(rs.getString("member_id"), rs.getString("type"),
                        rs.getInt("card_amount"), rs.getInt("cash_amount"), rs.getInt("point_amount"));
                p.setPayId(rs.getInt("pay_id"));
                p.setStaffId(rs.getString("staff_id"));
                p.setPaidAt(rs.getTimestamp("paid_at").toLocalDateTime());
                return p;
            }
        }
    }

    // 오늘 매출 합계 (type: Payment.TIME / Payment.FOOD). 환불(음수)도 함께 더해진다
    public int sumToday(String type) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE type = ? AND DATE(paid_at) = CURDATE()";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // 오늘 결제 수단별 합계: {카드, 현금, 포인트}
    public int[] sumTodayByMethod() throws SQLException {
        String sql = "SELECT COALESCE(SUM(card_amount), 0), COALESCE(SUM(cash_amount), 0), "
                + "COALESCE(SUM(point_amount), 0) FROM payment WHERE DATE(paid_at) = CURDATE()";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)};
        }
    }
}
