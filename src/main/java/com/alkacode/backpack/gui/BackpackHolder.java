package com.alkacode.backpack.gui;

import com.alkacode.backpack.model.Backpack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Holder do inventario de conteudo da mochila. Diferente de BaseGui porque o
 * conteudo permite drag/drop de itens (o GuiListener do Core cancela arrastes);
 * por isso usa InventoryHolder proprio + BackpackInventoryListener, como o
 * AlkaEnderChest faz com seu EnderChestHolder.
 */
public class BackpackHolder implements InventoryHolder {

    private final String backpackId;
    private final int tier;
    private final int page;
    private final boolean admin;
    private final Backpack backpack;
    private Inventory inventory;

    public BackpackHolder(Backpack backpack, String backpackId, int tier, int page, boolean admin) {
        this.backpack = backpack;
        this.backpackId = backpackId;
        this.tier = tier;
        this.page = page;
        this.admin = admin;
    }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public String getBackpackId() { return backpackId; }
    public int getTier() { return tier; }
    public int getPage() { return page; }
    public boolean isAdmin() { return admin; }
    public Backpack getBackpack() { return backpack; }
}
