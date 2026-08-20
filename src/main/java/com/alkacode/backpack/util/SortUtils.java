package com.alkacode.backpack.util;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordenacao e fusao de itens num inventario (padrao do AlkaEnderChest,
 * generalizado para um array de ItemStack). Usado pelo botao "organizar" e
 * pela coleta automatica de itens iguais.
 */
public final class SortUtils {

    private SortUtils() {
    }

    /** Empilha itens iguais (mesmo tipo/dados) respeitando o stack size. */
    public static List<ItemStack> stackItems(List<ItemStack> items) {
        List<ItemStack> stacked = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            boolean fullyMerged = false;
            for (ItemStack s : stacked) {
                if (s.isSimilar(item)) {
                    int room = s.getMaxStackSize() - s.getAmount();
                    if (room > 0) {
                        int transfer = Math.min(room, item.getAmount());
                        s.setAmount(s.getAmount() + transfer);
                        item.setAmount(item.getAmount() - transfer);
                        if (item.getAmount() <= 0) {
                            fullyMerged = true;
                            break;
                        }
                    }
                }
            }
            if (!fullyMerged && item.getAmount() > 0) {
                stacked.add(item);
            }
        }
        return stacked;
    }

    /** Ordena uma lista de itens por material e depois por nome de display. */
    public static List<ItemStack> sort(List<ItemStack> items) {
        List<ItemStack> copy = new ArrayList<>(items);
        copy.sort((i1, i2) -> {
            if (i1.getType() != i2.getType()) {
                return i1.getType().compareTo(i2.getType());
            }
            return i1.displayName().toString().compareTo(i2.displayName().toString());
        });
        return copy;
    }
}
