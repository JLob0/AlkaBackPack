package com.alkacode.backpack.gui;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.tier.BackpackTier;
import com.alkacode.backpack.tier.TierRegistry;
import com.alkacode.backpack.util.MenuItems;
import com.alkacode.backpack.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Monta e abre o inventario de conteudo da mochila (holder proprio, permite
 * drag/drop). Layout via {@link BackpackLayout}: fileiras de armazenamento +
 * fileira de controle na base, nunca excedendo 54 slots.
 */
public class BackpackMenu {

    private final JavaPlugin plugin;
    private final Messages messages;
    private final TierRegistry tierRegistry;

    public BackpackMenu(JavaPlugin plugin, Messages messages, TierRegistry tierRegistry) {
        this.plugin = plugin;
        this.messages = messages;
        this.tierRegistry = tierRegistry;
    }

    public void open(Player viewer, Backpack backpack, int page, boolean admin) {
        Optional<BackpackTier> opt = tierRegistry.get(backpack.getTier());
        if (opt.isEmpty()) return;
        BackpackTier tier = opt.get();

        int slots = tier.getSlots();
        int storageEnd = BackpackLayout.storageEnd(slots);
        int inventorySize = BackpackLayout.inventorySize(slots);
        int unlockedPages = Math.max(1, backpack.getUnlockedPages());
        if (page < 0) page = 0;
        if (page >= unlockedPages) page = unlockedPages - 1;

        // A barra de titulo do inventario do Minecraft tem largura fixa em pixels (nao
        // quebra linha nem encolhe fonte) - nomes de tier com "Mochila " completo +
        // sufixo de pagina passam de ~21 caracteres e ficam cortados na tela. Como o
        // jogador ja sabe que esta numa mochila (acabou de abrir), tira o prefixo
        // redundante so no titulo; o nome completo continua em chat/lore/list.
        String shortName = tier.getNome().replaceFirst("^Mochila\\s+", "");
        String title = (admin ? "<gold>❖ Admin: " : "") + tier.getCor() + shortName;
        if (unlockedPages > 1) {
            title += " <white>[" + (page + 1) + "/" + unlockedPages + "]";
        }
        Component titleComponent = MiniMessage.miniMessage().deserialize(title);

        BackpackHolder holder = new BackpackHolder(backpack, backpack.getId(), backpack.getTier(), page, admin);
        Inventory inv = Bukkit.createInventory(holder, inventorySize, titleComponent);
        holder.setInventory(inv);

        ItemStack[] pageItems = backpack.getPage(page);
        for (int slot = 0; slot < storageEnd; slot++) {
            if (slot < pageItems.length && pageItems[slot] != null && !pageItems[slot].getType().isAir()) {
                inv.setItem(slot, pageItems[slot].clone());
            }
        }

        // lacra os slots de sobra entre o armazenamento e a fileira de controle
        int controlStart = BackpackLayout.controlStart(slots);
        for (int i = storageEnd; i < controlStart; i++) {
            inv.setItem(i, glass(Material.BLACK_STAINED_GLASS_PANE));
        }

        setupControlBar(inv, page, unlockedPages, slots);
        viewer.openInventory(inv);
    }

    private void setupControlBar(Inventory inv, int page, int unlockedPages, int slots) {
        int controlStart = BackpackLayout.controlStart(slots);
        int controlEnd = BackpackLayout.controlEnd(slots);
        if (controlStart > controlEnd) return; // sem espaco para controle

        ConfigurationSection control = plugin instanceof AlkaBackpackPlugin abp
                ? abp.getMenus().getMenu("control") : null;

        int idx = 0;
        if (page > 0) {
            inv.setItem(controlStart + idx, controlItem(control, "prev"));
        }
        idx++;
        inv.setItem(controlStart + idx, controlItem(control, "manage"));
        idx++;
        inv.setItem(controlStart + idx, controlItem(control, "upgrade"));
        idx++;
        inv.setItem(controlStart + idx, controlItem(control, "password"));
        idx++;
        if (page < unlockedPages - 1 && controlStart + idx <= controlEnd) {
            inv.setItem(controlEnd, controlItem(control, "next"));
        }
    }

    private ItemStack controlItem(ConfigurationSection control, String key) {
        if (control != null) {
            ConfigurationSection it = control.getConfigurationSection(key);
            if (it != null) return MenuItems.build(it);
        }
        // fallback caso o yml nao tenha
        Material mat = switch (key) {
            case "prev", "next" -> Material.ARROW;
            case "manage" -> Material.HOPPER;
            case "upgrade" -> Material.EXPERIENCE_BOTTLE;
            case "password" -> Material.BOOK;
            default -> Material.STONE;
        };
        return item(mat, key);
    }

    private ItemStack item(Material material, String miniName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage()
                .deserialize(miniName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack glass(Material mat) {
        ItemStack glass = new ItemStack(mat);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        return glass;
    }
}
