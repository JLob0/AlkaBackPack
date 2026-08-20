package com.alkacode.backpack.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Carrega menus.yml (titulos, tamanhos, icones e acoes de todos os menus) com
 * fallback ao resource embutido. Recarregavel via /abackpack reload.
 */
public class Menus {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public Menus(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "menus.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("menus.yml")) {
                if (in != null) Files.copy(in, file.toPath());
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar menus.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        try (InputStream defaultStream = plugin.getResource("menus.yml")) {
            if (defaultStream != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao carregar defaults de menus.yml: " + e.getMessage());
        }
    }

    public ConfigurationSection getMenu(String name) {
        return config.getConfigurationSection(name);
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }
}
