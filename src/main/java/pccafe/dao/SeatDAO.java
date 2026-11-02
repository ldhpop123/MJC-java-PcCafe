package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SeatDAO {

    public List<Seat> findAll() throws SQLException {
        String sql = "SELECT * FROM seat ORDER BY seat_no";
        List<Seat> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Seat(rs.getInt("seat_no"), rs.getString("status"), rs.getString("member_id")));
            }
        }
        return list;
    }

    public List<Integer> findEmptySeatNos() throws SQLException {
        String sql = "SELECT seat_no FROM seat WHERE status = 'EMPTY' ORDER BY seat_no";
        List<Integer> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getInt("seat_no"));
            }
        }
        return list;
    }

    // 빈 좌석일 때만 사용 시작. 다른 손님이 먼저 앉았으면 false
    public boolean startUse(int seatNo, String memberId) throws SQLException {
        String sql = "UPDATE seat SET status = 'USING', member_id = ? WHERE seat_no = ? AND status = 'EMPTY'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId);
            ps.setInt(2, seatNo);
            return ps.executeUpdate() == 1;
        }
    }

    public void endUse(int seatNo) throws SQLException {
        String sql = "UPDATE seat SET status = 'EMPTY', member_id = NULL WHERE seat_no = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seatNo);
            ps.executeUpdate();
        }
    }

    // 프로그램 시작 시 호출: 지난번 비정상 종료로 남은 '사용중' 상태를 비운다
    public void resetAll() throws SQLException {
        String sql = "UPDATE seat SET status = 'EMPTY', member_id = NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
