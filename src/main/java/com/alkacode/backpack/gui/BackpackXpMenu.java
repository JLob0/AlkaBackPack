package com.alkacode.backpack.gui;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.util.MenuItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Submenu de experiencia da mochila. Icones/textos/slots vem de menus.yml
 * (secao "xp"); deposita/retira niveis via botoes.
 */
public class BackpackXpMenu extends YmlMenu {

    private final AlkaBackpackPlugin plugin;
    private final Backpack backpack;
    private final int page;
    private final boolean admin;

    public BackpackXpMenu(AlkaBackpackPlugin plugin, Player player, Backpack backpack, int page, boolean admin) {
        super(plugin, player, plugin.getMenus(), "xp", "backpack-xp");
        this.plugin = plugin;
        this.backpack = backpack;
        this.page = page;
        this.admin = admin;
    }

    @Override
    protected ItemStack iconFor(String key, ConfigurationSection it) {
        return MenuItems.build(it, "{xp}", String.valueOf(backpack.getXpLevels()));
    }

    @Override
    protected void onClick(String key, ConfigurationSection it, org.bukkit.event.inventory.InventoryClickEvent e) {
        String action = it.getString("action", "");
        if ("back".equals(action)) {
            new BackpackManageMenu(plugin, player, backpack, page, admin).open();
            return;
        }
        if (!action.startsWith("xp:")) return;
        String[] parts = action.split(":");
        boolean deposit = parts[1].equals("deposit");
        int amount;
        if (parts[2].equals("all")) {
            amount = deposit ? player.getLevel() : backpack.getXpLevels();
        } else {
            amount = Integer.parseInt(parts[2]);
        }
        if (deposit) depositXp(amount);
        else withdrawXp(amount);
    }

    private void depositXp(int levels) {
        if (levels <= 0) return;
        int toDeposit = Math.min(levels, player.getLevel());
        if (toDeposit <= 0) return;
        player.setLevel(player.getLevel() - toDeposit);
        backpack.setXpLevels(backpack.getXpLevels() + toDeposit);
        plugin.getBackpackService().save(backpack);
        refresh();
    }

    private void withdrawXp(int levels) {
        if (levels <= 0) return;
        int toWithdraw = Math.min(levels, backpack.getXpLevels());
        if (toWithdraw <= 0) return;
        backpack.setXpLevels(backpack.getXpLevels() - toWithdraw);
        player.setLevel(player.getLevel() + toWithdraw);
        plugin.getBackpackService().save(backpack);
        refresh();
    }
}
