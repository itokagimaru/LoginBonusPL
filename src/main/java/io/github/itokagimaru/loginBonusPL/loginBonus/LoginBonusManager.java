package io.github.itokagimaru.loginBonusPL.loginBonus;

import com.zaxxer.hikari.HikariDataSource;
import io.github.itokagimaru.loginBonusPL.servise.LoginBonusService;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class LoginBonusManager {
    private Map<Integer,LoginBonusEvent> loginBonusList;
    LoginBonusService loginBonusService;
    HikariDataSource dataSource;

    public LoginBonusManager(HikariDataSource dataSource, LoginBonusService loginBonusService) throws SQLException {
        this.dataSource = dataSource;
        this.loginBonusService = loginBonusService;
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

    //ゲッターってやつ
    public Map<Integer,LoginBonusEvent> getLoginBonusList() {
        return loginBonusList;
    }
}
