package com.alkacode.backpack.util;

import com.alkacode.backpack.hook.ItemsAdderHook;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Constroi ItemStack a partir de uma secao de menus.yml/skins.yml. Suporta:
 * `material`, `texture` (cabeca base64), `itemsadder-id`, `name` e `lore`
 * (MiniMessage) e `custom_model_data`. Placeholders {x} sao substituidos antes do parse.
 */
public final class MenuItems {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MenuItems() {
    }

    public static ItemStack build(ConfigurationSection item, String... placeholders) {
        if (item == null) return new ItemStack(Material.STONE);

        String texture = item.getString("texture", "");
        String iaId = item.getString("itemsadder-id", "");

        ItemStack built;
        if (texture != null && !texture.isEmpty()) {
            built = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) built.getItemMeta();
            ItemBuilder.applyTexture(meta, texture);
            built.setItemMeta(meta);
        } else if (iaId != null && !iaId.isEmpty()) {
            ItemStack fromIa = ItemsAdderHook.getItem(iaId);
            built = fromIa != null ? fromIa : fallbackMaterial(item);
        } else {
            built = fallbackMaterial(item);
        }

        ItemMeta meta = built.getItemMeta();
        String name = item.getString("name");
        if (name != null) {
            meta.displayName(MM.deserialize(apply(placeholders, name))
                    .decoration(TextDecoration.ITALIC, false));
        }
        List<String> lore = item.getStringList("lore");
        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> comps = new ArrayList<>();
            for (String line : lore) {
                comps.add(MM.deserialize(apply(placeholders, line)).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(comps);
        }
        if (item.contains("custom_model_data")) {
            meta.setCustomModelData(item.getInt("custom_model_data"));
        }
        if (item.getBoolean("glow", false)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.addItemFlags(ItemFlag.values());
        built.setItemMeta(meta);
        return built;
    }

    /** Aplica o brilho "encantado sem mostrar encantamento" num icone ja pronto - usado
     * pelas GUIs para brilhar botoes com estado dinamico (ex.: toggle ligado). */
    public static ItemStack glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack fallbackMaterial(ConfigurationSection item) {
        String matName = item.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;
        return new ItemStack(mat);
    }

    public static String apply(String[] placeholders, String text) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }
        return text;
    }
}
