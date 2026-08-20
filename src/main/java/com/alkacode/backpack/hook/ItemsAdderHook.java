package com.alkacode.backpack.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * Hook leve para ItemsAdder via reflexao (softdepend, sem dependencia de compilacao).
 * Resolve "namespace:id" -> ItemStack do ItemsAdder; retorna null se ausente/erro.
 */
public final class ItemsAdderHook {

    private static boolean checked = false;
    private static boolean available = false;

    private ItemsAdderHook() {
    }

    public static boolean isAvailable() {
        if (!checked) {
            available = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
            checked = true;
        }
        return available;
    }

    public static ItemStack getItem(String id) {
        if (!isAvailable() || id == null || id.isEmpty()) return null;
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object instance = customStack.getMethod("getInstance", String.class).invoke(null, id);
            if (instance == null) return null;
            return (ItemStack) customStack.getMethod("getItemStack").invoke(instance);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getCustomId(ItemStack item) {
        if (!isAvailable() || item == null) return null;
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object instance = customStack.getMethod("byItemStack", ItemStack.class).invoke(null, item);
            if (instance == null) return null;
            return (String) customStack.getMethod("getNamespacedID").invoke(instance);
        } catch (Throwable t) {
            return null;
        }
    }
}
