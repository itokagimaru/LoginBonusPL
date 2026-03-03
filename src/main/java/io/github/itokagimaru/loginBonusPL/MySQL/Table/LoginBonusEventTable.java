package io.github.itokagimaru.loginBonusPL.MySQL.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class LoginBonusEventTable {
    ExecutorService dbExecutor;
    private final DataSource dataSource;

    public LoginBonusEventTable(ExecutorService dbExecutor, DataSource dataSource) {
        this.dbExecutor = dbExecutor;
        this.dataSource = dataSource;
    }

    public CompletableFuture<Void> createTable() throws RuntimeException {
        return CompletableFuture.runAsync(() -> {
            String sql = """
            CREATE TABLE IF NOT EXISTS login_bonus_event (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(64) NOT NULL,
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                max_day_count INT NOT NULL,
                is_active BOOLEAN NOT NULL
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
