# ADR 0005 — Off-ramp Router provider-first

**Data:** 24/08/2026
**Status:** Aceita — **contexto superado pela
[ADR 0006](0006-inversao-b2b-api-first.md)** (26/08/2026)
**Contexto de produto:** [product-strategy.md](../product-strategy.md)

---

> ## ⚠️ Leia antes
>
> **A decisão desta ADR continua valendo:** não construímos KYC, KYT, AML,
> custódia, liquidez nem liquidação PIX, e ocupamos a posição de roteador
> neutro entre providers.
>
> **O contexto em que ela foi tomada, não.** O texto abaixo assume um cliente
> self-custody e descreve a camada própria como aquela que fica entre a
> carteira do usuário e a infraestrutura — com wallet connect, token discovery
> e rota via DEX. A [ADR 0006](0006-inversao-b2b-api-first.md) inverteu essa
> premissa: o cliente primário passou a ser a empresa que integra a API, e
> wallet, DEX e cauda longa saíram do MVP.
>
> Leia este documento como registro histórico da decisão de fronteira, e a 0006
> para o escopo vigente.

## Contexto

A v0.1 dos requisitos assumia que construiríamos a maior parte do fluxo
Crypto → PIX, integrando parceiros regulados apenas para KYC, liquidez e
liquidação — mas mantendo a intenção de, com o tempo, internalizar capacidades.

O levantamento competitivo de 24/08/2026 mostrou que essa camada já está
construída e operando no mercado brasileiro:

- **BRL0** — compra e venda de cripto com PIX, 1.200+ ativos, Solana, API REST,
  off-ramp, webhooks, painel operacional e white-label. Anuncia liquidação em
  até ~10 minutos.
- **Foxbit Infra** — PIX ↔ cripto, off-ramp, KYC, KYT, AML, antifraude,
  custódia, liquidez agregada de 110+ venues com market making próprio,
  Smart Order Routing, webhooks, REST/WebSocket e infraestrutura regulatória.
  Permite que fintechs operem sob o registro regulatório da própria Foxbit.

Construir o que essas empresas já têm significaria meses de trabalho para chegar,
na melhor hipótese, empatado — sem licença, sem liquidez e sem histórico
operacional.

Ao mesmo tempo, nenhuma das duas oferece a camada que fica **entre a carteira do
usuário e essa infraestrutura**: conexão nativa com wallet, descoberta de tokens,
rota de liquidez via DEX para tokens de cauda longa, e escolha entre múltiplos
off-ramps.

E há uma razão estrutural para isso: um provider não tem incentivo para rotear
volume ao concorrente. A posição de roteador neutro só é ocupável por quem não é
provider.

## Decisão

**Não construiremos KYC, KYT, AML, custódia, liquidez ou liquidação PIX.**
Essas capacidades serão consumidas de providers regulados.

**Construiremos a camada de orquestração**, composta de:

1. Wallet-native UX (Phantom, Solflare, Backpack)
2. Token discovery e elegibilidade por liquidez
3. Route Engine — rota de liquidez via DEX até uma stablecoin off-rampável
4. **Off-ramp Router** — seleção de provider por valor, latência,
   disponibilidade e risco
5. Quote, Order, Journal probatório, tracking em tempo real e reconciliação multi-provider

Todo provider de off-ramp fica atrás de um adapter. Nenhuma classe de domínio
importa SDK de fornecedor. Trocar, adicionar ou desativar um provider é
configuração e um adapter novo — nunca mudança no domínio.

## Consequências

### Positivas

- Chegamos ao diferencial em semanas, não em meses, porque não recriamos
  compliance, custódia e liquidez.
- Operar sob o registro regulatório de um provider reduz drasticamente o custo e
  o prazo de entrada.
- Multi-provider dá redundância operacional: um provider fora do ar deixa de ser
  incidente de plataforma e vira roteamento.
- A posição de roteador neutro é defensável justamente por não sermos provider.

### Negativas

- **Dependência de terceiro no caminho crítico do dinheiro.** A latência de
  liquidação PIX é do provider, não nossa. O SLA <30s da tese é, por enquanto,
  hipótese não medida.
- **O apetite de compliance do provider limita nosso produto.** Se o provider
  recusar fluxo originado de swap de memecoin, o caso de uso emblemático morre
  em contrato, não em engenharia.
- **Margem comprimida.** Ganhamos no spread sobre o spread do provider.
- **Reconciliação deixa de ser opcional no MVP.** Cada provider tem extrato, fee
  e timing próprios; sem reconciliação automática, divergência vira prejuízo
  silencioso.
- **Roteamento com um provider é só um adapter.** O valor só aparece com N ≥ 2
  contratados e ativos.

### Neutras

- Os requisitos `RF-*` de disciplina financeira continuam integralmente válidos.
  O journal de transações imutável (append-only), idempotência e auditoria ficam **mais** necessários
  com múltiplas contrapartes, não menos.

## Alternativas consideradas

**Construir a infraestrutura completa.** Rejeitada: prazo de licenciamento,
custo de liquidez e ausência de histórico operacional tornam inviável competir
de frente com quem já opera.

**Integrar um único provider e não rotear.** Rejeitada como destino, aceita como
etapa. É a Fase 2 do roadmap. Como produto final, nos deixaria sem diferencial
defensável — seria um front-end bonito para o produto de outra empresa.

**Ser apenas white-label de um provider.** Rejeitada: entrega margem e roadmap
para o provider e não constrói ativo próprio.

## Validação pendente

Esta decisão depende de premissas ainda não medidas. Ver seção 9 da
[estratégia de produto](../product-strategy.md):

- **Spike 0** — latência real de liquidação PIX em sandbox dos providers
- **Spike 1** — apetite de compliance para fluxo originado de DEX
- **Spike 2** — viabilidade de contratar dois providers em paralelo
- **Spike 3** — price impact real na Jupiter para tickets de R$ 100 a R$ 1.000

Um "não" no Spike 1 ou um resultado ruim no Spike 0 exige revisitar esta ADR.
