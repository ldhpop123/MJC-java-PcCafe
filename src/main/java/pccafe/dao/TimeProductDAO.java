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
}
