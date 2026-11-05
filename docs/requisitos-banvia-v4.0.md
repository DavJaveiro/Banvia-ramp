# Banvia — Requisitos do Sistema v4.0

> ## ⚠️ Este documento não é o escopo do MVP
>
> **O escopo da primeira versão está em
> [`requisitos-banvia-mvp-v1.md`](requisitos-banvia-mvp-v1.md).**
>
> A [ADR 0007](adr/0007-reducao-de-escopo-do-mvp.md) reduziu o MVP a um caminho único —
> USDC, uma rede, PIX — com **routing por disponibilidade**, não por score. O motivo:
> o valor do Route Engine descrito aqui só é demonstrável com volume e histórico, que
> o MVP não tem. Há uma hipótese anterior e mais barata a testar — a de que empresas
> valorizam a abstração dos providers.
>
> **O que este documento continua sendo:** a arquitetura-alvo. `RouteScore`,
> `RoutingPolicy` versionada, normalização multi-venue, reconciliação em cinco fontes e
> multi-tenancy plena são o que a **V3 e a V4** implementam.
>
> **O que dele vale desde o MVP:** idempotência (§10), o estado de resultado
> desconhecido (§6.1), evidência de liquidação para concluir (RN-STATE-001), a fronteira
> de adapters (§7) e a fronteira regulatória (§15).

**Plataforma/marca:** Banvia
**Produto inicial:** **Banvia Ramp**
**Categoria:** **Crypto ↔ PIX Orchestration Infrastructure**
**Posicionamento externo:** **Crypto to PIX infrastructure. One API.**
**Tese interna:** **Banvia é a camada de orquestração entre stablecoins e payment rails.**
**Substitui:** [`requisitos-banvia-ramp-v3.3.md`](requisitos-banvia-ramp-v3.3.md) quanto a prioridade de produto
**Status:** **arquitetura-alvo** — vigente para V3 em diante, não para o MVP
**Decisão que o originou:** [ADR 0006 — Inversão B2B e API-first](adr/0006-inversao-b2b-api-first.md)
**Leitura longa:** [`Banvia_Monografia_ABNT_Orquestracao_Crypto_PIX_v4.0.pdf`](Banvia_Monografia_ABNT_Orquestracao_Crypto_PIX_v4.0.pdf)
**Índice da documentação:** [`README.md`](README.md)

---

## 1. A mudança da v3.3 para a v4.0

A v3.3 modelava o Banvia Ramp como um produto **wallet-first**: o usuário self-custody
conecta a Phantom, o sistema descobre o patrimônio, avalia elegibilidade, monta um Route
Graph que pode passar por Jupiter, e entrega PIX. A API B2B aparecia na Fase 6.

A v4.0 **inverte essa ordem**.

| Tema | v3.3 | v4.0 |
|---|---|---|
| Cliente primário | usuário self-custody | **empresa que integra a API** |
| Interface primária | wallet UI | **REST API + webhooks** |
| Intenção expressa | "quero sacar o que tenho nesta wallet" | **"entregue R$ X via PIX a este beneficiário"** |
| Jornada inicial | portfolio discovery → eligibility → route | **quote → route → payout** |
| Ativos | SOL/USDC + cauda longa | **USDC/USDT primeiro** |
| Wallet Standard | núcleo do MVP | extensão futura |
| Jupiter/DEX | parte importante | fora do MVP |
| B2B API | Fase 6 | **produto inicial** |
| Moat | cobertura + UX de wallet | **dados de execução + confiabilidade + DX** |

### 1.1 Por que a inversão

O paralelo é a Onramper. Ela começou resolvendo *"como colocar fiat dentro de uma
wallet?"* e, segundo entrevista de 15/06/2026 com os co-CEOs Rick Thomas e Maurits
Dewina, está transformando aquela solução específica numa camada de infraestrutura
financeira — identidade reutilizável, contas virtuais, programas de cartão e APIs de
stablecoin, com o objetivo declarado de virar um *"operating system for user onboarding
in Web3"*.

Duas leituras importam para nós:

**Agregação pura está virando commodity.** A própria liderança da Onramper reconhece
isso. O diferencial declarado deixou de ser "temos 30 providers" e passou a ser a
qualidade da escolha: menos KYC repetido, maior taxa de sucesso e seleção automática do
melhor caminho, alimentada por anos de dados de execução.

