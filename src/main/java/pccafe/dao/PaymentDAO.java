package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PaymentDAO {

    public void insert(Payment p) throws SQLException {
        String sql = "INSERT INTO payment (member_id, type, amount) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getMemberId());
            ps.setString(2, p.getType());
            ps.setInt(3, p.getAmount());
            ps.executeUpdate();
        }
    }

    // 오늘 매출 합계 (type: Payment.TIME / Payment.FOOD)
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
}
