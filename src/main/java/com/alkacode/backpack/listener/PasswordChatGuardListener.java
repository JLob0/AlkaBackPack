package com.alkacode.backpack.listener;

import com.alkacode.backpack.AlkaBackpackPlugin;
import com.alkacode.backpack.gui.BackpackMenu;
import com.alkacode.backpack.model.Backpack;
import com.alkacode.backpack.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fluxo de senha via chat: captura o que o jogador digitar quando o plugin pedir
 * (definir nova senha ou digitar a existente). Hash SHA-256 antes de armazenar.
 */
public class PasswordChatGuardListener implements Listener {

    private enum Mode { SET, ENTER }

    /** Em SET, `firstPassword` guarda a 1a digitacao ate a confirmacao (2a). */
    private record Request(Mode mode, String backpackId, String firstPassword) {
        Request(Mode mode, String backpackId) {
            this(mode, backpackId, null);
        }
    }

    private final AlkaBackpackPlugin plugin;
    private final Messages messages;
    private final Map<UUID, Request> awaiting = new HashMap<>();

    public PasswordChatGuardListener(AlkaBackpackPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void requestSetPassword(Player player, String backpackId) {
        awaiting.put(player.getUniqueId(), new Request(Mode.SET, backpackId));
        player.sendMessage(messages.get("password-required"));
    }

    public void requestPassword(Player player, String backpackId) {
        awaiting.put(player.getUniqueId(), new Request(Mode.ENTER, backpackId));
        player.sendMessage(messages.get("password-required"));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Request request = awaiting.get(event.getPlayer().getUniqueId());
        if (request == null) return;
        event.setCancelled(true);

        Backpack bp = plugin.getBackpackService().getById(request.backpackId());
        if (bp == null) {
            awaiting.remove(event.getPlayer().getUniqueId());
            return;
        }

        if (request.mode() == Mode.SET) {
            // 1a digitacao: guarda e pede pra repetir
            if (request.firstPassword() == null) {
                awaiting.put(event.getPlayer().getUniqueId(),
                        new Request(Mode.SET, request.backpackId(), event.getMessage()));
                event.getPlayer().sendMessage(messages.get("password-confirm"));
                return;
            }
            // 2a digitacao: confirma
            if (!event.getMessage().equals(request.firstPassword())) {
                event.getPlayer().sendMessage(messages.get("password-mismatch"));
                awaiting.remove(event.getPlayer().getUniqueId());
                return;
            }
            bp.setPasswordHash(hash(request.firstPassword()));
            plugin.getBackpackService().save(bp);
            // destrava na hora pra quem acabou de definir - nao faz sentido pedir
            // pra digitar de novo o que a pessoa acabou de digitar.
            plugin.getBackpackService().unlock(event.getPlayer().getUniqueId(), request.backpackId());
            event.getPlayer().sendMessage(messages.get("password-set"));
            awaiting.remove(event.getPlayer().getUniqueId());
            return;
        }

        // ENTER
        if (hash(event.getMessage()).equals(bp.getPasswordHash())) {
            plugin.getBackpackService().unlock(event.getPlayer().getUniqueId(), request.backpackId());
            event.getPlayer().sendMessage(messages.get("password-correct"));
            awaiting.remove(event.getPlayer().getUniqueId());
            Player p = event.getPlayer();
            new BackpackMenu(plugin, messages, plugin.getTierRegistry())
                    .open(p, bp, bp.getCurrentPage(), false);
        } else {
            event.getPlayer().sendMessage(messages.get("password-incorrect"));
        }
    }

    static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : encoded) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
