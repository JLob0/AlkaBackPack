package com.alkacode.backpack.tier;

/**
 * Representa um nivel (tier) de mochila configurado em config.yml. Imutavel;
 * o TierRegistry monta uma lista a partir da configuracao.
 */
public class BackpackTier {

    private final int id;
    private final String key;
    private final String nome;
    private final String nomeTier;
    private final String cor;
    private final int slots;
    private final int capacidade;
    private final int paginasIniciais;
    private final int paginasMax;
    private final String permissao;
    private final String textura;
    private final boolean editavel;
    private final boolean minerar;
    private final boolean converterLingote;

    public BackpackTier(int id, String key, String nome, String nomeTier, String cor, int slots, int capacidade,
                        int paginasIniciais, int paginasMax, String permissao, String textura,
                        boolean editavel, boolean minerar, boolean converterLingote) {
        this.id = id;
        this.key = key;
        this.nome = nome;
        this.nomeTier = nomeTier;
        this.cor = cor;
        this.slots = slots;
        this.capacidade = capacidade;
        this.paginasIniciais = paginasIniciais;
        this.paginasMax = paginasMax;
        this.permissao = permissao;
        this.textura = textura;
        this.editavel = editavel;
        this.minerar = minerar;
        this.converterLingote = converterLingote;
    }

    public int getId() { return id; }
    public String getKey() { return key; }
    public String getNome() { return nome; }
    public String getNomeTier() { return nomeTier; }
    public String getCor() { return cor; }
    public int getSlots() { return slots; }
    public int getCapacidade() { return capacidade; }
    public int getPaginasIniciais() { return paginasIniciais; }
    public int getPaginasMax() { return paginasMax; }
    public String getPermissao() { return permissao; }
    public String getTextura() { return textura; }
    public boolean isEditavel() { return editavel; }
    public boolean podeMinerar() { return minerar; }
    public boolean podeConverterLingote() { return converterLingote; }

    /** Linha de exibicao no padrao do chat: "[❖] Nome <white>usados/total <gray>slots" */
    public String chatLine(int usados) {
        return cor + "[❖] " + nome + " <white>" + usados + "/" + slots + " <gray>slots";
    }
}
