package com.alkacode.backpack.listener;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.gui.BackpackHolder;
import com.alkacode.backpack.gui.BackpackLayout;
import com.alkacode.backpack.gui.BackpackManageMenu;
import com.alkacode.backpack.gui.BackpackMenu;
import com.alkacode.backpack.gui.BackpackUpgradeMenu;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.tier.BackpackTier;
import com.alkacode.backpack.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Controla o inventario de conteudo da mochila (BackpackHolder): permite colocar/
 * tirar itens, valida blacklist, trata a fileira de controle (paginas, gerenciar,
 * upgrade, senha) e sincroniza de volta ao Backpack ao fechar.
 */
public class BackpackInventoryListener implements Listener {

    private final AlkaBackpackPlugin plugin;
    private final BackpackService service;
    private final Messages messages;

    public BackpackInventoryListener(AlkaBackpackPlugin plugin, BackpackService service, Messages messages) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackHolder holder)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        int storageEnd = BackpackLayout.storageEnd(slots(holder));
        int controlStart = BackpackLayout.controlStart(slots(holder));
        int controlEnd = BackpackLayout.controlEnd(slots(holder));
        if (event.getClickedInventory() == event.getInventory()) {
            // fileira de controle / lacre - sempre clicavel, mesmo com a mochila
            // trancada por senha: e o unico jeito de digitar a senha e destrancar.
            if (event.getSlot() >= storageEnd) {
                if (event.getSlot() >= controlStart && event.getSlot() <= controlEnd) {
                    handleControl(player, holder, event.getSlot());
                }
                return;
            }
            if (!canEdit(player, holder)) return;
            // colocando item da mao -> se blacklistado, bloqueia
            ItemStack placing = event.getCursor() != null ? event.getCursor() : event.getCurrentItem();
            if (service.isBlacklisted(placing)) {
                player.sendMessage(messages.get("item-not-allowed"));
                return;
            }
            event.setCancelled(false);
            return;
        }
        if (!canEdit(player, holder)) return;
        // clique no inventario do proprio jogador
        ItemStack carried = event.getCurrentItem();
        if (carried != null && service.isBlacklisted(carried)) {
            player.sendMessage(messages.get("item-not-allowed"));
            return;
        }
        if (carried != null && event.isShiftClick()) {
            // O shift-click padrao do Bukkit varre TODO o inventario de cima, inclusive
            // a fileira de controle (paginas/gerenciar/upgrade/senha) - um item podia
            // cair num slot de botao e sumir pra sempre (syncPage so le a area de
            // storage). Move manualmente so dentro da area de storage.
            shiftIntoStorage(event, carried, storageEnd);
            return;
        }
        event.setCancelled(false);
    }

    /** Distribui `carried` nos slots de storage (0..storageEnd-1) do inventario de cima:
     * primeiro tenta empilhar em pilhas compativeis, depois usa slots vazios. Sobra (mochila
     * cheia) fica no slot de origem, igual o comportamento vanilla de shift-click normal. */
    private void shiftIntoStorage(InventoryClickEvent event, ItemStack carried, int storageEnd) {
        Inventory top = event.getInventory();
        int remaining = carried.getAmount();
        for (int i = 0; i < storageEnd && remaining > 0; i++) {
            ItemStack slotItem = top.getItem(i);
            if (slotItem != null && !slotItem.getType().isAir() && slotItem.isSimilar(carried)) {
                int space = slotItem.getMaxStackSize() - slotItem.getAmount();
                if (space > 0) {
                    int move = Math.min(space, remaining);
                    slotItem.setAmount(slotItem.getAmount() + move);
                    remaining -= move;
                }
            }
        }
        for (int i = 0; i < storageEnd && remaining > 0; i++) {
            ItemStack slotItem = top.getItem(i);
            if (slotItem == null || slotItem.getType().isAir()) {
                int move = Math.min(carried.getMaxStackSize(), remaining);
                ItemStack toPlace = carried.clone();
                toPlace.setAmount(move);
                top.setItem(i, toPlace);
                remaining -= move;
            }
        }
        event.setCurrentItem(remaining <= 0 ? null : withAmount(carried, remaining));
        event.setCancelled(true);
    }

    private ItemStack withAmount(ItemStack base, int amount) {
        ItemStack copy = base.clone();
        copy.setAmount(amount);
        return copy;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackHolder holder)) return;
        Player player = (Player) event.getWhoClicked();
        if (!canEdit(player, holder)) {
            event.setCancelled(true);
            return;
        }
        int storageEnd = BackpackLayout.storageEnd(slots(holder));
        for (Integer slot : event.getRawSlots()) {
            if (slot >= storageEnd || service.isBlacklisted(event.getOldCursor())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackHolder holder)) return;
        syncPage(holder, event.getInventory());
    }

    // ------------------------------------------------------------------

    private boolean canEdit(Player player, BackpackHolder holder) {
        if (holder.isAdmin()) return true;
        Backpack bp = holder.getBackpack();
        if (!bp.getOwner().equals(player.getUniqueId())) return false;
        if (bp.isProtected() && !service.isUnlocked(player.getUniqueId(), holder.getBackpackId())) return false;
        return true;
    }

    private int slots(BackpackHolder holder) {
        return plugin.getTierRegistry().get(holder.getTier()).map(BackpackTier::getSlots).orElse(9);
    }

    private void handleControl(Player player, BackpackHolder holder, int slot) {
        Backpack bp = holder.getBackpack();
        int controlStart = BackpackLayout.controlStart(slots(holder));
        int controlEnd = BackpackLayout.controlEnd(slots(holder));
        int unlocked = Math.max(1, bp.getUnlockedPages());
        int page = holder.getPage();
        int idx = slot - controlStart;

        if (slot == controlEnd && page < unlocked - 1) {
            new BackpackMenu(plugin, messages, plugin.getTierRegistry()).open(player, bp, page + 1, holder.isAdmin());
        } else if (idx == 1) {
            new BackpackManageMenu(plugin, player, bp, page, holder.isAdmin()).open();
        } else if (idx == 2) {
            new BackpackUpgradeMenu(plugin, player, bp, page, holder.isAdmin()).open();
        } else if (idx == 3) {
            if (bp.isProtected()) {
                plugin.getPasswordGuard().requestPassword(player, holder.getBackpackId());
            } else {
                plugin.getPasswordGuard().requestSetPassword(player, holder.getBackpackId());
            }
        } else if (idx == 0 && page > 0) {
            new BackpackMenu(plugin, messages, plugin.getTierRegistry()).open(player, bp, page - 1, holder.isAdmin());
        }
    }

    private void syncPage(BackpackHolder holder, org.bukkit.inventory.Inventory inv) {
        Backpack bp = holder.getBackpack();
        int storageEnd = BackpackLayout.storageEnd(slots(holder));
        ItemStack[] items = new ItemStack[storageEnd];
        for (int i = 0; i < storageEnd; i++) {
            items[i] = inv.getItem(i);
        }
        bp.setPage(holder.getPage(), items, storageEnd);
        service.save(bp);
    }
}