**A empresa que vende para empresas escala diferente.** Um usuário self-custody traz uma
transação. Uma wallet, uma fintech ou uma plataforma de payroll traz um fluxo recorrente,
com contrato, previsibilidade e integração profunda.

### 1.2 O que a v3.3 já acertou e permanece

A inversão de prioridade **não descarta** o trabalho conceitual da v3.3. Permanecem
integralmente válidos:

- **Provider-neutral** e **adapter boundary** (RN-008, RN-010);
- **Best executable route** — melhor entre as rotas efetivamente integradas (RN-006/007);
- toda a Seção 23.7 — **System Design e correção distribuída** (RN-SD-001 a RN-SD-042);
- **evidence over inference** e `COMPLETED` exigindo evidência (RN-SD-003);
- **idempotência** e ausência de exactly-once global (RN-SD-007 a RN-SD-011);
- **outbox/inbox** transacional (RN-SD-012/013);
- **reconciliação como mecanismo de correção** (RN-SD-031 a RN-SD-033);
- **dinheiro em `BigDecimal`** com escala e arredondamento explícitos (RN-SD-024/026);
- **governança do ranking** — reproduzibilidade, versionamento e revenue share separado
  (RN-RNK-001 a RN-RNK-006);
- o **glossário** da Seção 37 e a regra editorial da Seção 1.3.

Esses fundamentos passam a servir um produto B2B desde o primeiro dia, em vez de esperar
a Fase 6.

---

## 2. O que a Banvia é na v4.0

Uma plataforma de software e orquestração que oferece **uma API única** para:

1. descobrir rotas disponíveis entre um ativo de origem e o PIX;
2. obter e **normalizar quotes** de múltiplos exit venues;
3. **selecionar a melhor rota executável** conforme política versionada;
4. iniciar e acompanhar payouts;
5. normalizar webhooks e lifecycle heterogêneos;
6. reconciliar evidências de execução e liquidação;
7. produzir dados operacionais que melhoram o próprio routing;
8. abstrair diferenças de APIs, regras, ativos, redes, limites e estados entre venues.

### 2.1 A tese em uma frase

> **Uma integração para transformar cripto em PIX, escolhendo e operando o melhor exit
> venue disponível para cada contexto.**

### 2.2 O que a Banvia não é no MVP

Mantém-se integralmente a fronteira da v3.3, agora reforçada pelo enquadramento
regulatório de 2026:

- exchange;
- custodiante de ativos virtuais;
- emissora de stablecoin;
- DEX;
- PSP/participante PIX próprio;
- instituição que mantém saldo fiat do cliente;
- wallet de varejo;
- corretora B2C;
- liquidity desk ou market maker.

> **Alerta que permanece da v3.3:** chamar o produto de "orquestrador" **não garante**
> que ele fique fora do perímetro regulatório. O enquadramento depende de quem executa
> cada atividade, dos contratos, do fluxo de fundos, da custódia e do relacionamento com
> o cliente. Parecer jurídico específico é gate de produção.

---

## 3. Cliente-alvo

### 3.1 ICP inicial

Empresas com demanda de pagamentos ao Brasil que querem usar cripto sem construir a
ponte completa até o PIX:

1. wallets e apps Web3 que querem oferecer saque em BRL;
2. fintechs e neobanks internacionais;
3. plataformas de payroll e pagamento de contractors;
4. marketplaces globais e plataformas de freelancers;
5. empresas de remessas;
6. plataformas de treasury/settlement em stablecoins;
7. jogos e plataformas com economia interna, onde legalmente aplicável.

### 3.2 O que o cliente compra

Não compra "cripto". Compra **redução de integração**: cobertura, confiabilidade
operacional, observabilidade, reconciliação e a possibilidade de trocar ou combinar
venues sem reescrever a própria plataforma.

Sem Banvia, cada cliente resolve separadamente: quais venues atendem Brasil, quais
aceitam qual ativo em qual rede, KYC/KYB/KYT/AML e responsabilidades contratuais, quotes
com TTL incompatível, limites, requisitos de beneficiário, criação e status de payout,
webhooks duplicados e fora de ordem, timeout com resultado desconhecido, fallback entre
venues, reconciliação e evidência de settlement.

