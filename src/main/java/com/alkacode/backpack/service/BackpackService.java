package com.alkacode.backpack.service;

import com.alkacode.backpack.database.BackpackRepository;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.tier.BackpackTier;
import com.alkacode.backpack.tier.TierRegistry;
import com.alkacode.backpack.util.ItemBuilder;
import com.alkacode.core.api.AlkaAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fachada central do AlkaBackpack: cache de mochilas em memoria, criacao de itens
 * fisicos, regras de uso (mundos/blacklist), limites de paginas e persistencia
 * assincrona via AlkaAPI#getScheduler().
 */
public class BackpackService {

    private final JavaPlugin plugin;
    private final AlkaAPI api;
    private final BackpackRepository repository;
    private final TierRegistry tierRegistry;
    private final FileConfiguration config;

    private final NamespacedKey KEY_BACKPACK;
    private final NamespacedKey KEY_ID;
    private final NamespacedKey KEY_TIER;
    private final NamespacedKey KEY_OWNER;

    private final Map<String, Backpack> cache = new ConcurrentHashMap<>();
    private final java.util.Set<String> unlocked = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Boolean> backCosmeticPrefCache = new ConcurrentHashMap<>();

    public BackpackService(JavaPlugin plugin, AlkaAPI api, BackpackRepository repository,
                           TierRegistry tierRegistry, FileConfiguration config) {
        this.plugin = plugin;
        this.api = api;
        this.repository = repository;
        this.tierRegistry = tierRegistry;
        this.config = config;
        this.KEY_BACKPACK = new NamespacedKey(plugin, "alka_backpack");
        this.KEY_ID = new NamespacedKey(plugin, "alka_id");
        this.KEY_TIER = new NamespacedKey(plugin, "alka_tier");
        this.KEY_OWNER = new NamespacedKey(plugin, "alka_owner");
    }

    // ---------------------------------------------------------------- acesso

    public String virtualId(UUID owner, int tier) {
        return owner.toString() + ":virtual:" + tier;
    }

    public Backpack getOrCreateVirtual(UUID owner, int tier) {
        String id = virtualId(owner, tier);
        Backpack bp = cache.get(id);
        if (bp != null) return bp;
        Backpack loaded = repository.load(id);
        if (loaded != null) {
            cache.put(id, loaded);
            return loaded;
        }
        Backpack created = new Backpack(id, owner, tier);
        created.setUnlockedPages(tierRegistry.get(tier).map(BackpackTier::getPaginasIniciais).orElse(1));
        save(created);
        return created;
    }

    /** Carrega por id (fisica ou virtual) usando o cache. */
    public Backpack getById(String id) {
        return cache.computeIfAbsent(id, k -> repository.load(k));
    }

    public void save(Backpack bp) {
        cache.put(bp.getId(), bp);
        api.getScheduler().runAsync(() -> repository.save(bp));
    }

    public void remove(String id) {
        cache.remove(id);
        api.getScheduler().runAsync(() -> repository.delete(id));
    }

    // --------------------------------------------------------- desbloqueio de senha

    public boolean isUnlocked(UUID viewer, String backpackId) {
        return unlocked.contains(viewer.toString() + ":" + backpackId);
    }

    public void unlock(UUID viewer, String backpackId) {
        unlocked.add(viewer.toString() + ":" + backpackId);
    }

    public void lock(UUID viewer, String backpackId) {
        unlocked.remove(viewer.toString() + ":" + backpackId);
    }

    public void clearCache(UUID owner) {
        cache.entrySet().removeIf(e -> e.getKey().startsWith(owner.toString() + ":"));
    }

    /** Flush sincrono do cache no onDisable (o scheduler async para de aceitar tarefas nesse ponto). */
    public void saveAll() {
        for (Backpack bp : cache.values()) {
            repository.save(bp);
        }
        cache.clear();
    }

    // --------------------------------------------------------- itens fisicos

    public ItemStack createPhysicalItem(int tier, UUID owner) {
        String id = owner.toString() + ":physical:" + UUID.randomUUID().toString().substring(0, 8);
        return buildPhysicalItem(id, tier, owner);
    }

