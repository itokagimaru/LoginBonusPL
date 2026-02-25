package io.github.itokagimaru.loginBonusPL.gui.listener;
import io.github.itokagimaru.loginBonusPL.gui.BaseGuiHolder;
import io.github.itokagimaru.loginBonusPL.gui.adminGUI.anvilGUI.AnvilGUIOpening;
import io.github.itokagimaru.loginBonusPL.gui.adminGUI.anvilGUI.NamingAnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

public class ClickInventoryListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getView() instanceof AnvilView){
            Player player = (Player) event.getWhoClicked();
            if (!(AnvilGUIOpening.isOpening(player))){
                return;
            }
            NamingAnvilGUI namingAnvilGUI = AnvilGUIOpening.anvilOpening.get(player.getUniqueId());
            namingAnvilGUI.onClick(event);
            return;
        }
        if ((event.getView().getTopInventory().getHolder() instanceof BaseGuiHolder baseGuiHolder)) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            event.setCancelled(true);
            baseGuiHolder.onClick(event);
        }
    }
}
