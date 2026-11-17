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
    // 게스트 창과 관리자 탭이 MenuDAO를 각자 만들기 때문에, 모든 인스턴스가 같은 자물쇠를 쓰도록 static으로 둔다.
    // 재고를 바꾸는 메서드는 이 자물쇠를 잡은 스레드 하나만 실행할 수 있다.
    private static final Object LOCK = new Object();

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

    // 재고가 충분할 때만 차감. 부족하면 false
    public boolean decreaseStock(int menuId, int qty) throws SQLException {
        synchronized (LOCK) {
            String sql = "UPDATE menu SET stock = stock - ? WHERE menu_id = ? AND stock >= ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, qty);
                ps.setInt(2, menuId);
                ps.setInt(3, qty);
                return ps.executeUpdate() == 1;
            }
        }
    }

    public void increaseStock(int menuId, int qty) throws SQLException {
        synchronized (LOCK) {
            String sql = "UPDATE menu SET stock = stock + ? WHERE menu_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, qty);
                ps.setInt(2, menuId);
                ps.executeUpdate();
            }
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
