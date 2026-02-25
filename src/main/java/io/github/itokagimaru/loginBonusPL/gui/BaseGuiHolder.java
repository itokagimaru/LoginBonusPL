package io.github.itokagimaru.loginBonusPL.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class BaseGuiHolder implements InventoryHolder {
    protected Inventory inv;
    public boolean closeFlag = true;

    public abstract void onClick(InventoryClickEvent event);

    public abstract void onClose(Player player);

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
