package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // 트랜잭션 안에서 호출한다. 새로 만든 order_id를 돌려준다
    public int insert(Connection conn, Order o) throws SQLException {
        String sql = "INSERT INTO orders (pay_id, member_id, seat_no, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getPayId());
            ps.setString(2, o.getMemberId());
            ps.setInt(3, o.getSeatNo());
            ps.setString(4, o.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int orderId = keys.getInt(1);
                o.setOrderId(orderId);
                return orderId;
            }
        }
    }

    // 대기 주문 먼저, 그 안에서는 최근 주문 먼저.
    // 품목은 payment_detail에서 "라면 2개, 콜라 1개"로 묶고, 금액은 결제 당시 금액(payment.amount)을 쓴다
    public List<Order> findAll() throws SQLException {
        String sql = "SELECT o.order_id, o.pay_id, o.member_id, o.seat_no, o.status, o.ordered_at, "
                + "MAX(p.amount) AS amount, "
                + "GROUP_CONCAT(CONCAT(d.item_name, ' ', d.qty, '개') ORDER BY d.detail_id SEPARATOR ', ') AS items "
                + "FROM orders o "
                + "LEFT JOIN payment p ON p.pay_id = o.pay_id "
                + "LEFT JOIN payment_detail d ON d.pay_id = o.pay_id "
                + "GROUP BY o.order_id "
                + "ORDER BY o.status <> 'WAIT', o.ordered_at DESC";
        List<Order> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Order o = new Order(rs.getInt("pay_id"), rs.getString("member_id"), rs.getInt("seat_no"));
                o.setOrderId(rs.getInt("order_id"));
                o.setStatus(rs.getString("status"));
                o.setOrderedAt(rs.getTimestamp("ordered_at").toLocalDateTime());
                o.setAmount(rs.getInt("amount"));
                o.setItems(rs.getString("items"));
                list.add(o);
            }
        }
        return list;
    }

    // 트랜잭션 안에서 취소할 주문을 읽는다. FOR UPDATE로 잠가서 두 관리자가 동시에 취소하지 못하게 한다
    public Order findForUpdate(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Order o = new Order(rs.getInt("pay_id"), rs.getString("member_id"), rs.getInt("seat_no"));
                o.setOrderId(orderId);
                o.setStatus(rs.getString("status"));
                return o;
            }
        }
    }

    // 대기(WAIT) 중인 주문만 상태를 바꾼다. 이미 처리된 주문이면 false
    public boolean updateStatusIfWaiting(Connection conn, int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ? AND status = 'WAIT'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateStatusIfWaiting(int orderId, String status) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            return updateStatusIfWaiting(conn, orderId, status);
        }
    }
}
