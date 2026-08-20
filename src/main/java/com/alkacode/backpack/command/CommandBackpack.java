package com.alkacode.backpack.command;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.economy.EconomyService;
import com.alkacode.backpack.gui.BackpackMenu;
import com.alkacode.backpack.gui.BackpackSelectorMenu;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.tier.BackpackTier;
import com.alkacode.backpack.tier.TierRegistry;
import com.alkacode.backpack.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Comando principal /abackpack (e /aabackpack). Subcomandos de jogador e de admin,
 * com permissao por acao. No comando sem argumento abre o seletor de mochilas.
 */
public class CommandBackpack implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "auto", "cosmetico", "give", "giveall", "list", "info", "reload", "reset", "wipe", "open", "edit");

    private final AlkaBackpackPlugin plugin;
    private final BackpackService service;
    private final Messages messages;
    private final EconomyService economy;
    private final TierRegistry tierRegistry;

    public CommandBackpack(AlkaBackpackPlugin plugin, BackpackService service, Messages messages,
                           EconomyService economy, TierRegistry tierRegistry) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
        this.economy = economy;
        this.tierRegistry = tierRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messages.get("invalid-tier"));
                return true;
            }
            if (!player.hasPermission("alkabackpack.use")) {
                player.sendMessage(messages.get("no-permission"));
                return true;
            }
            new BackpackSelectorMenu(plugin, player, player.getUniqueId(), false).open();
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "auto" -> handleAuto(sender, args);
            case "cosmetico" -> handleCosmetico(sender);
            case "give" -> handleGive(sender, args);
            case "giveall" -> handleGiveAll(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "wipe" -> handleWipe(sender, args);
            case "open" -> handleOpen(sender, args, true);
            case "edit" -> handleOpen(sender, args, true);
            default -> sender.sendMessage(messages.get("invalid-tier"));
        }
        return true;
    }

    // ------------------------------------------------------------------ auto

    private void handleAuto(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (!player.hasPermission("alkabackpack.auto")) {
            player.sendMessage(messages.get("no-permission"));
            return;
        }
        boolean any = false;
        boolean newValue = false;
        for (BackpackTier tier : tierRegistry.getAll()) {
            if (!player.hasPermission(tier.getPermissao())) continue;
            Backpack bp = service.getOrCreateVirtual(player.getUniqueId(), tier.getId());
            newValue = !bp.isAutoCollect();
            bp.setAutoCollect(newValue);
            service.save(bp);
            any = true;
        }
        if (!any) {
            player.sendMessage(messages.get("no-tier-unlocked"));
            return;
        }
        player.sendMessage(messages.get(newValue ? "auto-enabled" : "auto-disabled"));
    }

    // ------------------------------------------------------------------ cosmetico

    private void handleCosmetico(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        boolean newValue = !service.isBackCosmeticEnabled(player.getUniqueId());
        service.setBackCosmeticEnabled(player.getUniqueId(), newValue);

        var reconciler = plugin.getCosmeticReconciler();
        if (reconciler != null) reconciler.reconcile(player);

        if (!newValue) {
            player.sendMessage(messages.get("cosmetic-disabled"));
        } else if (reconciler != null && reconciler.findDesiredCosmeticKey(player) == null) {
            player.sendMessage(messages.get("cosmetic-enabled-no-skin"));
        } else {
            player.sendMessage(messages.get("cosmetic-enabled"));
        }
    }

    // ------------------------------------------------------------------ give

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkabackpack.give")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        Optional<BackpackTier> tier = resolveTier(args[1]);
        if (tier.isEmpty()) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.get("player-not-found", "<player>", args[2]));
            return;
        }
        int amount = args.length >= 4 ? parseAmount(args[3], 1) : 1;
        for (int i = 0; i < amount; i++) {
            ItemStack backpack = service.createPhysicalItem(tier.get().getId(), target.getUniqueId());
            target.getInventory().addItem(backpack).values().forEach(rest ->
                    target.getWorld().dropItemNaturally(target.getLocation(), rest));
        }
        target.sendMessage(messages.get("given-backpack", "<quantidade>", String.valueOf(amount),
                "<nome>", tier.get().getCor() + tier.get().getNome()));
        if (!sender.equals(target)) {
            sender.sendMessage(messages.get("given-backpack-other", "<quantidade>", String.valueOf(amount),
                    "<nome>", tier.get().getCor() + tier.get().getNome(), "<player>", target.getName()));
        }
    }

    private void handleGiveAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkabackpack.giveall")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        Optional<BackpackTier> tier = resolveTier(args[1]);
        if (tier.isEmpty()) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        int amount = args.length >= 3 ? parseAmount(args[2], 1) : 1;
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < amount; i++) {
                player.getInventory().addItem(service.createPhysicalItem(tier.get().getId(), player.getUniqueId()));
            }
        }
        sender.sendMessage(messages.get("giveall-success", "<quantidade>", String.valueOf(amount),
                "<nome>", tier.get().getCor() + tier.get().getNome()));
    }

    // ------------------------------------------------------------------ list/info

    private void handleList(CommandSender sender) {
        for (BackpackTier tier : tierRegistry.getAll()) {
            int used = 0;
            if (sender instanceof Player player && player.hasPermission(tier.getPermissao())) {
                Backpack bp = service.getOrCreateVirtual(player.getUniqueId(), tier.getId());
                used = service.countUsedSlots(bp);
            }
            sender.sendMessage(messages.parse(tier.chatLine(used)));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Optional<BackpackTier> tier = args.length >= 2 ? resolveTier(args[1]) : tierRegistry.get(1);
        if (tier.isEmpty()) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        BackpackTier t = tier.get();
        sender.sendMessage(messages.parse(t.chatLine(0)));
        sender.sendMessage(messages.parse("<gray>Nome do tier: <white>" + t.getNomeTier()));
        sender.sendMessage(messages.parse("<gray>Slots: <white>" + t.getSlots() + "/" + t.getCapacidade()));
        sender.sendMessage(messages.parse("<gray>Paginas: <white>" + t.getPaginasIniciais() + "-" + t.getPaginasMax()));
        sender.sendMessage(messages.parse("<gray>Minerar para o estoque: <white>" + (t.podeMinerar() ? "Sim" : "Nao")));
    }

    // ------------------------------------------------------------------ reload

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("alkabackpack.reload")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        plugin.reloadConfig();
        plugin.saveDefaultConfig();
        plugin.getMessages().load();
        plugin.getMenus().load();
        plugin.getSkins().load();
        plugin.getTierRegistry().load(plugin.getConfig());
        plugin.getEconomyService().reload();
        sender.sendMessage(messages.get("reload-success"));
    }

    // ------------------------------------------------------------------ reset/wipe

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkabackpack.reset")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        Optional<BackpackTier> tier = resolveTier(args[2]);
        if (tier.isEmpty()) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        UUID owner = resolvePlayerUuid(args[1]);
        if (owner == null) {
            sender.sendMessage(messages.get("player-not-found", "<player>", args[1]));
            return;
        }
        service.remove(service.virtualId(owner, tier.get().getId()));
        service.clearCache(owner);
        sender.sendMessage(messages.get("reset-success", "<player>", args[1]));
    }

    private void handleWipe(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkabackpack.wipe")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        Optional<BackpackTier> tier = resolveTier(args[1]);
        if (tier.isEmpty()) {
            sender.sendMessage(messages.get("invalid-tier"));
            return;
        }
        plugin.getBackpackService().getRepository().wipeTier(tier.get().getId());
        plugin.getBackpackService().saveAll();
        sender.sendMessage(messages.get("wipe-success", "<nivel>", String.valueOf(tier.get().getId())));
    }

    // ------------------------------------------------------------------ open/edit

    private void handleOpen(CommandSender sender, String[] args, boolean admin) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (!viewer.hasPermission("alkabackpack.open")) {
            viewer.sendMessage(messages.get("no-permission"));
            return;
        }
        UUID target;
        if (args.length >= 2) {
            target = resolvePlayerUuid(args[1]);
            if (target == null) {
                viewer.sendMessage(messages.get("player-not-found", "<player>", args[1]));
                return;
            }
        } else {
            target = viewer.getUniqueId();
        }

        if (args.length >= 3) {
            Optional<BackpackTier> tier = resolveTier(args[2]);
            if (tier.isEmpty()) {
                viewer.sendMessage(messages.get("invalid-tier"));
                return;
            }
            Backpack bp = service.getOrCreateVirtual(target, tier.get().getId());
            new BackpackMenu(plugin, messages, tierRegistry).open(viewer, bp, 0, true);
        } else {
            new BackpackSelectorMenu(plugin, viewer, target, true).open();
        }
    }

    // ------------------------------------------------------------------ utils

    private Optional<BackpackTier> resolveTier(String raw) {
        try {
            int id = Integer.parseInt(raw);
            return tierRegistry.get(id);
        } catch (NumberFormatException e) {
            return tierRegistry.getByKey(raw);
        }
    }

    private int parseAmount(String raw, int def) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private UUID resolvePlayerUuid(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player != null) return player.getUniqueId();
        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("giveall")
                || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("open")
                || args[0].equalsIgnoreCase("edit"))) {
            return tierKeys(args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("open")
                || args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("reset"))) {
            return filter(onlineNames(), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s.startsWith(prefix.toLowerCase(Locale.ROOT))) out.add(s);
        }
        return out;
    }

    private List<String> tierKeys(String prefix) {
        List<String> out = new ArrayList<>();
        for (BackpackTier t : tierRegistry.getAll()) {
            out.add(t.getKey());
            out.add(String.valueOf(t.getId()));
        }
        return filter(out, prefix);
    }

    private List<String> onlineNames() {
        List<String> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        return out;
    }
}
