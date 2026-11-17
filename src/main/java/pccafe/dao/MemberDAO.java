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
    // 회원 검색 기준
    public static final String BY_NAME = "이름";
    public static final String BY_PHONE_LAST4 = "전화번호 뒤 4자리";
    public static final String BY_ID = "아이디";

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

    // 기준(by)에 맞는 회원. keyword가 빈 문자열이면 전체
    public List<Member> search(String by, String keyword) throws SQLException {
        String where;
        String pattern;
        if (BY_PHONE_LAST4.equals(by)) {
            // 010-1234-5678과 01012345678을 똑같이 찾도록 '-'를 빼고 끝자리를 비교
            where = "REPLACE(phone, '-', '') LIKE ?";
            pattern = "%" + keyword;
        } else if (BY_ID.equals(by)) {
            where = "id LIKE ?";
            pattern = "%" + keyword + "%";
        } else {
            where = "name LIKE ?";
            pattern = "%" + keyword + "%";
        }
        String sql = "SELECT * FROM member WHERE " + where + " ORDER BY created_at";
        List<Member> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
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

    // 결제할 때: 포인트 use만큼 쓰고 earn만큼 적립한다. 보유 포인트가 use보다 적으면 false
    public boolean usePoint(Connection conn, String id, int use, int earn) throws SQLException {
        String sql = "UPDATE member SET point = point - ? + ? WHERE id = ? AND point >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, use);
            ps.setInt(2, earn);
            ps.setString(3, id);
            ps.setInt(4, use);
            return ps.executeUpdate() == 1;
        }
    }

    // 환불할 때: 포인트를 delta만큼 바꾼다. 결과가 음수가 되면(적립분을 이미 써 버렸으면) 바꾸지 않고 false
    public boolean addPoint(Connection conn, String id, int delta) throws SQLException {
        String sql = "UPDATE member SET point = point + ? WHERE id = ? AND point + ? >= 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setString(2, id);
            ps.setInt(3, delta);
            return ps.executeUpdate() == 1;
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
        m.setPoint(rs.getInt("point"));
        return m;
    }
}
