package com.alkacode.backpack.economy;

import com.alkacode.economy.CurrencyDefinition;
import com.alkacode.economy.EconomyManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

/**
 * Ponte para uma moeda do AlkaEconomy resolvida por id configurável em
 * economias.yml (currency-id, padrao "alkarion"). Mesmo padrao do
 * EconomyService do AlkaEnderChest - consome o EconomyManager diretamente,
 * sem Vault como intermediario (AlkaEconomy e depend obrigatorio).
 */
public class EconomyService {

    private static final String DEFAULT_CURRENCY_ID = "alkarion";

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private FileConfiguration economiasConfig;
    private String currencyId;
    private String currencyName;

    public EconomyService(JavaPlugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.economiasConfig = loadEconomiasConfig(plugin);
        resolveCurrency();
    }

    public void reload() {
        economiasConfig = loadEconomiasConfig(plugin);
        resolveCurrency();
    }

    private void resolveCurrency() {
        String requested = economiasConfig.getString("currency-id", DEFAULT_CURRENCY_ID).toLowerCase(Locale.ROOT);
        if (!economyManager.isValidCurrency(requested)) {
            plugin.getLogger().warning("Moeda '" + requested + "' configurada em economias.yml (currency-id) nao "
                    + "existe no AlkaEconomy - usando '" + DEFAULT_CURRENCY_ID + "'.");
            requested = DEFAULT_CURRENCY_ID;
        }
        String resolved = requested;
        this.currencyId = resolved;
        this.currencyName = economyManager.getCurrencies().stream()
                .filter(currency -> currency.id().equals(resolved))
                .findFirst()
                .map(CurrencyDefinition::name)
                .orElse(resolved);
    }

    private FileConfiguration loadEconomiasConfig(JavaPlugin plugin) {
        File economiasFile = new File(plugin.getDataFolder(), "economias.yml");
        if (!economiasFile.exists()) {
            try (InputStream in = plugin.getResource("economias.yml")) {
                if (in != null) Files.copy(in, economiasFile.toPath());
            } catch (IOException e) {
                plugin.getLogger().warning("Nao foi possivel criar economias.yml: " + e.getMessage());
            }
        }
        return YamlConfiguration.loadConfiguration(economiasFile);
    }

    public boolean has(UUID uuid, double amount) {
        return economyManager.has(uuid, currencyId, amount);
    }

    public double getBalance(UUID uuid) {
        return economyManager.getBalance(uuid, currencyId);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!economyManager.has(uuid, currencyId, amount)) {
            return false;
        }
        economyManager.removeBalance(uuid, currencyId, amount);
        return true;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return withdraw(player.getUniqueId(), amount);
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public double getUpgradePrice(int tier) {
        return economiasConfig.getDouble("upgrade-prices." + tier, 0.0);
    }

    public double getPagePrice() {
        return economiasConfig.getDouble("page-price", 2500.0);
    }

    public String format(double amount) {
        return EconomyManager.formatValue(amount);
    }
}
