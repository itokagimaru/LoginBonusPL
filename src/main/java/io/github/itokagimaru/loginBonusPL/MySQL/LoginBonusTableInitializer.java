package io.github.itokagimaru.loginBonusPL.MySQL;

import io.github.itokagimaru.loginBonusPL.MySQL.Table.LoginBonusEventTable;
import io.github.itokagimaru.loginBonusPL.MySQL.Table.LoginBonusRewardTable;
import io.github.itokagimaru.loginBonusPL.MySQL.Table.PlayerLoginProgressTable;

import javax.sql.DataSource;
import java.sql.SQLException;

public class LoginBonusTableInitializer {
    private final DataSource dataSource;

    public LoginBonusTableInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() throws SQLException {
        new LoginBonusEventTable(dataSource).createTable();
        new LoginBonusRewardTable(dataSource).createTable();
        new PlayerLoginProgressTable(dataSource).createTable();
    }
}
