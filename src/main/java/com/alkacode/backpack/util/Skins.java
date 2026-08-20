package com.alkacode.backpack.util;

import com.alkacode.backpack.hook.CosmeticsCoreHook;
import com.alkacode.backpack.hook.ItemsAdderHook;
import com.alkacode.backpack.service.BackpackService;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * Loja de skins: carrega skins.yml, cria o token de skin (item que, ao ser
 * aplicado na mochila, muda a aparencia) e aplica a skin no item da mochila.
 * Tudo (skins, token, preco, permissao) vem de skins.yml.
 */
public class Skins {

    private final JavaPlugin plugin;
    private final BackpackService service;
    private final NamespacedKey TOKEN_KEY;
    private final NamespacedKey SKIN_ID_KEY;
    private final NamespacedKey SKIN_APPLIED_KEY;
    private FileConfiguration config;

    public Skins(JavaPlugin plugin, BackpackService service) {
        this.plugin = plugin;
        this.service = service;
        this.TOKEN_KEY = new NamespacedKey(plugin, "alka_skin_token");
        this.SKIN_ID_KEY = new NamespacedKey(plugin, "alka_skin_id");
        this.SKIN_APPLIED_KEY = new NamespacedKey(plugin, "alka_skin");
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "skins.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("skins.yml")) {
                if (in != null) Files.copy(in, file.toPath());
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar skins.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        try (InputStream defaultStream = plugin.getResource("skins.yml")) {
            if (defaultStream != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao carregar defaults de skins.yml: " + e.getMessage());
        }
    }

    public ConfigurationSection getSkin(String id) {
        return config.getConfigurationSection("skins." + id);
    }

    public List<String> getSkinIds() {
        ConfigurationSection section = config.getConfigurationSection("skins");
        return section == null ? List.of() : section.getKeys(false).stream().toList();
    }

    public ConfigurationSection getTokenConfig() {
        return config.getConfigurationSection("settings.skin-token");
    }

    // ---------------------------------------------------------------- token

    public ItemStack createToken(String skinId) {
        ConfigurationSection token = getTokenConfig();
        if (token == null) return null;
        ConfigurationSection skin = getSkin(skinId);
        String nome = skin != null ? skin.getString("name", skinId) : skinId;
        ItemStack item = MenuItems.build(token, "{nome}", nome);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(TOKEN_KEY, PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(SKIN_ID_KEY, PersistentDataType.STRING, skinId);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isToken(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(TOKEN_KEY, PersistentDataType.STRING);
    }

    public String getTokenSkinId(ItemStack item) {
        if (!isToken(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(SKIN_ID_KEY, PersistentDataType.STRING);
    }

    // ---------------------------------------------------------------- aplicar

    /**
     * Aplica a skin num item de mochila: muda a textura (ou o item ItemsAdder) e
     * grava o id da skin no PDC. Preserva os dados de tier/id/dono da mochila.
     * Se a skin (velha e/ou nova) tiver um cosmetico BODY_ITEM registrado no
     * CosmeticsCore (mesmo id da skin), desequipa o antigo e equipa o novo —
     * e o que faz a mochila aparecer de verdade nas costas do jogador.
     */
    public ItemStack applySkin(Player player, ItemStack backpackItem, String skinId) {
        ConfigurationSection skin = getSkin(skinId);
        if (skin == null) return backpackItem;

        String texture = skin.getString("texture", "");
        String iaId = skin.getString("itemsadder-id", "");
        String nome = skin.getString("name", null);

        String id = service.getBackpackId(backpackItem);
        int tier = service.getItemTier(backpackItem);
        UUID owner = service.getItemOwner(backpackItem);
        if (id == null) return backpackItem;

        String oldSkinId = getAppliedSkinId(backpackItem);
        if (oldSkinId != null && !oldSkinId.equals(skinId)) {
            CosmeticsCoreHook.unequip(player, resolveCosmeticKey(oldSkinId));
        }
        if (service.isBackCosmeticEnabled(player.getUniqueId())) {
            CosmeticsCoreHook.equip(player, resolveCosmeticKey(skinId));
        }

        ItemStack result;
        if (texture != null && !texture.isEmpty()) {
            result = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) result.getItemMeta();
            ItemBuilder.applyTexture(meta, texture);
            result.setItemMeta(meta);
        } else if (iaId != null && !iaId.isEmpty()) {
            ItemStack fromIa = ItemsAdderHook.getItem(iaId);
            result = fromIa != null ? fromIa : backpackItem.clone();
        } else {
            result = backpackItem.clone();
        }

        result = service.stampIdentity(result, id, tier, owner);

        ItemMeta meta = result.getItemMeta();
        if (nome != null) {
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize(nome).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.getPersistentDataContainer().set(SKIN_APPLIED_KEY, PersistentDataType.STRING, skinId);
        result.setItemMeta(meta);
        return result;
    }

    public boolean hasSkin(ItemStack item) {
        return getAppliedSkinId(item) != null;
    }

    public String getAppliedSkinId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(SKIN_APPLIED_KEY, PersistentDataType.STRING);
    }

    /**
     * Chave do cosmetico BODY_ITEM no CosmeticsCore pra essa skin: usa
     * "cosmeticscore-id" se a skin tiver (ex: packs de terceiro, onde a chave
     * do vendor diverge do id local), senao cai pro proprio id da skin (ex: as
     * skins do Traveler's Backpack, onde controlamos os dois lados e as chaves
     * ja sao identicas de proposito).
     */
    public String resolveCosmeticKey(String skinId) {
        ConfigurationSection s = getSkin(skinId);
        return s != null ? s.getString("cosmeticscore-id", skinId) : skinId;
    }

    /** Todas as chaves de cosmetico que ESTE plugin gerencia (uma por skin). Usado pelo
     * reconciliador pra nunca mexer num cosmetico equipado por outro sistema (chapeu,
     * balao etc do proprio CosmeticsCore). */
    public java.util.Set<String> getAllCosmeticKeys() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (String skinId : getSkinIds()) keys.add(resolveCosmeticKey(skinId));
        return keys;
    }
}
