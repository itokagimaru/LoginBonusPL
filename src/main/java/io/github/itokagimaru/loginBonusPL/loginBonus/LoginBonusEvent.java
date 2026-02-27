package io.github.itokagimaru.loginBonusPL.loginBonus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
        if (loginBonusManager.isAltAccountRestricted()) { //サブ垢対策が有効なら...
            List<PlayerLoginProgress> playerLoginProgressList;
            try {
                playerLoginProgressList = loginBonusManager.getAltAccountLoginProgress(player.getUniqueId(), this);
            } catch (SQLException e) {
                player.sendMessage(Component.text("ログイン情報の取得に失敗したため、ログイン処理を停止しました").color(NamedTextColor.RED));
                player.sendMessage(Component.text("時間を開けて再度ログインしてください"));
                //todo プレイヤーには見えない形でエラーを吐くように.他のところも直す
                return;
            }
            if (loginBonusManager.getLastLoginDate(playerLoginProgressList).isBefore(LocalDate.now())) {
                if (loginBonusManager.getMaxTotalLogins(playerLoginProgressList) >= maxDayCount) {
                    player.sendMessage(Component.text("このログインボーナスの報酬は全て受取済みです").color(NamedTextColor.RED));
                    return;
                }
                try {
                    updatePlayerLoginProgressForAltAccount(playerLoginProgressList, loginBonusManager);
                } catch (SQLException e) {
                    player.sendMessage(Component.text("ログイン情報の更新に失敗したため、ログイン処理を停止しました").color(NamedTextColor.RED));
                    player.sendMessage(Component.text("時間を開けて再度ログインしてください"));
                    // todo エラー文を吐くようにする
                    return;
                }
                giveRewardItem(loginBonusManager.getMaxTotalLogins(playerLoginProgressList), player);
            } else {
                player.sendMessage(Component.text("本日のログインボーナスは受取済みです").color(NamedTextColor.RED));
                return;
            }
        } else {//サブ垢対策が無効なら...
            PlayerLoginProgress playerLoginProgress;
            try {
                playerLoginProgress = loginBonusManager.getOrCreatePlayerLoginProgress(player.getUniqueId(), this);
            } catch (SQLException e) {
                player.sendMessage(Component.text("ログイン情報の取得に失敗したため、ログイン処理を停止しました").color(NamedTextColor.RED));
                player.sendMessage(Component.text("時間を開けて再度ログインしてください"));
                // todo エラー文を吐くようにする
                return;
            }

            LocalDate lastLoginDate = playerLoginProgress.getLastLoginDate();
            if (lastLoginDate.isBefore(LocalDate.now())) {
                if (playerLoginProgress.getTotalLoginDays() >= maxDayCount) {
                    player.sendMessage(Component.text("このログインボーナスの報酬は全て受取済みです").color(NamedTextColor.RED));
                    return;
                }
                playerLoginProgress.addTotalLoginDays();
                playerLoginProgress.setLastLoginDate(LocalDate.now());
                int betweenDay;
                try {
                    betweenDay = Math.toIntExact(ChronoUnit.DAYS.between(lastLoginDate, LocalDate.now()));
                } catch (ArithmeticException e){
                    betweenDay = 2;
                }
                if (betweenDay == 1) {
                    playerLoginProgress.addContinuousDays();
                }
                try {
                    loginBonusManager.updatePlayerLoginProgress(player.getUniqueId(), playerLoginProgress);
                } catch (SQLException e) {
                    player.sendMessage(Component.text("ログイン情報の更新に失敗したため、ログイン処理を停止しました").color(NamedTextColor.RED));
                    player.sendMessage(Component.text("時間を開けて再度ログインしてください"));
                    // todo エラー文を吐くようにする
                    return;
                }
                giveRewardItem(playerLoginProgress.getTotalLoginDays(),  player);
            } else {
                player.sendMessage(Component.text("本日のログインボーナスは受取済みです").color(NamedTextColor.RED));
            }
        }



    }

    private void giveRewardItem(int day, Player player) {
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

    //メソッド名長すぎ良いのを思いついたら短くする
    private void updatePlayerLoginProgressForAltAccount(List<PlayerLoginProgress> altAccountList, LoginBonusManager loginBonusManager) throws SQLException {
        LocalDate lastLoginDate = loginBonusManager.getLastLoginDate(altAccountList);
        int totalLoginDays = loginBonusManager.getMaxTotalLogins(altAccountList);
        int continuousDays = loginBonusManager.getMaxContinuousDays(altAccountList);
        int betweenDay;
        try {
            betweenDay = Math.toIntExact(ChronoUnit.DAYS.between(lastLoginDate, LocalDate.now()));
        } catch (ArithmeticException e){
            betweenDay = 2;
        }
        for (PlayerLoginProgress progress : altAccountList) {
            progress.setLastLoginDate(LocalDate.now());
            progress.setTotalLoginDays(totalLoginDays + 1);
            if (betweenDay == 1) progress.setContinuousDays(continuousDays + 1);
            else progress.setContinuousDays(continuousDays);
            loginBonusManager.updatePlayerLoginProgress(progress.getUuid(), progress);
        }
    }

}
