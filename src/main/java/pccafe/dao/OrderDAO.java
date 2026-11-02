package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public void insert(Order o) throws SQLException {
        String sql = "INSERT INTO orders (member_id, seat_no, menu_id, qty, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getMemberId());
            ps.setInt(2, o.getSeatNo());
            ps.setInt(3, o.getMenuId());
            ps.setInt(4, o.getQty());
            ps.setString(5, o.getStatus());
            ps.executeUpdate();
        }
    }

    public List<Order> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY ordered_at";
        List<Order> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = new Order(rs.getString("member_id"), rs.getInt("seat_no"),
                            rs.getInt("menu_id"), rs.getInt("qty"));
                    o.setOrderId(rs.getInt("order_id"));
                    o.setStatus(rs.getString("status"));
                    o.setOrderedAt(rs.getTimestamp("ordered_at").toLocalDateTime());
                    list.add(o);
                }
            }
        }
        return list;
    }

    public void updateStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    public void delete(int orderId) throws SQLException {
        String sql = "DELETE FROM orders WHERE order_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
