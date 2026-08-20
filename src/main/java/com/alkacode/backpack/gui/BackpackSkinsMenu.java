package com.alkacode.backpack.gui;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.hook.ItemsAdderHook;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.util.ItemBuilder;
import com.alkacode.backpack.util.MenuItems;
import com.alkacode.backpack.util.Menus;
import com.alkacode.backpack.util.Skins;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Loja de skins: lista os skins de skins.yml em paginas (slots de menus.yml),
 * compra com AlkaEconomy e entrega o token de skin. Totalmente dirigido por YAML.
 */
public class BackpackSkinsMenu extends YmlMenu {

    private final AlkaBackpackPlugin plugin;
    private final Backpack backpack;
    private final int page;
    private final boolean admin;
    private int currentPage;

    public BackpackSkinsMenu(AlkaBackpackPlugin plugin, Player player, Backpack backpack, int page, boolean admin) {
        super(plugin, player, plugin.getMenus(), "skins", "backpack-skins");
        this.plugin = plugin;
        this.backpack = backpack;
        this.page = page;
        this.admin = admin;
        this.currentPage = 0;
    }

    @Override
    protected void onRender() {
        List<String> skinIds = plugin.getSkins().getSkinIds();
        List<Integer> slots = def.getIntegerList("skin-slots");
        int perPage = Math.max(1, slots.size());
        int start = currentPage * perPage;

        for (int i = 0; i < slots.size(); i++) {
            int idx = start + i;
            if (idx >= skinIds.size()) break;
            int slot = slots.get(i);
            setItem(slot, shopIcon(skinIds.get(idx)), e -> buy(skinIds.get(idx)));
        }
    }

    @Override
    protected void onClick(String key, ConfigurationSection it, org.bukkit.event.inventory.InventoryClickEvent e) {
        String action = it.getString("action", "");
        switch (action) {
            case "prev" -> {
                if (currentPage > 0) {
                    currentPage--;
                    refresh();
                }
            }
            case "next" -> {
                List<String> ids = plugin.getSkins().getSkinIds();
                if ((currentPage + 1) * Math.max(1, def.getIntegerList("skin-slots").size()) < ids.size()) {
                    currentPage++;
                    refresh();
                }
            }
            case "back" -> new BackpackManageMenu(plugin, player, backpack, page, admin).open();
            default -> {
            }
        }
    }

    private ItemStack shopIcon(String skinId) {
        ConfigurationSection skin = plugin.getSkins().getSkin(skinId);
        if (skin == null) return new ItemStack(Material.PAPER);

        String texture = skin.getString("texture", "");
        String iaId = skin.getString("itemsadder-id", "");
        String matName = skin.getString("item", "PAPER");
        String nome = skin.getString("name", skinId);

        ItemStack icon;
        if (texture != null && !texture.isEmpty()) {
            icon = ItemBuilder.skullTexture(texture, null, null);
        } else if (iaId != null && !iaId.isEmpty()) {
            ItemStack fromIa = ItemsAdderHook.getItem(iaId);
            icon = fromIa != null ? fromIa : new ItemStack(Material.matchMaterial(matName) != null ? Material.matchMaterial(matName) : Material.PAPER);
        } else {
            Material mat = Material.matchMaterial(matName);
            icon = new ItemStack(mat != null ? mat : Material.PAPER);
        }

        double price = skin.getDouble("price", 0.0);
        String preco = plugin.getEconomyService().format(price);
        String moeda = plugin.getEconomyService().getCurrencyName();
        List<String> loreRaw = def.getStringList("item-lore");
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : loreRaw) {
            lore.add(MiniMessage.miniMessage().deserialize(MenuItems.apply(new String[]{
                    "{preco}", preco, "{moeda}", moeda}, line)).decoration(TextDecoration.ITALIC, false));
        }

        ItemMeta meta = icon.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(nome).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private void buy(String skinId) {
        ConfigurationSection skin = plugin.getSkins().getSkin(skinId);
        if (skin == null) return;
        double price = skin.getDouble("price", 0.0);
        String permission = skin.getString("permission", "");
        if (!permission.isEmpty() && !player.hasPermission(permission)) {
            player.sendMessage(plugin.getMessages().get("no-permission"));
            return;
        }
        if (!plugin.getEconomyService().withdraw(player, price)) {
            player.sendMessage(plugin.getMessages().get("no-funds", "<preco>", plugin.getEconomyService().format(price),
                    "<moeda>", plugin.getEconomyService().getCurrencyName()));
            return;
        }
        ItemStack token = plugin.getSkins().createToken(skinId);
        if (token == null) return;
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(token);
        } else {
            player.getWorld().dropItem(player.getLocation(), token);
        }
        player.sendMessage(plugin.getMessages().get("given-backpack", "<quantidade>", "1",
                "<nome>", skin.getString("name", skinId)));
        player.closeInventory();
    }
}
