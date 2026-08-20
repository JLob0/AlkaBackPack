package com.alkacode.backpack.gui;

import com.alkacode.backpack.util.MenuItems;
import com.alkacode.backpack.util.Menus;
import com.alkacode.core.gui.BaseGui;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base para menus dirigidos por menus.yml: le titulo, tamanho, filler e itens
 * (slot/material/texture/itemsadder-id/name/lore/action). A logica de cada botao
 * fica na subclasse via {@link #onClick}; icones/textos/posicoes ficam no YAML.
 */
public abstract class YmlMenu extends BaseGui {

    protected final Menus menus;
    protected final ConfigurationSection def;

    public YmlMenu(JavaPlugin plugin, Player player, Menus menus, String menuKey, String id) {
        super(plugin, player, menus.getString(menuKey + ".title", "Menu"), menuSize(menus, menuKey), id);
        this.menus = menus;
        this.def = menus.getMenu(menuKey);
    }

    private static int menuSize(Menus menus, String menuKey) {
        ConfigurationSection menu = menus.getMenu(menuKey);
        return menu != null ? menu.getInt("size", 3) : 3;
    }

    @Override
    public void render() {
        renderFiller();
        if (def == null) return;
        ConfigurationSection items = def.getConfigurationSection("items");
        if (items == null) return;
        for (String key : items.getKeys(false)) {
            ConfigurationSection it = items.getConfigurationSection(key);
            if (it == null) continue;
            int slot = it.getInt("slot", -1);
            if (slot < 0) continue;
            ItemStack icon = iconFor(key, it);
            boolean interactive = it.contains("action") || it.contains("liquid");
            if (interactive) {
                setItem(slot, icon, e -> onClick(key, it, e));
            } else {
                setItem(slot, icon);
            }
        }
        onRender();
    }

    protected void renderFiller() {
        if (def == null) return;
        ConfigurationSection filler = def.getConfigurationSection("filler");
        if (filler == null) return;
        ItemStack f = MenuItems.build(filler);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) setItem(i, f);
        }
    }

    /** Constroi o icone de um item; subclasses sobrescrevem para adicionar placeholders dinamicos. */
    protected ItemStack iconFor(String key, ConfigurationSection it) {
        return MenuItems.build(it);
    }

    /** Acao de um botao; subclasses sobrescrevem. */
    protected void onClick(String key, ConfigurationSection it, org.bukkit.event.inventory.InventoryClickEvent e) {
    }

    /** Hook apos montar os itens estaticos (paginas, etc). */
    protected void onRender() {
    }
}
