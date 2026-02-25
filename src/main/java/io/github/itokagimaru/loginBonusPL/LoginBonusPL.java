package io.github.itokagimaru.loginBonusPL;

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

public final class LoginBonusPL extends JavaPlugin {

    private HikariManager hikariManager;
    private static LoginBonusPL instance;
    private LuckPerms luckPerms;
    private LoginBonusManager loginBonusManager;

    @Override
    public void onEnable() {

        // config.yml が存在しなければ生成
        saveDefaultConfig();

        // config読み込み
        String host = getConfig().getString("database.host");
        int port = getConfig().getInt("database.port");
        String name = getConfig().getString("database.name");
        String username = getConfig().getString("database.username");
        String password = getConfig().getString("database.password");
        int poolSize = getConfig().getInt("database.pool-size");

        // Hikari初期化
        hikariManager = new HikariManager(
                host,
                port,
                name,
                username,
                password,
                poolSize
        );

        // テーブル生成
        try {
            new LoginBonusTableInitializer(
                    hikariManager.getDataSource()
            ).initialize();

            getLogger().info("データベース接続 & テーブル初期化完了");

        } catch (Exception e) {
            getLogger().severe("データベース初期化失敗");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        // DAOの作成,サービス層,マネージャの作成
        try{
            LoginBonusEventDAO loginBonusEventDAO = new LoginBonusEventDAO(hikariManager.getDataSource());
            RewardDAO eventRewardDAO = new RewardDAO(hikariManager.getDataSource());
            PlayerLoginDAO playerLoginDAO = new PlayerLoginDAO(hikariManager.getDataSource());
            LoginBonusService loginBonusService = new LoginBonusService(hikariManager.getDataSource(), loginBonusEventDAO, eventRewardDAO, playerLoginDAO);
            loginBonusManager = new LoginBonusManager(hikariManager.getDataSource(), loginBonusService);
        } catch (SQLException e) {
            getLogger().severe("マネージャの作成失敗");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        // LuckPermsの起動
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
            registerCommandWithTabCompleter("loginbonus", new LoginBonusCommand(loginBonusManager), new LoginBonusCommand(loginBonusManager));
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
        if (hikariManager != null) {
            hikariManager.shutdown();
        }
    }

    public HikariManager getHikariManager() {
        return hikariManager;
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
