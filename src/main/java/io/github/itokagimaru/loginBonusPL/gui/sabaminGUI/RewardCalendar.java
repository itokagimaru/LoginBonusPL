package io.github.itokagimaru.loginBonusPL.gui.sabaminGUI;

import io.github.itokagimaru.loginBonusPL.LoginBonusPL;
import io.github.itokagimaru.loginBonusPL.gui.BaseGuiHolder;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class RewardCalendar extends BaseGuiHolder {
    LoginBonusManager loginBonusManager;
    LoginBonusEvent loginBonusEvent;

    NamespacedKey iconKey = new NamespacedKey("loginbonus", "reward_calendar_icon");
    NamespacedKey dayKey = new NamespacedKey("loginbonus", "reward_calendar_day");

    private enum IconID{
        REWARD_ICON("reward_icon"),
        NEXT("next"),
        BACK("back"),;
        final String id;
        IconID(String id){
            this.id = id;
        }
        private String getId(){
            return id;
        }
        private static IconID fromString(String id){
            for(IconID iconID : IconID.values()){
                if(id.equals(iconID.getId())){
                    return iconID;
                }
            }
            return null;
        }
    }

    int INV_SIZE = 54;
    int pagePerReward = INV_SIZE/9 * 7;
    int page = 0;

    public RewardCalendar(LoginBonusManager loginBonusManager, LoginBonusEvent loginBonusEvent) {
        this.loginBonusManager = loginBonusManager;
        this.loginBonusEvent = loginBonusEvent;
        inv = Bukkit.createInventory(this, INV_SIZE, Component.text("Login Bonus: " + loginBonusEvent.getName()));
        setup();
    }

    private void setup() {
        inv.clear();
        int day = pagePerReward * page + 1;//1オリジンがいいから+1
        for (int i = 0; i < INV_SIZE; i++) {
            int slotX = (i % 9);
            if (slotX == 0 || slotX == 8) {
                ItemStack nullIcon = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                nullIcon.editMeta(meta -> {
                    if (slotX == 0){
                        meta.customName(Component.text("前のページへ"));
                        meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.BACK.getId());
                    } else {
                        meta.customName(Component.text("次のページへ"));
                        meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.NEXT.getId());
                    }

                });
                inv.setItem(i, nullIcon);
                continue;
            }
            ItemStack dayIcon = new ItemStack(Material.CHEST);
            int finalDay = day;
            dayIcon.editMeta(meta -> {
                meta.customName(Component.text(finalDay + "日目"));
                meta.lore(List.of(
                        Component.text("左クリック : 報酬内容確認").color(NamedTextColor.YELLOW)
                ));
                meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.REWARD_ICON.getId());
                meta.getPersistentDataContainer().set(dayKey, PersistentDataType.INTEGER, finalDay);
                meta.setMaxStackSize(99);
            });
            dayIcon.setAmount(day);
            if (!(day > loginBonusEvent.getMaxDayCount())) inv.setItem(i, dayIcon);
            day++;
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        IconID iconID = IconID.fromString(item.getPersistentDataContainer().get(iconKey, PersistentDataType.STRING));
        if (iconID == null) return;
        switch (iconID) {
            case REWARD_ICON -> {
                closeFlag = false;
                RewardViewer rewardViewer = new RewardViewer(item.getPersistentDataContainer().get(dayKey, PersistentDataType.INTEGER), loginBonusEvent, loginBonusManager);
                player.openInventory(rewardViewer.getInventory());
            }
            case NEXT -> {
                if (pagePerReward * (page + 1) + 1 >= loginBonusEvent.getMaxDayCount()) return;
                page++;
                setup();
            }
            case BACK -> {
                if (page <= 0) return;
                page--;
                setup();
            }
        }
    }

    @Override
    public void onClose(Player player) {
        if (!closeFlag) return;
        closeFlag = false;
        LoginbonusMenu loginbonusMenu = new LoginbonusMenu(loginBonusManager);
        Bukkit.getScheduler().runTask(LoginBonusPL.getInstance(), () -> {
            player.openInventory(loginbonusMenu.getInventory());
        });
    }
}