---

## 4. Princípios de produto

Os princípios da v3.3 são preservados; três são reescritos pela inversão B2B e dois são
novos.

**P-001 — API-first e B2B-first.** *(novo)* O produto deve ser utilizável sem nenhum
frontend Banvia.

**P-002 — Provider-neutral.** Nenhum exit venue é favorecido por acoplamento técnico ou
comissão. Mantém RN-008 e RN-RNK-002.

**P-003 — Best executable route.** "Melhor" significa melhor alternativa entre venues
**efetivamente integrados, contratados, disponíveis e elegíveis** para aquele contexto.
Mantém RN-006/RN-007.

**P-004 — Non-custodial e sem saldo por padrão.** A Banvia não mantém saldo do cliente
nem controla chaves privadas no MVP. Reescreve RN-001 para o contexto B2B.

**P-005 — Stablecoin-first.** *(reescrito)* O MVP prioriza USDC e USDT. Cauda longa e
memecoins deixam de ser requisito inicial.

**P-006 — PIX-first, não PIX-only.** *(novo)* O PIX é o primeiro rail local. O domínio
deve permitir outros rails sem reescrita.

**P-007 — Evidence over inference.** Estados financeiros só avançam com evidência
reproduzível. Mantém RN-SD-001 e RN-SD-003.

**P-008 — Idempotency everywhere.** Toda operação com efeito externo tem chave idempotente
ou estratégia equivalente. Mantém RN-SD-007 a RN-SD-011.

**P-009 — Reconciliation is product logic.** Reconciliação é fluxo, não tarefa manual de
fim de mês. Mantém RN-014 e RN-SD-031.

**P-010 — No public SLA without production data.** Latência medida em P50/P95/P99 antes
de qualquer promessa comercial. Mantém a Seção 23.5 da v3.3.

**P-011 — Regulatory boundary is explicit.** Cada fluxo registra quem executa custódia,
conversão, KYC/KYT/AML, câmbio quando aplicável e liquidação PIX. Mantém RN-013 e
RF-CMP-017.

**P-012 — Data becomes routing intelligence.** Decisões e resultados realizados alimentam
métricas por venue, ativo, rede, ticket e contexto. É a leitura direta do que a Onramper
declara como diferencial.

**P-013 — Complexity belongs to Banvia.** Mantido da v3.3, agora com o cliente sendo uma
empresa: a plataforma absorve a complexidade acidental sem esconder risco material.

---

## 5. Escopo do MVP

### 5.1 Fluxo mínimo

```text
Empresa cliente
     ↓
POST /v1/quotes
     ↓
Banvia consulta ≥ 2 exit venues
     ↓
normaliza preço, fees, TTL, limites e ETA
     ↓
Route Engine seleciona/recomenda
     ↓
POST /v1/payouts   (Idempotency-Key obrigatório)
     ↓
venue executa conversão + PIX
     ↓
webhooks / status polling
     ↓
Banvia normaliza lifecycle
     ↓
reconciliação + settlement evidence
```

A unidade de valor do MVP é **um payout B2B**.

### 5.2 Ativos e redes iniciais

Prioridade de discovery e contratação:

- **USDC** e **USDT** como ativos;
- **Solana**, **Base** e **Polygon** como redes candidatas.

A lista final é determinada por contratos reais, liquidez, custo de rede, cobertura dos
venues e demanda dos design partners — não por preferência técnica.

### 5.3 Fora do MVP

Herda a Seção 28 da v3.3 e acrescenta o que a inversão B2B remove:

- portfolio discovery de wallet;
- conexão Phantom/Solflare/Backpack como jornada principal;
- Jupiter/DEX routing;
- memecoins e cauda longa;
- Banvia ID / KYC reutilizável *(vira Fase 3)*;
- cartão, conta virtual, on-ramp BRL → cripto;
- ACH/SEPA/SPEI *(viram Fase 5)*;
- custódia própria, liquidity desk próprio, emissão de stablecoin;
- route splitting entre venues;
- IA/ML no ranking antes de dataset suficiente.

---

