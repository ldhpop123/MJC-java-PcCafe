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

    // 재고가 충분할 때만 차감. 부족하면 false.
    // 주문 트랜잭션의 Connection으로 실행한다. 동시에 들어오는 주문은 PaymentService가 synchronized로 한 줄로 세운다
    public boolean decreaseStock(Connection conn, int menuId, int qty) throws SQLException {
        String sql = "UPDATE menu SET stock = stock - ? WHERE menu_id = ? AND stock >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, menuId);
            ps.setInt(3, qty);
            return ps.executeUpdate() == 1;
        }
    }

    // 주문 취소 트랜잭션에서 재고를 되돌릴 때
    public void increaseStock(Connection conn, int menuId, int qty) throws SQLException {
        String sql = "UPDATE menu SET stock = stock + ? WHERE menu_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, menuId);
            ps.executeUpdate();
        }
    }

    // 메뉴 관리 탭의 재고 입고
    public void increaseStock(int menuId, int qty) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            increaseStock(conn, menuId, qty);
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
