package io.github.itokagimaru.loginBonusPL.gui.adominGUI.anvilGUI;

import io.github.itokagimaru.loginBonusPL.LoginBonusPL;
import io.github.itokagimaru.loginBonusPL.gui.adominGUI.BonusEventEditor;
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
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class NamingAnvilGUI {
    LoginBonusEvent loginBonusEvent;
    LoginBonusManager loginBonusManager;
    AnvilView anvilInv;
    Boolean closeFlag = true;//onCloseを動かすかどうかのflag

    NamespacedKey iconKey = new NamespacedKey("loginbonus", "naminganvil");

    public NamingAnvilGUI(LoginBonusEvent event, LoginBonusManager manager){
        this.loginBonusEvent = event;
        this.loginBonusManager = manager;
    }

    public void open(Player player){
        anvilInv = MenuType.ANVIL.builder()
                .title(Component.text("EventNaming")) // GUI の上部タイトル
                .location(player.getLocation()) // プレイヤー視点位置
                .checkReachable(false) // 本物のブロック必要なし
                .build(player);// プレイヤー向けにビュー作成
        setup();
        anvilInv.open();
        AnvilGUIOpening.anvilOpening.put(player.getUniqueId(), this);
    }
    public void setup(){
        ItemStack decision = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        decision.editMeta(meta -> {
            meta.customName(Component.text(loginBonusEvent.getName()).color(NamedTextColor.YELLOW));
            meta.lore(List.of(Component.text("クリックで決定").color(NamedTextColor.GREEN)));
            meta.getPersistentDataContainer().set(iconKey, PersistentDataType.STRING, "decision");
        });
        anvilInv.setItem(0, decision);

        ItemStack bar = new ItemStack(Material.BARRIER);
        bar.editMeta(meta -> {
            meta.customName(Component.text("").color(NamedTextColor.GREEN));
        });
        anvilInv.setItem(1, bar);
    }

    public void onClick(InventoryClickEvent event){// こいつだけ処理が特殊なため、キャンセルは処理が終わってから行うこと
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        String input = anvilInv.getRenameText();

        if (("decision").equals(clicked.getPersistentDataContainer().get(iconKey, PersistentDataType.STRING))){
            loginBonusEvent.setName(input);
            closeFlag = false;
            onClose(player);
            BonusEventEditor bonusEventEditor = new BonusEventEditor(loginBonusEvent, loginBonusManager);
            player.openInventory(bonusEventEditor.getInventory());
            AnvilGUIOpening.anvilOpening.remove(player.getUniqueId());
        }else{
            event.setCancelled(true);
        }
    }

    public void onClose(Player player){
        anvilInv.setItem(0,null);
        anvilInv.setItem(1,null);
        if (!closeFlag)return;
        closeFlag = false;
        Bukkit.getScheduler().runTask(LoginBonusPL.getInstance(), () -> {
            BonusEventEditor bonusEventEditor = new BonusEventEditor(loginBonusEvent, loginBonusManager);
            player.openInventory(bonusEventEditor.getInventory());
            AnvilGUIOpening.anvilOpening.remove(player.getUniqueId());
        });
    }
}
