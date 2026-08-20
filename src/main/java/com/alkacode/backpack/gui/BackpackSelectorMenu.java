package com.alkacode.backpack.gui;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.tier.BackpackTier;
import com.alkacode.backpack.util.ItemBuilder;
import com.alkacode.backpack.util.MenuItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Menu de selecao de mochila virtual: lista os tiers liberados nos slots de
 * menus.yml (secao "selector", chave "tier-slots"). Tambem usado pelo admin.
 */
public class BackpackSelectorMenu extends YmlMenu {

    private final AlkaBackpackPlugin plugin;
    private final UUID target;
    private final boolean admin;

    public BackpackSelectorMenu(AlkaBackpackPlugin plugin, Player player, UUID target, boolean admin) {
        super(plugin, player, plugin.getMenus(), "selector", "backpack-selector");
        this.plugin = plugin;
        this.target = target;
        this.admin = admin;
    }

    @Override
    protected void onRender() {
        BackpackService service = plugin.getBackpackService();
        List<Integer> slots = def.getIntegerList("tier-slots");
        List<String> loreTemplate = def.getStringList("item-lore");
        int slotIndex = 0;
        boolean any = false;
        for (BackpackTier tier : plugin.getTierRegistry().getAll()) {
            if (!admin && !player.hasPermission(tier.getPermissao())) continue;
            if (slotIndex >= slots.size()) break;
            any = true;
            int slot = slots.get(slotIndex++);

            String[] placeholders = {
                    "{slots}", String.valueOf(tier.getSlots()),
                    "{capacidade}", String.valueOf(tier.getCapacidade()),
                    "{paginas}", String.valueOf(tier.getPaginasIniciais())
            };
            List<String> lore = loreTemplate.stream()
                    .map(line -> MenuItems.apply(placeholders, line))
                    .toList();

            var item = ItemBuilder.skullTexture(tier.getTextura(), tier.getCor() + tier.getNome(), lore);
            int t = tier.getId();
            setItem(slot, item, e -> {
                Backpack bp = service.getOrCreateVirtual(target, t);
                new BackpackMenu(plugin, plugin.getMessages(), plugin.getTierRegistry())
                        .open(player, bp, 0, admin);
            });
        }
        if (!any) {
            setItem(13, createItem(org.bukkit.Material.BARRIER, def.getString("empty-name", "<red>Nenhuma mochila desbloqueada")));
        }
    }
}
