package com.alkacode.backpack;

import com.alkacode.backpack.command.CommandAbrirMochila;
import com.alkacode.backpack.command.CommandBackpack;
import com.alkacode.backpack.database.BackpackRepository;
import com.alkacode.backpack.economy.EconomyService;
import com.alkacode.backpack.listener.BackpackFunctionListener;
import com.alkacode.backpack.listener.BackpackInteractListener;
import com.alkacode.backpack.listener.BackpackInventoryListener;
import com.alkacode.backpack.listener.PasswordChatGuardListener;
import com.alkacode.backpack.listener.SkinApplyListener;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.tier.TierRegistry;
import com.alkacode.backpack.util.Menus;
import com.alkacode.backpack.util.Messages;
import com.alkacode.backpack.util.Skins;
import com.alkacode.core.api.AlkaAPI;
import com.alkacode.core.plugin.AlkaPlugin;
import com.alkacode.economy.AlkaEconomyPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Base do AlkaBackpack sobre o AlkaCore (banco/HikariCP via api.getDatabase(),
 * BaseGui, scheduler) e o AlkaEconomy (moeda via EconomyService). Estende
 * AlkaPlugin - garante AlkaAPI pronta (via depend: [AlkaCore, AlkaEconomy]) e nao
 * registra o GuiListener de novo (o Core ja faz isso uma unica vez).
 */
public final class AlkaBackpackPlugin extends AlkaPlugin {

    private static AlkaBackpackPlugin instance;

    private Messages messages;
    private Menus menus;
    private Skins skins;
    private com.alkacode.backpack.task.CosmeticReconciler cosmeticReconciler;
    private TierRegistry tierRegistry;
    private BackpackRepository repository;
    private BackpackService service;
    private EconomyService economyService;
    private PasswordChatGuardListener passwordGuard;

    @Override
    protected void onPluginEnable() {
        instance = this;
        saveDefaultConfig();
        this.messages = new Messages(this);
        this.menus = new Menus(this);

        AlkaAPI api = getAlkaAPI();

        if (!(getServer().getPluginManager().getPlugin("AlkaEconomy") instanceof AlkaEconomyPlugin alkaEconomy)) {
            getLogger().severe("AlkaEconomy e obrigatorio e nao foi encontrado. Desativando.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.tierRegistry = new TierRegistry();
        tierRegistry.load(getConfig());

        this.repository = new BackpackRepository(api.getDatabase(), getLogger());
        this.service = new BackpackService(this, api, repository, tierRegistry, getConfig());
        this.skins = new Skins(this, service);
        this.economyService = new EconomyService(this, alkaEconomy.getEconomyManager());
        this.passwordGuard = new PasswordChatGuardListener(this, messages);

        BackpackInteractListener interactListener = new BackpackInteractListener(this, service, messages);
        getServer().getPluginManager().registerEvents(interactListener, this);
        getServer().getPluginManager().registerEvents(new BackpackInventoryListener(this, service, messages), this);
        getServer().getPluginManager().registerEvents(new BackpackFunctionListener(this, service, messages), this);
        getServer().getPluginManager().registerEvents(new SkinApplyListener(this, service, skins, messages), this);
        getServer().getPluginManager().registerEvents(passwordGuard, this);

        getCommand("abackpack").setExecutor(new CommandBackpack(this, service, messages, economyService, tierRegistry));
        getCommand("abackpack").setTabCompleter(new CommandBackpack(this, service, messages, economyService, tierRegistry));
        getCommand("aabackpack").setExecutor(new CommandBackpack(this, service, messages, economyService, tierRegistry));
        CommandAbrirMochila abrirMochila = new CommandAbrirMochila(service, messages, tierRegistry, interactListener);
        getCommand("abrirmochila").setExecutor(abrirMochila);
        getCommand("abrirmochila").setTabCompleter(abrirMochila);

        scheduleAutosave(api);
        scheduleBackup(api);
        scheduleCosmeticReconciler();

        getLogger().info("AlkaBackpack habilitado (" + tierRegistry.size() + " tiers, moeda: "
                + economyService.getCurrencyName() + ").");
    }

    private void scheduleAutosave(AlkaAPI api) {
        int interval = Math.max(30, getConfig().getInt("settings.autosave-interval", 300));
        api.getScheduler().runAsyncRepeating(() -> service.saveAll(), 20L * interval, 20L * interval);
    }

    private void scheduleBackup(AlkaAPI api) {
        int interval = getConfig().getInt("settings.backup-interval", 0);
        if (interval <= 0) return;
        int retention = Math.max(1, getConfig().getInt("settings.backup-retention", 5));
        api.getScheduler().runAsyncRepeating(() -> performBackup(retention), 20L * interval, 20L * interval);
    }

    /**
     * Roda em thread principal (nao no AlkaScheduler async): equip/unequip do
     * CosmeticsCore mexe com estado de entidade/jogador, nao e seguro fora da main.
     */
    private void scheduleCosmeticReconciler() {
        this.cosmeticReconciler = new com.alkacode.backpack.task.CosmeticReconciler(service, skins);
        int interval = Math.max(10, getConfig().getInt("settings.cosmetic-reconcile-interval", 30));
        getServer().getScheduler().runTaskTimer(this, cosmeticReconciler, 20L * interval, 20L * interval);
    }

    public com.alkacode.backpack.task.CosmeticReconciler getCosmeticReconciler() { return cosmeticReconciler; }

    private void performBackup(int retention) {
        try {
            File backupDir = new File(getDataFolder(), "backups");
            if (!backupDir.exists()) backupDir.mkdirs();
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path target = backupDir.toPath().resolve("backup-" + stamp + ".txt");
            Files.writeString(target, service.getRepository().backupSnapshot(), StandardCharsets.UTF_8);

            // retencao: apaga backups antigos alem do limite
            File[] backups = backupDir.listFiles((d, name) -> name.startsWith("backup-"));
            if (backups != null && backups.length > retention) {
                java.util.Arrays.sort(backups);
                for (int i = 0; i < backups.length - retention; i++) {
                    Files.deleteIfExists(backups[i].toPath());
                }
            }
        } catch (IOException e) {
            getLogger().severe("Erro ao gerar backup: " + e.getMessage());
        }
    }

    @Override
    protected void onPluginDisable() {
        if (service != null) {
            service.saveAll();
        }
        instance = null;
    }

    public static AlkaBackpackPlugin getInstance() {
        return instance;
    }

    public Messages getMessages() { return messages; }
    public Menus getMenus() { return menus; }
    public Skins getSkins() { return skins; }
    public TierRegistry getTierRegistry() { return tierRegistry; }
    public BackpackService getBackpackService() { return service; }
    public EconomyService getEconomyService() { return economyService; }
    public PasswordChatGuardListener getPasswordGuard() { return passwordGuard; }
}
