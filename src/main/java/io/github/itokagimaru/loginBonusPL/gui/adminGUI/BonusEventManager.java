package io.github.itokagimaru.loginBonusPL.gui.adminGUI;

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

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class BonusEventManager extends BaseGuiHolder {

    LoginBonusManager loginBonusManager;

    NamespacedKey iconKey = new NamespacedKey("loginbonus", "bonusmanager");
    NamespacedKey eventIDKey = new NamespacedKey("loginbonus", "eventidkey");

    enum IconID {
        ACTIVE_EVENT("active_event"),
        INACTIVE_EVENT("inactive_event"),;
        final String id;
        IconID(String id) {
            this.id = id;
        }
        public String getId() {
            return id;
        }
        public static IconID fromString(String id) {
            for (IconID iconID : IconID.values()) {
                if (iconID.id.equals(id)) {
                    return iconID;
                }
            }
            return null;
        }
    }

    public BonusEventManager(LoginBonusManager loginBonusManager) {
        this.loginBonusManager = loginBonusManager;
        inv = Bukkit.createInventory(this, 54, Component.text("ログインボーナス開催設定"));
        setup();
    }

    private void setup() {
        int i = 0;
        for (LoginBonusEvent event : loginBonusManager.getLoginBonusList().values()){
            ItemStack eventIcon;

            if (event.isActive()) {
                eventIcon = new ItemStack(Material.CHEST);
                eventIcon.editMeta(meta -> {
                    meta.customName(Component.text(event.getName() + ": active").color(NamedTextColor.YELLOW));
                    meta.lore(makeLore(event));
                    meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.ACTIVE_EVENT.getId());
                    meta.getPersistentDataContainer().set(eventIDKey, PersistentDataType.INTEGER, event.getId());
                });
            } else {
                eventIcon = new ItemStack(Material.ENDER_CHEST);
                eventIcon.editMeta(meta -> {
                    meta.customName(Component.text(event.getName() + ": inactive").color(NamedTextColor.WHITE));
                    meta.lore(makeLore(event));
                    meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.INACTIVE_EVENT.getId());
                    meta.getPersistentDataContainer().set(eventIDKey, PersistentDataType.INTEGER, event.getId());
                });
            }
            inv.setItem(i, eventIcon);
            i++;
        }
    }


    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        IconID iconID = IconID.fromString(item.getPersistentDataContainer().get(iconKey, PersistentDataType.STRING));
        if (iconID == null) return;
        int eventID = item.getPersistentDataContainer().get(eventIDKey, PersistentDataType.INTEGER);
        LoginBonusEvent loginEvent = loginBonusManager.getLoginBonusList().get(eventID);
        if (loginEvent == null) {
            player.sendMessage("指定のログインボーナスは見つかりませんでした");
            return;
        }
        loginEvent.setActive(iconID != IconID.ACTIVE_EVENT);
        try {
            loginBonusManager.saveLoginBonus(loginEvent);
            player.sendMessage(Component.text(loginEvent.getName() + "の activeを " + loginEvent.isActive() + " に設定しました"));
        } catch (SQLException e) {
            player.sendMessage(Component.text("イベントのアクティブ設定に失敗しました: ").color(NamedTextColor.RED).append(Component.text(e.getMessage()).color(NamedTextColor.WHITE)) );
        }
        setup();
    }

    @Override
    public void onClose(Player player) {
        if (!closeFlag) return;
        closeFlag = false;
        MainMenu mainMenu = new MainMenu(loginBonusManager);
        Bukkit.getScheduler().runTask(LoginBonusPL.getInstance(), () -> {
            player.openInventory(mainMenu.getInventory());
        });
    }

    private List<Component> makeLore(LoginBonusEvent event) {
        LocalDate startDate = event.getStartDate();
        LocalDate endDate = event.getEndDate();
        return List.of(
                Component.text("開始: ").color(NamedTextColor.WHITE).append(Component.text(startDate.toString()).color(NamedTextColor.YELLOW)),
                Component.text("終了: ").color(NamedTextColor.WHITE).append(Component.text(endDate.toString()).color(NamedTextColor.YELLOW)),
                Component.text("最大ログイン報酬 :" + event.getMaxDayCount() + "日").color(NamedTextColor.WHITE)
        );
    }
}
