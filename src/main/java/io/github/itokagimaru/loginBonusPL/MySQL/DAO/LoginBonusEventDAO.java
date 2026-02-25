package io.github.itokagimaru.loginBonusPL.MySQL.DAO;

import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoginBonusEventDAO {

    private final DataSource dataSource;

    public LoginBonusEventDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int createEvent(String name,
                           LocalDate startDate,
                           LocalDate endDate,
                           boolean active) throws SQLException {

        String sql = """
            INSERT INTO login_bonus_event (name, start_date, end_date,max_day_count, is_active)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            ps.setInt(4, 0);
            ps.setBoolean(5, active);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Failed to create event");
    }

    public LoginBonusEvent getEventById(int value) throws SQLException {

        String sql = "SELECT * FROM login_bonus_event WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, value);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new LoginBonusEvent(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getBoolean("is_active"),
                            null // rewardsはServiceで詰める
                    );
                }
            }
        }

        return null;
    }

    public List<LoginBonusEvent> getAllEvents() throws SQLException {

        String sql = "SELECT * FROM login_bonus_event ORDER BY id ASC";

        List<LoginBonusEvent> events = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                events.add(new LoginBonusEvent(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getBoolean("is_active"),
                        null
                ));
            }
        }

        return events;
    }

    public void updateEvent(Connection conn, LoginBonusEvent event) throws SQLException {

        String sql = """
        UPDATE login_bonus_event
        SET name = ?, start_date = ?, end_date = ?, max_day_count = ?, is_active = ?
        WHERE id = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.getName());
            ps.setDate(2, Date.valueOf(event.getStartDate()));
            ps.setDate(3, Date.valueOf(event.getEndDate()));
            ps.setInt(4, event.getMaxDayCount());
            ps.setBoolean(5, event.isActive());
            ps.setInt(6, event.getId());
            ps.executeUpdate();
        }
    }

    public void deleteById(Connection conn, int eventId) throws SQLException {

        String sql = "DELETE FROM login_bonus_event WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }
    }

}