## 6. Domínio

Agregados iniciais. Onde há correspondência com a v3.3, ela está indicada.

| Entidade | Origem |
|---|---|
| `Tenant` | novo — substitui `Customer` como cliente primário |
| `ApiCredential` | novo |
| `CustomerReference` | reescreve `Customer` como referência do usuário final do tenant |
| `Beneficiary` | novo — substitui a conta PIX do próprio usuário |
| `QuoteRequest` | v3.3 |
| `VenueQuote` | reescreve `Quote` externa |
| `NormalizedQuote` | v3.3, RF-RTE-005 |
| `RouteCandidate` | v3.3 |
| `RouteGraph` / `RouteLeg` | v3.3 — permanece, com menos legs no MVP |
| `RouteDecision` | reescreve `RankingDecision` |
| `Payout` | reescreve `Order` |
| `VenueExecution` | reescreve `ExitExecution` |
| `SettlementEvidence` | reescreve `PixSettlement`, generalizando o rail |
| `WebhookEvent` | v3.3 |
| `RiskDecision` | v3.3 |
| `ComplianceReference` | v3.3 |
| `FeeRecord` | v3.3 |
| `ReconciliationCase` | v3.3 |
| `AuditEvent` | v3.3 |
| `ExitVenue` / `VenueCapability` | v3.3, RF-EXT-001 |
| `VenueHealthSnapshot` | v3.3, RF-EXT-010 |

O **Route Graph permanece** mesmo com rotas mais curtas: RF-GPH-004 já previa que rotas
diretas usam o mesmo modelo com menos legs. Isso evita reescrita quando a Fase 5 trouxer
DEX de volta.

### 6.1 Payout state machine

Deriva da Seção 15 da v3.3, removendo os estados de wallet e swap:

```text
CREATED
  ↓
QUOTED
  ↓
ROUTE_SELECTED
  ↓
SUBMITTED
  ↓
VENUE_PROCESSING
  ↓
SETTLEMENT_PENDING
  ↓
COMPLETED
```

Estados laterais:

```text
REQUIRES_ACTION
RISK_REVIEW
FAILED
EXPIRED
CANCELED
UNKNOWN
RECONCILIATION_REQUIRED
```

**RN-STATE-001.** `COMPLETED` exige **evidência de liquidação**, não apenas webhook de
sucesso. Mantém RN-SD-003.

**RN-STATE-002.** Timeout após operação não-idempotente produz `UNKNOWN` até
reconciliação. Mantém RN-SD-004/005/006.

**RN-STATE-003.** Evento atrasado não pode regredir estado irreversível sem regra
explícita. Mantém RN-SD-023.

---

## 7. Exit Venue abstraction

O contrato conceitual da v3.3 (Seção 12) permanece, com nomes ajustados ao vocabulário
B2B:

```java
public interface ExitVenueProvider {
    VenueCapabilities capabilities();
    VenueQuote        quote(PayoutQuoteRequest request);
    VenueExecution    createPayout(CreatePayoutRequest request);
    VenueExecution    getPayout(String externalId);
    WebhookResult     parseWebhook(String payload, Map<String, String> headers);
    VenueHealth       health();
}
```

**RN-ADP-001.** DTO/SDK externo não entra no domínio. Mantém RN-010 e RN-SD-037.

**RN-ADP-002.** Cada venue possui capability matrix versionada. Mantém RF-EXT-001.

**RN-ADP-003.** Mudança de payload/webhook exige contract test. Mantém RN-SD-038.

**RN-ADP-004.** Evento desconhecido é persistido para análise; nunca tratado
silenciosamente como sucesso. Mantém RN-SD-039.

**RN-ADP-005.** Foxbit Infra, BRL0, Transfero e Woovi são **candidatos**, não
capabilities. Um venue só existe operacionalmente após contrato, credenciais e validação
técnica. Mantém RF-EXT-002/003/004.

---

## 8. Quote API

### 8.1 Endpoint

`POST /v1/quotes`

```json
{
  "source": {
    "asset": "USDC",
    "network": "BASE",
    "amount": "200.00"
  },
  "destination": {
    "currency": "BRL",
    "rail": "PIX"
  },
  "beneficiary": {
    "country": "BR"
  }
}
```

