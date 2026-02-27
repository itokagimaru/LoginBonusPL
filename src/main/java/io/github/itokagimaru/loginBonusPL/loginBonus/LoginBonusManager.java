package io.github.itokagimaru.loginBonusPL.loginBonus;

import com.zaxxer.hikari.HikariDataSource;
import io.github.itokagimaru.loginBonusPL.servise.LoginBonusService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class LoginBonusManager {
    private Map<Integer,LoginBonusEvent> loginBonusList;
    LoginBonusService loginBonusService;
    AltAccountService altAccountService;
    HikariDataSource dataSource;

    public LoginBonusManager(HikariDataSource dataSource, LoginBonusService loginBonusService, AltAccountService altAccountService) throws SQLException {
        this.dataSource = dataSource;
        this.loginBonusService = loginBonusService;
        this.altAccountService = altAccountService;
        loginBonusList = loginBonusService.getAllEvents();
    }

    public void createNewLoginBonus() throws SQLException {
        LoginBonusEvent newEvent = loginBonusService.createNewLoginBonusEvent();
        loginBonusList.put(newEvent.getId(), newEvent);
    }

    public void saveLoginBonus(LoginBonusEvent loginBonus) throws SQLException {
        loginBonusService.updateEventAndReward(loginBonus);
        loginBonusList.put(loginBonus.getId(), loginBonus);
    }

    public LoginBonusEvent loadLoginBonusFromDB(int eventId) throws SQLException {
        return loginBonusService.getEventWithRewards(eventId);
    }

    public void deleteLoginBonus(LoginBonusEvent event) throws SQLException {
        loginBonusService.deleteEventAndReward(event);
        loginBonusList.remove(event.getId());
    }

    public PlayerLoginProgress getOrCreatePlayerLoginProgress(UUID playerUUID, LoginBonusEvent event) throws SQLException {
        return loginBonusService.getPlayerLoginProgress(playerUUID, event);
    }

    public void updatePlayerLoginProgress(UUID playerUUID, PlayerLoginProgress playerLoginProgress) throws SQLException {
        loginBonusService.updatePlayerLoginProgress(playerUUID, playerLoginProgress);
    }

    public boolean deletePlayerLoginProgress(UUID playerUUID, LoginBonusEvent event) throws SQLException {
        return loginBonusService.deletePlayerLogin(playerUUID, event);
    }

    public boolean deletePlayerLoginProgress(UUID playerUUID) throws SQLException {
        return loginBonusService.deletePlayerLogin(playerUUID);
    }

    public boolean deleteAllPlayerLoginProgress(int eventId) throws SQLException {
        return loginBonusService.deleteAllPlayerLogin(eventId);
    }

    public boolean isAltAccountRestricted(){
        return altAccountService.isEnabled();
    }

    public List<UUID> getAltAccounts(UUID playerUUID) throws SQLException {
        return altAccountService.getAltAccountList(playerUUID);
    }

    public List<PlayerLoginProgress> getAltAccountLoginProgress(UUID playerUUID, LoginBonusEvent event) throws SQLException {
        List<PlayerLoginProgress> progresses = new ArrayList<>();
        List<UUID> altAccounts = getAltAccounts(playerUUID);
        if (!altAccounts.contains(playerUUID)) altAccounts.add(playerUUID);
        for (UUID altAccountUUID : altAccounts) {
            progresses.add(getOrCreatePlayerLoginProgress(altAccountUUID, event));
        }
        return progresses;
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

    //ゲッターってやつ
    public Map<Integer,LoginBonusEvent> getLoginBonusList() {
        return loginBonusList;
    }
}
