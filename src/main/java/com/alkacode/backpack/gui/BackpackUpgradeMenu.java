package com.alkacode.backpack.gui;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.economy.EconomyService;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.tier.BackpackTier;
import com.alkacode.backpack.util.MenuItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Menu de upgrades (compra de paginas via AlkaEconomy), dirigido por menus.yml
 * (secao "upgrade").
 */
public class BackpackUpgradeMenu extends YmlMenu {

    private final AlkaBackpackPlugin plugin;
    private final Backpack backpack;
    private final int page;
    private final boolean admin;

    public BackpackUpgradeMenu(AlkaBackpackPlugin plugin, Player player, Backpack backpack, int page, boolean admin) {
        super(plugin, player, plugin.getMenus(), "upgrade", "backpack-upgrade");
        this.plugin = plugin;
        this.backpack = backpack;
        this.page = page;
        this.admin = admin;
    }

    @Override
    protected ItemStack iconFor(String key, ConfigurationSection it) {
        BackpackTier tier = plugin.getTierRegistry().get(backpack.getTier()).orElse(null);
        int max = tier != null ? tier.getPaginasMax() : 1;
        EconomyService economy = plugin.getEconomyService();
        String preco = economy.format(economy.getPagePrice());
        return MenuItems.build(it, "{paginas}", String.valueOf(backpack.getUnlockedPages()),
                "{max_paginas}", String.valueOf(max),
                "{preco}", preco,
                "{moeda}", economy.getCurrencyName());
    }

    @Override
    protected void onClick(String key, ConfigurationSection it, org.bukkit.event.inventory.InventoryClickEvent e) {
        switch (key) {
            case "buy" -> buyPage();
            case "back" -> new BackpackMenu(plugin, plugin.getMessages(), plugin.getTierRegistry())
                    .open(player, backpack, page, admin);
            default -> {
            }
        }
    }

    private void buyPage() {
        BackpackTier tier = plugin.getTierRegistry().get(backpack.getTier()).orElse(null);
        int max = tier != null ? tier.getPaginasMax() : 1;
        int unlocked = backpack.getUnlockedPages();
        EconomyService economy = plugin.getEconomyService();
        double price = economy.getPagePrice();

        if (unlocked >= max) {
            player.sendMessage(plugin.getMessages().get("max-pages"));
            reopen();
            return;
        }
        if (!economy.withdraw(player, price)) {
            player.sendMessage(plugin.getMessages().get("no-funds", "<preco>", economy.format(price),
                    "<moeda>", economy.getCurrencyName()));
            reopen();
            return;
        }
        backpack.setUnlockedPages(unlocked + 1);
        plugin.getBackpackService().save(backpack);
        player.sendMessage(plugin.getMessages().get("purchased-page", "<pagina>", String.valueOf(unlocked + 1),
                "<preco>", economy.format(price), "<moeda>", economy.getCurrencyName()));
        reopen();
    }

    private void reopen() {
        new BackpackUpgradeMenu(plugin, player, backpack, page, admin).open();
    }
}
