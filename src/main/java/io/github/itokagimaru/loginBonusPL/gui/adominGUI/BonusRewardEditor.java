package io.github.itokagimaru.loginBonusPL.gui.adominGUI;

import io.github.itokagimaru.loginBonusPL.LoginBonusPL;
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

public class BonusRewardEditor extends BaseGuiHolder {
    private final LoginBonusManager loginBonusManager;
    private final LoginBonusEvent loginBonusEvent;
    int day;

    NamespacedKey key = new NamespacedKey("loginbonus", "reward_editor");
    enum IconId{
        NEXT("next"),
        BACK("back"),
        REWARD("reward"),
        NULL_ICON("null_icon"),;
        private final String value;
        IconId(String value){
            this.value = value;
        }
        public String getValue(){
            return value;
        }
        private static IconId fromValue(String value){
            for(IconId id : IconId.values()){
                if(id.getValue().equals(value)){
                    return id;
                }
            }
            return null;
        }
    }

    int INV_SIZE = 27;
    String TITLE = "Bonus Reward Editor";

    public BonusRewardEditor(LoginBonusEvent event, int day, LoginBonusManager loginBonusManager) {
        this.loginBonusManager = loginBonusManager;
        this.loginBonusEvent = new LoginBonusEvent(event);
        this.day = day;

        inv = Bukkit.createInventory(this, INV_SIZE, Component.text(TITLE));
        setup(day);
    }

    private void setup(int day) {
        ItemStack nullIcon = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        nullIcon.editMeta(meta -> {
            meta.customName(Component.text(""));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconId.NULL_ICON.getValue());
        });
        for (int i = 0; i < INV_SIZE; i++) {
            inv.setItem(i, nullIcon);
        }
        ItemStack next = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        next.editMeta(meta -> {
            meta.customName(Component.text("次の日付へ"));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconId.NEXT.getValue());
        });
        ItemStack back = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        back.editMeta(meta -> {
            meta.customName(Component.text("前の日付へ"));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconId.BACK.getValue());
        });
        for (int i = 0; i < INV_SIZE/9; i++) {
            inv.setItem(i * 9, next);
            inv.setItem(i * 9 + 8, back);
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
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconId.REWARD.getValue());
                });
                inv.setItem(slot, reward);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (item == null || item.getType().equals(Material.AIR)) return;
        event.setCancelled(true);
        IconId iconId = IconId.fromValue(item.getPersistentDataContainer().get(key, PersistentDataType.STRING));
        switch (iconId) {
            case null -> {
                List<ItemStack> rewards = loginBonusEvent.getRewardByDay(day);
                if (rewards == null) {
                    rewards = new ArrayList<>();
                }
                rewards.add(item.clone());
                loginBonusEvent.setReward(day, rewards);
                setup(day);
            }
            case NEXT -> {
                if (day < 99) day++;
                else day = 1;
                setup(day);
            }
            case BACK -> {
                if (day > 1) day--;
                else day = 99;
                setup(day);
            }
            case REWARD -> {
                List<ItemStack> rewards = loginBonusEvent.getRewards().get(day);
                if (rewards == null) return;
                item.editMeta(meta -> {
                    meta.getPersistentDataContainer().remove(key);
                });
                rewards.remove(item);
                for (ItemStack itemStack : rewards) player.sendMessage(itemStack.toString());
                loginBonusEvent.setReward(day, rewards);
                setup(day);
            }
            case NULL_ICON -> {}
        }
    }

    @Override
    public void onClose(Player player) {
        if (closeFlag) {
            closeFlag = false;
            BonusEventEditor bonusEventEditor;
            bonusEventEditor = new BonusEventEditor(loginBonusEvent, loginBonusManager);
            Bukkit.getScheduler().runTask(LoginBonusPL.getInstance(), () -> {
                player.openInventory(bonusEventEditor.getInventory());
            });
        }
    }
}
