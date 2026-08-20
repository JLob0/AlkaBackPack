<div align="center">

# AlkaBackpack

### Mochilas físicas e virtuais, com tiers, skins e upgrade via economia

Sistema completo de mochilas: tiers progressivos, páginas expansíveis,
tanques de líquido, armazenamento de XP, senha por mochila e skins
customizáveis — incluindo visual real nas costas do jogador via
CosmeticsCore.

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.7-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

</div>

---

## 📋 Sobre o Projeto

O **AlkaBackpack** entrega mochilas **físicas** (item tradeable, com dono e
identidade própria) e **virtuais** (uma por tier, ilimitada, atrelada à
permissão do jogador) — com armazenamento de itens, líquidos e experiência,
páginas desbloqueáveis via upgrade, e uma loja de skins totalmente
YAML-driven.

## ✨ Funcionalidades Principais

- 🎒 **Tiers progressivos**: do comum ao divino, cada um com slots,
  capacidade e páginas próprias.
- 📄 **Páginas expansíveis**: upgrade de páginas extras via AlkaEconomy.
- 🧪 **Tanques de líquido**: armazena água, lava, leite, mel e neve.
- ⭐ **Armazenamento de XP**: guarda experiência para uso posterior.
- 🔒 **Senha por mochila**: protege o conteúdo, com fluxo de definição e
  confirmação via chat.
- ⛏️ **Coleta automática**: mineração direto para o estoque (integração
  com o hook de minas), toggle independente por jogador.
- 🎨 **Loja de skins 100% YAML-driven**: cada skin troca a aparência do
  item (textura de cabeça ou item ItemsAdder) e, quando disponível, o
  visual real **nas costas do jogador** via CosmeticsCore (`BODY_ITEM`) —
  com reconciliação periódica que corrige sozinha se a mochila for
  vendida, largada ou perdida, e um toggle por jogador pra quem não quer
  o visual nas costas.
- 🚪 **Abertura flexível**: clique direito na mão, ou `/abrirmochila`
  (acha a mochila física em qualquer slot do inventário — não precisa
  estar na hotbar — e escolhe a de maior tier automaticamente, ou um
  tier específico).

## 🎮 Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/abackpack` | Abre o seletor de mochilas virtuais | `alkabackpack.use` |
| `/abackpack auto` | Alterna coleta automática | `alkabackpack.auto` |
| `/abackpack cosmetico` | Liga/desliga o visual da skin nas costas | `alkabackpack.use` |
| `/abackpack give\|giveall <tier> <player>` | Dá mochila física | `alkabackpack.give`/`giveall` |
| `/abackpack list\|info` | Lista tiers e detalhes | — |
| `/abackpack reload` | Recarrega configuração | `alkabackpack.reload` |
| `/abackpack reset\|wipe` | Limpa mochilas (jogador ou tier inteiro) | `alkabackpack.reset`/`wipe` |
| `/abackpack open\|edit [player] [tier]` | Abre/edita mochila de qualquer jogador | `alkabackpack.open`/`edit` |
| `/abrirmochila [tier]` | Abre a mochila física (melhor tier, ou um específico) de qualquer slot do inventário | `alkabackpack.use` |

## 🔗 Integrações

Construído sobre o **AlkaCore** + **AlkaEconomy** (hard depend). Softdepend
com **ItemsAdder** (skins), **CosmeticsCore** (skins nas costas —
`BODY_ITEM`), **AlkaVips**, **LuckPerms** e **PlaceholderAPI**.

## 🔧 Tecnologias Utilizadas

- **Java 21**
- **Paper API 1.21.8**
- **AlkaCore** (banco/GUI compartilhados)
- **MiniMessage** para formatação de texto

## ⚙️ Instalação

1. Baixe o `AlkaBackpack.jar` mais recente.
2. Coloque na pasta `plugins/` do servidor.
3. Certifique-se de ter o **AlkaCore** e o **AlkaEconomy** instalados (dependências obrigatórias).
4. Reinicie o servidor.
5. Ajuste `plugins/AlkaBackpack/config.yml` e `skins.yml` conforme necessário.

## 🔐 Permissões

| Permissão | Descrição | Padrão |
|-----------|-----------|--------|
| `alkabackpack.use` | Permite abrir/usar mochilas | true |
| `alkabackpack.auto` | Permite alternar a coleta automática | true |
| `alkabackpack.give` / `.giveall` | Permite dar mochilas | op |
| `alkabackpack.reload` | Recarrega configuração | op |
| `alkabackpack.reset` / `.wipe` | Limpa mochilas de um jogador / de um tier | op |
| `alkabackpack.open` / `.edit` | Abre/edita mochila de qualquer jogador | op |
| `alkabackpack.admin` | Permissão administrativa completa | op |
| `alkabackpack.skin.<id>` | Permite comprar/usar uma skin específica (uma por entrada em `skins.yml`) | — |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do ecossistema**: `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
