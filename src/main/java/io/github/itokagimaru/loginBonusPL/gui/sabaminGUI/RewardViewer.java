package io.github.itokagimaru.loginBonusPL.gui.sabaminGUI;

import io.github.itokagimaru.loginBonusPL.gui.BaseGuiHolder;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RewardViewer extends BaseGuiHolder {
    LoginBonusManager loginBonusManager;
    LoginBonusEvent loginBonusEvent;
    int day;

    NamespacedKey key = new NamespacedKey("loginbonus", "rewardviewer_icon");
    private enum IconID {
        NULL_ICON("null_icon"),
        NEXT("next"),
        BACK("back"),
        REWARD("reward"),;
        String id;
        IconID(String id) {
            this.id = id;
        }
        private String getId() {
            return id;
        }
        private static IconID fromString(String id) {
            for (IconID iconID : IconID.values()) {
                if (iconID.getId().equals(id)) {
                    return iconID;
                }
            }
            return NULL_ICON;
        }
    }

    int INV_SIZE = 27;

    public RewardViewer(int day, LoginBonusEvent loginBonusEvent, LoginBonusManager loginBonusManager) {
        this.loginBonusEvent = loginBonusEvent;
        this.loginBonusManager = loginBonusManager;
        this.day = day;
        inv = Bukkit.createInventory(this, INV_SIZE, Component.text(loginBonusEvent.getName() + " / " + day + "日目"));
        setup();
    }

    private void setup() {
        ItemStack nullIcon = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        nullIcon.editMeta(meta -> {
            meta.customName(Component.text(""));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.NULL_ICON.getId());
        });
        for (int i = 0; i < INV_SIZE; i++) {
            inv.setItem(i, nullIcon);
        }
        ItemStack next = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        next.editMeta(meta -> {
            meta.customName(Component.text("次の日付へ"));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.NEXT.getId());
        });
        ItemStack back = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        back.editMeta(meta -> {
            meta.customName(Component.text("前の日付へ"));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.BACK.getId());
        });
        for (int i = 0; i < INV_SIZE/9; i++) {
            inv.setItem(i * 9, back);
            inv.setItem(i * 9 + 8, next);
        }
        List<ItemStack> rewards = loginBonusEvent.getRewardByDay(day);
        if (rewards == null || rewards.isEmpty()) return;
        for (int i = 0; i < INV_SIZE/9; i++) {
            for (int j = 1; j < 8; j++) {
                int slot = i * 9 + j;
                int rewardIndex = slot -1;
                if (rewardIndex >= rewards.size()) return;
                ItemStack reward = rewards.get(rewardIndex).clone();
                reward.editMeta(meta -> {
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.REWARD.getId());
                });
                inv.setItem(slot, reward);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        IconID iconID = IconID.fromString(item.getPersistentDataContainer().get(key, PersistentDataType.STRING));
        switch (iconID) {
            case NULL_ICON -> {}
            case NEXT -> {
                if (loginBonusEvent.getMaxDayCount() <= day) return;
                day++;
                closeFlag = false;
                RewardViewer rewardViewer = new RewardViewer(day, loginBonusEvent, loginBonusManager);
                player.openInventory(rewardViewer.getInventory());
            }
            case BACK -> {
                if (1 >= day) return;
                day--;
                closeFlag = false;
                RewardViewer rewardViewer = new RewardViewer(day, loginBonusEvent, loginBonusManager);
                player.openInventory(rewardViewer.getInventory());
            }
        }
    }

    @Override
    public void onClose(Player player) {
        if (!closeFlag) return;
        closeFlag = false;
        RewardCalendar rewardCalendar = new RewardCalendar(loginBonusManager, loginBonusEvent);
        player.openInventory(rewardCalendar.getInventory());
    }
}
