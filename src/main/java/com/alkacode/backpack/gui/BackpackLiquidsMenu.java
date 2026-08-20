package com.alkacode.backpack.gui;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.util.MenuItems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Submenu de tanques de liquido (agua, lava, leite, neve, mel). Icones, textos e
 * slots vem de menus.yml (secao "liquids"); esquerda deposita, direita retira.
 */
public class BackpackLiquidsMenu extends YmlMenu {

    private final AlkaBackpackPlugin plugin;
    private final Backpack backpack;
    private final int page;
    private final boolean admin;

    private static final Map<String, Material> LIQUID_ITEM = new HashMap<>();
    static {
        LIQUID_ITEM.put("WATER", Material.WATER_BUCKET);
        LIQUID_ITEM.put("LAVA", Material.LAVA_BUCKET);
        LIQUID_ITEM.put("MILK", Material.MILK_BUCKET);
        LIQUID_ITEM.put("SNOW", Material.SNOWBALL);
        LIQUID_ITEM.put("HONEY", Material.HONEY_BOTTLE);
    }

    public BackpackLiquidsMenu(AlkaBackpackPlugin plugin, Player player, Backpack backpack, int page, boolean admin) {
        super(plugin, player, plugin.getMenus(), "liquids", "backpack-liquids");
        this.plugin = plugin;
        this.backpack = backpack;
        this.page = page;
        this.admin = admin;
    }

    @Override
    protected ItemStack iconFor(String key, ConfigurationSection it) {
        if (it.contains("liquid")) {
            int capacity = plugin.getConfig().getInt("settings.liquid-capacity", 128);
            String type = it.getString("liquid");
            int current = backpack.getTank(type);
            return MenuItems.build(it, "{capacidade}", String.valueOf(capacity), "{atual}", String.valueOf(current));
        }
        return MenuItems.build(it);
    }

    @Override
    protected void onClick(String key, ConfigurationSection it, org.bukkit.event.inventory.InventoryClickEvent e) {
        String action = it.getString("action", "");
        if ("back".equals(action)) {
            reopen();
            return;
        }
        if (it.contains("liquid")) {
            String type = it.getString("liquid");
            if (e.isRightClick()) withdrawLiquid(type);
            else depositLiquid(type);
        }
    }

    private void depositLiquid(String type) {
        Material needed = LIQUID_ITEM.get(type);
        int slot = player.getInventory().first(needed);
        if (slot < 0) {
            player.sendMessage(plugin.getMessages().get("item-not-allowed"));
            return;
        }
        int capacity = plugin.getConfig().getInt("settings.liquid-capacity", 128);
        if (backpack.getTank(type) >= capacity) {
            player.sendMessage(plugin.getMessages().get("backpack-full"));
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        boolean returnsBucket = needed == Material.WATER_BUCKET || needed == Material.LAVA_BUCKET;
        player.getInventory().setItem(slot, stack.getAmount() > 1 ? stack.subtract(1) : null);
        if (returnsBucket) player.getInventory().addItem(new ItemStack(Material.BUCKET));
        backpack.setTank(type, backpack.getTank(type) + 1);
        plugin.getBackpackService().save(backpack);
        refresh();
    }

    private void withdrawLiquid(String type) {
        int current = backpack.getTank(type);
        if (current <= 0) {
            player.sendMessage(plugin.getMessages().get("backpack-full"));
            return;
        }
        if (!player.getInventory().addItem(new ItemStack(LIQUID_ITEM.get(type))).isEmpty()) {
            player.sendMessage(plugin.getMessages().get("backpack-full"));
            return;
        }
        backpack.setTank(type, current - 1);
        plugin.getBackpackService().save(backpack);
        refresh();
    }

    private void reopen() {
        new BackpackManageMenu(plugin, player, backpack, page, admin).open();
    }
}
