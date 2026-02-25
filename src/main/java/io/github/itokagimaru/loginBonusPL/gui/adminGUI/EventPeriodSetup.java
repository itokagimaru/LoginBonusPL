package io.github.itokagimaru.loginBonusPL.gui.adminGUI;

import io.github.itokagimaru.loginBonusPL.LoginBonusPL;
import io.github.itokagimaru.loginBonusPL.gui.BaseGuiHolder;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusEvent;
import io.github.itokagimaru.loginBonusPL.loginBonus.LoginBonusManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class EventPeriodSetup extends BaseGuiHolder {
    LoginBonusManager loginBonusManager;
    final LoginBonusEvent oldLoginBonusEvent;
    final LoginBonusEvent newLoginBonusEvent;
    boolean saveFlag = true;

    NamespacedKey key = new NamespacedKey("loginbonus", "event_period_setup");
    enum IconID{
        START_YEAR("start_year"),
        END_YEAR("end_year"),
        START_MONTH("start_month"),
        END_MONTH("end_month"),
        START_DAY("start_day"),
        END_DAY("end_day"),
        DECISION("decision"),
        CANCEL("cancel"),;
        private final String id;
        IconID(String id){
            this.id=id;
        }
        public String getId() {
            return id;
        }
        public static IconID getById(String id){
            for(IconID i:IconID.values()){
                if(i.getId().equals(id)){
                    return i;
                }
            }
            return null;
        }
    }

    enum DataType{
        START("開始"),
        END("終了");
        private final String type;
        DataType(String type){
            this.type=type;
        }
        public String getType() {
            return type;
        }
    }


    public EventPeriodSetup(LoginBonusEvent loginBonusEvent, LoginBonusManager loginBonusManager) {
        this.loginBonusManager = loginBonusManager;
        this.oldLoginBonusEvent = loginBonusEvent;
        this.newLoginBonusEvent = new LoginBonusEvent(loginBonusEvent);

        inv = Bukkit.createInventory(this, 27, Component.text("イベントの開始期間設定"));
        LocalDate startDate = loginBonusEvent.getStartDate();
        LocalDate endDate = loginBonusEvent.getEndDate();
        setup(startDate, endDate);
    }

    public void setup(LocalDate startDate, LocalDate endDate) {
        newLoginBonusEvent.setStartDate(startDate);
        newLoginBonusEvent.setEndDate(endDate);
        ItemStack[] startDays = getDayItems(startDate, DataType.START);
        ItemStack[] endDays = getDayItems(endDate, DataType.END);

        inv.setItem(1, startDays[0]);
        inv.setItem(2, startDays[1]);
        inv.setItem(3, startDays[2]);
        inv.setItem(5, endDays[0]);
        inv.setItem(6, endDays[1]);
        inv.setItem(7, endDays[2]);

        ItemStack decision = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        decision.editMeta(meta -> {
            meta.customName(Component.text("決定").color(NamedTextColor.GREEN));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.DECISION.getId());
        });

        ItemStack bar = new ItemStack(Material.BARRIER);
        bar.editMeta(meta -> {
            meta.customName(Component.text("決定").decorate(TextDecoration.STRIKETHROUGH).color(NamedTextColor.RED));
            meta.lore(List.of(Component.text("開始日と終了日の前後関係が異常です").color(NamedTextColor.RED),
                    Component.text("終了日が開始日よりも未来になるように設定してください").color(NamedTextColor.RED)
            ));
        });

        if (startDate.isBefore(endDate)) inv.setItem(21, decision);
        else inv.setItem(21, bar);

        
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        cancel.editMeta(meta -> {
            meta.customName(Component.text("キャンセル").color(NamedTextColor.RED));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.CANCEL.getId());
        });
        inv.setItem(23, cancel);
    }



    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if(item == null) return;
        IconID icon = IconID.getById(item.getPersistentDataContainer().get(key, PersistentDataType.STRING));
        if(icon == null) return;
        switch (icon) {
            case START_YEAR -> {
                int yearOffset = 0;
                if (event.isLeftClick()) {
                    if (event.isShiftClick()) yearOffset = 10;
                    else yearOffset += 1;
                }
                else if (event.isRightClick()) {
                    if (event.isShiftClick()) yearOffset -= 10;
                    else yearOffset -= 1;
                }
                LocalDate startDate = parseDate(newLoginBonusEvent.getStartDate(), yearOffset,0,0);
                LocalDate endDate = newLoginBonusEvent.getEndDate();
                setup(startDate, endDate);
            }
            case END_YEAR -> {
                int yearOffset = 0;
                if (event.isLeftClick()) {
                    if (event.isShiftClick()) yearOffset += 10;
                    else yearOffset += 1;
                }
                else if (event.isRightClick()) {
                    if (event.isShiftClick()) yearOffset -= 10;
                    else yearOffset -= 1;
                }
                LocalDate startDate = newLoginBonusEvent.getStartDate();
                LocalDate endDate = parseDate(newLoginBonusEvent.getEndDate(), yearOffset,0,0);
                setup(startDate, endDate);
            }
            case START_MONTH -> {
                int monthOffset = 0;
                if (event.isLeftClick()) {
                    if (event.isShiftClick()) monthOffset += 5;
                    else monthOffset += 1;
                }
                else if (event.isRightClick()) {
                    if (event.isShiftClick()) monthOffset -= 5;
                    else monthOffset -= 1;
                }
                LocalDate startDate = parseDate(newLoginBonusEvent.getStartDate(), 0,monthOffset,0);
                LocalDate endDate = newLoginBonusEvent.getEndDate();
                setup(startDate, endDate);
            }
            case END_MONTH -> {
                int monthOffset = 0;
                if (event.isLeftClick()) {
                    if (event.isShiftClick()) monthOffset += 5;
                    else monthOffset += 1;
                }
                else if (event.isRightClick()) {
                    if (event.isShiftClick()) monthOffset -= 5;
                    else monthOffset -= 1;
                }
                LocalDate startDate = newLoginBonusEvent.getStartDate();
                LocalDate endDate = parseDate(newLoginBonusEvent.getEndDate(), 0,monthOffset,0);
                setup(startDate, endDate);
            }
            case START_DAY -> {
                int dayOffset = 0;
                if (event.isLeftClick()) {
                    if (event.isShiftClick()) dayOffset += 10;
                    else dayOffset += 1;
                }
                else if (event.isRightClick()) {
                    if (event.isShiftClick()) dayOffset -= 10;
                    else dayOffset -= 1;
                }
                LocalDate startDate = parseDate(newLoginBonusEvent.getStartDate(), 0,0,dayOffset);
                LocalDate endDate = newLoginBonusEvent.getEndDate();
                setup(startDate, endDate);
            }
            case END_DAY -> {
                int dayOffset = 0;
                if (event.isLeftClick()) {
                    if (event.isShiftClick()) dayOffset += 10;
                    else dayOffset += 1;
                }
                else if (event.isRightClick()) {
                    if (event.isShiftClick()) dayOffset -= 10;
                    else dayOffset -= 1;
                }
                LocalDate startDate = newLoginBonusEvent.getStartDate();
                LocalDate endDate = parseDate(newLoginBonusEvent.getEndDate(), 0,0,dayOffset);
                setup(startDate, endDate);
            }
            case DECISION -> {
                saveFlag = true;
                closeFlag = true;
                player.closeInventory();
            }
            case CANCEL ->  {
                saveFlag = false;
                closeFlag = true;
                player.closeInventory();
            }
        }
    }

    @Override
    public void onClose(Player player) {
        if (!closeFlag) return;
        closeFlag = false;
        BonusEventEditor bonusEventEditor;
        if (saveFlag) {
            bonusEventEditor = new BonusEventEditor(newLoginBonusEvent, loginBonusManager);
        } else {
            bonusEventEditor = new BonusEventEditor(oldLoginBonusEvent, loginBonusManager);
        }
        Bukkit.getScheduler().runTask(LoginBonusPL.getInstance(), () -> {
            player.openInventory(bonusEventEditor.getInventory());
        });
    }



    public ItemStack getYearItem(int year, DataType type) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> {
            meta.customName(Component.text( type.getType() +"年 : " + year).color(NamedTextColor.YELLOW));
            meta.lore(List.of(Component.text("20xx年で設定可能です").color(NamedTextColor.WHITE),
                    Component.text(""),
                    Component.text("左クリック : +1").color(NamedTextColor.GREEN),
                    Component.text("Shift + 左クリック : +10").color(NamedTextColor.GREEN),
                    Component.text(""),
                    Component.text("右クリック : -1").color(NamedTextColor.GREEN),
                    Component.text("Shift + 右クリック : -10").color(NamedTextColor.GREEN)
                    ));
            if (type == DataType.START) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.START_YEAR.getId());
            } else {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.END_YEAR.getId());
            }

        });
        item.setAmount(year % 100);//下2桁が欲しい
        return item;
    }

    public ItemStack getMonthItem(int month, DataType type) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> {
            meta.customName(Component.text(type.getType() +"月 : " + month).color(NamedTextColor.YELLOW));
            meta.lore(List.of(Component.text("1月~12月で設定可能です").color(NamedTextColor.WHITE),
                    Component.text(""),
                    Component.text("左クリック : +1").color(NamedTextColor.GREEN),
                    Component.text("Shift + 左クリック : +5").color(NamedTextColor.GREEN),
                    Component.text(""),
                    Component.text("右クリック : -1").color(NamedTextColor.GREEN),
                    Component.text("Shift + 右クリック : -5").color(NamedTextColor.GREEN)
            ));
            if (type == DataType.START) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.START_MONTH.getId());
            } else {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.END_MONTH.getId());
            }
        });
        item.setAmount(month);
        return item;
    }

    private ItemStack getDayItem(int day, DataType type) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> {
            meta.customName(Component.text(type.getType() +"日 : " + day).color(NamedTextColor.YELLOW));
            meta.lore(List.of(Component.text("その月の有効な日にちで設定可能です").color(NamedTextColor.WHITE),
                    Component.text(""),
                    Component.text("左クリック : +1").color(NamedTextColor.GREEN),
                    Component.text("Shift + 左クリック : +10").color(NamedTextColor.GREEN),
                    Component.text(""),
                    Component.text("右クリック : -1").color(NamedTextColor.GREEN),
                    Component.text("Shift + 右クリック : -10").color(NamedTextColor.GREEN)
            ));
            if (type == DataType.START) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.START_DAY.getId());
            } else {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, IconID.END_DAY.getId());
            }
        });
        item.setAmount(day);
        return item;
    }

    private LocalDate safeDate(int year, int month, int day) {
        if (month < 1 || month > 12) {
            month = ((month - 1) % 12 + 12) % 12 + 1;
        }
        // 月が不正なら例外にする（必要ならここも補正可能）
        YearMonth yearMonth = YearMonth.of(year, month);

        int lastDay = yearMonth.lengthOfMonth();

        if (day < 1) {
            day = 1;
        } else if (day > lastDay) {
            day = lastDay;
        }

        return LocalDate.of(year, month, day);
    }

    private ItemStack[] getDayItems(LocalDate localDate, DataType type) {
        ItemStack[] items = new ItemStack[3];
        LocalDate date = safeDate(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
        items[0] = getYearItem(date.getYear(), type);
        items[1] = getMonthItem(date.getMonthValue(), type);
        items[2] = getDayItem(date.getDayOfMonth(), type);
        return items;
    }

    private LocalDate parseDate(LocalDate date, int yearOffset, int monthOffset, int dayOffset) {
        return date.plusYears(yearOffset).plusMonths(monthOffset).plusDays(dayOffset);
    }
}
