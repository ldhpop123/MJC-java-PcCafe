package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.TimeProduct;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TimeProductDAO {

    public List<TimeProduct> findAll() throws SQLException {
        String sql = "SELECT * FROM time_product ORDER BY minutes";
        List<TimeProduct> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TimeProduct(rs.getInt("product_id"), rs.getInt("minutes"), rs.getInt("price")));
            }
        }
        return list;
    }

    public void insert(TimeProduct p) throws SQLException {
        String sql = "INSERT INTO time_product (minutes, price) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getMinutes());
            ps.setInt(2, p.getPrice());
            ps.executeUpdate();
        }
    }

    public void update(TimeProduct p) throws SQLException {
        String sql = "UPDATE time_product SET minutes = ?, price = ? WHERE product_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getMinutes());
            ps.setInt(2, p.getPrice());
            ps.setInt(3, p.getProductId());
            ps.executeUpdate();
        }
    }

    public void delete(int productId) throws SQLException {
        String sql = "DELETE FROM time_product WHERE product_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }
}
