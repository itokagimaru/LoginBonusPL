package io.github.itokagimaru.loginBonusPL.MySQL.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LoginBonusEventTable {
    private final DataSource dataSource;

    public LoginBonusEventTable(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createTable() throws SQLException {
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

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}
