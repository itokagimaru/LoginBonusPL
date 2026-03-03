package io.github.itokagimaru.loginBonusPL.loginBonus;

import com.zaxxer.hikari.HikariDataSource;
import io.github.itokagimaru.loginBonusPL.servise.LoginBonusService;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LoginBonusManager {
    private Map<Integer,LoginBonusEvent> loginBonusList;
    LoginBonusService loginBonusService;
    AltAccountService altAccountService;
    HikariDataSource dataSource;
    Plugin plugin;

    public LoginBonusManager(HikariDataSource dataSource, LoginBonusService loginBonusService, AltAccountService altAccountService, Plugin plugin) throws SQLException {
        this.dataSource = dataSource;
        this.loginBonusService = loginBonusService;
        this.altAccountService = altAccountService;
        loginBonusList = loginBonusService.getAllEvents().join();//サーバー起動時のみの挙動の為 joinで待ってもいい
        this.plugin = plugin;
    }

    public CompletableFuture<Void> createNewLoginBonus() {
        return loginBonusService.createNewLoginBonusEvent().thenAccept(newEvent -> {
            loginBonusList.put(newEvent.getId(), newEvent);
        });
    }

    public CompletableFuture<Void> saveLoginBonus(LoginBonusEvent loginBonus) {
        return loginBonusService.updateEventAndReward(loginBonus).thenRun(() -> {
            loginBonusList.put(loginBonus.getId(), loginBonus);
        });
    }

    public CompletableFuture<LoginBonusEvent> loadLoginBonusFromDB(int eventId) {
        return loginBonusService.getEventWithRewards(eventId);
    }

    public CompletableFuture<Void> deleteLoginBonus(LoginBonusEvent event) {
        return loginBonusService.deleteEventAndReward(event).thenRun(() -> {
            loginBonusList.remove(event.getId());
        });

    }

    public CompletableFuture<PlayerLoginProgress>
    getOrCreatePlayerLoginProgress(UUID playerUUID, LoginBonusEvent event) {

        return loginBonusService.getPlayerLoginProgress(playerUUID, event)
                .thenCompose(optional -> {
                    if (optional.isPresent()) {
                        return CompletableFuture.completedFuture(optional.get());
                    }
                    return loginBonusService.createPlayerLogin(playerUUID, event)
                            .thenCompose(v ->
                                    loginBonusService.getPlayerLoginProgress(playerUUID, event)
                            )
                            .thenApply(Optional::get);
                });
    }

    public CompletableFuture<Void> updatePlayerLoginProgress(UUID playerUUID, PlayerLoginProgress playerLoginProgress) {
        return loginBonusService.updatePlayerLoginProgress(playerUUID, playerLoginProgress);
    }

    public CompletableFuture<Void> deletePlayerLoginProgress(UUID playerUUID, LoginBonusEvent event) {
        return loginBonusService.deletePlayerLogin(playerUUID, event);
    }

    public CompletableFuture<Void> deletePlayerLoginProgress(UUID playerUUID) {
        return loginBonusService.deletePlayerLogin(playerUUID);
    }

    public CompletableFuture<Void> deleteAllPlayerLoginProgress(int eventId) {
        return loginBonusService.deleteAllPlayerLogin(eventId);
    }

    public boolean isAltAccountRestricted(){
        return altAccountService.isEnabled();
    }

    public CompletableFuture<List<UUID>> getAltAccounts(UUID playerUUID) {
        return altAccountService.getAltAccountList(playerUUID);
    }

    public CompletableFuture<List<PlayerLoginProgress>>
    getAltAccountLoginProgress(UUID playerUUID, LoginBonusEvent event) {
        return getAltAccounts(playerUUID)
                .thenCompose(altAccounts -> {

                    if (!altAccounts.contains(playerUUID)) {
                        altAccounts.add(playerUUID);
                    }

                    List<CompletableFuture<PlayerLoginProgress>> futures =
                            altAccounts.stream()
                                    .map(uuid -> getOrCreatePlayerLoginProgress(uuid, event))
                                    .toList();

                    return CompletableFuture
                            .allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v ->
                                    futures.stream()
                                            .map(CompletableFuture::join)
                                            .toList()
                            );
                });
    }

    public LocalDate getLastLoginDate(List<PlayerLoginProgress> loginProgresses) {
        LocalDate lastLoginDate  = null;
        for (PlayerLoginProgress loginProgress : loginProgresses) {
            if (lastLoginDate == null) {
                lastLoginDate = loginProgress.getLastLoginDate();
                continue;
            }
            if (loginProgress.getLastLoginDate().isAfter(lastLoginDate)) {
                lastLoginDate = loginProgress.getLastLoginDate();
            }
        }
        if (lastLoginDate == null) return LocalDate.now().minusDays(1);
        return lastLoginDate;
    }

    public int getMaxTotalLogins(List<PlayerLoginProgress> loginProgresses) {
        int max = -1;
        for (PlayerLoginProgress loginProgress : loginProgresses) {
            max = Math.max(max, loginProgress.getTotalLoginDays());
        }
        return max;
    }

    public int getMaxContinuousDays(List<PlayerLoginProgress> loginProgresses) {
        int max = -1;
        for (PlayerLoginProgress loginProgress : loginProgresses) {
            max = Math.max(max, loginProgress.getContinuousDays());
        }
        return max;
    }

    public Map<Integer,LoginBonusEvent> getLoginBonusList() {
        return loginBonusList;
    }

    public void outPutError(String message) {
        plugin.getLogger().warning(message);
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
