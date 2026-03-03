package io.github.itokagimaru.loginBonusPL.MySQL.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class LoginBonusRewardTable {
    private final ExecutorService dbExecutor;
    private final DataSource dataSource;

    public LoginBonusRewardTable(ExecutorService dbExecutor, DataSource dataSource) {
        this.dbExecutor = dbExecutor;
        this.dataSource = dataSource;
    }

    public CompletableFuture<Void> createTable() throws RuntimeException {
        return CompletableFuture.runAsync(() -> {
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

            try {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sql);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, dbExecutor);

    }
}
