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
    // 게스트 창마다 SeatDAO를 따로 만들기 때문에, 모든 창이 같은 자물쇠를 쓰도록 static으로 둔다.
    // 좌석 상태를 바꾸는 메서드는 이 자물쇠를 잡은 스레드 하나만 실행할 수 있다.
    private static final Object LOCK = new Object();

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
        synchronized (LOCK) {
            String sql = "UPDATE seat SET status = 'USING', member_id = ? WHERE seat_no = ? AND status = 'EMPTY'";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, memberId);
                ps.setInt(2, seatNo);
                return ps.executeUpdate() == 1;
            }
        }
    }

    public void endUse(int seatNo) throws SQLException {
        synchronized (LOCK) {
            String sql = "UPDATE seat SET status = 'EMPTY', member_id = NULL WHERE seat_no = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, seatNo);
                ps.executeUpdate();
            }
        }
    }

    // 프로그램 시작 시 호출: 지난번 비정상 종료로 남은 '사용중' 상태를 비운다
    public void resetAll() throws SQLException {
        synchronized (LOCK) {
            String sql = "UPDATE seat SET status = 'EMPTY', member_id = NULL";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        }
    }

    // 마지막 번호 다음 번호로 빈 좌석을 하나 만들고 그 번호를 돌려준다
    public int insertNext() throws SQLException {
        synchronized (LOCK) {
            String maxSql = "SELECT COALESCE(MAX(seat_no), 0) + 1 FROM seat";
            String insertSql = "INSERT INTO seat (seat_no) VALUES (?)";
            try (Connection conn = DBUtil.getConnection()) {
                int next;
                try (PreparedStatement ps = conn.prepareStatement(maxSql);
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    next = rs.getInt(1);
                }
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, next);
                    ps.executeUpdate();
                }
                return next;
            }
        }
    }

    // 빈 좌석일 때만 삭제. 사용 중이라 지우지 못했으면 false
    public boolean deleteIfEmpty(int seatNo) throws SQLException {
        synchronized (LOCK) {
            String sql = "DELETE FROM seat WHERE seat_no = ? AND status = 'EMPTY'";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, seatNo);
                return ps.executeUpdate() == 1;
            }
        }
    }
}
