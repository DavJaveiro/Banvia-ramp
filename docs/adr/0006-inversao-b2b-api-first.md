# ADR 0006 — Inversão B2B e API-first

**Data:** 26/08/2026
**Status:** Aceita
**Supera em contexto:** [ADR 0005 — Off-ramp Router provider-first](0005-offramp-router-provider-first.md)
**Contexto de produto:** [product-strategy.md](../product-strategy.md) ·
[requisitos v4.0](../requisitos-banvia-v4.0.md)

---

## Contexto

A ADR 0005 decidiu não construir infraestrutura regulada e ocupar a posição de
roteador neutro entre providers. Essa decisão continua correta e não está sendo
revertida.

O que ela assumia, porém, era um cliente específico: o usuário self-custody. O
texto da 0005 descreve a camada própria como *"a que fica entre a carteira do
usuário e essa infraestrutura"*, e lista conexão nativa com wallet, descoberta
de tokens e rota de liquidez via DEX como o conteúdo dessa camada.

Três levantamentos posteriores tornaram essa premissa frágil.

**A agregação está virando commodity.** Em entrevista de 15/06/2026, os co-CEOs
da Onramper — uma agregadora — descrevem o próprio diferencial como qualidade da
escolha alimentada por dados de execução, e não como quantidade de providers
integrados. Quem lidera a categoria está dizendo que o catálogo não é o ativo.

**A curva de receita de um usuário não sustenta o custo da camada.** Um usuário
self-custody traz uma transação. A disciplina exigida para operá-la com
correção — idempotência, reconciliação, evidência, observabilidade — tem custo
fixo alto. Esse custo só se amortiza com fluxo recorrente, que vem de empresa
integrada, não de indivíduo.

**Os riscos da cauda longa não são de engenharia.** O fluxo cuja origem é swap
de memecoin é o que atrai maior escrutínio de AML, e quem carrega a licença é o
venue. Construir esse diferencial antes de confirmar apetite de compliance é
apostar um trimestre numa conversa de quinze minutos.

## Decisão

**O cliente primário passa a ser a empresa que integra a API.** O produto deve
ser inteiramente utilizável sem nenhum frontend Banvia.

**O ativo de origem prioritário passa a ser stablecoin** — USDC e USDT — em
redes de custo baixo, escolhidas por cobertura de venue contratado e não por
preferência técnica.

**Saem do escopo do MVP:** portfolio discovery de wallet, conexão de carteira
como jornada principal, DEX routing, memecoins e cauda longa, KYC reutilizável,
cartão, conta virtual, on-ramp BRL → cripto e route splitting entre venues.

**Entram como produto inicial:** Quote API com normalização multi-venue, Route
Engine com política versionada e decisão reproduzível, Payout API idempotente,
lifecycle normalizado por webhook, reconciliação multi-venue e multi-tenancy.

**Permanece integralmente:** a fronteira da 0005 — não construímos KYC, KYT,
AML, custódia, liquidez nem liquidação PIX — e toda a disciplina de correção
distribuída.

## Consequências

### Positivas

- O produto passa a ter um comprador com contrato e volume recorrente, em vez
  de uma transação avulsa.
- O escopo do MVP encolhe de forma significativa: sem wallet, sem DEX e sem
  cauda longa, o caminho até o primeiro payout real é muito mais curto.
- Os riscos de execução on-chain — slippage, tokens falsos, MEV, manipulação de
  preço — deixam de ser risco de produto e voltam a ser risco adiado.
- A conversa comercial com venues fica mais simples: stablecoin de liquidez
  profunda é a origem de fundos menos controversa possível.

### Negativas

- Perdemos o diferencial mais chamativo da v0.2 — memecoin de cauda longa
  virando PIX — que era o que tornava a demo memorável.
- O cliente empresarial tem ciclo de venda longo, exige integração assistida e
  cobra SLA. O produto deixa de poder ser lançado sozinho.
- Sem interface própria, a percepção de valor fica menos visível: o cliente vê
  uma API, não uma experiência.

### Neutras

- O **Route Graph permanece no domínio** mesmo com rotas mais curtas. Uma rota
  direta é o mesmo modelo com menos legs, o que evita reescrita quando a Fase 7
  trouxer DEX de volta.
- Os requisitos `RF-*` de disciplina financeira continuam integralmente
  válidos. Com múltiplas contrapartes, ficam **mais** necessários.
- A Banvia é 100% não-custodial e nunca manterá saldo de clientes ou posições de tesouraria.
  O modelo financeiro é exclusivamente o **journal imutável probatório**.
  Ver v4.0 §13.

## Alternativas consideradas

**Manter wallet-first e adiar B2B.** Rejeitada: adia a validação comercial para
depois de construir a parte mais arriscada do produto, e aposta em um
diferencial que depende de aprovação de compliance ainda não obtida.

**Fazer os dois em paralelo.** Rejeitada: dobra a superfície do MVP com o mesmo
time. As duas jornadas compartilham o core de payout mas divergem inteiramente
em aquisição, suporte, compliance e interface.

**Virar white-label de um venue.** Rejeitada pelo mesmo motivo da 0005: entrega
margem e roadmap ao venue e não constrói ativo próprio.

## Validação pendente

Esta decisão depende de premissas ainda não medidas. Ver seção 9 da
[estratégia de produto](../product-strategy.md):

- **Spike 0** — latência real de liquidação PIX no sandbox dos venues
- **Spike 1** — apetite de compliance por origem de fundos
- **Spike 2** — viabilidade de contratar dois venues em paralelo

Um resultado ruim no Spike 2 não derruba a inversão B2B, mas derruba o Route
Engine como diferencial — e o produto passaria a se justificar apenas por
redução de integração e confiabilidade operacional.
