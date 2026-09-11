package com.alkacode.backpack.listener;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.gui.BackpackLayout;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.util.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Funcoes de mochila fora do inventario: nao descartar no chao (block-drop),
 * manter na morte (keep-on-death) e minerar/coletar direto para o estoque.
 */
public class BackpackFunctionListener implements Listener {

    private final AlkaBackpackPlugin plugin;
    private final BackpackService service;
    private final Messages messages;

    public BackpackFunctionListener(AlkaBackpackPlugin plugin, BackpackService service, Messages messages) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // mochilas com keep-on-death voltam pro inventario em vez de cair no chao
        List<ItemStack> toKeep = new ArrayList<>();
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (service.isBackpack(drop)) {
                String id = service.getBackpackId(drop);
                Backpack bp = id != null ? service.getById(id) : null;
                if (bp != null && bp.isKeepOnDeath()) {
                    toKeep.add(drop);
                    it.remove();
                }
            }
        }
        if (!toKeep.isEmpty()) {
            Player p = event.getEntity();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (ItemStack keep : toKeep) {
                    p.getInventory().addItem(keep);
                }
            });
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfig().getBoolean("settings.block-drop", false)) return;
        if (service.isBackpack(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(messages.get("world-disabled")); // placeholder generico
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Optional<Backpack> miningBp = service.findMiningBackpack(player);
        if (miningBp.isEmpty()) return;

        Backpack bp = miningBp.get();
        int slots = BackpackLayout.storageEnd(plugin.getTierRegistry().get(bp.getTier()).map(t -> t.getSlots()).orElse(9));
        Block block = event.getBlock();
        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
        if (drops.isEmpty()) return;

        List<ItemStack> leftover = new ArrayList<>();
        boolean captured = false;
        for (ItemStack drop : drops) {
            ItemStack rest = service.addItem(bp, drop.clone(), slots);
            if (rest == null) {
                captured = true;
            } else {
                leftover.add(rest);
            }
        }
        if (captured) {
            // Cancelar de verdade (nao so setDropItems(false)) - senao o evento
            // continua "nao tratado" pra outros plugins que reagem a
            // BlockBreakEvent com ignoreCancelled=true (ex: BlockBreakListener do
            // AlkaDrop, prioridade HIGHEST), que recalculava os mesmos drops do
            // bloco ainda intacto e entregava de novo por cima - duplicava todo
            // bloco minerado com mochila de mineracao equipada, pra qualquer
            // jogador (alkadrop.use e collection.enabled-by-default sao true por
            // padrao pra todo mundo, nao so VIP).
            event.setCancelled(true);
            int xp = event.getExpToDrop();
            event.setExpToDrop(0);
            if (xp > 0) {
                player.giveExp(xp);
            }
            if (!leftover.isEmpty()) {
                for (ItemStack rest : leftover) {
                    block.getWorld().dropItemNaturally(block.getLocation(), rest);
                }
            }
            player.sendMessage(messages.get("mined-to-stock"));

            // applyPhysics=false + broadcast manual evita ghost block (mesmo padrao
            // do AlkaDrop/AlkaMines - ver feedback-mineblocklistener-ghostblock).
            Location location = block.getLocation();
            block.setType(Material.AIR, false);
            for (Player nearby : block.getWorld().getPlayers()) {
                if (nearby.getLocation().distanceSquared(location) < 2500) {
                    nearby.sendBlockChange(location, Material.AIR.createBlockData());
                }
            }
        }
    }

    @EventHandler
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.isCancelled()) return;
        if (!plugin.getConfig().getBoolean("settings.auto-collect-default", true)) return;

        // coleta automatica: itens iguais vao pra mochila virtual com autoCollect ativo
        for (com.alkacode.backpack.tier.BackpackTier tier : plugin.getTierRegistry().getAll()) {
            if (!player.hasPermission(tier.getPermissao())) continue;
            Backpack bp = service.getOrCreateVirtual(player.getUniqueId(), tier.getId());
            if (!bp.isAutoCollect()) continue;
            int slots = BackpackLayout.storageEnd(tier.getSlots());
            ItemStack drop = event.getItem().getItemStack().clone();
            ItemStack rest = service.addItem(bp, drop, slots);
            if (rest == null) {
                event.setCancelled(true);
                event.getItem().remove();
                player.sendActionBar(messages.getNoPrefix("action-bar-place",
                        "<quantidade>", String.valueOf(drop.getAmount()),
                        "<item>", drop.getType().name()));
                return;
            }
        }
    }
}
