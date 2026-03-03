package io.github.itokagimaru.loginBonusPL.MySQL;

import io.github.itokagimaru.loginBonusPL.MySQL.Table.LoginBonusEventTable;
import io.github.itokagimaru.loginBonusPL.MySQL.Table.LoginBonusRewardTable;
import io.github.itokagimaru.loginBonusPL.MySQL.Table.PlayerLoginProgressTable;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class LoginBonusTableInitializer {
    ExecutorService dbExecutor;
    private final DataSource dataSource;

    public LoginBonusTableInitializer(ExecutorService dbExecutor, DataSource dataSource) {
        this.dbExecutor = dbExecutor;
        this.dataSource = dataSource;
    }

    public CompletableFuture<Void> initialize() {

        CompletableFuture<Void> eventTableCreation =
                new LoginBonusEventTable(dbExecutor, dataSource).createTable();

        CompletableFuture<Void> rewardTableCreation =
                new LoginBonusRewardTable(dbExecutor, dataSource).createTable();

        CompletableFuture<Void> progressTableCreation =
                new PlayerLoginProgressTable(dbExecutor, dataSource).createTable();

        return CompletableFuture.allOf(
                eventTableCreation,
                rewardTableCreation,
                progressTableCreation
        );
    }
}
