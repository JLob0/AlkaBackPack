package com.alkacode.backpack.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * Hook leve para CosmeticsCore via reflexao (softdepend, sem dependencia de compilacao).
 * Equipa/desequipa cosmeticos BODY_ITEM (mochila nas costas) pelo id registrado
 * em resource-pack/CosmeticsCore/cosmetics/*.yml. O resultado visual nunca quebra
 * a aplicacao da skin (item continua trocando normalmente mesmo se isso falhar),
 * mas todo caminho de falha loga um warning — nada e engolido em silencio.
 */
public final class CosmeticsCoreHook {

    private static final Logger LOG = Logger.getLogger("AlkaBackpack");

    private static boolean checked = false;
    private static boolean available = false;

    private CosmeticsCoreHook() {
    }

    public static boolean isAvailable() {
        if (!checked) {
            available = Bukkit.getPluginManager().getPlugin("CosmeticsCore") != null;
            checked = true;
            if (!available) {
                LOG.warning("[CosmeticsCoreHook] Plugin CosmeticsCore nao encontrado — cosmeticos de mochila (nas costas) ficam desativados, so o item na mao troca.");
            }
        }
        return available;
    }

    /**
     * Silencioso de proposito — usado tambem pra filtrar em lote (ex: o
     * reconciliador periodico), onde "nao registrado" e normal (skins sem
     * cosmetico, tipo as de textura pura) e nao deve virar log a cada checagem.
     */
    public static boolean isRegistered(String key) {
        if (!isAvailable() || key == null || key.isEmpty()) return false;
        try {
            Class<?> api = Class.forName("dev.lone.cosmeticscore.api.temporary.CosmeticsCoreApi");
            return (boolean) api.getMethod("isCosmeticRegistered", String.class).invoke(null, key);
        } catch (Throwable t) {
            LOG.warning("[CosmeticsCoreHook] Falha ao checar cosmetico '" + key + "' via reflexao: " + t);
            return false;
        }
    }

    public static void equip(Player player, String key) {
        if (player == null) return;
        if (!isRegistered(key)) {
            LOG.warning("[CosmeticsCoreHook] Cosmetico '" + key + "' nao esta registrado no CosmeticsCore — "
                    + "confere se o yml da skin esta mesmo em plugins/CosmeticsCore/cosmetics/ no host e se deu reload nele.");
            return;
        }
        try {
            Object accessor = newAccessor(key, player);
            if (accessor != null) accessor.getClass().getMethod("equip").invoke(accessor);
        } catch (Throwable t) {
            LOG.warning("[CosmeticsCoreHook] Falha ao equipar cosmetico '" + key + "' em " + player.getName() + ": " + t);
        }
    }

    public static void unequip(Player player, String key) {
        if (player == null || !isAvailable() || key == null || key.isEmpty()) return;
        try {
            Object accessor = newAccessor(key, player);
            if (accessor == null) return;
            Method isEquipped = accessor.getClass().getMethod("isEquipped");
            if ((boolean) isEquipped.invoke(accessor)) {
                accessor.getClass().getMethod("unequip").invoke(accessor);
            }
        } catch (Throwable t) {
            LOG.warning("[CosmeticsCoreHook] Falha ao desequipar cosmetico '" + key + "' em " + player.getName() + ": " + t);
        }
    }

    /** Chaves de TODOS os cosmeticos equipados no jogador agora (qualquer tipo, nao so os nossos). */
    @SuppressWarnings("unchecked")
    public static List<String> getEquippedKeys(Player player) {
        if (player == null || !isAvailable()) return List.of();
        try {
            Class<?> api = Class.forName("dev.lone.cosmeticscore.api.temporary.CosmeticsCoreApi");
            Object result = api.getMethod("getEquippedCosmeticsKeys", org.bukkit.entity.Player.class).invoke(null, player);
            return result != null ? (List<String>) result : List.of();
        } catch (Throwable t) {
            LOG.warning("[CosmeticsCoreHook] Falha ao listar cosmeticos equipados de " + player.getName() + ": " + t);
            return List.of();
        }
    }

    private static Object newAccessor(String key, Player player) throws Exception {
        Class<?> api = Class.forName("dev.lone.cosmeticscore.api.temporary.CosmeticsCoreApi");
        Class<?> entityClass = Class.forName("org.bukkit.entity.Entity");
        return api.getMethod("newCosmeticAccessor", String.class, entityClass).invoke(null, key, player);
    }
}