Resposta normalizada:

```json
{
  "quoteId": "qt_01J8FKX",
  "expiresAt": "2026-08-25T18:00:30Z",
  "recommendedRoute": {
    "routeId": "rt_01J8FKY",
    "venue": "venue_b",
    "estimatedPayout": "1084.22",
    "currency": "BRL",
    "estimatedSettlementSeconds": 8,
    "fees": { "venue": "2.20", "banvia": "1.50" },
    "reason": "maior probabilidade de conclusão com liquidação em 8s"
  },
  "alternatives": [
    { "routeId": "rt_01J8FKZ", "venue": "venue_a", "estimatedPayout": "1084.90",
      "estimatedSettlementSeconds": 45, "label": "MAX_NET_PAYOUT" }
  ]
}
```

### 8.2 Normalização

Todo venue é convertido para campos comparáveis. Mantém RF-RTE-005 e RN-015 — **não
comparar o incomparável**:

input amount · estimated output · venue fees · network fees quando conhecidas · fee
Banvia · custo all-in · TTL · min/max ticket · settlement estimate · requisito de
KYC/KYB · requisitos de beneficiário · ativo e rede de origem · disponibilidade ·
histórico de sucesso · nível de confiança da estimativa.

---

## 9. Route Engine

### 9.1 Score

Reescreve RF-RTE-013 com a formulação da tese:

```text
RouteScore =
    priceScore        * W_price
  + successRateScore  * W_success
  + settlementScore   * W_speed
  + availabilityScore * W_health
  + liquidityScore    * W_liquidity
  - riskPenalty
  - frictionPenalty
```

Pesos iniciais sugeridos — `0.30 / 0.25 / 0.15 / 0.10 / 0.10`, com risco a `0.10` — são
**ponto de partida, não constante universal**. Devem ser versionados por `RoutingPolicy`
(RN-RNK-001) e reproduzíveis (RN-RNK-004).

> **O mais barato pode não ser o melhor.** Um venue que paga R$ 1.007 com 81% de sucesso
> e 40s de liquidação é pior que outro pagando R$ 1.000 com 99% e 7s. É essa decisão que
> a Banvia vende.

### 9.2 Dados que viram moat

Coletar por venue: latência de quote · quote win rate · payout acceptance rate · success
rate · P50/P95/P99 de settlement · timeout rate · unknown-result rate · reconciliation
mismatch rate · desvio entre quote e realizado · disponibilidade por janela · performance
por ativo, rede e ticket.

```text
1.000 transações   → success rate
10.000 transações  → success rate por venue, horário, valor, rede e banco
100.000 transações → modelo de routing próprio
```

Uma empresa nova copia endpoints. Não copia histórico operacional.

### 9.3 Fallback

**RN-RT-001.** Troca automática de venue somente **antes do ponto irreversível**. Mantém
RN-009 e RN-SD-011.

**RN-RT-002.** Após funding/commit externo, fallback exige semântica formal de
recuperação. Nunca "tentar outro" cegamente.

**RN-RT-003.** Circuit breaker de um venue não pode afetar os demais. Mantém RN-SD-029/030.

---

## 10. Payout API

`POST /v1/payouts`

Headers: `Authorization` · `Idempotency-Key` **obrigatório** · `X-Banvia-Tenant`

```json
{
  "quoteId": "qt_01J8FKX",
  "beneficiary": {
    "name": "...",
    "document": { "type": "CPF", "value": "..." },
    "pix": { "keyType": "CPF", "key": "..." }
  },
  "metadata": { "externalReference": "invoice_123" }
}
```

**RN-IDM-001.** Mesmo tenant + mesma chave + payload equivalente retorna o mesmo payout.
Mantém RN-SD-008.

**RN-IDM-002.** Mesma chave com payload conflitante retorna erro determinístico. Mantém
RN-SD-007.

**RN-IDM-003.** Idempotência persiste além de restart/deploy. Mantém RN-SD-016.

---

## 11. Webhooks para o tenant

```text
payout.created            payout.route_selected     payout.submitted
payout.processing         payout.requires_action    payout.settlement_pending
payout.completed          payout.failed             payout.reconciliation_required
```

