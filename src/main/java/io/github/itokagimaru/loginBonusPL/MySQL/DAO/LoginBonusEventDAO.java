package io.github.itokagimaru.loginBonusPL.MySQL.DAO;

import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class LoginBonusEventDAO {
    ExecutorService dbExecutor;
    private final DataSource dataSource;

    public LoginBonusEventDAO(ExecutorService dbExecutor, DataSource dataSource) {
        this.dbExecutor = dbExecutor;
        this.dataSource = dataSource;
    }

    public CompletableFuture<Integer> createEvent(String name,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  boolean active) throws RuntimeException {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
            INSERT INTO login_bonus_event (name, start_date, end_date,max_day_count, is_active)
            VALUES (?, ?, ?, ?, ?)
            """;
            try {
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
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, dbExecutor);

    }

    public CompletableFuture<LoginBonusEvent> getEventById(int value) throws RuntimeException {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM login_bonus_event WHERE id = ?";
            try {
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
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, dbExecutor);
    }

    public CompletableFuture<List<LoginBonusEvent>> getAllEvents() throws RuntimeException {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM login_bonus_event ORDER BY id ASC";

            List<LoginBonusEvent> events = new ArrayList<>();

            try {
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
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return events;
        }, dbExecutor);

    }

    public CompletableFuture<Void> updateEvent(Connection conn, LoginBonusEvent event) throws RuntimeException {
        return CompletableFuture.runAsync(() -> {
            String sql = """
            UPDATE login_bonus_event
            SET name = ?, start_date = ?, end_date = ?, max_day_count = ?, is_active = ?
            WHERE id = ?
            """;

            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, event.getName());
                    ps.setDate(2, Date.valueOf(event.getStartDate()));
                    ps.setDate(3, Date.valueOf(event.getEndDate()));
                    ps.setInt(4, event.getMaxDayCount());
                    ps.setBoolean(5, event.isActive());
                    ps.setInt(6, event.getId());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> deleteById(Connection conn, int eventId) throws RuntimeException {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM login_bonus_event WHERE id = ?";

            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, eventId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, dbExecutor);

    }

}
