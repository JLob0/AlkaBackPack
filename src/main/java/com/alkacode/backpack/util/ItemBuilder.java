package com.alkacode.backpack.util;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builder de ItemStack em MiniMessage. Reaproveita o padrao do AlkaEnderChest
 * (ItemBuilder.fromConfig) e adiciona suporte a cabeca com textura Base64 custom
 * (para as mochilas fisicas com skin por tier).
 */
public final class ItemBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ItemBuilder() {
    }

    public static ItemStack fromConfig(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.STONE);

        String matName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = section.getString("name");
        if (name != null) {
            meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
        }

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.lore(lore.stream()
                    .map(line -> MM.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .collect(Collectors.toList()));
        }

        if (section.contains("custom_model_data")) {
            meta.setCustomModelData(section.getInt("custom_model_data"));
        }

        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    /** PLAYER_HEAD com textura Base64 custom, nome e lore em MiniMessage (nao-italico). */
    public static ItemStack skullTexture(String base64Texture, String name, List<String> loreLines) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (name != null) {
            meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
        }
        if (loreLines != null && !loreLines.isEmpty()) {
            meta.lore(loreLines.stream()
                    .map(line -> MM.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .collect(Collectors.toList()));
        }
        applyTexture(meta, base64Texture);
        meta.addItemFlags(ItemFlag.values());
        skull.setItemMeta(meta);
        return skull;
    }

    public static void applyTexture(SkullMeta meta, String base64Texture) {
        try {
            com.destroystokyo.paper.profile.PlayerProfile profile =
                    org.bukkit.Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", base64Texture));
            meta.setPlayerProfile(profile);
        } catch (Exception ignored) {
            // textura invalida - deixa a cabeca padrao
        }
    }
}
