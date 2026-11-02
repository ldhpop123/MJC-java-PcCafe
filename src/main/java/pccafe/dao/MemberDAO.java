package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public void insert(Member m) throws SQLException {
        String sql = "INSERT INTO member (id, password, name, phone, remain_seconds) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getPassword());
            ps.setString(3, m.getName());
            ps.setString(4, m.getPhone());
            ps.setInt(5, m.getRemainSeconds());
            ps.executeUpdate();
        }
    }

    public Member findById(String id) throws SQLException {
        String sql = "SELECT * FROM member WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // 아이디/비밀번호가 맞으면 회원, 틀리면 null
    public Member login(String id, String password) throws SQLException {
        String sql = "SELECT * FROM member WHERE id = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsById(String id) throws SQLException {
        return findById(id) != null;
    }

    public List<Member> findAll() throws SQLException {
        String sql = "SELECT * FROM member ORDER BY created_at";
        List<Member> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public void update(Member m) throws SQLException {
        String sql = "UPDATE member SET password = ?, name = ?, phone = ?, remain_seconds = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getPassword());
            ps.setString(2, m.getName());
            ps.setString(3, m.getPhone());
            ps.setInt(4, m.getRemainSeconds());
            ps.setString(5, m.getId());
            ps.executeUpdate();
        }
    }

    public void updateRemainSeconds(String id, int seconds) throws SQLException {
        String sql = "UPDATE member SET remain_seconds = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seconds);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM member WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    private Member map(ResultSet rs) throws SQLException {
        Member m = new Member(rs.getString("id"), rs.getString("password"),
                rs.getString("name"), rs.getString("phone"));
        m.setRemainSeconds(rs.getInt("remain_seconds"));
        m.setAdmin(rs.getBoolean("is_admin"));
        return m;
    }
}
