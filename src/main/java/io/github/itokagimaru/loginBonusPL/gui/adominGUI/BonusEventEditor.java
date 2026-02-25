package io.github.itokagimaru.loginBonusPL.gui.adominGUI;

import io.github.itokagimaru.loginBonusPL.LoginBonusPL;
import io.github.itokagimaru.loginBonusPL.gui.BaseGuiHolder;
import io.github.itokagimaru.loginBonusPL.gui.adominGUI.anvilGUI.NamingAnvilGUI;
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

public class BonusEventEditor extends BaseGuiHolder {
    LoginBonusEvent event;
    LoginBonusManager loginBonusManager;
    boolean saveFlag = true;

    NamespacedKey iconKey = new NamespacedKey("login_bonus", "event_editor_icon");
    NamespacedKey dayKey = new NamespacedKey("login_bonus", "event_editor_day");
    enum IconID {
        BONUS_ICON("bonus_icon"),
        NEXT_PAGE("next_page"),
        BACK_PAGE("back_page"),
        CLOSE("close"),
        NAMED("named"),
        SAVE("save"),
        NULL_ICON("null_icon"),
        EVENT_PERIOD_SETUP("event_period_setup"),;
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
    final int BONUS_ICONS_COUNT = (INV_SIZE/9 - 1) * (9-2);
    int page = 0;

    public BonusEventEditor(LoginBonusEvent event, LoginBonusManager loginBonusManager) {
        this.event = new LoginBonusEvent(event);// 保存するか決めれるようにコピーで所持
        this.loginBonusManager = loginBonusManager;
        inv = Bukkit.createInventory(this, INV_SIZE, Component.text("BonusEventEditor: " + event.getName()));
        setup(page);
    }
    private void setup(int page) {
        ItemStack nullIcon = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        nullIcon.editMeta(meta -> {
            meta.customName(Component.text(" "));
            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.NULL_ICON.getValue());
        });
        for(int i = 0; i < INV_SIZE; i++){
            inv.setItem(i,nullIcon);
        }

        int ammo = 1 + (page * BONUS_ICONS_COUNT);
        for(int i = 0; i < INV_SIZE; i++){
            final int slot = i % 9;
            if (slot == 0 || slot == 8) {
                ItemStack pageNavigationIcon = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
                pageNavigationIcon.editMeta(meta -> {
                    if (slot == 0) {
                        meta.customName(Component.text("前のページへ"));
                        meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.BACK_PAGE.getValue());
                    } else {
                        meta.customName(Component.text("次のページへ"));
                        meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.NEXT_PAGE.getValue());
                    }

                });
                inv.setItem(i,pageNavigationIcon);
                continue;
            }
            if (i > INV_SIZE - 9) {
                switch (slot) {
                    case 2 -> {
                        ItemStack named = new ItemStack(Material.NAME_TAG);
                        named.editMeta(meta -> {
                            meta.customName(Component.text("名前を変更").color(NamedTextColor.YELLOW));
                            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.NAMED.getValue());
                        });
                        inv.setItem(i,named);
                    }
                    case 4 -> {
                        ItemStack eventPeriod = new ItemStack(Material.OAK_SIGN);
                        eventPeriod.editMeta(meta -> {
                            meta.customName(Component.text("開催期間を設定"));
                            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.EVENT_PERIOD_SETUP.getValue());
                        });
                        inv.setItem(i,eventPeriod);
                    }
                    case 6 -> {
                        ItemStack close = new ItemStack(Material.BARRIER);
                        close.editMeta(meta -> {
                            meta.customName(Component.text("保存せずに閉じる").color(NamedTextColor.RED));
                            meta.lore(List.of(Component.text("保存して閉じる場合はそのまま閉じてください").color(NamedTextColor.YELLOW)));
                            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.CLOSE.getValue());
                        });
                        inv.setItem(i,close);

                    }

                }
                continue;
            }
            ItemStack bonusIcon = getBonusIcon(ammo);
            if (bonusIcon != null) inv.setItem(i,bonusIcon);
            ammo++;
        }
    }

    private ItemStack getBonusIcon(int day) {
        if (day > 99) return null;
        Material material;
        material = Material.CHEST;
        ItemStack bonusIcon = new ItemStack(material, day);
        bonusIcon.editMeta(meta -> {
            meta.customName(Component.text("ログインボーナス:"+ day +"日目"));
            meta.lore(List.of(Component.text("左クリック:編集").color(NamedTextColor.WHITE)));
            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, IconID.BONUS_ICON.getValue());
            meta.getPersistentDataContainer().set(dayKey, PersistentDataType.INTEGER, day);
            meta.setMaxStackSize(99);
        });
        return bonusIcon;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (clickedItem == null) return;
        event.setCancelled(true);
        IconID iconID = IconID.fromValue(clickedItem.getPersistentDataContainer().get(iconKey, PersistentDataType.STRING));
        if (iconID == null) return;
        switch (iconID) {
            case BONUS_ICON -> {
                int day = clickedItem.getPersistentDataContainer().get(dayKey, PersistentDataType.INTEGER);
                closeFlag = false;
                BonusRewardEditor bonusRewardEditor = new BonusRewardEditor(this.event, day, loginBonusManager);
                player.openInventory(bonusRewardEditor.getInventory());
            }
            case SAVE -> {
                saveFlag = true;
                closeFlag = true;
                player.closeInventory();
            }
            case CLOSE -> {
                saveFlag = false;
                closeFlag = true;
                player.closeInventory();
            }
            case NEXT_PAGE -> {
                if ((page + 1) * BONUS_ICONS_COUNT < 100) page++;
                setup(page);
            }
            case BACK_PAGE -> {
                if (page > 0) page--;
                setup(page);
            }
            case NAMED -> {
                saveFlag = false;
                closeFlag = false;
                NamingAnvilGUI namingAnvilGUI = new NamingAnvilGUI(this.event, loginBonusManager);
                namingAnvilGUI.open(player);
            }
            case EVENT_PERIOD_SETUP -> {
                saveFlag = false;
                closeFlag = false;
                EventPeriodSetup eventPeriodSetup = new EventPeriodSetup(this.event, loginBonusManager);
                player.openInventory(eventPeriodSetup.getInventory());
            }
        }
    }

    @Override
    public void onClose(Player player) {
        if (closeFlag) {
            if (saveFlag) {
                try {
                    loginBonusManager.saveLoginBonus(event);
                    player.sendMessage(event.getName() + "を保存しました");
                } catch (SQLException e) {
                    player.sendMessage("ログインボーナスの保存に失敗しました : " + e.getMessage());
                }
            }
            closeFlag = false;
            BonusEventEditorMenu bonusEventEditorMenu = new BonusEventEditorMenu(loginBonusManager);
            Bukkit.getScheduler().runTask(LoginBonusPL.getInstance(), () -> {
                player.openInventory(bonusEventEditorMenu.getInventory());
            });
        }
    }
}
