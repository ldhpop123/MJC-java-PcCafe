package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.Menu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MenuDAO {

    public void insert(Menu m) throws SQLException {
        String sql = "INSERT INTO menu (name, price, category, stock) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setInt(2, m.getPrice());
            ps.setString(3, m.getCategory());
            ps.setInt(4, m.getStock());
            ps.executeUpdate();
        }
    }

    public List<Menu> findAll() throws SQLException {
        String sql = "SELECT * FROM menu ORDER BY category, name";
        List<Menu> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Menu(rs.getInt("menu_id"), rs.getString("name"), rs.getInt("price"),
                        rs.getString("category"), rs.getInt("stock")));
            }
        }
        return list;
    }

    public void update(Menu m) throws SQLException {
        String sql = "UPDATE menu SET name = ?, price = ?, category = ?, stock = ? WHERE menu_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setInt(2, m.getPrice());
            ps.setString(3, m.getCategory());
            ps.setInt(4, m.getStock());
            ps.setInt(5, m.getMenuId());
            ps.executeUpdate();
        }
    }

    public void delete(int menuId) throws SQLException {
        String sql = "DELETE FROM menu WHERE menu_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, menuId);
            ps.executeUpdate();
        }
    }
}
