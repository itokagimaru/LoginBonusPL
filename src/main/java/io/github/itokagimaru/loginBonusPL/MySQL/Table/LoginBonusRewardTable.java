package io.github.itokagimaru.loginBonusPL.MySQL.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LoginBonusRewardTable {
    private final DataSource dataSource;

    public LoginBonusRewardTable(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS login_bonus_reward (
                value INT AUTO_INCREMENT PRIMARY KEY,
                event_id INT NOT NULL,
                day INT NOT NULL,
                reward_data TEXT NOT NULL,
                UNIQUE (event_id, day),
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
