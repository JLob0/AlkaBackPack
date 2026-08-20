package com.alkacode.backpack.command;

import com.alkacode.backpack.listener.BackpackInteractListener;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.tier.BackpackTier;
import com.alkacode.backpack.tier.TierRegistry;
import com.alkacode.backpack.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * /abrirmochila [tier]: acha mochila(s) fisica(s) do jogador em qualquer slot
 * do inventario (nao so a hotbar/mao) e abre. Sem argumento, abre a de maior
 * tier (empate = escolhe aleatoria entre as empatadas); com argumento, abre
 * uma do tier pedido (chave ou id), tambem aleatoria se houver mais de uma.
 */
public class CommandAbrirMochila implements CommandExecutor, TabCompleter {

    private final BackpackService service;
    private final Messages messages;
    private final TierRegistry tierRegistry;
    private final BackpackInteractListener interactListener;

    public CommandAbrirMochila(BackpackService service, Messages messages, TierRegistry tierRegistry,
                                BackpackInteractListener interactListener) {
        this.service = service;
        this.messages = messages;
        this.tierRegistry = tierRegistry;
        this.interactListener = interactListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("no-permission"));
            return true;
        }

        List<ItemStack> backpacks = service.findPhysicalBackpacks(player);

        if (args.length >= 1) {
            Optional<BackpackTier> tier = resolveTier(args[0]);
            if (tier.isEmpty()) {
                player.sendMessage(messages.get("invalid-tier"));
                return true;
            }
            backpacks.removeIf(item -> service.getItemTier(item) != tier.get().getId());
            if (backpacks.isEmpty()) {
                player.sendMessage(messages.get("no-physical-backpack-tier",
                        "<tier>", tier.get().getCor() + tier.get().getNome()));
                return true;
            }
            interactListener.tryOpen(player, pickRandom(backpacks));
            return true;
        }

        if (backpacks.isEmpty()) {
            player.sendMessage(messages.get("no-physical-backpack"));
            return true;
        }
        int bestTier = backpacks.stream().mapToInt(service::getItemTier).max().orElse(0);
        backpacks.removeIf(item -> service.getItemTier(item) != bestTier);
        interactListener.tryOpen(player, pickRandom(backpacks));
        return true;
    }

    private ItemStack pickRandom(List<ItemStack> items) {
        return items.size() == 1 ? items.get(0) : items.get(ThreadLocalRandom.current().nextInt(items.size()));
    }

    private Optional<BackpackTier> resolveTier(String raw) {
        try {
            return tierRegistry.get(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return tierRegistry.getByKey(raw);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> out = new ArrayList<>();
        for (BackpackTier t : tierRegistry.getAll()) {
            out.add(t.getKey());
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        out.removeIf(k -> !k.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }
}
