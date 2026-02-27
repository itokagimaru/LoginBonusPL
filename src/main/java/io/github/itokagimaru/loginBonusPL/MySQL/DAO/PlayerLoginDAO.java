package io.github.itokagimaru.loginBonusPL.MySQL.DAO;

import io.github.itokagimaru.loginBonusPL.loginBonus.PlayerLoginProgress;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class PlayerLoginDAO {

    private final DataSource dataSource;

    public PlayerLoginDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ------------------------
    // 取得
    // ------------------------
    public Optional<PlayerLoginProgress> find(UUID uuid, int eventId) throws SQLException {

        String sql = """
            SELECT last_login_date, continuous_days, total_login_days
            FROM player_login_progress
            WHERE uuid = ? AND event_id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setInt(2, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                Date sqlDate = rs.getDate("last_login_date");
                LocalDate lastLoginDate = sqlDate != null ? sqlDate.toLocalDate() : null;

                return Optional.of(new PlayerLoginProgress(
                        uuid,
                        eventId,
                        lastLoginDate,
                        rs.getInt("continuous_days"),
                        rs.getInt("total_login_days")
                ));
            }
        }
    }

    // ------------------------
    // 保存 / 更新
    // ------------------------
    public void upsert(UUID uuid,
                       int eventId,
                       LocalDate lastLoginDate,
                       int continuousDays,
                       int totalLoginDays) throws SQLException {

        String sql = """
            INSERT INTO player_login_progress
            (uuid, event_id, last_login_date, continuous_days, total_login_days)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                last_login_date = VALUES(last_login_date),
                continuous_days = VALUES(continuous_days),
                total_login_days = VALUES(total_login_days)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setInt(2, eventId);

            if (lastLoginDate != null) {
                ps.setDate(3, Date.valueOf(lastLoginDate));
            } else {
                ps.setNull(3, Types.DATE);
            }

            ps.setInt(4, continuousDays);
            ps.setInt(5, totalLoginDays);

            ps.executeUpdate();
        }
    }

    // ------------------------
    // 削除
    // ------------------------
    public void deleteByUUIDAndEvent(UUID uuid, int eventId) throws SQLException {

        String sql = """
            DELETE FROM player_login_progress
            WHERE uuid = ? AND event_id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setInt(2, eventId);
            ps.executeUpdate();
        }
    }

    public void deleteByUUID(UUID uuid) throws SQLException {
        String sql = """
            DELETE FROM player_login_progress
            WHERE uuid = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void deleteByEvent(int eventId) throws SQLException {
        String sql = """
            DELETE FROM player_login_progress
            WHERE event_id = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }
    }
}
