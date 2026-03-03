package io.github.itokagimaru.loginBonusPL.command;

import io.github.itokagimaru.loginBonusPL.gui.adminGUI.MainMenu;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import io.github.itokagimaru.loginBonusPL.loginBonus.PlayerLoginProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LoginBonusOPCommand implements CommandExecutor, TabCompleter {
    LoginBonusManager loginBonusManager;
    public LoginBonusOPCommand(LoginBonusManager manager) {
        this.loginBonusManager = manager;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ実行可能です。");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "progress" -> handleProgress(player, args);

            case "gui" -> openGUI(player, args);

            default -> showHelp(player);
        }

        return true;
    }

    private void handleProgress(Player player, String[] args) {

        if (args.length < 2) {
            showHelp(player);
            return;
        }

        switch (args[1].toLowerCase()) {

            case "delete" -> handleDelete(player, args);

            case "set" -> handleSet(player, args);

            default -> showHelp(player);
        }
    }

    private void handleDelete(Player player, String[] args) {

        if (!(args.length == 3 || args.length == 4)) {
            player.sendMessage(Component.text("引数が不足しています").color(NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            player.sendMessage(Component.text("対象プレイヤーが見つかりません").color(NamedTextColor.RED));
            return;
        }

        UUID uuid = target.getUniqueId();

        if (args.length == 3) {

            if (loginBonusManager.isAltAccountRestricted()) {

                loginBonusManager.getAltAccounts(uuid)
                        .thenCompose(altAccounts -> {
                            List<CompletableFuture<Void>> futures = altAccounts.stream()
                                    .map(loginBonusManager::deletePlayerLoginProgress)
                                    .toList();
                            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                        })
                        .thenRun(() -> runSync(() ->
                                player.sendMessage(Component.text("全ログイン情報を削除しました")
                                        .color(NamedTextColor.YELLOW))
                        ))
                        .exceptionally(ex -> {
                            handleError(player, "削除に失敗しました", ex);
                            return null;
                        });

            } else {

                loginBonusManager.deletePlayerLoginProgress(uuid)
                        .thenRun(() -> runSync(() ->
                                player.sendMessage(Component.text("削除しました")
                                        .color(NamedTextColor.YELLOW))
                        ))
                        .exceptionally(ex -> {
                            handleError(player, "削除に失敗しました", ex);
                            return null;
                        });
            }

            return;
        }

        LoginBonusEvent targetEvent = null;
        for (LoginBonusEvent event : loginBonusManager.getLoginBonusList().values()) {
            if (event.getName().equals(args[3])) {
                targetEvent = event;
                break;
            }
        }

        if (targetEvent == null) {
            player.sendMessage(Component.text("イベントが見つかりません")
                    .color(NamedTextColor.RED));
            return;
        }

        final LoginBonusEvent finalEvent = targetEvent;

        if (loginBonusManager.isAltAccountRestricted()) {

            loginBonusManager.getAltAccounts(uuid)
                    .thenCompose(uuids -> {
                        List<CompletableFuture<Void>> futures = uuids.stream()
                                .map(id -> loginBonusManager.deletePlayerLoginProgress(id, finalEvent))
                                .toList();
                        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                    })
                    .thenRun(() -> runSync(() ->
                            player.sendMessage(Component.text("削除しました")
                                    .color(NamedTextColor.YELLOW))
                    ))
                    .exceptionally(ex -> {
                        handleError(player, "削除に失敗しました", ex);
                        return null;
                    });

        } else {

            loginBonusManager.deletePlayerLoginProgress(uuid, finalEvent)
                    .thenRun(() -> runSync(() ->
                            player.sendMessage(Component.text("削除しました")
                                    .color(NamedTextColor.YELLOW))
                    ))
                    .exceptionally(ex -> {
                        handleError(player, "削除に失敗しました", ex);
                        return null;
                    });
        }

    }

    private void handleSet(Player player, String[] args) {

        if (!(args.length == 5 || args.length == 8)) {
            player.sendMessage(Component.text("引数が不足しています").color(NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            player.sendMessage(Component.text("対象プレイヤーが見つかりません").color(NamedTextColor.RED));
            return;
        }

        LoginBonusEvent targetEvent = null;
        for (LoginBonusEvent event : loginBonusManager.getLoginBonusList().values()) {
            if (event.getName().equals(args[3])) {
                targetEvent = event;
                break;
            }
        }

        if (targetEvent == null) {
            player.sendMessage(Component.text("イベントが見つかりません")
                    .color(NamedTextColor.RED));
            return;
        }

        UUID uuid = target.getUniqueId();
        final LoginBonusEvent finalEvent = targetEvent;

        CompletableFuture<List<PlayerLoginProgress>> future;

        if (loginBonusManager.isAltAccountRestricted()) {
            future = loginBonusManager.getAltAccountLoginProgress(uuid, finalEvent);
        } else {
            future = loginBonusManager
                    .getOrCreatePlayerLoginProgress(uuid, finalEvent)
                    .thenApply(List::of);
        }

        future.thenCompose(progresses -> {

                    int totalDays;
                    try {
                        if (args[4].equalsIgnoreCase("unmodified")) {
                            totalDays = loginBonusManager.getMaxTotalLogins(progresses);
                        } else {
                            totalDays = Integer.parseInt(args[4]);
                        }
                    } catch (NumberFormatException e) {
                        runSync(() -> {
                            player.sendMessage(Component.text("TotalDaysが不正")
                                    .color(NamedTextColor.RED));
                            showHelp(player);
                        });
                        return CompletableFuture.completedFuture(null);
                    }

                    LocalDate date;
                    if (args.length == 5) {
                        date = LocalDate.now().minusDays(1);
                    } else {
                        try {
                            date = LocalDate.of(
                                    Integer.parseInt(args[5]),
                                    Integer.parseInt(args[6]),
                                    Integer.parseInt(args[7])
                            );
                        } catch (NumberFormatException e) {
                            runSync(() -> {
                                player.sendMessage(Component.text("日付が不正")
                                        .color(NamedTextColor.RED));
                                showHelp(player);
                            });
                            return CompletableFuture.completedFuture(null);
                        }
                    }

                    List<CompletableFuture<Void>> updates = progresses.stream()
                            .map(progress -> {
                                progress.setTotalLoginDays(totalDays);
                                progress.setLastLoginDate(date);
                                return loginBonusManager.updatePlayerLoginProgress(progress.getUuid(), progress);
                            })
                            .toList();

                    return CompletableFuture.allOf(updates.toArray(new CompletableFuture[0]));
                })
                .thenRun(() -> runSync(() ->
                        player.sendMessage(Component.text("更新完了")
                                .color(NamedTextColor.YELLOW))
                ))
                .exceptionally(ex -> {
                    handleError(player, "更新に失敗しました", ex);
                    return null;
                });
    }

    private void openGUI(Player player, String[] args) {
        if (args.length != 1) {
            showHelp(player);
            return;
        }
        MainMenu mainMenu = new MainMenu(loginBonusManager);
        player.openInventory(mainMenu.getInventory());
    }

    private void showHelp(Player player){
        player.sendMessage(Component.text("---help---").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/loginbonusop help: このヘルプを表示").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/loginbonusop gui: ログインボーナス編集GUIを開く").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/loginbonusop progress deleteByUUIDAndEvent <PlayerName> <LoginBonusName>: " +
                "\n指定したプレイヤーとそのプレイヤーのサブアカウントの指定したイベントのログイン履歴削除します" +
                "\nイベントの指定がなければすべてのログイン履歴を削除します").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/loginbonusop progress set <PlayerName> <LoginBonusName> <TotalDays> <LastLoginDayOffset>: " +
                "\n指定したプレイヤーとそのプレイヤーのサブアカウントのログイン履歴を設定します" +
                "\nLastLoginDayは省略可能です.省略すると昨日の日付で自動補完されます").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (!sender.hasPermission("loginbonus.op")) return list;
        if (args.length == 1) {
            list.add("help");
            list.add("gui");
            list.add("progress");
        }
        if (args.length == 2) {
            if(args[0].equals("progress")) {
                list.add("set");
                list.add("delete");
            }
        }
        if (args.length == 3) {
            if (args[0].equals("progress")) {
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

    private void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(loginBonusManager.getPlugin(), runnable);
    }

    private void handleError(Player player, String errare, Throwable ex) {
        Bukkit.getScheduler().runTask(loginBonusManager.getPlugin(), () -> {
            player.sendMessage(errare);
            loginBonusManager.outPutError(ex.getMessage());
        });
    }
}