Assinatura HMAC · timestamp anti-replay · retry com backoff · event ID idempotente ·
**ordering não pode ser pressuposto** (RN-SD-023) · portal de replay · retention definida.

---

## 12. Reconciliação

`Payout ↔ VenueExecution ↔ BlockchainEvidence ↔ VenueSettlement ↔ PixEvidence`

Mantém integralmente RF-REC-001 a RF-REC-005 e RN-SD-031 a RN-SD-033.

---

## 13. Journal de transações imutável

**RF-LED-001.** A Banvia é 100% não-custodial e possui **immutable transaction journal** (append-only) como fonte de verdade probatória.

**RF-LED-002.** A Banvia não mantém saldos de clientes, custódia de fundos, prefunding nem tesouraria própria. O modelo financeiro do sistema é estritamente o Transaction Journal probatório por payout.

**RF-LED-003.** Fee Banvia e revenue share conciliáveis desde o início. Mantém RN-025 e
RN-ECO-005.

---

## 14. Monetização

Reescreve a Seção 13.1 da v3.3. A microfee B2C dá lugar a um modelo de infraestrutura:

1. **Platform fee / SaaS** — acesso à API, dashboard, ambientes, SLA e suporte;
2. **Usage fee** — bps ou valor fixo por payout;
3. **Revenue share do venue** — quando permitido e contratualmente transparente;
4. **Enterprise** — white-label, routing policy customizada, relatórios e limites maiores.

**Não depender de spread oculto.** Mantém RN-ECO-001 e RN-RNK-002.

O conceito de `UserNetOutcome` da v3.3 permanece útil, agora como **`TenantNetOutcome`**:
o cliente precisa conseguir demonstrar que a rota Banvia entrega mais do que a integração
direta com um único venue, depois da nossa fee.

---

## 15. Compliance e fronteira regulatória

Desde **2 de fevereiro de 2026**, a Resolução BCB nº 520/2025 disciplina a atuação das
Sociedades Prestadoras de Serviços de Ativos Virtuais, e a nº 519/2025 trata do processo
de autorização. Em julho de 2026 o BC classificou as SPSAVs como instituições Tipo 3 para
fins prudenciais, com parte do regime aplicável a partir de 2027.

Matriz obrigatória por fluxo: customer of record · quem executa KYC/KYB · quem executa
KYT/AML · quem recebe e custodia o ativo · quem converte · quem opera câmbio quando
aplicável · quem inicia e liquida o PIX · quem produz Travel Rule · quem atende
chargeback/dispute/refund · quem mantém evidências e por quanto tempo.

**Gate de produção:** parecer jurídico sobre o fluxo real e os contratos reais — não
sobre o nome "orquestrador". Mantém RF-CMP-018.

---

## 16. Observabilidade

Contexto obrigatório: `tenantId` · `payoutId` · `quoteId` · `routeId` · `venue` ·
`externalExecutionId` · `correlationId` · `pixE2E` quando permitido · `txHash/signature`
quando aplicável. Mantém RN-SD-034.

**Routing:** `QuoteLatencyP50/P95/P99` · `VenueQuoteAvailability` · `VenueWinRate` ·
`RouteGenerationLatency` · `RouteFallbackRateBeforeCommit`

**Payout:** `PayoutSuccessRate` · `PayoutAcceptanceRate` · `SettlementP50/P95/P99` ·
`UnknownOutcomeRate` · `ReconciliationMismatchRate` · `AutoReconciliationRate`

**Economia:** `GMV` · `NetTakeRate` · `ContributionMarginPerPayout` ·
`RevenueShareByVenue` · `AverageAllInCost`

### North Star

> **GMV liquidado em BRL via PIX pela rota recomendada, reconciliado, dentro do SLA
> contratado e sem incidente material de segurança ou compliance.**

---

## 17. Multi-tenancy

Isolamento lógico forte · credenciais por tenant · webhook endpoint por tenant · routing
policy configurável · fee policy por contrato · quota e limites por tenant · audit trail
por tenant. **Dados de um tenant não podem vazar para outro**; analytics agregado só pode
usar dados de forma compatível com contrato e privacidade.

---

## 18. Arquitetura

Mantém integralmente RN-SD-040/041/042 da v3.3:

