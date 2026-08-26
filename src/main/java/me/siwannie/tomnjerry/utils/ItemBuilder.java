package me.siwannie.tomnjerry.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(Component name) {
        if (meta != null) meta.displayName(name);
        return this;
    }

    public ItemBuilder lore(Component... lines) {
        if (meta != null) meta.lore(Arrays.asList(lines));
        return this;
    }

    public ItemBuilder lore(List<Component> lines) {
        if (meta != null) meta.lore(lines);
        return this;
    }

    public ItemBuilder addTag(Plugin plugin, String key, String value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    public static boolean hasTag(ItemStack item, Plugin plugin, String key, String expectedValue) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        String value = item.getItemMeta().getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
        return expectedValue.equals(value);
    }

    public ItemBuilder unbreakable() {
        if (meta != null) meta.setUnbreakable(true);
        return this;
    }
}