package pccafe.dao;

import pccafe.dto.PaymentDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// 결제 상세는 항상 결제와 함께 저장하므로, 모든 메서드가 트랜잭션의 Connection을 받는다
public class PaymentDetailDAO {

    public void insert(Connection conn, int payId, PaymentDetail d) throws SQLException {
        String sql = "INSERT INTO payment_detail (pay_id, menu_id, product_id, item_name, qty, unit_price, subtotal) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payId);
            setNullableInt(ps, 2, d.getMenuId());
            setNullableInt(ps, 3, d.getProductId());
            ps.setString(4, d.getItemName());
            ps.setInt(5, d.getQty());
            ps.setInt(6, d.getUnitPrice());
            ps.setInt(7, d.getSubtotal());
            ps.executeUpdate();
        }
        d.setPayId(payId);
    }

    public List<PaymentDetail> findByPayId(Connection conn, int payId) throws SQLException {
        String sql = "SELECT * FROM payment_detail WHERE pay_id = ? ORDER BY detail_id";
        List<PaymentDetail> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentDetail d = new PaymentDetail(
                            (Integer) rs.getObject("menu_id"), (Integer) rs.getObject("product_id"),
                            rs.getString("item_name"), rs.getInt("qty"), rs.getInt("unit_price"));
                    d.setDetailId(rs.getInt("detail_id"));
                    d.setPayId(payId);
                    list.add(d);
                }
            }
        }
        return list;
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
