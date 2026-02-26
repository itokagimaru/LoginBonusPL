package io.github.itokagimaru.loginBonusPL.gui.sabaminGUI;

import io.github.itokagimaru.loginBonusPL.gui.BaseGuiHolder;
import io.github.itokagimaru.loginBonusPL.loginBonus.AltAccountService;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import io.github.itokagimaru.loginBonusPL.loginBonus.PlayerLoginProgress;
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
import java.util.Map;

public class LoginbonusMenu extends BaseGuiHolder {
    LoginBonusManager loginBonusManager;
    NamespacedKey iconKey = new NamespacedKey("loginbonus", "loginbonusmenu_icon");
    NamespacedKey eventKey = new NamespacedKey("loginbonus", "loginbonusmenu_event");

    enum IconID{
        BONUS_ICON("bonus_icon"),
        NULL_ICON("null_icon"),;
        IconID(String value){
            this.value=value;
        }
        private final String value;
        private String getValue(){
            return value;
        }
        private static IconID fromValue(String value){
            for(IconID id : IconID.values()){
                if(id.getValue().equals(value)){
                    return id;
                }
            }
            return null;
        }
    }

    public LoginbonusMenu(LoginBonusManager loginBonusManager) {
        this.loginBonusManager = loginBonusManager;
        inv = Bukkit.createInventory(this, INV_SIZE, Component.text("開催中の LoginBonus 一覧").color(NamedTextColor.BLUE));
        setup();
    }
    int INV_SIZE = 27;
    public void setup() {
        Map<Integer, LoginBonusEvent> loginBonusList = loginBonusManager.getLoginBonusList();
        vieLoginBonusList(loginBonusList);
    }

    private void vieLoginBonusList(Map<Integer, LoginBonusEvent> loginBonusList) {
        ItemStack nullIcon = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        nullIcon.editMeta(meta -> {
            meta.customName(Component.text(""));
            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.NULL_ICON.getValue());
        });
        for(int i = 0; i < INV_SIZE; i++){
            inv.setItem(i,nullIcon);
        }

        int i = 0;
        for (LoginBonusEvent loginBonus : loginBonusList.values()) {
            if (loginBonus.isActive()){
                ItemStack icon = new ItemStack(Material.CHEST);
                icon.editMeta(meta -> {
                    meta.customName(Component.text(loginBonus.getName()));
                    meta.lore(List.of(
                            Component.text("左クリック:報酬カレンダーを開く").color(NamedTextColor.WHITE),
                            Component.text("shift + 左クリック:今日の報酬を受け取る").color(NamedTextColor.YELLOW))
                    );
                    meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.BONUS_ICON.getValue());
                    meta.getPersistentDataContainer().set(eventKey, PersistentDataType.INTEGER, loginBonus.getId());
                });
                inv.setItem(i,icon);
                i++;
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (item == null) return;
        IconID iconID = IconID.fromValue(item.getPersistentDataContainer().get(iconKey, PersistentDataType.STRING));
        if (iconID != IconID.BONUS_ICON) return;
        if (!event.isLeftClick()) return;
        int eventId = item.getPersistentDataContainer().get(eventKey, PersistentDataType.INTEGER);
        LoginBonusEvent loginBonusEvent = loginBonusManager.getLoginBonusList().get(eventId);
        if (loginBonusEvent == null) return;
        if (event.isShiftClick()) {
            loginBonusEvent.giveReward(player, loginBonusManager);
        } else {
            closeFlag = false;
            RewardCalendar rewardCalendar = new RewardCalendar(loginBonusManager, loginBonusEvent);
            player.openInventory(rewardCalendar.getInventory());
        }
    }

    @Override
    public void onClose(Player player) {}
}
