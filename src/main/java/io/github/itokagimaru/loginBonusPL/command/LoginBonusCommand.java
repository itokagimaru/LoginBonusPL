package io.github.itokagimaru.loginBonusPL.command;

import io.github.itokagimaru.loginBonusPL.gui.sabaminGUI.LoginbonusMenu;
import io.github.itokagimaru.loginBonusPL.loginBonus.AltAccountService;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LoginBonusCommand implements CommandExecutor, TabCompleter {
    LoginBonusManager loginBonusManager;
    AltAccountService altAccountService;
    public LoginBonusCommand(LoginBonusManager loginBonusManager, AltAccountService altAccountService) {
        this.loginBonusManager = loginBonusManager;
        this.altAccountService = altAccountService;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます");
            return true;
        }

        switch (args[0]){
            case "help" -> {
                showHelp(player);
            }
            case "guiopen" -> {
                LoginbonusMenu loginbonusMenu = new LoginbonusMenu(loginBonusManager);
                player.openInventory(loginbonusMenu.getInventory());
            }
        }
        return true;
    }

    private void showHelp(Player player){
        player.sendMessage("---help---");
        player.sendMessage("/loginbonus help: このヘルプを表示");
        player.sendMessage("/loginbonus guiopen: ログインボーナスのGUIを開きます");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("help");
            list.add("guiopen");
        }
        return list;
    }
}