```text
                API Gateway / Auth
                       │
          ┌────────────┴────────────┐
      Quote API                 Payout API
          └────────────┬────────────┘
                Orchestration Core
          ┌────────────┼────────────┐
     Route Engine  Risk/Policy   Reconciliation
          │            │            │
     Exit Venue Adapter Boundary     │
      ┌──────┼────────┼──────┐       │
      V1     V2       V3     Vn      │
      └──────┴────────┴──────┘       │
                       └── PIX settlement
```

Java 25 · Spring Boot · Spring Modulith · PostgreSQL · outbox/inbox transacional ·
workers controlados · OpenTelemetry. Redis só com necessidade concreta.
**Kafka, Kubernetes e microservices não são requisito do MVP.**

> **Regra de engenharia, mantida da v3.3:** a sofisticação inicial deve estar na semântica
> de correção — idempotência, estados, evidência, recuperação e reconciliação — e não na
> quantidade de serviços implantados.

### 18.1 Módulos

```text
com.banvia
  ├── tenant          ├── auth           ├── quote
  ├── routing         ├── payout         ├── provider
  ├── webhook         ├── settlement     ├── reconciliation
  ├── journal         ├── risk           ├── compliance
  ├── billing         ├── audit          └── shared
```

Wallet, DEX, asset discovery e cauda longa podem existir futuramente em módulos separados
**sem contaminar o core de payout**.

---

## 19. Critérios de aceite do MVP

Reescreve as Seções 25 e 26 da v3.3 para o contexto B2B:

1. dois exit venues reais contratados e integrados;
2. ambos cotam ao menos um cenário USDC/USDT → BRL/PIX comparável;
3. quotes são normalizadas;
4. route decision é **reproduzível** (RN-RNK-004);
5. criação de payout é idempotente;
6. timeout, duplicata e webhook fora de ordem possuem teste;
7. venue A indisponível não derruba venue B;
8. lifecycle normalizado exposto por API e webhook;
9. `COMPLETED` possui settlement evidence;
10. reconciliação detecta divergências;
11. cliente-piloto integra **sem SDK do venue**;
12. fee e revenue share são contabilmente conciliáveis;
13. backoffice permite investigação por payout;
14. responsabilidades regulatórias aprovadas juridicamente;
15. SLOs internos medidos **antes** de SLA público.

O **checklist de System Design da Seção 35 da v3.3 permanece obrigatório** — as 15
perguntas continuam valendo palavra por palavra, trocando `Order` por `Payout`.

---

## 20. Roadmap

O paralelo com a Onramper é deliberado:

| | Onramper | Banvia |
|---|---|---|
| início | onramps | **Crypto ↔ PIX** |
| ↓ | aggregation | aggregation |
| ↓ | routing | routing |
| 2026 | Onramper ID | **Banvia ID** |
| ↓ | stablecoin APIs | stablecoin APIs |
| ↓ | virtual accounts | global payment rails |
| destino | Financial OS | **Stablecoin Payment OS** |

**Fase 0 — Viabilidade comercial e jurídica.** 3–5 venues candidatos, contratos, KYB,
origem dos fundos, ativos/redes aceitos, parecer jurídico, 10–20 entrevistas B2B, 2–3
design partners. As 24 perguntas da Seção 29 da v3.3 continuam sendo o roteiro.

**Fase 1 — Banvia Ramp Sandbox.** API keys e tenants, Quote API, Payout API simulada,
webhook simulator, SPI de adapter, state machine, idempotência, dashboard mínimo.

**Fase 2 — Primeiro venue real.** USDC/USDT → PIX, ambiente controlado, settlement
evidence, reconciliação real, limites conservadores.

**Fase 3 — Orquestração real.** Segundo venue, quote normalization, routing policy,
health e circuit breaker, fallback pré-commit, métricas comparativas.

**Fase 4 — Pilotos B2B.** 2–3 clientes, SLA privado, billing, runbooks, post-mortems, DX.

**Fase 5 — Data-driven routing.** Score baseado em histórico real, segmentação por
ativo/rede/ticket, modelos de performance por venue.

