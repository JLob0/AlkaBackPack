package com.alkacode.backpack.listener;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.gui.BackpackMenu;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.util.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Abre a mochila fisica ao clicar com o botao direito no item. Valida mundo
 * desabilitado, cooldown e permissao; senha e checada antes de abrir.
 */
public class BackpackInteractListener implements Listener {

    private final AlkaBackpackPlugin plugin;
    private final BackpackService service;
    private final Messages messages;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public BackpackInteractListener(AlkaBackpackPlugin plugin, BackpackService service, Messages messages) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR || !service.isBackpack(item)) return;

        // se houver um token de skin na outra mao, quem trata e o SkinApplyListener
        if (plugin.getSkins().isToken(player.getInventory().getItemInOffHand())) return;

        event.setCancelled(true);
        tryOpen(player, item);
    }

    /**
     * Abre a mochila fisica representada por {@code item} pra {@code player},
     * com todas as validacoes (permissao, mundo, cooldown, senha). Usado tanto
     * pelo clique direito na mao quanto por comandos que acham o item em
     * qualquer slot do inventario (nao precisa estar na hotbar).
     */
    public void tryOpen(Player player, ItemStack item) {
        if (!player.hasPermission("alkabackpack.use")) {
            player.sendMessage(messages.get("no-permission"));
            return;
        }
        if (!service.canUseInWorld(player)) {
            player.sendMessage(messages.get("world-disabled"));
            return;
        }
        long now = System.currentTimeMillis();
        int cooldown = plugin.getConfig().getInt("settings.open-cooldown", 1);
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && (now - last) < cooldown * 1000L) {
            player.sendMessage(messages.get("cooldown"));
            return;
        }
        cooldowns.put(player.getUniqueId(), now);

        String id = service.getBackpackId(item);
        if (id == null) return;
        Backpack bp = service.getById(id);
        if (bp == null) {
            // mochila fisica sem registro - cria com base no PDC do item
            UUID owner = service.getItemOwner(item);
            if (owner == null) owner = player.getUniqueId();
            int tier = service.getItemTier(item);
            if (tier < 1) tier = 1;
            bp = new Backpack(id, owner, tier);
            bp.setUnlockedPages(plugin.getTierRegistry().get(tier).map(t -> t.getPaginasIniciais()).orElse(1));
            service.save(bp);
        }

        if (bp.isProtected() && !bp.getOwner().equals(player.getUniqueId())
                && !service.isUnlocked(player.getUniqueId(), id)) {
            player.sendMessage(messages.get("password-required"));
            return;
        }

        new BackpackMenu(plugin, messages, plugin.getTierRegistry()).open(player, bp, bp.getCurrentPage(), false);
    }
}
