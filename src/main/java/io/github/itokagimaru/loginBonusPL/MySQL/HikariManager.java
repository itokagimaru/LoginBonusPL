package io.github.itokagimaru.loginBonusPL.MySQL;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariManager {

    private final HikariDataSource dataSource;

    public HikariManager(String host, int port, String database,
                         String username, String password, int poolSize) {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC"
                + "&characterEncoding=utf8");
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void shutdown() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

