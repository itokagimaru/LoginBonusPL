package io.github.itokagimaru.loginBonusPL.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemStackUtil {

    // List<ItemStack> → YAML文字列
    public static String toYaml(List<ItemStack> items) {

        YamlConfiguration config = new YamlConfiguration();

        for (int i = 0; i < items.size(); i++) {
            config.set("items." + i, items.get(i));
        }

        return config.saveToString();
    }

    // YAML文字列 → List<ItemStack>
    public static List<ItemStack> fromYaml(String yaml) {

        YamlConfiguration config = new YamlConfiguration();

        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        List<ItemStack> items = new ArrayList<>();

        if (config.contains("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                items.add(config.getItemStack("items." + key));
            }
        }

        return items;
    }
}
