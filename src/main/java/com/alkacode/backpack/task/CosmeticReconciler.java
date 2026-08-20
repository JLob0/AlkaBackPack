package com.alkacode.backpack.task;

import com.alkacode.backpack.hook.CosmeticsCoreHook;
import com.alkacode.backpack.service.BackpackService;
import com.alkacode.backpack.util.Skins;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Corrige periodicamente o cosmetico BODY_ITEM (mochila nas costas) de cada
 * jogador online pra bater com a realidade: se ele desligou o cosmetico nas
 * preferencias, ou vendeu/largou/perdeu a mochila com a skin equipada, o
 * CosmeticsCore nao sabe disso sozinho (equip() e por jogador, nao por item) —
 * essa task e quem mantem os dois sincronizados. So mexe em chaves que o
 * proprio AlkaBackpack gerencia (Skins#getAllCosmeticKeys); nunca toca em
 * chapeu/balao/etc equipados pelo jogador direto no CosmeticsCore.
 */
public class CosmeticReconciler implements Runnable {

    private final BackpackService service;
    private final Skins skins;

    public CosmeticReconciler(BackpackService service, Skins skins) {
        this.service = service;
        this.skins = skins;
    }

    @Override
    public void run() {
        if (!CosmeticsCoreHook.isAvailable()) return;
        Set<String> ourKeys = skins.getAllCosmeticKeys();
        for (Player player : Bukkit.getOnlinePlayers()) {
            reconcile(player, ourKeys);
        }
    }

    /** Reconcilia so um jogador, na hora — usado pra dar feedback imediato (ex: comando de toggle) em vez de esperar o proximo tick da task. */
    public void reconcile(Player player) {
        if (!CosmeticsCoreHook.isAvailable()) return;
        reconcile(player, skins.getAllCosmeticKeys());
    }

    private void reconcile(Player player, Set<String> ourKeys) {
        String desiredKey = service.isBackCosmeticEnabled(player.getUniqueId())
                ? findDesiredCosmeticKey(player)
                : null;

        List<String> equipped = CosmeticsCoreHook.getEquippedKeys(player);
        for (String key : equipped) {
            if (ourKeys.contains(key) && !key.equals(desiredKey)) {
                CosmeticsCoreHook.unequip(player, key);
            }
        }
        if (desiredKey != null && !equipped.contains(desiredKey)) {
            CosmeticsCoreHook.equip(player, desiredKey);
        }
    }

    /** Maior tier entre as mochilas fisicas com skin aplicada e cosmetico registrado; primeira em empate (deterministico, sem flicker). Null = nada pra mostrar. */
    public String findDesiredCosmeticKey(Player player) {
        String bestKey = null;
        int bestTier = Integer.MIN_VALUE;
        for (ItemStack item : service.findPhysicalBackpacks(player)) {
            String skinId = skins.getAppliedSkinId(item);
            if (skinId == null) continue;
            String cosmeticKey = skins.resolveCosmeticKey(skinId);
            if (!CosmeticsCoreHook.isRegistered(cosmeticKey)) continue;
            int tier = service.getItemTier(item);
            if (tier > bestTier) {
                bestTier = tier;
                bestKey = cosmeticKey;
            }
        }
        return bestKey;
    }
}