**Fase 6 — Banvia ID.** KYC reutilizável entre venues. **Só existe com base contratual,
técnica, jurídica e de privacidade** — mantém RN-030 e RF-CMP-006. O KYC Graph da Seção
14.3 da v3.3 é o desenho de referência.

**Fase 7 — Novos rails e wallet/consumer.** BRL → cripto quando justificável, outros
rails LATAM, e então hosted checkout, wallet SDK, wallet-to-PIX, DEX routing e cauda longa
sob eligibility/risk policy — retomando as Seções 6 a 11 da v3.3.

**Trilha de implantação.** A ida para a AWS corre em paralelo a estas fases, sem
alterar o escopo de nenhuma, e está em [`evolucao-aws.md`](evolucao-aws.md)
([ADR 0008](adr/0008-aws-como-alvo-de-implantacao.md)). A restrição da §18 vale para
ela: Kubernetes é laboratório, e Redis só entra com necessidade concreta.

---

## 21. Definição operacional da tese

A Banvia cumpre sua proposta quando uma empresa expressa uma intenção simples:

> **"Entregue R$ X via PIX a este beneficiário usando esta cripto como origem."**

E a plataforma responde, com segurança e evidência: quais rotas estão disponíveis · quanto
o beneficiário recebe · quanto custa · qual venue foi escolhido e por quê · qual o estado
atual · se houve liquidação · como recuperar uma divergência.

```text
EMPRESA
   ↓
BANVIA API
   ↓
MELHOR ROTA INTEGRADA E EXECUTÁVEL
   ↓
PIX
```

**A Banvia não precisa ser o rail. Precisa ser a melhor camada para escolher, operar e
observar os rails.**

O PIX é a porta de entrada, não o produto final. A pergunta que abre a empresa é *"como
qualquer empresa do mundo transforma cripto em dinheiro brasileiro via uma única API?"*.
A pergunta seguinte — *"por que apenas PIX?"* — é o que a torna grande.

---

## 22. Referências

- BANCO CENTRAL DO BRASIL. Resoluções BCB nº 519/2025 e nº 520/2025, vigentes desde
  02/02/2026, e Resolução nº 580 sobre classificação prudencial das SPSAVs.
- BLOCKSTER. *From Onramps to Identity: Why Onramper's New Co-CEOs Are Thinking Bigger
  Than Aggregation*. Entrevista com Rick Thomas e Maurits Dewina, 15 jun. 2026.
- ONRAMPER. Documentação pública da API — separação entre assets, payment methods,
  providers e quotes.
- KLEPPMANN, Martin; RICCOMINI, Chris. *Designing Data-Intensive Applications*. 2ª ed.
  O'Reilly, 2026. Guia de estudo da Seção 36 da v3.3 permanece válido.
- Documentação pública de venues candidatos é **discovery**; capability real exige
  contrato, sandbox e validação jurídica e técnica.
- BANVIA. *Crypto ↔ PIX Orchestration Infrastructure* — monografia v4.0, 45 p.,
  ago. 2026. Leitura estendida deste documento.
- BANVIA. *Estratégia de produto* v0.3, ago. 2026. Cenário competitivo, moat e
  spikes de validação.

---

## 23. Seções da v3.3 que permanecem vigentes sem alteração

Para evitar duplicação, estas seções da [v3.3](requisitos-banvia-ramp-v3.3.md) continuam
canônicas e devem ser lidas junto deste documento:

| Seção v3.3 | Assunto |
|---|---|
| 1.3 | convenção terminológica e regra editorial |
| 9 | Route Graph e RouteLeg |
| 13 | governança do ranking |
| 19 | reconciliação |
| 21 | backoffice |
| 23.1 – 23.6 | segurança, idempotência, resiliência, observabilidade, performance |
| **23.7** | **System Design e correção distribuída — RN-SD-001 a RN-SD-042** |
| 29 | perguntas comerciais obrigatórias aos venues |
| 32 | gates de produção |
| 35 | checklist de System Design e Definition of Done financeira |
| 36 | guia de estudo técnico |
| 37 | glossário |

As Seções 6, 7, 8, 11 e 14 da v3.3 — wallet, portfolio discovery, eligibility, DEX e KYC
Graph — permanecem como **desenho de referência para as Fases 6 e 7**, não como requisito
de MVP.
