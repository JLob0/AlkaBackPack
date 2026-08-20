package com.alkacode.backpack.gui;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.util.BlockCraftUtils;
import com.alkacode.backpack.util.MenuItems;
import com.alkacode.backpack.util.SortUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu de gerenciamento da pagina atual (dirigido por menus.yml, secao "manage"):
 * organizar, minerar, coleta automatica, converter lingotes em blocos, e atalhos
 * para Liquidos, XP e Loja de Skins. Opera direto na pagina do Backpack e salva.
 */
public class BackpackManageMenu extends YmlMenu {

    private final AlkaBackpackPlugin plugin;
    private final Backpack backpack;
    private final int page;
    private final boolean admin;

    public BackpackManageMenu(AlkaBackpackPlugin plugin, Player player, Backpack backpack, int page, boolean admin) {
        super(plugin, player, plugin.getMenus(), "manage", "backpack-manage");
        this.plugin = plugin;
        this.backpack = backpack;
        this.page = page;
        this.admin = admin;
    }

    @Override
    protected ItemStack iconFor(String key, ConfigurationSection it) {
        if ("mining".equals(key)) {
            String status = backpack.isMining() ? "<green>ON" : "<red>OFF";
            ItemStack icon = MenuItems.build(it, "{status}", status);
            return backpack.isMining() ? MenuItems.glow(icon) : icon;
        }
        if ("auto".equals(key)) {
            String status = backpack.isAutoCollect() ? "<green>ON" : "<red>OFF";
            ItemStack icon = MenuItems.build(it, "{status}", status);
            return backpack.isAutoCollect() ? MenuItems.glow(icon) : icon;
        }
        return MenuItems.build(it);
    }

    @Override
    protected void onClick(String key, ConfigurationSection it, org.bukkit.event.inventory.InventoryClickEvent e) {
        switch (key) {
            case "sort" -> {
                sortPage();
                reopen();
            }
            case "mining" -> {
                backpack.setMining(!backpack.isMining());
                plugin.getBackpackService().save(backpack);
                reopen();
            }
            case "auto" -> {
                backpack.setAutoCollect(!backpack.isAutoCollect());
                plugin.getBackpackService().save(backpack);
                reopen();
            }
            case "convert" -> {
                convertIngots();
                reopen();
            }
            case "liquids" -> new BackpackLiquidsMenu(plugin, player, backpack, page, admin).open();
            case "xp" -> new BackpackXpMenu(plugin, player, backpack, page, admin).open();
            case "skins" -> new BackpackSkinsMenu(plugin, player, backpack, page, admin).open();
            case "back" -> reopen();
            default -> {
            }
        }
    }

    private void sortPage() {
        ItemStack[] pageItems = backpack.getPage(page);
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : pageItems) {
            if (item != null && !item.getType().isAir()) items.add(item);
        }
        List<ItemStack> sorted = SortUtils.sort(SortUtils.stackItems(items));
        ItemStack[] fixed = new ItemStack[pageItems.length];
        for (int i = 0; i < sorted.size() && i < fixed.length; i++) fixed[i] = sorted.get(i);
        backpack.setPage(page, fixed, pageItems.length);
        plugin.getBackpackService().save(backpack);
    }

    private void convertIngots() {
        ItemStack[] pageItems = backpack.getPage(page);
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : pageItems) {
            if (item == null || item.getType().isAir()) continue;
            ItemStack block = BlockCraftUtils.tryConvert(item);
            if (block != null) {
                int leftover = item.getAmount() % 9;
                if (leftover > 0) {
                    ItemStack rest = item.clone();
                    rest.setAmount(leftover);
                    result.add(rest);
                }
                result.add(block);
            } else {
                result.add(item);
            }
        }
        ItemStack[] fixed = new ItemStack[pageItems.length];
        for (int i = 0; i < result.size() && i < fixed.length; i++) fixed[i] = result.get(i);
        backpack.setPage(page, fixed, pageItems.length);
        plugin.getBackpackService().save(backpack);
    }

    private void reopen() {
        new BackpackMenu(plugin, plugin.getMessages(), plugin.getTierRegistry())
                .open(player, backpack, page, admin);
    }
}
