package io.github.itokagimaru.loginBonusPL.command;

import io.github.itokagimaru.loginBonusPL.gui.adominGUI.MainMenu;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import io.github.itokagimaru.loginBonusPL.loginBonus.PlayerLoginProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoginBonusOPCommand implements CommandExecutor, TabCompleter {
    LoginBonusManager loginBonusManager;
    LuckPerms luckPerms;
    public LoginBonusOPCommand(LoginBonusManager loginBonusManager, LuckPerms luckPerms) {
        this.loginBonusManager = loginBonusManager;
        this.luckPerms = luckPerms;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("loginbonus.op")) {
            sender.sendMessage(Component.text("権限がありません"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます");
            return false;
        }

        switch (args[0]){
            case "help" ->{
                showHelp(player);
                return true;
            }
            case "gui" ->{
                MainMenu mainMenu = new MainMenu(loginBonusManager);
                player.openInventory(mainMenu.getInventory());
                return true;
            }
            case "permission" ->{
                Player target = Bukkit.getPlayer(args[2]);
                if(target == null) {
                    player.sendMessage(Component.text("player " + args[2] + " は見つかりませんでした"));
                    return true;
                }
                switch (args[1]){
                    case "add" ->{
                        if (givePermission(target, "loginbonus.op")){
                            player.sendMessage(Component.text(target.getName() + " に権限を付与しました").color(NamedTextColor.YELLOW));
                            target.sendMessage(Component.text("ログインボーナス編集権限が付与されました").color(NamedTextColor.YELLOW));
                        } else {
                            player.sendMessage(Component.text(target.getName() + " への権限付与は失敗しました").color(NamedTextColor.RED));
                            if (player.hasPermission("loginbonus.op")) player.sendMessage(Component.text(target.getName() + " は既に権限を持っています"));
                        }
                    }
                    case "remove" ->{
                        if (removePermission(target, "loginbonus.op")){
                            player.sendMessage(Component.text(target.getName() + " から権限を剥奪しました").color(NamedTextColor.YELLOW));
                            target.sendMessage(Component.text("ログインボーナス編集権限が剥奪されました").color(NamedTextColor.YELLOW));
                        } else {
                            player.sendMessage(Component.text(target.getName() + " からの権限剥奪は失敗しました").color(NamedTextColor.RED));
                            if (!player.hasPermission("loginbonus.op")) player.sendMessage(Component.text(target.getName() + " は権限を持っていません"));
                        }
                    }
                }
            }
            case "progress" ->{// 馬鹿長いから切り出した方がいいかも todo 提出前か後にリファクタリングは挟もう
                switch (args[1]){
                    case "set" ->{
                        if (!(args.length == 8 || args.length == 5)) {
                            player.sendMessage(Component.text("引数が不足,もしくは多いです"));
                            return false;
                        }
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            player.sendMessage(Component.text("対象のプレイヤーが見つかりませんでした").color(NamedTextColor.RED));
                            return true;
                        }
                        LoginBonusEvent targetEvent = null;
                        for (LoginBonusEvent event : loginBonusManager.getLoginBonusList().values()){
                            if (event.getName().equals(args[3])) {
                                targetEvent = event;
                                break;
                            }
                        }
                        if (targetEvent == null) {
                            player.sendMessage(Component.text("対象のログインボーナスが見つかりませんでした").color(NamedTextColor.RED));
                            return true;
                        }
                        PlayerLoginProgress progress;
                        try {
                            progress = loginBonusManager.getOrCreatePlayerLoginProgress(target.getUniqueId(), targetEvent);
                        } catch (SQLException e) {
                            player.sendMessage(Component.text("ログイン進捗の読み込みに失敗しました: " + e.getMessage()).color(NamedTextColor.RED));
                            return true;
                        }
                        try {
                            progress.setTotalLoginDays(Integer.parseInt(args[4]));
                        } catch (NumberFormatException e) {
                            player.sendMessage(Component.text("TotalDays の値が不正です").color(NamedTextColor.RED));
                            return false;
                        }
                        LocalDate date;
                        if (args.length == 5) {
                            date = LocalDate.now().minusDays(1);
                        } else {
                            try {
                                date = LocalDate.of(Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]));
                            } catch (NumberFormatException e) {
                                player.sendMessage(Component.text("LastLoginDay の値が不正です").color(NamedTextColor.RED));
                                return false;
                            }
                        }
                        progress.setLastLoginDate(date);
                        try {
                            loginBonusManager.updatePlayerLoginProgress(target.getUniqueId(), progress);
                        } catch (SQLException e) {
                            player.sendMessage(Component.text("ログイン進捗の更新に失敗しました").color(NamedTextColor.RED));
                            return true;
                        }
                    }
                    case "delete" ->{
                        if (!(args.length == 3 ||  args.length == 4)) {
                            player.sendMessage(Component.text("引数が不足,もしくは多いです"));
                            return false;
                        }
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            player.sendMessage(Component.text("対象のプレイヤーが見つかりませんでした"));
                            return true;
                        }

                        if (args.length == 3) {//全てのイベントに対して削除を行う
                            for (LoginBonusEvent event : loginBonusManager.getLoginBonusList().values()){
                                try {
                                    loginBonusManager.deletePlayerLoginProgress(target.getUniqueId(), event);
                                    player.sendMessage(Component.text(target.getName() + " の " + event.getName() + "におけるログイン進捗を削除しました").color(NamedTextColor.YELLOW));
                                } catch (SQLException e) {
                                    player.sendMessage(Component.text(target.getName() + " の " + event.getName() + "におけるログイン進捗の削除に失敗しました: " + e.getMessage()).color(NamedTextColor.YELLOW));
                                }
                            }
                            return true;
                        }
                        LoginBonusEvent targetEvent = null;
                        for (LoginBonusEvent event : loginBonusManager.getLoginBonusList().values()){
                            if (event.getName().equals(args[3])) {
                                targetEvent = event;
                                break;
                            }
                        }
                        if (targetEvent == null) {
                            player.sendMessage(Component.text("対象のログインボーナスが見つかりませんでした").color(NamedTextColor.RED));
                            return true;
                        }
                        try {
                            loginBonusManager.deletePlayerLoginProgress(target.getUniqueId(), targetEvent);
                        } catch (SQLException e) {
                            player.sendMessage(Component.text(target.getName() + " の " + targetEvent.getName() + "におけるログイン進捗の削除に失敗しました: " + e.getMessage()).color(NamedTextColor.YELLOW));
                        }
                    }
                }
            }
        }
        return false;
    }

    private void showHelp(Player player){
        player.sendMessage("---help---");
        player.sendMessage("/loginbonusop help: このヘルプを表示");
        player.sendMessage("/loginbonusop permission add <PlayerName>: /loginbonusopを使うための権限を与える");
        player.sendMessage("/loginbonusop permission remove <PlayerName>: /loginbonusopを使うための権限を剥がす");
        player.sendMessage("/loginbonusop gui: ログインボーナス編集GUIを開く");
        player.sendMessage("/loginbonusop progress delete <PlayerName> <LoginBonusName>: 指定したプレイヤーの指定したイベントのログイン履歴削除します" +
                "\nイベントの指定がなければすべてのログイン履歴を削除します"); // todo 内容実装
        player.sendMessage("/loginbonusop progress set <PlayerName> <LoginBonusName> <TotalDays> <LastLoginDayOffset>: 指定したプレイヤーのログイン履歴を設定します" +
                "\nLastLoginDayは省略可能です.省略すると今日の日付で自動補完されます");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (!sender.hasPermission("loginbonus.op")) return list;
        if (args.length == 1) {
            list.add("help");
            list.add("permission");
            list.add("gui");
            list.add("progress");
        }
        if (args.length == 2) {
            if(args[0].equals("permission")) {
                list.add("add");
                list.add("remove");
            }
            if(args[0].equals("progress")) {
                list.add("set");
                list.add("delete");
            }
        }
        if (args.length == 3) {
            if (args[0].equals("permission") || args[0].equals("progress")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    list.add(player.getName());
                }
            }
        }
        if (args.length == 4) {
            if (args[0].equals("progress")) {
                if (args[1].equals("delete") || args[1].equals("set")){
                    for (LoginBonusEvent event: loginBonusManager.getLoginBonusList().values()){
                        if (!event.isActive()) continue;
                        list.add(event.getName());
                    }
                }
            }
        }
        if (args.length == 5) {
            if (args[1].equals("set")){
                list.add("unmodified");
                list.add("0");
            }
        }
        if (args.length == 6) {
            if (args[0].equals("progress") && args[1].equals("set")) {
                list.add(String.valueOf(LocalDate.now().getYear()));
            }
        }
        if (args.length == 7) {
            if (args[0].equals("progress") && args[1].equals("set")) {
                list.add(String.valueOf(LocalDate.now().getMonthValue()));
            }
        }
        if (args.length == 8) {
            if (args[0].equals("progress") && args[1].equals("set")) {
                list.add(String.valueOf(LocalDate.now().getDayOfMonth()));
            }
        }
        return list;
    }

    public boolean givePermission(Player player, String permission) {
        if (player.hasPermission(permission)) return false;
        luckPerms.getUserManager()
                .loadUser(player.getUniqueId())
                .thenAccept(user -> {

                    Node node = Node.builder(permission).build();

                    user.data().add(node);

                    luckPerms.getUserManager().saveUser(user);
                });
        return true;
    }

    public boolean removePermission(Player player, String permission) {
        if (!player.hasPermission(permission)) return false;
        luckPerms.getUserManager()
                .loadUser(player.getUniqueId())
                .thenAccept(user -> {

                    Node node = Node.builder(permission).build();

                    user.data().remove(node);

                    luckPerms.getUserManager().saveUser(user);
                });
        return true;
    }


}
