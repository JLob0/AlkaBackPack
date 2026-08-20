package com.alkacode.backpack.model;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Estado de uma mochila (fisica ou virtual). O conteudo e guardado por paginas:
 * Map<Integer, ItemStack[]> onde o array tem tamanho fixo = slots do tier.
 * Persistido pelo BackpackRepository (blob base64 de todas as paginas).
 */
public class Backpack {

    private final String id;
    private final UUID owner;
    private int tier;
    private int currentPage;
    private int unlockedPages;
    private final Map<Integer, ItemStack[]> pages = new TreeMap<>();
    private final Map<String, Integer> tanks = new HashMap<>();
    private int xpLevels;
    private boolean autoCollect;
    private boolean mining;
    private String passwordHash;
    private boolean keepOnDeath;
    private long createdAt;

    public Backpack(String id, UUID owner, int tier) {
        this.id = id;
        this.owner = owner;
        this.tier = tier;
        this.autoCollect = true;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getUnlockedPages() { return unlockedPages; }
    public void setUnlockedPages(int unlockedPages) { this.unlockedPages = Math.max(1, unlockedPages); }
    public Map<Integer, ItemStack[]> getPages() { return pages; }
    public boolean isAutoCollect() { return autoCollect; }
    public void setAutoCollect(boolean autoCollect) { this.autoCollect = autoCollect; }
    public boolean isMining() { return mining; }
    public void setMining(boolean mining) { this.mining = mining; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isProtected() { return passwordHash != null && !passwordHash.isEmpty(); }
    public boolean isKeepOnDeath() { return keepOnDeath; }
    public void setKeepOnDeath(boolean keepOnDeath) { this.keepOnDeath = keepOnDeath; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // ----------------------------------------------------------------- líquidos/XP

    /** Tanques de liquido: tipo (WATER/LAVA/MILK/SNOW/HONEY) -> quantidade em baldes. */
    public Map<String, Integer> getTanks() { return tanks; }

    public int getTank(String type) {
        return tanks.getOrDefault(type, 0);
    }

    public void setTank(String type, int amount) {
        if (amount <= 0) tanks.remove(type);
        else tanks.put(type, amount);
    }

    public int getXpLevels() { return xpLevels; }

    public void setXpLevels(int xpLevels) { this.xpLevels = Math.max(0, xpLevels); }

    public ItemStack[] getPage(int page) {
        ItemStack[] items = pages.get(page);
        return items != null ? items : new ItemStack[0];
    }

    public void setPage(int page, ItemStack[] items, int slots) {
        ItemStack[] fixed = new ItemStack[slots];
        System.arraycopy(items, 0, fixed, 0, Math.min(items.length, slots));
        pages.put(page, fixed);
    }
}
