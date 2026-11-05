# Estratégia de produto — Banvia

**Versão:** 0.4
**Data:** 27/08/2026
**Status:** tese de produto vigente — escopo do MVP em
[`requisitos-banvia-mvp-v1.md`](requisitos-banvia-mvp-v1.md), arquitetura-alvo na
[v4.0](requisitos-banvia-v4.0.md)
**Substitui:** v0.3 (26/08/2026), que tratava o Route Engine como núcleo do MVP

---

> **O que mudou da v0.3 para esta versão**
>
> A v0.3 colocava o Route Engine no centro do MVP e pedia para validar que a
> seleção de rota entrega mais que a integração direta. Essa hipótese exige
> volume e histórico que o MVP não tem — e a própria §8.2 já registrava que,
> com um venue, roteamento é adapter.
>
> A [ADR 0007](adr/0007-reducao-de-escopo-do-mvp.md) troca a hipótese do MVP por
> uma anterior e mais barata: **a abstração, não a otimização**. Routing por
> score vira V3.
>
> O que **não** mudou: não construir infraestrutura regulada, a neutralidade
> entre providers e a disciplina de correção financeira.

---

## 1. A tese em uma frase

> Uma API para empresas enviarem stablecoin e realizarem payouts via PIX no
> Brasil. Você integra uma vez; nós lidamos com os provedores por baixo.

O cliente é uma **empresa**, não uma pessoa. A interface primária é uma **API**,
não uma tela.

O produto não é converter USDC em reais — providers já fazem isso. O produto é
**oferecer uma interface estável enquanto os provedores por baixo podem mudar**.

A formulação anterior — *"escolhendo e operando o melhor exit venue disponível
para cada contexto"* — descreve o destino, e volta a valer a partir da V3.

---

## 2. Por que a inversão

Duas leituras a sustentam.

**Agregação pura está virando commodity.** Ter N integrações deixou de ser
argumento quando qualquer entrante consegue as mesmas N. O que permanece escasso
é saber qual usar em cada contexto — e isso depende de histórico de execução,
não de catálogo. A própria liderança da Onramper reconhece isso publicamente:
em entrevista de 15/06/2026, os co-CEOs descrevem o diferencial declarado como
qualidade da escolha, não quantidade de providers.

**Empresa e usuário têm curvas de receita diferentes.** Um usuário
self-custody traz uma transação. Uma wallet, uma fintech ou uma plataforma de
payroll traz fluxo recorrente, contrato, previsibilidade e integração profunda.
Para um produto que vive de confiabilidade operacional, a diferença é
estrutural.

---

## 3. Cenário competitivo

> ⚠️ **Levantamento de 24/08/2026, não verificado de forma independente.**
> Os dados vieram de material público. Capacidades de produto mudam rápido.
> Revalidar antes de qualquer decisão de contrato ou arquitetura irreversível.

| Capacidade | BRL0 | Foxbit Infra | Banvia |
|---|---|---|---|
| Cripto → PIX | ✅ | ✅ | ✅ foco |
| API B2B | ✅ | ✅ | ✅ |
| Webhooks | ✅ | ✅ | ✅ normalizados |
| White-label | ✅ | ✅ | ✅ |
| KYC/KYT/AML | via parceiros | ✅ forte | **via venue** |
| Liquidez própria/agregada | não documentado | ✅ | **via venue** |
| Custódia institucional | via parceiros | ✅ | não |
| Lifecycle único sobre N providers | ❌ | ❌ | **✅ no MVP** |
| Troca de provider sem o cliente reintegrar | ❌ | ❌ | **✅ no MVP** |
| Roteamento **entre** off-ramps | ❌ | roteia liquidez interna | V3 |
| Quote normalization multi-venue | ❌ | ❌ | V3 |
| Reconciliação multi-venue | — | interna | V3 |

As duas primeiras linhas são o que o MVP entrega e o que precisa ser verdadeiro
no dia 1. As três seguintes são o destino — não devem entrar em material
comercial antes de existirem.

### A leitura

**BRL0 e Foxbit Infra ocupam a camada que decidimos não construir.** Recriá-la
custaria meses para chegar, na melhor hipótese, empatado — sem licença, sem
liquidez e sem histórico.

**Onramper deixa de ser lida como concorrente e passa a ser o precedente mais
informativo.** A trajetória dela — agregação → roteamento → identidade →
infraestrutura — é o roteiro que a v4.0 §20 adota deliberadamente.

---

## 4. A inversão que sustenta a posição

> **BRL0, Foxbit, Transfero e Woovi não são concorrentes. São venues candidatos.**

