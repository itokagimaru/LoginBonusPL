package io.github.itokagimaru.loginBonusPL.MySQL.DAO;

import io.github.itokagimaru.loginBonusPL.util.ItemStackUtil;
import org.bukkit.inventory.ItemStack;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class RewardDAO {

    private final DataSource dataSource;

    public RewardDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insertReward(Connection conn,
                             int eventId,
                             int day,
                             String rewardData) throws SQLException {

        String sql = """
        INSERT INTO login_bonus_reward
        (event_id, day, reward_data)
        VALUES (?, ?, ?)
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, day);
            ps.setString(3, rewardData);
            ps.executeUpdate();
        }
    }


    public Map<Integer, List<ItemStack>> getRewardsByEventId(int eventId)
            throws SQLException {

        String sql = """
            SELECT day, reward_data
            FROM login_bonus_reward
            WHERE event_id = ?
        """;

        Map<Integer, List<ItemStack>> rewards = new HashMap<>();

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

        return rewards;
    }

    public void saveOrUpdateReward(Connection conn, int eventId,
                                   int day,
                                   List<ItemStack> items)
            throws SQLException {

        String yaml = ItemStackUtil.toYaml(items);

        String sql = """
        INSERT INTO login_bonus_reward (event_id, day, reward_data)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE
            reward_data = VALUES(reward_data)
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventId);
            ps.setInt(2, day);
            ps.setString(3, yaml);

            ps.executeUpdate();
        }
    }

    public void deleteAllByEventId(Connection conn, int eventId) throws SQLException {

        String sql = "DELETE FROM login_bonus_reward WHERE event_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }
    }

}
