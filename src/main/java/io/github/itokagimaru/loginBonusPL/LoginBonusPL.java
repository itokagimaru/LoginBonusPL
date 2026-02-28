package io.github.itokagimaru.loginBonusPL;

import io.github.itokagimaru.loginBonusPL.MySQL.DAO.ConnectionLogDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.LoginBonusEventDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.PlayerLoginDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.RewardDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.HikariManager;
import io.github.itokagimaru.loginBonusPL.MySQL.LoginBonusTableInitializer;
import io.github.itokagimaru.loginBonusPL.command.LoginBonusCommand;
import io.github.itokagimaru.loginBonusPL.command.LoginBonusOPCommand;
import io.github.itokagimaru.loginBonusPL.gui.listener.ClickInventoryListener;
import io.github.itokagimaru.loginBonusPL.gui.listener.CloseInventoryListeners;
import io.github.itokagimaru.loginBonusPL.listener.PlayerJoinListener;
import io.github.itokagimaru.loginBonusPL.loginBonus.AltAccountService;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import io.github.itokagimaru.loginBonusPL.servise.LoginBonusService;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LoginBonusPL extends JavaPlugin {

    private HikariManager loginBonusHikariManager;
    private HikariManager altAccountHikariManager;
    private static LoginBonusPL instance;
    private LuckPerms luckPerms;
    private LoginBonusManager loginBonusManager;
    private ExecutorService dbExecutor;

    @Override
    public void onEnable() {
        // config.yml が存在しなければ生成
        saveDefaultConfig();

        // config読み込み
        String bonusHost = getConfig().getString("loginBonusDatabase.host");
        int bonusPort = getConfig().getInt("loginBonusDatabase.port");
        String bonusName = getConfig().getString("loginBonusDatabase.name");
        String bonusUsername = getConfig().getString("loginBonusDatabase.username");
        String bonusPassword = getConfig().getString("loginBonusDatabase.password");
        int bonusPoolSize = getConfig().getInt("loginBonusDatabase.pool-size");

        boolean altAccountEnabled = getConfig().getBoolean("altAccountDatabase.enabled");
        String altAccountHost = getConfig().getString("altAccountDatabase.host");
        int altAccountPort = getConfig().getInt("altAccountDatabase.port");
        String altAccountName = getConfig().getString("altAccountDatabase.name");
        String altAccountUsername = getConfig().getString("altAccountDatabase.username");
        String altAccountPassword = getConfig().getString("altAccountDatabase.password");
        int altAccountPoolSize = getConfig().getInt("altAccountDatabase.pool-size");
        String altAccountTableName = getConfig().getString("altAccountDatabase.table-name");
        String altAccountColumnUUIDName = getConfig().getString("altAccountDatabase.column.uuid");
        String altAccountColumnIPName = getConfig().getString("altAccountDatabase.column.ip");

        // Hikari初期化
        loginBonusHikariManager = new HikariManager(
                bonusHost,
                bonusPort,
                bonusName,
                bonusUsername,
                bonusPassword,
                bonusPoolSize
        );
        altAccountHikariManager = new HikariManager(
                altAccountHost,
                altAccountPort,
                altAccountName,
                altAccountUsername,
                altAccountPassword,
                altAccountPoolSize
        );

        // テーブル生成
        try {
            new LoginBonusTableInitializer(
                    loginBonusHikariManager.getDataSource()
            ).initialize();

            getLogger().info("データベース接続 & テーブル初期化完了");

        } catch (Exception e) {
            getLogger().severe("データベース初期化失敗");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        // 非同期処理のためのexecutorの作成
        dbExecutor = Executors.newFixedThreadPool(4);

        // サブ垢対策用のDAO+クラスの作成
        AltAccountService altAccountService = new AltAccountService(null, false);//私のサーバではサブ垢対策用のDBにアクセスできないので他機能テスト時にエラーを吐くためon/off機能が欲しかった
        if (altAccountEnabled) {
            try {
                ConnectionLogDAO connectionLogDAO = new ConnectionLogDAO(dbExecutor, altAccountHikariManager.getDataSource(), altAccountTableName, altAccountColumnUUIDName, altAccountColumnIPName);
                altAccountService = new AltAccountService(connectionLogDAO, true);
            } catch (RuntimeException e) {
                getLogger().warning("AltAccountDataBase への接続に失敗: " + e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
            }
        }

        // DAOの作成,サービス層,マネージャの作成
        try{
            LoginBonusEventDAO loginBonusEventDAO = new LoginBonusEventDAO(loginBonusHikariManager.getDataSource());
            RewardDAO eventRewardDAO = new RewardDAO(loginBonusHikariManager.getDataSource());
            PlayerLoginDAO playerLoginDAO = new PlayerLoginDAO(loginBonusHikariManager.getDataSource());
            LoginBonusService loginBonusService = new LoginBonusService(loginBonusHikariManager.getDataSource(), loginBonusEventDAO, eventRewardDAO, playerLoginDAO);
            loginBonusManager = new LoginBonusManager(loginBonusHikariManager.getDataSource(), loginBonusService, altAccountService, this);
        } catch (SQLException e) {
            getLogger().warning("LoginBonusDataBase への接続に失敗: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }



        // LuckPerms の起動
        RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);

        if (provider != null) {
            luckPerms = provider.getProvider();
        } else {
            getLogger().severe("LuckPerms not found!");
            getServer().getPluginManager().disablePlugin(this);
        }

        //command の登録
        if (loginBonusManager != null) {
            registerCommandWithTabCompleter("loginbonusop", new LoginBonusOPCommand(loginBonusManager, luckPerms), new LoginBonusOPCommand(loginBonusManager, luckPerms));
            registerCommandWithTabCompleter("loginbonus", new LoginBonusCommand(loginBonusManager, altAccountService), new LoginBonusCommand(loginBonusManager, altAccountService));
        }


        // Listener の登録
        registerListeners(
            new ClickInventoryListener(),
            new CloseInventoryListeners(),
            new PlayerJoinListener(loginBonusManager)
        );

        instance = this;
    }

    @Override
    public void onDisable() {
        if (loginBonusHikariManager != null) {
            loginBonusHikariManager.shutdown();
        }
        if (altAccountHikariManager != null) {
            altAccountHikariManager.shutdown();
        }
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
    }

    private void registerListeners(Listener... listeners) {
        PluginManager pluginManager = getServer().getPluginManager();
        for (Listener listener : listeners) {
            pluginManager.registerEvents(listener, this);
        }
    }

    private void registerCommandWithTabCompleter(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getSLF4JLogger().warn("コマンド {} が見つかりませんでした", name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }


    public static LoginBonusPL getInstance() {
        return instance;
    }
}
