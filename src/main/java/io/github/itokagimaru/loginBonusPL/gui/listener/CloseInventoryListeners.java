package io.github.itokagimaru.loginBonusPL.gui.listener;

import io.github.itokagimaru.loginBonusPL.gui.BaseGuiHolder;

import io.github.itokagimaru.loginBonusPL.gui.adminGUI.anvilGUI.AnvilGUIOpening;
import io.github.itokagimaru.loginBonusPL.gui.adminGUI.anvilGUI.NamingAnvilGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.view.AnvilView;

public class CloseInventoryListeners implements Listener {
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // is player
        if (!(event.getPlayer() instanceof Player player)) return;

        // get inventory and check
        Inventory inv = event.getInventory();

        if(event.getView() instanceof AnvilView){
            if(!(AnvilGUIOpening.isOpening(player))) return;
            NamingAnvilGUI namingAnvilGUI = AnvilGUIOpening.anvilOpening.get(player.getUniqueId());
            if(namingAnvilGUI == null) return;
            namingAnvilGUI.onClose(player);
        }

        if (!(inv.getHolder() instanceof BaseGuiHolder guiHolder)) return;
        // call onClose
        guiHolder.onClose(player);
    }
}
