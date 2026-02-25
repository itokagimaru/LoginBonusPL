package io.github.itokagimaru.loginBonusPL.gui.adominGUI;

import io.github.itokagimaru.loginBonusPL.gui.BaseGuiHolder;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

public class MainMenu extends BaseGuiHolder {
    LoginBonusManager loginBonusManager;
    private final NamespacedKey key = new NamespacedKey("login_bonus", "icon_id");
    static enum IconID {
        BONUS_EDITOR("bonus_editor"),
        TEMPLATE_EDITOR("template_editor"),
        BONUS_MANAGER("bonus_manager"),
        CLOSE("close"),
        NULL_ICON("null_icon"),;
        IconID(String value){
            this.value=value;
        }
        private final String value;
        private String getValue(){
            return value;
        }
        static IconID fromValue(String value){
            for(IconID id : IconID.values()){
                if(id.getValue().equals(value)){
                    return id;
                }
            }
            return null;
        }
    }

    final int INV_SIZE = 27;

    public MainMenu(LoginBonusManager loginBonusManager) {
        this.loginBonusManager = loginBonusManager;
        inv = Bukkit.createInventory(this,INV_SIZE, Component.text("AdminMenu").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        setup();
    }

    private void setup(){

        ItemStack nullIcon = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        nullIcon.editMeta(meta -> {
            meta.customName(Component.text(" "));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.NULL_ICON.getValue());
        });
        for(int i = 0; i < INV_SIZE; i++){
            inv.setItem(i,nullIcon);
        }

        ItemStack loginBonusEditorIcon = new ItemStack(Material.CHEST);
        loginBonusEditorIcon.editMeta(meta -> {
            meta.customName(Component.text("LoginBonus内容編集").color(NamedTextColor.YELLOW));
            meta.lore(new ArrayList<>(java.util.List.of(Component.text("LoginBonusの内容を編集できます").color(NamedTextColor.WHITE))));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.BONUS_EDITOR.getValue());
        });
        inv.setItem(10, loginBonusEditorIcon);

//        ItemStack templateEditor = new ItemStack(Material.ENDER_CHEST);
//        templateEditor.editMeta(meta -> {
//            meta.customName(Component.text("ボーナステンプレート編集").color(NamedTextColor.YELLOW));
//            meta.lore(new ArrayList<>(java.util.List.of(Component.text("ログインボーナスの内容テンプレートの作成,編集を行います").color(NamedTextColor.WHITE))));
//            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.TEMPLATE_EDITOR.getValue());
//        });
//        inv.setItem(12, templateEditor);

        ItemStack bonusManager = new ItemStack(Material.OAK_SIGN);
        bonusManager.editMeta(meta -> {
            meta.customName(Component.text("開催ログインボーナスマネージャー").color(NamedTextColor.YELLOW));
            meta.lore(new ArrayList<>(java.util.List.of(Component.text("ログインボーナスの開催,中止を管理します").color(NamedTextColor.WHITE))));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.BONUS_MANAGER.getValue());
        });
        inv.setItem(13, bonusManager);

        ItemStack close = new ItemStack(Material.BARRIER);
        close.editMeta(meta -> {
            meta.customName(Component.text("閉じる").color(NamedTextColor.RED));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.CLOSE.getValue());
        });
        inv.setItem(17, close);
    }

    @Override
    public void onClick(InventoryClickEvent event){
        ItemStack clickedItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (clickedItem == null) return;
        event.setCancelled(true);
        IconID iconID = IconID.fromValue(clickedItem.getPersistentDataContainer().get(key, PersistentDataType.STRING));
        if (iconID == null) return;
        switch (iconID){
            case BONUS_EDITOR -> {
                closeFlag = false;
                BonusEventEditorMenu bonusEventEditorMenu = new BonusEventEditorMenu(loginBonusManager);
                player.openInventory(bonusEventEditorMenu.getInventory());
            }
            case TEMPLATE_EDITOR -> {}
            case BONUS_MANAGER -> {
                closeFlag = false;
                BonusEventManager bonusEventManager = new BonusEventManager(loginBonusManager);
                player.openInventory(bonusEventManager.getInventory());
            }
            case CLOSE -> {
                player.closeInventory();
            }
        }
    }
    @Override
    public void onClose(Player player){}
}
