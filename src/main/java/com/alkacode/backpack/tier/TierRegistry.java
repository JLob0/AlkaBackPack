package com.alkacode.backpack.tier;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registro dos 9 tiers lidos de config.yml (secao "tiers"). Recarregavel via
 * /abackpack reload. A ordem e a dos numeros de tier (1..9).
 */
public class TierRegistry {

    private final Map<Integer, BackpackTier> tiers = new LinkedHashMap<>();

    public void load(FileConfiguration config) {
        Map<Integer, BackpackTier> loaded = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("tiers");
        if (section != null) {
            for (String rawId : section.getKeys(false)) {
                ConfigurationSection t = section.getConfigurationSection(rawId);
                if (t == null) continue;
                int id;
                try {
                    id = Integer.parseInt(rawId);
                } catch (NumberFormatException e) {
                    continue;
                }
                loaded.put(id, new BackpackTier(
                        id,
                        t.getString("key", "tier" + id),
                        t.getString("nome", "Mochila"),
                        t.getString("nome-tier", "Tier " + id),
                        t.getString("cor", "<white>"),
                        t.getInt("slots", 9),
                        t.getInt("capacidade", t.getInt("slots", 9)),
                        t.getInt("paginas-iniciais", 1),
                        t.getInt("paginas-max", 1),
                        t.getString("permissao", "alkabackpack.use"),
                        t.getString("textura", ""),
                        t.getBoolean("editavel", true),
                        t.getBoolean("recursos.minerar", false),
                        t.getBoolean("recursos.converter-lingote", false)
                ));
            }
        }
        tiers.clear();
        tiers.putAll(loaded);
    }

    public List<BackpackTier> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(tiers.values()));
    }

    public int size() {
        return tiers.size();
    }

    public Optional<BackpackTier> get(int id) {
        return Optional.ofNullable(tiers.get(id));
    }

    public Optional<BackpackTier> getByKey(String key) {
        for (BackpackTier t : tiers.values()) {
            if (t.getKey().equalsIgnoreCase(key)) return Optional.of(t);
        }
        return Optional.empty();
    }
}
