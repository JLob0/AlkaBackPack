package com.alkacode.backpack.gui;

/**
 * Calcula o layout do inventario de uma mochila a partir do numero de slots do
 * tier. Garante que o inventario nunca exceda 54 slots (maximo do Bukkit) e que
 * a fileira de controle nao se sobreponha ao armazenamento.
 *
 * Regra: fileiras de armazenamento = ceil(slots/9). Se isso couber em <= 5
 * fileiras, sobra 1 fileira extra para o controle (total <= 6). Se ocupar 6
 * fileiras (slots > 45), nao ha fileira extra: o controle fica nos slots de
 * sobra no fim da grade (para slots == 54 nao ha espaco, entao nao ha controles).
 */
public final class BackpackLayout {

    private BackpackLayout() {
    }

    public static int totalRows(int slots) {
        int storageRows = storageRows(slots);
        return storageRows <= 5 ? storageRows + 1 : 6;
    }

    public static int storageRows(int slots) {
        return (int) Math.ceil(slots / 9.0);
    }

    public static int inventorySize(int slots) {
        return totalRows(slots) * 9;
    }

    /** Fim do armazenamento (slots 0..storageEnd-1 sao de colocacao). */
    public static int storageEnd(int slots) {
        return Math.min(slots, controlStart(slots));
    }

    /**
     * Inicio da fileira de controle. Tiers de ate 5 fileiras ganham 1 fileira extra.
     * Tiers de 6 fileiras (slots > 45) nao tem fileira extra, entao reserva-se a
     * fileira de baixo (slots 45..53) para o controle, sacrificando ate 9 slots de
     * armazenamento (o inventario maximo do Bukkit e 54).
     */
    public static int controlStart(int slots) {
        int storageRows = storageRows(slots);
        if (storageRows <= 5) return storageRows * 9;
        return 45;
    }

    /** Ultimo slot de controle (inclusive) ou -1 se nao ha controles. */
    public static int controlEnd(int slots) {
        return inventorySize(slots) - 1;
    }
}
