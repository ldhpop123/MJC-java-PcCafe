package pccafe.dao;

import pccafe.db.DBUtil;
import pccafe.dto.ChatMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatDAO {

    public void insert(ChatMessage m) throws SQLException {
        String sql = "INSERT INTO chat_message (member_id, seat_no, sender, content) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getMemberId());
            ps.setInt(2, m.getSeatNo());
            ps.setString(3, m.getSender());
            ps.setString(4, m.getContent());
            ps.executeUpdate();
        }
    }

    // 한 손님과 나눈 최근 대화 limit개를 오래된 순서로 돌려준다
    public List<ChatMessage> findRecent(String memberId, int limit) throws SQLException {
        String sql = "SELECT * FROM chat_message WHERE member_id = ? ORDER BY msg_id DESC LIMIT ?";
        List<ChatMessage> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatMessage m = new ChatMessage(rs.getString("member_id"), rs.getInt("seat_no"),
                            rs.getString("sender"), rs.getString("content"));
                    m.setMsgId(rs.getInt("msg_id"));
                    m.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
                    list.add(m);
                }
            }
        }
        Collections.reverse(list);   // 최신 limit개를 뽑은 뒤 화면에는 시간 순서로
        return list;
    }
}
