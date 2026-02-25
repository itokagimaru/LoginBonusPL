package io.github.itokagimaru.loginBonusPL.MySQL.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerLoginProgressTable {
    private final DataSource dataSource;

    public PlayerLoginProgressTable(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_login_progress (
                uuid VARCHAR(36) NOT NULL,
                event_id INT NOT NULL,
                last_login_date DATE,
                continuous_days INT NOT NULL,
                total_login_days INT NOT NULL,
                PRIMARY KEY (uuid, event_id),
                FOREIGN KEY (event_id)
                    REFERENCES login_bonus_event(value)
                    ON DELETE CASCADE
            );
        """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}
