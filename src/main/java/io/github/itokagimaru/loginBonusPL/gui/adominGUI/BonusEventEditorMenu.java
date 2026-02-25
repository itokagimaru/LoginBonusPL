package io.github.itokagimaru.loginBonusPL.gui.adominGUI;

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
import java.util.List;
import java.util.Map;

public class BonusEventEditorMenu extends BaseGuiHolder {
    NamespacedKey iconKey = new NamespacedKey("login_bonus", "event_editor_menu_icon");
    NamespacedKey eventKey = new NamespacedKey("login_bonus", "bonus_event");
    LoginBonusManager loginBonusManager;
    private enum IconID {
        BONUS_ICON("bonus_icon"),
        ADD_NEW_BONUS("add_new_bonus"),
        CLOSE("close"),
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

    final int INV_SIZE = 54;

    public BonusEventEditorMenu(LoginBonusManager loginBonusManager) {
        this.loginBonusManager = loginBonusManager;
        inv = Bukkit.createInventory(this, INV_SIZE, Component.text("BonusEventEditorMenu"));
        setup();
    }
    private void setup() {
        Map<Integer, LoginBonusEvent> loginBonusList = loginBonusManager.getLoginBonusList();
        vieLoginBonusList(loginBonusList);
    }

    private void vieLoginBonusList(Map<Integer, LoginBonusEvent> loginBonusList) {
        ItemStack nullIcon = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        nullIcon.editMeta(meta -> {
            meta.customName(Component.text(" "));
            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.NULL_ICON.getValue());
        });
        for(int i = 0; i < INV_SIZE; i++){
            inv.setItem(i,nullIcon);
        }

        int i = 0;
        for (LoginBonusEvent loginBonus : loginBonusList.values()) {
            ItemStack icon = new ItemStack(Material.CHEST);
            icon.editMeta(meta -> {
                meta.customName(Component.text(loginBonus.getName()));
                meta.lore(List.of(Component.text("左クリック:編集").color(NamedTextColor.WHITE),Component.text("shift + 右クリック:削除").color(NamedTextColor.RED)));
                meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.BONUS_ICON.getValue());
                meta.getPersistentDataContainer().set(eventKey, PersistentDataType.INTEGER, loginBonus.getId());
            });
            inv.setItem(i,icon);
            i++;
        }
        ItemStack addNewBonus = new ItemStack(Material.PAPER);
        addNewBonus.editMeta(meta -> {
            meta.customName(Component.text("新規ログインボーナスを作成"));
            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.ADD_NEW_BONUS.getValue());
        });
        if(!(i >= INV_SIZE)){
            inv.setItem(i,addNewBonus);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if(clickedItem == null) return;
        event.setCancelled(true);
        IconID iconID = IconID.fromValue(clickedItem.getPersistentDataContainer().get(iconKey, PersistentDataType.STRING));
        if (iconID == null) return;
        switch(iconID){
            case BONUS_ICON -> {
                if (event.isLeftClick()){
                    int eventId = clickedItem.getPersistentDataContainer().get(eventKey, PersistentDataType.INTEGER);
                    BonusEventEditor bonusEventEditor = new BonusEventEditor(loginBonusManager.getLoginBonusList().get(eventId), loginBonusManager);
                    closeFlag = false;
                    player.openInventory(bonusEventEditor.getInventory());
                } else if (event.isRightClick() && event.isShiftClick()) {
                    try{
                        int eventId = clickedItem.getPersistentDataContainer().get(eventKey, PersistentDataType.INTEGER);
                        loginBonusManager.deleteLoginBonus(loginBonusManager.getLoginBonusList().get(eventId));
                        vieLoginBonusList(loginBonusManager.getLoginBonusList());
                    } catch (SQLException e) {
                        player.sendMessage("削除に失敗しました: " + e.getMessage());
                    }
                }
            }
            case ADD_NEW_BONUS -> {
                if (!event.isLeftClick()) return;
                try {
                    loginBonusManager.createNewLoginBonus();
                    vieLoginBonusList(loginBonusManager.getLoginBonusList());
                } catch (SQLException e) {
                    player.sendMessage("新規作成に失敗しました: " + e.getMessage());
                }
            }
            case CLOSE -> {
                player.closeInventory();
            }
        }
    }

    @Override
    public void onClose(Player player) {
        if (closeFlag){
            closeFlag = false;
            MainMenu mainMenu = new MainMenu(loginBonusManager);
            Bukkit.getScheduler().runTask(LoginBonusPL.getInstance(), () -> {
                player.openInventory(mainMenu.getInventory());
            });
        }
    }
}
