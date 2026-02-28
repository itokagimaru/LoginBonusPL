package io.github.itokagimaru.loginBonusPL.MySQL.DAO;

import io.github.itokagimaru.loginBonusPL.util.ItemStackUtil;
import org.bukkit.inventory.ItemStack;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RewardDAO {
    ExecutorService dbExecutor;
    private final DataSource dataSource;

    public RewardDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CompletableFuture<Map<Integer, List<ItemStack>>> getRewardsByEventId(int eventId) throws RuntimeException {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
            SELECT day, reward_data
            FROM login_bonus_reward
            WHERE event_id = ?
        """;

            Map<Integer, List<ItemStack>> rewards = new HashMap<>();

            try {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, eventId);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int day = rs.getInt("day");
                            String yaml = rs.getString("reward_data");

                            rewards.put(day, ItemStackUtil.fromYaml(yaml));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return rewards;
        }, dbExecutor);

    }

    public CompletableFuture<Void> saveOrUpdateReward(Connection conn,
                                   int eventId,
                                   int day,
                                   List<ItemStack> items)
            throws RuntimeException {
        return CompletableFuture.runAsync(() -> {
            String yaml = ItemStackUtil.toYaml(items);

            String sql = """
                INSERT INTO login_bonus_reward (event_id, day, reward_data)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                reward_data = VALUES(reward_data)
            """;

            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setInt(1, eventId);
                    ps.setInt(2, day);
                    ps.setString(3, yaml);

                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, dbExecutor);

    }

    public CompletableFuture<Void> deleteAllByEventId(Connection conn, int eventId) throws RuntimeException {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM login_bonus_reward WHERE event_id = ?";

            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, eventId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, dbExecutor);
    }

}
