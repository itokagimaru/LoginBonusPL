package io.github.itokagimaru.loginBonusPL.servise;

import com.zaxxer.hikari.HikariDataSource;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.LoginBonusEventDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.PlayerLoginDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.RewardDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.HikariManager;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import io.github.itokagimaru.loginBonusPL.loginBonus.PlayerLoginProgress;
import org.bukkit.inventory.ItemStack;

public class LoginBonusService {

    private final HikariDataSource dataSource;
    private final LoginBonusEventDAO eventDAO;
    private final RewardDAO rewardDAO;
    private final PlayerLoginDAO playerLoginDAO;

    public LoginBonusService(HikariDataSource dataSource, LoginBonusEventDAO eventDAO, RewardDAO rewardDAO, PlayerLoginDAO playerLoginDAO) {
        this.dataSource = dataSource;
        this.eventDAO = eventDAO;
        this.rewardDAO = rewardDAO;
        this.playerLoginDAO = playerLoginDAO;
    }

    public LoginBonusEvent createNewLoginBonusEvent() throws SQLException {
        String name = "new LoginBonus";
        LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        LocalDate endDate = LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(30);
        int id = eventDAO.createEvent(name, startDate, endDate, false);
        return new LoginBonusEvent(id, name, startDate, endDate, false, null);
    }

    public LoginBonusEvent getEventWithRewards(int id) throws SQLException {

        LoginBonusEvent baseEvent = eventDAO.getEventById(id);
        if (baseEvent == null) return null;

        Map<Integer, List<ItemStack>> rewards =
                rewardDAO.getRewardsByEventId(id);

        return new LoginBonusEvent(
                baseEvent.getId(),
                baseEvent.getName(),
                baseEvent.getStartDate(),
                baseEvent.getEndDate(),
                baseEvent.isActive(),
                rewards
        );
    }

    public Map<Integer, LoginBonusEvent> getAllEvents() throws SQLException {
        Map<Integer, LoginBonusEvent> returnEvents = new HashMap<>();
        for (LoginBonusEvent baseEvent : eventDAO.getAllEvents()) {
            int id = baseEvent.getId();
            returnEvents.put(id, getEventWithRewards(id));
        }
        return returnEvents;
    }

    public void updateAllEvents(List<LoginBonusEvent> events) throws SQLException {

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            for (LoginBonusEvent event : events) {
                eventDAO.updateEvent(conn, event);// event情報のアップデートめそっと
                for (Map.Entry<Integer, List<ItemStack>> entry : event.getRewards().entrySet()) {

                    int day = entry.getKey();
                    List<ItemStack> items = entry.getValue();

                    updateReward(conn, event.getId(), day, items);
                }
            }

            conn.commit();
        }
    }

    public void updateReward(Connection conn, int eventId, int day, List<ItemStack> items) throws SQLException {
        rewardDAO.saveOrUpdateReward(conn, eventId, day, items);
    }

    public void updateEventAndReward(LoginBonusEvent event) throws SQLException {

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                eventDAO.updateEvent(conn, event);
                rewardDAO.deleteAllByEventId(conn, event.getId());
                for (Map.Entry<Integer, List<ItemStack>> entry : event.getRewards().entrySet()) {
                    rewardDAO.saveOrUpdateReward(
                            conn,
                            event.getId(),
                            entry.getKey(),
                            entry.getValue()
                    );
                }

                conn.commit();
                return;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean deleteEventAndReward(LoginBonusEvent event) {
        int eventId = event.getId();
        try (Connection conn = dataSource.getConnection()) {

            conn.setAutoCommit(false);

            try {

                rewardDAO.deleteAllByEventId(conn, eventId);
                eventDAO.deleteById(conn, eventId);

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                return false;
            }

        } catch (SQLException e) {
            return false;
        }
    }

    public boolean createPlayerLogin(UUID playerUUID, LoginBonusEvent loginBonusEvent) throws SQLException {
        PlayerLoginProgress playerLoginProgress = playerLoginDAO.find(playerUUID, loginBonusEvent.getId()).orElse(null);
        if (playerLoginProgress != null) return false;
        playerLoginDAO.upsert(playerUUID, loginBonusEvent.getId(), LocalDate.now().minusDays(1), 0,0);
        return true;
    }

    public PlayerLoginProgress getPlayerLoginProgress(UUID playerUUID, LoginBonusEvent loginBonusEvent) throws SQLException {
        PlayerLoginProgress playerLoginProgress = playerLoginDAO.find(playerUUID, loginBonusEvent.getId()).orElse(null);
        if (playerLoginProgress != null) return playerLoginProgress;
        if (createPlayerLogin(playerUUID, loginBonusEvent)) return getPlayerLoginProgress(playerUUID, loginBonusEvent);
        throw new SQLException();
    }

    public void updatePlayerLoginProgress(UUID playerUUID, PlayerLoginProgress playerLoginProgress) throws SQLException {
        playerLoginDAO.upsert(playerUUID, playerLoginProgress.getEventId(), playerLoginProgress.getLastLoginDate(), playerLoginProgress.getContinuousDays(),playerLoginProgress.getTotalLoginDays());
    }

    public boolean deletePlayerLogin(UUID playerUUID, LoginBonusEvent loginBonusEvent) throws SQLException {
        PlayerLoginProgress playerLoginProgress = playerLoginDAO.find(playerUUID, loginBonusEvent.getId()).orElse(null);
        if (playerLoginProgress == null) return false;
        playerLoginDAO.delete(playerUUID, loginBonusEvent.getId());
        return true;
    }
}