Não competimos com a infraestrutura deles. Nós a consumimos, e competimos na
decisão de **qual usar, quando e por quê**:

```
Venue A    paga R$ 1.007   sucesso 81%   liquida em 40s
Venue B    paga R$ 1.000   sucesso 99%   liquida em  7s
Venue C    —                              indisponível
```

O motor escolhe B. Nenhum dos dois primeiros faria essa comparação, porque
**um provider não tem incentivo para mandar volume ao concorrente**.

Esse é o ponto defensável: a posição de roteador neutro só é ocupável por quem
**não é** venue.

---

## 5. A camada que é nossa

### No MVP

```
     Empresa cliente (wallet · fintech · payroll · marketplace)
                          │
                          ▼
                     API Banvia            ← 4 endpoints, sem escolher provider
                          │
                 ┌────────┴────────┐
                 ▼                 ▼
             Provider A        Provider B   ← disponibilidade decide
                 └────────┬────────┘
                          ▼
                         PIX
```

Quatro capacidades, nenhuma oferecida pelos providers:

1. **Interface estável** — o cliente integra uma vez; trocar de provider por
   baixo não muda uma linha do código dele.
2. **Lifecycle normalizado** — um vocabulário de estados sobre N providers, cada
   um com o seu.
3. **Idempotência de ponta a ponta** — chave obrigatória que sobrevive a restart
   e deploy.
4. **Evidência** — `SETTLED` exige o E2E do PIX, não webhook de sucesso.

### A partir da V3

Entram quote normalization multi-venue, `RouteScore` com política versionada e
reconciliação em cinco fontes. O desenho está na
[v4.0](requisitos-banvia-v4.0.md) e não deve ser antecipado no discurso.

---

## 6. Onde o moat se forma

> **No MVP, não há moat — e isso é aceitável.** A primeira versão testa se alguém
> quer a abstração. O acervo abaixo é o que se forma **depois**, com volume, e é
> o que sustenta a V3 em diante.

Não são os endpoints. Qualquer equipe competente reproduz a API em semanas.

```
1.000 payouts    → taxa de sucesso confiável
10.000 payouts   → sucesso por venue, horário, valor, rede e banco
100.000 payouts  → modelo de routing próprio
```

Uma empresa nova copia endpoint. Não copia histórico operacional.

**Implicação desconfortável:** no dia 1 não temos moat. Temos uma tese sobre
onde ele se forma, uma arquitetura que permite formá-lo e a disciplina para que
o volume acumulado seja confiável em vez de apenas grande.

---

## 7. Monetização

**No MVP**, o modelo é uma linha só: fee por payout, explícita na cotação. O
exemplo de referência é `0,60%` do provider + `0,15%` da Banvia.

Spread é possível, e carrega um custo diferido: a partir da V3, quando o routing
passar a escolher por preço, o cliente vai perguntar como sabemos que escolhemos
a melhor rota — e spread cobrado hoje enfraquece essa resposta amanhã.

As quatro linhas abaixo são o destino, detalhadas em
[v4.0 §14](requisitos-banvia-v4.0.md):

1. **Platform fee** — acesso a API, dashboard, ambientes, suporte e SLA.
2. **Usage fee** — bps ou valor fixo por payout, visível como linha própria.
3. **Revenue share** — quando contratualmente permitido, divulgado e governado
   **em separado do ranking**.
4. **Enterprise** — white-label, routing policy própria, relatórios, limites.

**Sem spread oculto.** A regra é estrutural, não só ética: um modelo que depende
de margem embutida no câmbio cria incentivo para favorecer o venue que permite
maior margem — o que destrói a neutralidade da seção 4, que é a única razão pela
qual temos direito de existir entre o cliente e os venues.

---

## 8. Riscos da tese

### 8.1 O SLA depende de terceiro

Controlamos decisão de rota e orquestração. **Não controlamos a liquidação PIX**,
que é do venue. Nenhuma promessa pública de tempo antes de medir P50/P95/P99 em
produção.

### 8.2 Roteamento com um venue é só um adapter — *resolvido pela ADR 0007*

Este era o risco central da v0.3, e a resposta foi mudar a hipótese: o MVP não
vende melhor execução, vende abstração. Dois providers provam desacoplamento sem
precisar de otimização.

O risco permanece, deslocado: **a partir da V3**, quando o discurso voltar a ser
seleção de rota, ele precisa de N ≥ 2 venues ativos e de histórico real. Não
contar essa história antes disso.

### 8.3 O compliance do venue limita o produto

Quem carrega a licença é o venue. Se ele recusar a origem dos fundos que
pretendemos processar, o produto muda no contrato, não na engenharia.
**Perguntar antes de construir.**

