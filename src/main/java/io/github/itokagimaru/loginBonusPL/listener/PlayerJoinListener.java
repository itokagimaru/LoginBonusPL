package io.github.itokagimaru.loginBonusPL.listener;

import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;

public class PlayerJoinListener implements Listener {
    LoginBonusManager loginBonusManager;
    public PlayerJoinListener(LoginBonusManager loginBonusManager) {
        this.loginBonusManager = loginBonusManager;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Map<Integer, LoginBonusEvent> loginBonusEventMap = loginBonusManager.getLoginBonusList();
        if (isActive(loginBonusEventMap)) {
            player.sendMessage(Component.text("ログインボーナス開催中!!"));
            player.sendMessage(sendClickableMessage("[ここ]","クリックでGUIを開く","loginbonus guiopen").append(Component.text("をクリックしてログインボーナスを受け取ろう！！").color(NamedTextColor.YELLOW)));
        } else {
            player.sendMessage("開催中のログインボーナスはありません");
        }

    }

    private boolean isActive(Map<Integer, LoginBonusEvent> loginBonusEventMap) {
        for (LoginBonusEvent loginBonusEvent : loginBonusEventMap.values()) {
            if (loginBonusEvent.isActive()) {
                return true;
            }
        }
        return false;
    }

    public Component sendClickableMessage(String text,String hover, String command) {

        return Component.text(text)
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/" + command))
                .hoverEvent(HoverEvent.showText(
                        Component.text(hover)
                                .color(NamedTextColor.GRAY)
                ));
    }
}