    private ItemStack buildPhysicalItem(String id, int tier, UUID owner) {
        Optional<BackpackTier> opt = tierRegistry.get(tier);
        BackpackTier t = opt.orElseGet(() -> tierRegistry.getAll().stream().findFirst().orElseThrow());
        String name = t.getCor() + "[❖] " + t.getNome();
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("<gray>Slots: <white>" + t.getSlots() + "</white>/" + t.getCapacidade());
        lore.add("<gray>Paginas: <white>" + t.getPaginasIniciais() + "</white>");
        lore.add(" ");
        lore.add("<yellow>Clique direito para abrir.");
        ItemStack item = ItemBuilder.skullTexture(t.getTextura(), name, lore);

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_BACKPACK, PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, id);
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.INTEGER, tier);
        meta.getPersistentDataContainer().set(KEY_OWNER, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isBackpack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_BACKPACK, PersistentDataType.STRING);
    }

    public String getBackpackId(ItemStack item) {
        if (!isBackpack(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_ID, PersistentDataType.STRING);
    }

    public int getItemTier(ItemStack item) {
        if (!isBackpack(item)) return -1;
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(KEY_TIER, PersistentDataType.INTEGER);
        return tier == null ? -1 : tier;
    }

    /** Grava a identidade da mochila (marcador, id, tier, dono) num item - usado ao trocar a skin. */
    public ItemStack stampIdentity(ItemStack item, String id, int tier, UUID owner) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_BACKPACK, PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, id);
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.INTEGER, tier);
        meta.getPersistentDataContainer().set(KEY_OWNER, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
        return item;
    }

    public UUID getItemOwner(ItemStack item) {
        if (!isBackpack(item)) return null;
        String owner = item.getItemMeta().getPersistentDataContainer().get(KEY_OWNER, PersistentDataType.STRING);
        if (owner == null) return null;
        try {
            return UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ----------------------------------------------------------------- regras

    public boolean canUseInWorld(Player player) {
        String world = player.getWorld().getName();
        for (String disabled : config.getStringList("disabled-worlds")) {
            if (disabled.equalsIgnoreCase(world)) return false;
        }
        return true;
    }

    public boolean isBlacklisted(ItemStack item) {
        if (!config.getBoolean("blacklist.enabled", true)) return false;
        for (String mat : config.getStringList("blacklist.items")) {
            if (mat.equalsIgnoreCase("ALKA_BACKPACK") && isBackpack(item)) return true;
            try {
                if (item.getType() == org.bukkit.Material.valueOf(mat)) return true;
            } catch (IllegalArgumentException ignored) {
            }
        }
        return false;
    }

    public int effectiveMaxPages(int tier) {
        return tierRegistry.get(tier).map(BackpackTier::getPaginasMax).orElse(1);
    }

    /** Quantidade de slots ocupados (itens nao-air) na pagina atual. */
    public int countUsedSlots(Backpack bp) {
        int used = 0;
        for (ItemStack[] page : bp.getPages().values()) {
            if (page == null) continue;
            for (ItemStack item : page) {
                if (item != null && !item.getType().isAir()) used++;
            }
        }
        return used;
    }

    public BackpackRepository getRepository() { return repository; }

    /**
     * Tenta inserir {@code item} numa mochila (nas paginas desbloqueadas): empilha
     * com itens iguais e depois preenche slots vazios. Retorna o que sobrou (null se
     * armazenou tudo). Sempre valida blacklist.
     */
    public ItemStack addItem(Backpack bp, ItemStack item, int slots) {
        if (item == null || item.getType().isAir()) return null;
        if (isBlacklisted(item)) return item;

        ItemStack remaining = item.clone();
        int unlocked = Math.max(1, bp.getUnlockedPages());
        for (int page = 0; page < unlocked && remaining.getAmount() > 0; page++) {
            ItemStack[] pageItems = bp.getPage(page);
            if (pageItems.length != slots) {
                pageItems = new ItemStack[slots];
                bp.getPages().put(page, pageItems);
            }
            // empilha com itens iguais
            for (int i = 0; i < slots && remaining.getAmount() > 0; i++) {
                ItemStack existing = pageItems[i];
                if (existing != null && existing.isSimilar(remaining)) {
                    int room = existing.getMaxStackSize() - existing.getAmount();
                    if (room > 0) {
                        int transfer = Math.min(room, remaining.getAmount());
                        existing.setAmount(existing.getAmount() + transfer);
                        remaining.setAmount(remaining.getAmount() - transfer);
                    }
                }
            }
            // preenche slots vazios
            for (int i = 0; i < slots && remaining.getAmount() > 0; i++) {
                if (pageItems[i] == null || pageItems[i].getType().isAir()) {
                    int transfer = Math.min(remaining.getMaxStackSize(), remaining.getAmount());
                    ItemStack placed = remaining.clone();
                    placed.setAmount(transfer);
                    pageItems[i] = placed;
                    remaining.setAmount(remaining.getAmount() - transfer);
                }
            }
            bp.getPages().put(page, pageItems);
        }
        save(bp);
        return remaining.getAmount() > 0 ? remaining : null;
    }

    // --------------------------------------------------------- preferencia de cosmetico

    /** Se o jogador quer ver o cosmetico (mochila nas costas via CosmeticsCore) ligado. Default true. */
    public boolean isBackCosmeticEnabled(UUID owner) {
        return backCosmeticPrefCache.computeIfAbsent(owner, id -> {
            Boolean saved = repository.loadBackCosmeticPref(id);
            return saved == null || saved;
        });
    }

    public void setBackCosmeticEnabled(UUID owner, boolean enabled) {
        backCosmeticPrefCache.put(owner, enabled);
        api.getScheduler().runAsync(() -> repository.saveBackCosmeticPref(owner, enabled));
    }

    // --------------------------------------------------------- busca fisica

    /** Todas as mochilas fisicas de {@code player} em qualquer slot do inventario (nao so a hotbar). */
    public List<ItemStack> findPhysicalBackpacks(Player player) {
        List<ItemStack> found = new ArrayList<>();
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item != null && isBackpack(item)) found.add(item);
        }
        ItemStack offhand = inv.getItemInOffHand();
        if (isBackpack(offhand)) found.add(offhand);
        return found;
    }

    /** Se existe mochila virtual do tier em que o jogador pode minerar com mining ligado. */
    public Optional<Backpack> findMiningBackpack(org.bukkit.entity.Player player) {
        for (BackpackTier tier : tierRegistry.getAll()) {
            if (!player.hasPermission(tier.getPermissao()) || !tier.podeMinerar()) continue;
            Backpack bp = getOrCreateVirtual(player.getUniqueId(), tier.getId());
            if (bp.isMining()) return Optional.of(bp);
        }
        return Optional.empty();
    }
}
