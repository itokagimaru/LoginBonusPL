package io.github.itokagimaru.loginBonusPL.servise;

import com.zaxxer.hikari.HikariDataSource;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.LoginBonusEventDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.PlayerLoginDAO;
import io.github.itokagimaru.loginBonusPL.MySQL.DAO.RewardDAO;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import io.github.itokagimaru.loginBonusPL.loginBonus.PlayerLoginProgress;
import org.bukkit.inventory.ItemStack;

public class LoginBonusService {
    ExecutorService dbExecutor;//DAOから引っ張ってくるのもありかも...
    private final HikariDataSource dataSource;
    private final LoginBonusEventDAO eventDAO;
    private final RewardDAO rewardDAO;
    private final PlayerLoginDAO playerLoginDAO;

    public LoginBonusService(ExecutorService dbExecutor, HikariDataSource dataSource, LoginBonusEventDAO eventDAO, RewardDAO rewardDAO, PlayerLoginDAO playerLoginDAO) {
        this.dbExecutor = dbExecutor;
        this.dataSource = dataSource;
        this.eventDAO = eventDAO;
        this.rewardDAO = rewardDAO;
        this.playerLoginDAO = playerLoginDAO;
    }

    public CompletableFuture<LoginBonusEvent> createNewLoginBonusEvent() {
        String name = "new LoginBonus";
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);
        return eventDAO.createEvent(name, startDate, endDate, false).thenApply(id -> {
            return new LoginBonusEvent(id, name, startDate, endDate, false, null);
        });
    }

    public CompletableFuture<LoginBonusEvent> getEventWithRewards(int id) {
        return eventDAO.getEventById(id)
                .thenCompose(baseEvent -> {
                    if (baseEvent == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return rewardDAO.getRewardsByEventId(id)
                            .thenApply(rewards ->
                                    new LoginBonusEvent(
                                            baseEvent.getId(),
                                            baseEvent.getName(),
                                            baseEvent.getStartDate(),
                                            baseEvent.getEndDate(),
                                            baseEvent.isActive(),
                                            rewards
                                    )
                            );
                });
    }

    public CompletableFuture<Map<Integer, LoginBonusEvent>> getAllEvents() throws SQLException {
        return eventDAO.getAllEvents()
                .thenCompose(baseEvents -> {

                    Map<Integer, CompletableFuture<LoginBonusEvent>> futureMap = new HashMap<>();

                    for (LoginBonusEvent baseEvent : baseEvents) {
                        int id = baseEvent.getId();
                        futureMap.put(id, getEventWithRewards(id));
                    }

                    CompletableFuture<Void> allFutures =
                            CompletableFuture.allOf(
                                    futureMap.values()
                                            .toArray(new CompletableFuture[0])
                            );

                    return allFutures.thenApply(v -> {
                        Map<Integer, LoginBonusEvent> result = new HashMap<>();
                        for (Map.Entry<Integer, CompletableFuture<LoginBonusEvent>> entry : futureMap.entrySet()) {
                            result.put(entry.getKey(), entry.getValue().join());
                        }
                        return result;
                    });
                });
    }

    public CompletableFuture<Void> updateAllEvents(List<LoginBonusEvent> events) {

        return CompletableFuture.runAsync(() -> {

            try (Connection conn = dataSource.getConnection()) {

                conn.setAutoCommit(false);

                try {
                    for (LoginBonusEvent event : events) {

                        eventDAO.updateEvent(conn, event);

                        for (Map.Entry<Integer, List<ItemStack>> entry :
                                event.getRewards().entrySet()) {

                            int day = entry.getKey();
                            List<ItemStack> items = entry.getValue();

                            updateReward(conn, event.getId(), day, items);
                        }
                    }

                    conn.commit();

                } catch (Exception e) {
                    conn.rollback();
                    throw new CompletionException(e);
                }

            } catch (Exception e) {
                throw new CompletionException(e);
            }

        });
    }

    public CompletableFuture<Void> updateReward(Connection conn, int eventId, int day, List<ItemStack> items) throws SQLException {
        return rewardDAO.saveOrUpdateReward(conn, eventId, day, items);
    }

    public CompletableFuture<Void> updateEventAndReward(LoginBonusEvent event) {

        return CompletableFuture.runAsync(() -> {

            try (Connection conn = dataSource.getConnection()) {

                conn.setAutoCommit(false);

                try {
                    eventDAO.updateEvent(conn, event);

                    rewardDAO.deleteAllByEventId(conn, event.getId());

                    for (Map.Entry<Integer, List<ItemStack>> entry :
                            event.getRewards().entrySet()) {

                        rewardDAO.saveOrUpdateReward(
                                conn,
                                event.getId(),
                                entry.getKey(),
                                entry.getValue()
                        );
                    }

                    conn.commit();

                } catch (Exception e) {
                    conn.rollback();
                    throw new CompletionException(e);
                }

            } catch (Exception e) {
                throw new CompletionException(e);
            }

        }, dbExecutor);
    }

    public CompletableFuture<Void> deleteEventAndReward(LoginBonusEvent event) {

        return CompletableFuture.runAsync(() -> {
            int eventId = event.getId();
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    rewardDAO.deleteAllByEventId(conn, eventId);
                    eventDAO.deleteById(conn, eventId);

                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw new CompletionException(e);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> createPlayerLogin(UUID playerUUID, LoginBonusEvent loginBonusEvent) {
        return playerLoginDAO
                .find(playerUUID, loginBonusEvent.getId())
                .thenCompose(optional -> {

                    if (optional.isPresent()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return playerLoginDAO.upsert(
                            playerUUID,
                            loginBonusEvent.getId(),
                            LocalDate.now().minusDays(1),
                            0,
                            0
                    );
                });
    }

    public CompletableFuture<Optional<PlayerLoginProgress>> getPlayerLoginProgress(UUID playerUUID, LoginBonusEvent loginBonusEvent) {
        return playerLoginDAO.find(playerUUID, loginBonusEvent.getId());
    }

    public CompletableFuture<Void> updatePlayerLoginProgress(UUID playerUUID, PlayerLoginProgress playerLoginProgress) {
        return playerLoginDAO.upsert(playerUUID, playerLoginProgress.getEventId(), playerLoginProgress.getLastLoginDate(), playerLoginProgress.getContinuousDays(),playerLoginProgress.getTotalLoginDays());
    }

    public CompletableFuture<Void> deletePlayerLogin(UUID playerUUID, LoginBonusEvent loginBonusEvent) {
        return playerLoginDAO.deleteByUUIDAndEvent(playerUUID, loginBonusEvent.getId());
    }

    public CompletableFuture<Void> deletePlayerLogin(UUID playerUUID) {
        return playerLoginDAO.deleteByUUID(playerUUID);
    }

    public CompletableFuture<Void> deleteAllPlayerLogin(int eventID) {
        return playerLoginDAO.deleteByEvent(eventID);
    }
}

