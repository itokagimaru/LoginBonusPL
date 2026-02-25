package io.github.itokagimaru.loginBonusPL.gui.adominGUI.anvilGUI;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnvilGUIOpening {
    public static final Map<UUID, NamingAnvilGUI> anvilOpening = new HashMap<>();
    public static boolean isOpening(Player player){
        NamingAnvilGUI anvilGUI = anvilOpening.get(player.getUniqueId());
        return anvilGUI != null;
    }
}