### 8.4 O fosso é fino se formos devagar

Se a tese é qualidade de seleção sustentada por dados, e os dados ainda não
existem, nada impede que um venue com escala construa a mesma camada. A defesa
não é tecnológica — é a neutralidade, e ela só se consolida com multi-venue real
e volume.

### 8.5 A cauda longa saiu do MVP, os riscos dela não sumiram

Slippage, tokens falsos, liquidez insuficiente, manipulação de preço e MEV
continuam existindo. Eles apenas deixaram de ser urgentes. Quando a Fase 7
trouxer DEX de volta, cada um precisa de resposta explícita — o desenho de
referência está na v3.3, seções 6 a 11.

---

## 9. Como derrubar a tese rápido

Quatro spikes que custam dias e podem economizar meses. Nenhum exige o backend
construído. Em ordem de poder destrutivo:

**Spike 0 — latência real de liquidação.**
Medir, em sandbox dos venues candidatos, o tempo entre solicitação de payout e
liquidação confirmada. Todo discurso de velocidade depende desse número.

**Spike 1 — apetite de compliance.**
Perguntar diretamente qual origem de fundos cada venue aceita. Um "não"
reescreve o produto.

**Spike 2 — viabilidade de dois venues.**
Confirmar que dá para contratar dois em paralelo, em prazo e custo razoáveis.
Sem isso, o Route Engine é arquitetura sem função.

**Spike 3 — economia da rota DEX.** *(só quando a Fase 7 se aproximar)*
Medir price impact real para os tickets pretendidos.

---

## 10. Métricas

**No MVP**, a métrica é uma só:

> **TPV — volume processado através da Banvia.**

Complementares: `payout success rate` · tempo médio de liquidação ·
`provider failure rate` · volume por provider · clientes B2B ativos · e a taxa de
payouts que pararam em `UNKNOWN`, que mede a saúde da integração.

**Critério de sucesso do MVP:** 3 a 5 clientes B2B fazendo payouts reais pela
mesma API, com pelo menos 2 providers.

A partir da V3, a North Star da v4.0 §16 volta a valer:

> GMV liquidado em BRL via PIX pela rota recomendada, reconciliado, dentro do
> SLA contratado e sem incidente material de segurança ou compliance.

Cada qualificador dessa frase exclui uma forma de enganar a si mesmo com um
número bonito.

**A métrica que mede o valor que só nós entregamos:**

> **`TenantNetOutcome`** — quanto o cliente recebeu a mais por termos escolhido
> o venue, comparado à integração direta com um venue único de referência,
> **já descontada a nossa fee**.

Se esse número for consistentemente próximo de zero, o Route Engine não está
gerando valor e a tese precisa se apoiar em confiabilidade e redução de
integração — que são valores reais, mas menores.

Complementares: `PayoutSuccessRate` · `SettlementP50/P95/P99` ·
`UnknownOutcomeRate` · `ReconciliationMismatchRate` · `VenueWinRate` ·
disponibilidade efetiva (% de payouts com ≥2 venues disponíveis).

---

## 11. Roadmap

| Versão | Conteúdo |
|---|---|
| **MVP** | USDC → PIX, 2 providers, routing por disponibilidade, 4 endpoints |
| **V2** | mais providers, health checks, failover automático |
| **V3** | routing por preço, liquidez e success rate — a v4.0 começa aqui |
| **V4** | políticas customizadas por cliente |
| **V5** | marketplace / orchestration network |

Antes do MVP, a Fase 0 permanece: parecer jurídico, apetite de compliance dos
providers e viabilidade de contratar dois em paralelo. Nenhuma delas exige código.

O smart routing é **consequência do crescimento, não premissa do produto**.

---

## 12. Decisões registradas

- [ADR 0005 — Off-ramp Router provider-first](adr/0005-offramp-router-provider-first.md)
  *(superada em contexto pela 0006; a decisão de não construir infraestrutura
  regulada permanece válida)*
- [ADR 0006 — Inversão B2B e API-first](adr/0006-inversao-b2b-api-first.md)
- [ADR 0007 — Redução de escopo do MVP](adr/0007-reducao-de-escopo-do-mvp.md)

ADRs 0001 a 0004 (monólito modular, journal imutável, adapter boundary, venue
regulado) permanecem a escrever — as decisões já foram tomadas, falta registrá-las.

---

## 13. Documento longo

A leitura estendida desta tese, em formato de monografia, está em
[`Banvia_Monografia_ABNT_Orquestracao_Crypto_PIX_v4.0.pdf`](Banvia_Monografia_ABNT_Orquestracao_Crypto_PIX_v4.0.pdf).
