package com.alkacode.backpack.database;

import com.alkacode.backpack.model.Backpack;
import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unico ponto de acesso ao banco do AlkaBackpack, sobre o DatabaseProvider do
 * AlkaCore (HikariCP) - sem conexao propria. Uma linha por mochila (fisica ou
 * virtual), todas as paginas serializadas juntas na coluna `pages`.
 */
public final class BackpackRepository extends AbstractRepository {

    private final Logger logger;

    public BackpackRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTable();
        createPrefsTable();
        migrate();
    }

    private void createTable() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alka_backpack_data (
                        id VARCHAR(64) PRIMARY KEY,
                        owner_uuid VARCHAR(36),
                        tier INT DEFAULT 1,
                        pages TEXT,
                        current_page INT DEFAULT 0,
                        max_pages INT DEFAULT 1,
                        auto_collect BOOLEAN DEFAULT 1,
                        mining BOOLEAN DEFAULT 0,
                        password_hash VARCHAR(255),
                        keep_on_death BOOLEAN DEFAULT 0,
                        liquids TEXT,
                        xp_levels INT DEFAULT 0,
                        created_at BIGINT DEFAULT 0
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabela alka_backpack_data", e);
        }
    }

    /** Preferencias por jogador (nao por mochila) — hoje so o toggle do cosmetico nas costas. */
    private void createPrefsTable() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alka_backpack_prefs (
                        owner_uuid VARCHAR(36) PRIMARY KEY,
                        back_cosmetic BOOLEAN DEFAULT 1
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabela alka_backpack_prefs", e);
        }
    }

    /** Null = sem preferencia salva ainda (chamador deve assumir default true). */
    public Boolean loadBackCosmeticPref(UUID owner) {
        String sql = "SELECT back_cosmetic FROM alka_backpack_prefs WHERE owner_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean("back_cosmetic") : null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar preferencia de cosmetico de " + owner, e);
            return null;
        }
    }

    public void saveBackCosmeticPref(UUID owner, boolean enabled) {
        String sql = upsert("alka_backpack_prefs", new String[]{"owner_uuid", "back_cosmetic"}, new String[]{"owner_uuid"});
        try {
            execute(sql, ps -> {
                ps.setString(1, owner.toString());
                ps.setBoolean(2, enabled);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar preferencia de cosmetico de " + owner, e);
        }
    }

    /**
     * Adiciona colunas que possam ter sido criadas depois da primeira versao da
     * tabela (CREATE TABLE IF NOT EXISTS nao altera tabela existente). Rodado no
     * enable para nao exigir apagar o banco nem ALTER manual.
     */
    private void migrate() {
        java.util.Set<String> columns = new java.util.HashSet<>();
        if (db.isSQLite()) {
            try (Connection conn = db.getConnection();
                 var rs = conn.createStatement().executeQuery("PRAGMA table_info(alka_backpack_data)")) {
                while (rs.next()) columns.add(rs.getString("name"));
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Erro ao inspecionar colunas de alka_backpack_data", e);
                return;
            }
        } else {
            String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'alka_backpack_data'";
            try (Connection conn = db.getConnection();
                 var rs = conn.createStatement().executeQuery(sql)) {
                while (rs.next()) columns.add(rs.getString("COLUMN_NAME"));
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Erro ao inspecionar colunas de alka_backpack_data", e);
                return;
            }
        }

        String[][] additions = {
                {"max_pages", "INT DEFAULT 1"},
                {"auto_collect", "BOOLEAN DEFAULT 1"},
                {"mining", "BOOLEAN DEFAULT 0"},
                {"password_hash", "VARCHAR(255)"},
                {"keep_on_death", "BOOLEAN DEFAULT 0"},
                {"liquids", "TEXT"},
                {"xp_levels", "INT DEFAULT 0"},
        };
        for (String[] col : additions) {
            if (columns.contains(col[0])) continue;
            try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE alka_backpack_data ADD COLUMN " + col[0] + " " + col[1]);
                logger.info("Migracao: coluna adicionada a alka_backpack_data -> " + col[0]);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Erro ao adicionar coluna " + col[0] + " em alka_backpack_data", e);
            }
        }
    }

    public Backpack load(String id) {
        String sql = "SELECT owner_uuid, tier, pages, current_page, max_pages, auto_collect, mining, password_hash, "
                + "keep_on_death, liquids, xp_levels, created_at FROM alka_backpack_data WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    Backpack bp = new Backpack(id, owner, rs.getInt("tier"));
                    bp.setCurrentPage(rs.getInt("current_page"));
                    bp.setUnlockedPages(rs.getInt("max_pages"));
                    bp.setAutoCollect(rs.getBoolean("auto_collect"));
                    bp.setMining(rs.getBoolean("mining"));
                    String hash = rs.getString("password_hash");
                    bp.setPasswordHash(hash);
                    bp.setKeepOnDeath(rs.getBoolean("keep_on_death"));
                    bp.setXpLevels(rs.getInt("xp_levels"));
                    bp.getTanks().putAll(deserializeTanks(rs.getString("liquids")));
                    bp.setCreatedAt(rs.getLong("created_at"));
                    String pagesRaw = rs.getString("pages");
                    if (pagesRaw != null && !pagesRaw.isEmpty()) {
                        bp.getPages().putAll(deserializePages(pagesRaw));
                    }
                    return bp;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao carregar mochila " + id, e);
        }
        return null;
    }

    public void save(Backpack bp) {
        String sql = upsert("alka_backpack_data",
                new String[]{"id", "owner_uuid", "tier", "pages", "current_page", "max_pages", "auto_collect", "mining",
                        "password_hash", "keep_on_death", "liquids", "xp_levels", "created_at"},
                new String[]{"id"});
        try {
            execute(sql, ps -> {
                ps.setString(1, bp.getId());
                ps.setString(2, bp.getOwner().toString());
                ps.setInt(3, bp.getTier());
                ps.setString(4, serializePages(bp.getPages()));
                ps.setInt(5, bp.getCurrentPage());
                ps.setInt(6, bp.getUnlockedPages());
                ps.setBoolean(7, bp.isAutoCollect());
                ps.setBoolean(8, bp.isMining());
                ps.setString(9, bp.getPasswordHash());
                ps.setBoolean(10, bp.isKeepOnDeath());
                ps.setString(11, serializeTanks(bp.getTanks()));
                ps.setInt(12, bp.getXpLevels());
                ps.setLong(13, bp.getCreatedAt());
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar mochila " + bp.getId(), e);
        }
    }

    public void delete(String id) {
        String sql = "DELETE FROM alka_backpack_data WHERE id = ?";
        try {
            execute(sql, ps -> ps.setString(1, id));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao apagar mochila " + id, e);
        }
    }

    public void deleteByOwner(UUID owner) {
        String sql = "DELETE FROM alka_backpack_data WHERE owner_uuid = ?";
        try {
            execute(sql, ps -> ps.setString(1, owner.toString()));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao apagar mochilas de " + owner, e);
        }
    }

    public void wipeTier(int tier) {
        String sql = "DELETE FROM alka_backpack_data WHERE tier = ?";
        try {
            execute(sql, ps -> ps.setInt(1, tier));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao apagar mochilas do tier " + tier, e);
        }
    }

    /** Copia de seguranca: serializa tudo em base64 num arquivo (feito em thread async pelo chamador). */
    public String backupSnapshot() {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT id, owner_uuid, tier, pages, current_page, max_pages, auto_collect, mining, password_hash, "
                + "keep_on_death, liquids, xp_levels, created_at FROM alka_backpack_data";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sb.append(rs.getString("id")).append('|')
                        .append(rs.getString("owner_uuid")).append('|')
                        .append(rs.getInt("tier")).append('|')
                        .append(rs.getString("pages") == null ? "" : rs.getString("pages")).append('|')
                        .append(rs.getInt("current_page")).append('|')
                        .append(rs.getInt("max_pages")).append('|')
                        .append(rs.getBoolean("auto_collect")).append('|')
                        .append(rs.getBoolean("mining")).append('|')
                        .append(rs.getString("password_hash") == null ? "" : rs.getString("password_hash")).append('|')
                        .append(rs.getBoolean("keep_on_death")).append('|')
                        .append(rs.getString("liquids") == null ? "" : rs.getString("liquids")).append('|')
                        .append(rs.getInt("xp_levels")).append('|')
                        .append(rs.getLong("created_at")).append('\n');
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao gerar snapshot de backup", e);
        }
        return sb.toString();
    }

    private String serializeTanks(Map<String, Integer> tanks) {
        if (tanks.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : new TreeMap<>(tanks).entrySet()) {
            if (e.getValue() <= 0) continue;
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    private Map<String, Integer> deserializeTanks(String data) {
        Map<String, Integer> tanks = new TreeMap<>();
        if (data == null || data.isEmpty()) return tanks;
        for (String part : data.split(";")) {
            int idx = part.indexOf(':');
            if (idx <= 0) continue;
            try {
                tanks.put(part.substring(0, idx), Integer.parseInt(part.substring(idx + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return tanks;
    }

    // ---------------------------------------------------------------- serial

    private String serializePages(Map<Integer, ItemStack[]> pages) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            Map<Integer, ItemStack[]> ordered = new TreeMap<>(pages);
            dataOutput.writeInt(ordered.size());
            for (Map.Entry<Integer, ItemStack[]> entry : ordered.entrySet()) {
                dataOutput.writeInt(entry.getKey());
                dataOutput.writeInt(entry.getValue().length);
                for (ItemStack item : entry.getValue()) {
                    dataOutput.writeObject(item);
                }
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao serializar paginas da mochila.", e);
        }
    }

    private Map<Integer, ItemStack[]> deserializePages(String data) {
        Map<Integer, ItemStack[]> pages = new TreeMap<>();
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int pageCount = dataInput.readInt();
            for (int i = 0; i < pageCount; i++) {
                int pageIndex = dataInput.readInt();
                int length = dataInput.readInt();
                ItemStack[] items = new ItemStack[length];
                for (int slot = 0; slot < length; slot++) {
                    items[slot] = (ItemStack) dataInput.readObject();
                }
                pages.put(pageIndex, items);
            }
            dataInput.close();
            return pages;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Erro ao desserializar paginas da mochila.", e);
            return new TreeMap<>();
        }
    }
}
