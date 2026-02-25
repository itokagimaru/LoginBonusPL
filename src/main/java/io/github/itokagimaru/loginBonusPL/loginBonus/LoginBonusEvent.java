package io.github.itokagimaru.loginBonusPL.loginBonus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginBonusEvent {

    private final int id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxDayCount = 0;
    private boolean active;
    private final Map<Integer, List<ItemStack>> rewards;

    public LoginBonusEvent(int id,
                           String name,
                           LocalDate startDate,
                           LocalDate endDate,
                           boolean active,
                           Map<Integer, List<ItemStack>> rewards) {

        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        this.rewards = new HashMap<>();
        if (rewards == null) return;
        int max = 0;
        for (Map.Entry<Integer, List<ItemStack>> entry : rewards.entrySet()) {

            List<ItemStack> copiedList = new ArrayList<>();

            for (ItemStack item : entry.getValue()) {
                copiedList.add(item.clone());
            }

            this.rewards.put(entry.getKey(), copiedList);
            max = Math.max(max, entry.getKey());
        }
        this.maxDayCount = max;
    }

    public LoginBonusEvent(LoginBonusEvent other) {
        this.id = other.id;
        this.name = other.name;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.active = other.active;
        this.maxDayCount = other.maxDayCount;

        this.rewards = new HashMap<>();
        if (other.getRewards() == null) return;
        for (Map.Entry<Integer, List<ItemStack>> entry : other.rewards.entrySet()) {

            List<ItemStack> copiedList = new ArrayList<>();

            for (ItemStack item : entry.getValue()) {
                copiedList.add(item.clone());
            }

            this.rewards.put(entry.getKey(), copiedList);
        }
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public Map<Integer, List<ItemStack>> getRewards() {
        return rewards;
    }
    public List<ItemStack> getRewardByDay(int day) {
        if (rewards != null) return rewards.get(day);
        return null;
    }
    public void setReward(int day, List<ItemStack> item) {
        if (item == null || isEmpty(item)) rewards.remove(day);
        else rewards.put(day, item);
        int max = 0;
        for (Map.Entry<Integer, List<ItemStack>> entry : rewards.entrySet()) {
            max = Math.max(max, entry.getKey());
        }
        this.maxDayCount = max;
    }

    private boolean isEmpty(List<ItemStack> items) {
        if (items == null || items.isEmpty()) return true;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    public int getMaxDayCount() {
        return maxDayCount;
    }
    public void giveReward(Player player, LoginBonusManager loginBonusManager) {
        PlayerLoginProgress loginProgress;
        try {
            loginProgress = loginBonusManager.getOrCreatePlayerLoginProgress(player.getUniqueId(), this);
        } catch (SQLException e) {
            player.sendMessage(Component.text("ログイン情報の取得に失敗しました :").color(NamedTextColor.RED));
            player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            return;
        }
        int betweenDay;
        try {
            betweenDay = Math.toIntExact(ChronoUnit.DAYS.between(loginProgress.getLastLoginDate(), LocalDate.now()));
        } catch (ArithmeticException e){
            betweenDay = 2;
        }
        if (betweenDay > 0) {
            loginProgress.addTotalLoginDays();
            if (betweenDay == 1) loginProgress.addContinuousDays();
            else loginProgress.setContinuousDays(0);
            loginProgress.setLastLoginDate(LocalDate.now());
            try {
                loginBonusManager.updatePlayerLoginProgress(loginProgress.getUuid(), loginProgress);
            } catch (SQLException e) {
                player.sendMessage(Component.text("報酬の受け取りに失敗しました").color(NamedTextColor.RED));
                return;
            }
            giveReward(loginProgress.getTotalLoginDays(), player);
        }
    }

    private void giveReward(int day, Player player) {
        if (this.getMaxDayCount() < day) return;
        List<ItemStack> items = rewards.get(day);
        if (countEmptySlots(player) < items.size()) {
            player.sendMessage(Component.text("インベントリに空きがありません").color(NamedTextColor.RED));
            player.sendMessage(Component.text("空きスロットを " + items.size() + " スロット以上確保してください").color(NamedTextColor.RED));
            return;
        }

        for (ItemStack item : items) {
            player.give(item);
            Component itemName = item.getItemMeta().customName();
            if (itemName == null) itemName = Component.text(item.getType().name());
            player.sendMessage(Component.text("[LoginBonus:" + this.name + " / " + day + "日目] ").color(NamedTextColor.YELLOW).append(itemName.append(Component.text(" を受け取りました").color(NamedTextColor.YELLOW))));
        }
    }

    public int countEmptySlots(Player player) {
        int count = 0;

        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                count++;
            }
        }

        return count;
    }
}
