package com.alkacode.backpack.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Fonte unica de mensagens do AlkaBackpack - so MiniMessage, nunca codigos '&'/'§'.
 * Cada mensagem tem um caminho fixo em messages.yml; placeholders sao pares
 * <tag>/valor substituidos antes do parse (padrao da rede Alka*).
 */
public class Messages {

    private final JavaPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private FileConfiguration config;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) Files.copy(in, file.toPath());
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar messages.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        try (InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Falha ao carregar defaults de messages.yml: " + e.getMessage());
        }
    }

    public String raw(String path) {
        String value = config.getString(path);
        return value != null ? value : "<red>Mensagem ausente: " + path;
    }

    private String applyPlaceholders(String text, String... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }
        return text;
    }

    public Component prefix() {
        return mm.deserialize(raw("prefix"));
    }

    public Component get(String path, String... placeholders) {
        return prefix().append(mm.deserialize(applyPlaceholders(raw(path), placeholders)));
    }

    public Component getNoPrefix(String path, String... placeholders) {
        return mm.deserialize(applyPlaceholders(raw(path), placeholders));
    }

    public List<Component> getList(String path, String... placeholders) {
        List<Component> lines = new ArrayList<>();
        for (String line : config.getStringList(path)) {
            lines.add(mm.deserialize(applyPlaceholders(line, placeholders)));
        }
        return lines;
    }

    /** Parseia um texto MiniMessage cru (nao e uma chave de messages.yml) - para linhas dinamicas. */
    public Component parse(String miniMessage) {
        return mm.deserialize(miniMessage);
    }
}
