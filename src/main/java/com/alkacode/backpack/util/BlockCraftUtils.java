package com.alkacode.backpack.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Conversao de lingotes em blocos (9 ingots -> 1 block) para o botao
 * "converter lingotes em blocos" do menu de gerenciamento.
 */
public final class BlockCraftUtils {

    private BlockCraftUtils() {
    }

    private static final Map<Material, Material> INGOT_TO_BLOCK = new HashMap<>();

    static {
        register(Material.IRON_INGOT, Material.IRON_BLOCK);
        register(Material.GOLD_INGOT, Material.GOLD_BLOCK);
        register(Material.DIAMOND, Material.DIAMOND_BLOCK);
        register(Material.EMERALD, Material.EMERALD_BLOCK);
        register(Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK);
        register(Material.LAPIS_LAZULI, Material.LAPIS_BLOCK);
        register(Material.REDSTONE, Material.REDSTONE_BLOCK);
        register(Material.COAL, Material.COAL_BLOCK);
        register(Material.QUARTZ, Material.QUARTZ_BLOCK);
        register(Material.RAW_IRON, Material.RAW_IRON_BLOCK);
        register(Material.RAW_GOLD, Material.RAW_GOLD_BLOCK);
        register(Material.RAW_COPPER, Material.RAW_COPPER_BLOCK);
        register(Material.COPPER_INGOT, Material.COPPER_BLOCK);
        register(Material.AMETHYST_SHARD, Material.AMETHYST_BLOCK);
        register(Material.INK_SAC, Material.BLACK_DYE);
    }

    private static void register(Material ingot, Material block) {
        INGOT_TO_BLOCK.put(ingot, block);
    }

    /**
     * Tenta converter um ItemStack de ingote em bloco. Retorna o ItemStack de bloco
     * com o maximo possivel de unidades (9 ingots -> 1 bloco), ou null se o item nao
     * for conversivel ou nao tiver 9+ unidades.
     */
    public static ItemStack tryConvert(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        Material block = INGOT_TO_BLOCK.get(item.getType());
        if (block == null) return null;
        int ingots = item.getAmount();
        int blocks = ingots / 9;
        if (blocks <= 0) return null;
        ItemStack result = new ItemStack(block, blocks);
        return result;
    }

    public static boolean isConvertible(ItemStack item) {
        return item != null && INGOT_TO_BLOCK.containsKey(item.getType());
    }
}
