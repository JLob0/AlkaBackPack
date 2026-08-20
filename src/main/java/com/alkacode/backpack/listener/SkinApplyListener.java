package com.alkacode.backpack.listener;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.util.Messages;
import com.alkacode.backpack.util.Skins;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Aplica um token de skin na mochila: segure a mochila na mao e o token na outra,
 * e clique com o direito. A skin muda a aparencia (textura/ItemsAdder) da mochila
 * e o token e consumido.
 */
public class SkinApplyListener implements Listener {

    private final AlkaBackpackPlugin plugin;
    private final BackpackService service;
    private final Skins skins;
    private final Messages messages;

    public SkinApplyListener(AlkaBackpackPlugin plugin, BackpackService service, Skins skins, Messages messages) {
        this.plugin = plugin;
        this.service = service;
        this.skins = skins;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (main == null || main.getType() == Material.AIR || off == null || off.getType() == Material.AIR) return;

        boolean mainBackpack = service.isBackpack(main);
        boolean offBackpack = service.isBackpack(off);
        boolean mainToken = skins.isToken(main);
        boolean offToken = skins.isToken(off);
        if (!(mainBackpack && offToken) && !(offBackpack && mainToken)) return;

        ItemStack backpackItem = mainBackpack ? main : off;
        ItemStack token = mainToken ? main : off;
        String skinId = skins.getTokenSkinId(token);
        if (skinId == null) return;

        ItemStack skinned = skins.applySkin(player, backpackItem, skinId);
        if (mainBackpack) player.getInventory().setItemInMainHand(skinned);
        else player.getInventory().setItemInOffHand(skinned);

        // consome o token
        if (mainToken) player.getInventory().setItemInMainHand(null);
        else if (offToken) player.getInventory().setItemInOffHand(null);

        event.setCancelled(true);
        player.sendMessage(messages.get("skin-applied"));
    }
}
