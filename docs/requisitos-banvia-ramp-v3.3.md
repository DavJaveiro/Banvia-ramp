# Banvia Ramp — Requisitos do Sistema v3.3

> ## ⚠️ Status — substituído em prioridade de produto
>
> **Escopo vigente:** [`requisitos-banvia-mvp-v1.md`](requisitos-banvia-mvp-v1.md).
> **Arquitetura-alvo:** [`requisitos-banvia-v4.0.md`](requisitos-banvia-v4.0.md).
>
> A v4.0 inverteu o cliente primário de **usuário self-custody** para **empresa
> que integra a API** — ver [ADR 0006](adr/0006-inversao-b2b-api-first.md).
> Em consequência, ficaram **fora do MVP** as seções deste documento que tratam
> de portfolio discovery de wallet, conexão Phantom/Solflare/Backpack como
> jornada principal, DEX routing via Jupiter, memecoins e cauda longa. As
> seções 6, 7, 8, 11 e 14 permanecem como **desenho de referência para as Fases
> 6 e 7**, não como requisito atual.
>
> **O que continua canônico aqui**, e deve ser lido junto da v4.0 (ver §23 dela):
>
> | Seção | Assunto |
> |---|---|
> | 1.3 | convenção terminológica e regra editorial |
> | 9 | Route Graph e RouteLeg |
> | 13 | governança do ranking |
> | 19 | reconciliação |
> | 21 | backoffice |
> | 23.1 – 23.6 | segurança, idempotência, resiliência, observabilidade, performance |
> | **23.7** | **System Design e correção distribuída — RN-SD-001 a RN-SD-042** |
> | 29 | perguntas comerciais obrigatórias aos venues |
> | 32 | gates de produção |
> | 35 | checklist de System Design e Definition of Done financeira |
> | 36 | guia de estudo técnico |
> | 37 | glossário |
>
> Ao ler as seções acima, troque `Order` por `Payout` e `Customer` por
> `Tenant` + `Beneficiary`.

## 1. Identidade do produto

**Produto:** Banvia Ramp  
**Plataforma/marca:** Banvia  
**Categoria:** **Web3 Off-Ramp Orchestration Platform**  
**Posicionamento funcional:** **Wallet-to-PIX orchestration para o Brasil**

Banvia Ramp é o produto inicial da plataforma Banvia. O produto não deve ser modelado como exchange, custodiante, DEX, instituição de pagamento ou off-ramp próprio. Seu papel é **orquestrar a saída de patrimônio mantido em wallets Web3 para o sistema financeiro brasileiro**, utilizando infraestrutura de terceiros quando isso produzir a rota mais adequada.

### 1.1 Missão

> **Remover a complexidade existente entre o patrimônio mantido em wallets Web3 e o sistema financeiro brasileiro.**

### 1.2 Tese estratégica

> **Não queremos ser o melhor off-ramp. Queremos saber qual é o melhor off-ramp, e todo o caminho necessário para chegar até ele.**

Para fins técnicos e comerciais, “melhor” significa **melhor rota executável entre as alternativas efetivamente integradas, disponíveis e elegíveis para aquele usuário, ativo, rede, valor e momento**. O Banvia Ramp não deve alegar conhecer ou superar rotas que não consegue cotar, verificar ou executar.

A rota vencedora pode usar Foxbit, BRL0, outro provider, um agregador como Onramper ou uma CEX como Binance **somente quando existir integração comercial e técnica compatível**. Enquanto uma empresa não estiver integrada, ela pode ser benchmark ou concorrente, mas não deve ser tratada como capability operacional do sistema.

### 1.3 Convenção terminológica e regra editorial

Este documento usa termos de Web3, pagamentos e sistemas distribuídos. Para evitar que a documentação vire uma coleção de siglas, **todo termo técnico novo deve ser explicado na primeira ocorrência relevante e também mantido no glossário da Seção 37**. Quando um termo em inglês for usado por ser o vocabulário dominante do mercado, a documentação deve apresentar uma definição curta em português.

Termos centrais desde o início:

- **wallet** — software/carteira que permite ao usuário controlar endereços e autorizar transações; a wallet não deve ser confundida com a blockchain nem com um saldo bancário;
- **self-custody** — modelo em que o próprio usuário controla as chaves/credenciais que autorizam movimentações;
- **off-ramp** — processo de saída de criptoativos para moeda fiduciária, aqui com destino principal em BRL via PIX;
- **orchestration (orquestração)** — coordenação de múltiplos sistemas e fornecedores para completar uma intenção ponta a ponta;
- **Exit Venue** — entidade externa integrada capaz de executar uma etapa de saída, como off-ramp direto, exchange centralizada ou agregador;
- **quote (cotação)** — proposta temporária de execução com valores, custos, condições e validade;
- **Route (rota)** — sequência completa de etapas necessárias para levar o patrimônio de origem até o PIX;
- **friction (fricção)** — custo não necessariamente monetário imposto ao usuário, como novo cadastro, KYC, redirecionamento, espera ou assinaturas adicionais.

**Regra documental:** nenhuma sigla ou conceito crítico deve ser introduzido como se fosse autoexplicativo. Quando um agente de IA gerar documentação nova, ele deve preservar esta regra.

## 2. Princípios de produto

1. **Wallet-first.** A jornada começa no patrimônio que o usuário já possui, não no catálogo de moedas de um provider.
2. **Wallet-neutral by design.** Banvia Ramp não exige uma wallet proprietária. O sistema deve suportar protocolos/capabilities de wallets, começando por Wallet Standard no ecossistema Solana.
3. **Provider-neutral.** Nenhum exit venue deve ser favorecido por acoplamento técnico ou comissão comercial.
4. **Best executable route.** O resultado all-in da viagem completa importa mais que a maior quote de um único trecho.
5. **Complexity belongs to Banvia.** A plataforma absorve decisões técnicas acidentais sempre que isso puder ser feito sem esconder risco material.
6. **Non-custodial by default.** O usuário mantém o controle da assinatura e Banvia não coleta seed phrase ou chave privada.
7. **Evidence over inference.** Tracking, status e reconciliação devem afirmar somente o que puder ser demonstrado por evidência on-chain/provider/PIX.
8. **No universal superiority claim.** Banvia Ramp pode perder para Binance, Foxbit, BRL0 ou outro caminho em uma operação. O produto deve medir e aprender com isso.
9. **Economic self-justification.** A taxa Banvia deve ser pequena, explícita e incorporada ao cálculo all-in; quando a recomendação for econômica, ela não pode eliminar a vantagem usada para justificar a troca de rota.
10. **Friction-aware routing.** A melhor rota é específica para aquele usuário. KYC já concluído, necessidade de novo cadastro, tempo de onboarding e ações adicionais fazem parte da decisão, não são detalhes externos ao Route Engine.
11. **KYC minimization.** Banvia deve evitar exigir nova verificação de identidade quando o ganho marginal de outra rota não justificar a fricção, sem impedir o usuário de escolher conscientemente uma alternativa com maior payout.

## 3. Escopo do MVP

O primeiro fluxo real deve provar:

```text
Wallet Solana
   ↓
SOL ou USDC
   ↓
Exit Venue A
   ↓
BRL
   ↓
PIX
```

O segundo marco é adicionar um **Exit Venue B** e comparar ambos em condições reais. Somente depois disso o Route Engine pode ser considerado multi-venue.

O suporte a tokens de cauda longa entra depois:

```text
Wallet
  ↓
Token elegível
  ↓
Jupiter / DEX
  ↓
USDC ou SOL
  ↓
Exit Venue selecionado
  ↓
BRL / PIX
```

A Banvia é 100% não-custodial e não realiza custódia de fundos, market making, conta de pagamento própria, liquidez própria ou controle de private keys pelo backend.

## 4. Atores e componentes externos

### 4.1 Usuário self-custody

Pessoa que mantém ativos em wallet própria e deseja transformar parte do patrimônio em BRL via PIX.

### 4.2 Wallet

Software externo que controla as credenciais e apresenta transações ao usuário para assinatura. Prioridade inicial: Phantom, Solflare e Backpack, sem limitar a arquitetura a essas marcas.

### 4.3 DEX Router

Serviço que descobre e/ou constrói uma rota on-chain. Jupiter é a implementação inicial para Solana.

### 4.4 Exit Venue

Entidade externa capaz de executar uma ou mais etapas de saída.

Tipos previstos:

- `DIRECT_OFFRAMP` — provider cripto→fiat/PIX, como Foxbit Infra ou BRL0 quando contratados;
- `CEX_EXIT` — exchange que possa receber o ativo, converter e permitir saída fiat, como Binance **somente se houver integração comercial/API compatível**;
- `AGGREGATOR` — agregador de ramps, como Onramper, se houver decisão de build-versus-buy favorável à integração.

Um Exit Venue pode ser simultaneamente concorrente em B2C e fornecedor/rail em B2B.

### 4.5 Sistema financeiro brasileiro

Instituições e infraestrutura que completam a liquidação em BRL, especialmente PIX.

### 4.6 Operador Banvia

Usuário interno autorizado a acompanhar ordens, incidentes, divergências e reconciliação.

### 4.7 Parceiro B2B

Wallet, dApp, jogo, fintech ou plataforma que integra Banvia por API, SDK ou hosted checkout.

## 5. Regras de negócio transversais

**RN-001 — Sem chave privada.** Banvia não solicita, recebe, transmite ou armazena seed phrase/chave privada do usuário.

**RN-002 — Assinatura explícita.** Qualquer transação on-chain que mova ativos do usuário depende de autorização explícita na wallet, salvo futuro modelo autorizado com requisitos próprios.

**RN-003 — Identidade técnica do ativo.** Na Solana, decisões financeiras usam mint address; ticker e nome são apenas metadados.

**RN-004 — Quote expira.** Toda quote/rota possui TTL e condições de validade.

**RN-005 — Sem promessa de uma assinatura.** O produto minimiza ações, mas não promete uma assinatura universal.

**RN-006 — Melhor rota executável.** A seleção considera a viagem completa, e não apenas a quote de um provider.

**RN-007 — Melhor rota integrada.** A expressão “melhor rota” refere-se somente ao universo de rotas integradas, disponíveis e elegíveis no momento.

**RN-008 — Provider-neutral.** Revenue share, rebates ou comissões não podem silenciosamente determinar a recomendação.

**RN-009 — Fallback antes do commit.** Troca automática de venue só é permitida antes de ponto irreversível ou quando a recuperação for comprovadamente segura.

**RN-010 — Adapter boundary.** DTOs/SDKs externos não podem vazar para o domínio central.

**RN-011 — Transparência all-in.** Antes da assinatura, o usuário deve ver payout líquido estimado, validade da quote e custos materiais conhecidos.

**RN-012 — Estado compreensível.** Webhooks e status externos são normalizados e traduzidos para linguagem humana.

**RN-013 — Compliance contratual.** Responsabilidades de KYC/KYT/AML/Travel Rule seguem contrato e enquadramento jurídico; terceirização não implica ausência de obrigação própria.

**RN-014 — Reconciliação obrigatória.** Ordem concluída deve ser conciliável entre Banvia, blockchain, DEX quando houver, exit venue e PIX.

**RN-015 — Não comparar o incomparável.** Rotas com condições diferentes de KYC, TTL, valor alvo, rede ou beneficiário não podem ser ranqueadas como se fossem equivalentes sem normalização.

**RN-016 — Irreversibilidade explícita.** Cada leg da rota indica se ainda pode ser cancelada/revertida.

**RN-017 — Sem saldo fictício.** Valor indicativo de carteira não é tratado como valor sacável até existir rota executável.

**RN-018 — Sem claim de superioridade universal.** Marketing não pode afirmar “sempre mais barato/rápido que Binance” ou equivalente sem evidência compatível.

**RN-019 — Microfee transparente.** A taxa Banvia deve ser conhecida antes da assinatura/commit e incluída no cálculo da rota.

**RN-020 — User Net Outcome.** A recomendação deve considerar o valor que efetivamente permanece para o usuário após DEX fees, network fees, slippage/price impact esperado, custos do exit venue e fee Banvia.

**RN-021 — Baseline comparável.** Toda alegação de vantagem econômica deve possuir baseline definida e comparável quanto a ativo, rede, valor, KYC, beneficiário e momento.

**RN-022 — Vantagem líquida não negativa.** Quando a política de otimização for econômica, Banvia não deve recomendar uma troca de rota se a fee Banvia transformar a vantagem frente à baseline em resultado líquido negativo.

**RN-023 — Política de fee versionada.** Fórmula, percentual/basis points, valor fixo, piso, teto, arredondamento e exceções da fee devem possuir versão auditável associada à quote/order.

**RN-024 — Shared savings é opcional e futuro.** Um modelo de captura de fração da economia criada pode ser experimentado somente após validação jurídica, comercial, contábil e de UX.

**RN-025 — Revenue share separado do ranking.** Receita recebida do exit venue deve ser contabilizada separadamente e não pode ser tratada como vantagem econômica do usuário nem fator oculto de recomendação.

**RN-026 — KYC é parte da rota.** O estado de identidade/verificação do usuário em cada exit venue deve ser considerado na elegibilidade e no ranking.

**RN-027 — Melhor rota é contextual.** A mesma combinação de ativo e valor pode produzir recomendações diferentes para usuários distintos porque KYC, limites, preferências e ações pendentes também diferem.

**RN-028 — Evitar KYC desnecessário.** Na política `RECOMMENDED`, uma rota que exige novo KYC não deve ser apresentada como superior apenas por uma vantagem econômica marginal sem explicitar a fricção. A política de ranking deve versionar como essa fricção é tratada.

**RN-029 — Alternativa com maior payout continua visível.** Quando uma rota com novo KYC pagar mais, o sistema pode apresentá-la como “maior valor”, separando-a da rota “recomendada” ou “menor fricção”.

**RN-030 — KYC não é universal por pressuposto.** Banvia não deve assumir que um KYC realizado por um venue é automaticamente válido em outro. Reuso/delegação só pode ocorrer quando houver base contratual, técnica, jurídica e de privacidade explicitamente validada.

**RN-031 — KYC Graph sem documentos por padrão.** O mapa de relacionamento de identidade deve armazenar, sempre que possível, apenas status, referências externas, validade, escopo e evidência mínima; documentos brutos não devem ser copiados entre venues sem necessidade e base legal.

**RN-032 — KYC pode expirar.** Um estado `APPROVED` deve possuir, quando aplicável, validade/escopo e poder evoluir para `REVERIFICATION_REQUIRED`.

**RN-033 — “Uma wallet, um KYC, qualquer rota” é visão futura.** A frase representa uma direção de produto, não uma promessa do MVP.

## 6. Requisitos funcionais — Wallet

**RF-WAL-001.** Detectar wallets Solana compatíveis com Wallet Standard disponíveis no ambiente.

**RF-WAL-002.** Destacar Phantom, Solflare e Backpack na interface quando disponíveis.

**RF-WAL-003.** Permitir conexão/desconexão sem segredo de wallet.

**RF-WAL-004.** Persistir somente dados não sensíveis necessários à sessão e consentidos.

**RF-WAL-005.** Suportar troca de conta quando a wallet expuser múltiplas contas.

**RF-WAL-006.** Tratar rejeição de conexão/assinatura como evento normal de UX.

**RF-WAL-007.** Prever mobile por protocolos/deep links adequados.

**RF-WAL-008.** A arquitetura deve permitir adicionar outras redes e padrões de wallet sem alterar o domínio de Order/Route.

**RF-WAL-009.** Banvia Ramp não deve exigir migração para wallet Banvia para utilizar o produto.

## 7. Requisitos funcionais — Descoberta de patrimônio

**RF-AST-001.** Ler SOL e tokens SPL da conta conectada.

**RF-AST-002.** Resolver metadados por mint.

**RF-AST-003.** Separar `indicativeValue` de `executableValue`.

**RF-AST-004.** Exibir preço somente com fonte de confiança mínima configurada.

**RF-AST-005.** Ativos sem preço/liquidez confiável devem aparecer como não estimáveis, não como patrimônio realizável.

**RF-AST-006.** Calcular `eligibleWithdrawableValue` separadamente do patrimônio indicativo total.

**RF-AST-007.** Registrar snapshot de holdings usado para a quote sem tratá-lo como saldo custodiado pelo Banvia.

## 8. Requisitos funcionais — Eligibility Engine

Estados sugeridos:

- `ELIGIBLE_DIRECT`
- `ELIGIBLE_SWAP_REQUIRED`
- `LIMITED`
- `REVIEW`
- `UNSUPPORTED`
- `BLOCKED`

**RF-ELG-001.** Verificar identidade do ativo por mint.

**RF-ELG-002.** Verificar se existe rota DEX quando necessária.

**RF-ELG-003.** Avaliar liquidez e price impact para o ticket exato.

**RF-ELG-004.** Considerar authorities/status e sinais de risco relevantes.

**RF-ELG-005.** Cruzar ativo/rede/valor com capabilities dos exit venues.

**RF-ELG-006.** Permitir allowlist/denylist e políticas versionadas.

**RF-ELG-007.** Produzir explicação auditável da decisão.

**RF-ELG-008.** Diferenciar “ativo tem preço” de “ativo possui caminho confiável para PIX”.

**RF-ELG-009.** Permitir limite dinâmico por ticket, não apenas `allowed=true/false`.

## 9. Route Graph

Uma rota Banvia é uma sequência/grafo de legs tipadas. Tipos iniciais:

```text
DEX_SWAP
ONCHAIN_TRANSFER
PROVIDER_FUNDING
CEX_DEPOSIT
FIAT_CONVERSION
PIX_SETTLEMENT
```

Cada `RouteLeg` deve possuir, quando aplicável:

- `legId`
- `type`
- `sourceAsset`
- `targetAsset`
- `sourceNetwork`
- `targetNetwork`
- `venue`
- `expectedInput`
- `expectedOutput`
- `fees`
- `ttl`
- `estimatedLatency`
- `reversibility`
- `userActionRequired`
- `evidenceType`

**RF-GPH-001.** Toda RouteCandidate deve possuir Route Graph persistível/auditável.

**RF-GPH-002.** O sistema deve calcular custo e risco end-to-end pela soma/composição dos legs, não pelo último trecho.

**RF-GPH-003.** Cada leg deve expor seu ponto de irreversibilidade.

**RF-GPH-004.** Rotas diretas são representadas pelo mesmo modelo, apenas com menos legs.

**RF-GPH-005.** O motor deve poder explicar ao usuário a rota em linguagem simples sem expor detalhes desnecessários.

## 10. Quote e Route Engine

**RF-RTE-001.** Receber intenção por ativo/quantidade ou por valor alvo em BRL.

**RF-RTE-002.** Gerar múltiplas RouteCandidates quando houver alternativas reais.

**RF-RTE-003.** Considerar rota direta antes de adicionar swap desnecessário.

**RF-RTE-004.** Considerar rota composta via DEX quando economicamente/operacionalmente necessária.

**RF-RTE-005.** Normalizar quotes externas em modelo Banvia.

**RF-RTE-006.** Calcular payout líquido estimado em BRL.

**RF-RTE-007.** Decompor custos conhecidos: network fee, DEX fee, price impact, slippage/tolerância, provider spread/fee e fee Banvia.

**RF-RTE-008.** Atribuir score determinístico, versionado e auditável.

**RF-RTE-009.** Excluir venues indisponíveis, incompatíveis ou com circuit breaker aberto.

**RF-RTE-010.** Incorporar estado de KYC/ação pendente na avaliação de fricção.

**RF-RTE-011.** Produzir pelo menos quatro modos de comparação quando houver dados suficientes: `RECOMMENDED`, `MAX_NET_PAYOUT`, `FASTEST` e `LOWEST_FRICTION`.

**RF-RTE-012.** O modo `RECOMMENDED` deve considerar fricção de identidade sem convertê-la silenciosamente em uma “taxa” monetária; qualquer penalidade interna de scoring deve ser política versionada e explicável.

**RF-RTE-013.** Quando a rota de maior payout exigir novo KYC e outra rota já verificada tiver resultado próximo, exibir explicitamente o trade-off: valor adicional versus nova verificação/tempo estimado.

**RF-RTE-014.** O resultado de ranking deve registrar `kycState`, `kycActionRequired`, `frictionClass`, `estimatedOnboardingTime` quando conhecido e a razão textual da recomendação.

**RF-RTE-011.** Persistir candidatos, filtros aplicados e justificativa da escolha.

**RF-RTE-012.** Quando houver somente uma rota operacional, usar “rota disponível/recomendada”, não “melhor rota do mercado”.

**RF-RTE-013.** O score deve considerar, no mínimo:

```text
NetPayout
ProbabilityOfCompletion
ExpectedLatency
LiquidityAndSlippageRisk
VenueHealth
KycFriction
NumberOfUserActions
IrreversibleLegCount
ComplianceCompatibility
```

**RF-RTE-014.** A política inicial deve ser `BALANCED`. Preferências `MAX_PAYOUT` ou `FASTEST` são futuras e só entram com regras explícitas.

**RF-RTE-015.** Route splitting entre vários venues não faz parte do MVP.

**RF-RTE-016.** Calcular `userNetOutcome` para cada rota candidata.

**RF-RTE-017.** Calcular `routeAdvantage = bestRouteNetBeforeBanviaFee - baselineNetOutcome` quando existir baseline comparável.

**RF-RTE-018.** Calcular `userNetAdvantage = routeAdvantage - banviaFee`.

**RF-RTE-019.** Uma recomendação `MAX_PAYOUT`/econômica só pode ser apresentada como vantajosa quando `userNetAdvantage > 0`, salvo arredondamentos/tolerâncias formalmente definidos.

**RF-RTE-020.** Quando a baseline continuar melhor após todos os custos, o sistema deve informar que não encontrou alternativa economicamente superior em vez de forçar uma troca de venue.

**RF-RTE-021.** Se o usuário escolher explicitamente otimizar por velocidade, conveniência ou fricção, a interface deve separar essa preferência de uma alegação de maior payout.

## 11. Requisitos funcionais — DEX/Jupiter

**RF-DEX-001.** Integrar Jupiter por porta interna `DexRouter`.

**RF-DEX-002.** Utilizar APIs vigentes/recomendadas no momento da implementação.

**RF-DEX-003.** Obter quote por input mint, output mint e amount.

**RF-DEX-004.** Capturar output, price impact, slippage/tolerância e rota.

**RF-DEX-005.** Construir/receber transação não assinada para wallet.

**RF-DEX-006.** Acompanhar assinatura, submissão e confirmação.

**RF-DEX-007.** Não armazenar segredo de wallet.

**RF-DEX-008.** Suportar futuros DEX routers atrás da mesma porta.

## 12. Requisitos funcionais — Exit Venues

Contrato conceitual:

```text
capabilities()
quote(request)
createExit(order)
getFundingInstructions(order)
getStatus(externalOrderId)
normalizeWebhook(payload)
cancel(order)       // se suportado e reversível
health()
```

### 12.1 Capability Matrix

**RF-EXT-001.** Manter capabilities por venue:

- tipo (`DIRECT_OFFRAMP`, `CEX_EXIT`, `AGGREGATOR`);
- redes;
- ativos;
- limites;
- moedas fiat;
- PIX/métodos de payout;
- regras de titularidade;
- KYC mode;
- KYT/compliance constraints;
- funding model;
- wallet initiation;
- webhooks;
- SLA conhecido;
- refund/cancel support;
- sandbox;
- estado operacional.

**RF-EXT-002.** Foxbit Infra e BRL0 são candidates/adapters somente após contrato e credenciais.

**RF-EXT-003.** Binance é `CEX_EXIT_CANDIDATE`/benchmark até existir interface comercial compatível; não deve haver adapter fictício.

**RF-EXT-004.** Onramper é concorrente/benchmark e pode ser avaliado como `AGGREGATOR` somente após decisão build-versus-buy e contrato.

**RF-EXT-005.** Suportar sandbox separado de produção.

**RF-EXT-006.** Validar autenticidade de webhooks.

**RF-EXT-007.** Processar eventos idempotentemente.

**RF-EXT-008.** Persistir evento bruto de forma protegida para auditoria.

**RF-EXT-009.** Detectar divergência entre consulta e webhook.

**RF-EXT-010.** Expor health/circuit breaker ao Route Engine.

## 13. Governança do ranking

**RN-RNK-001.** A política de ranking deve possuir versão identificável em toda ordem.

**RN-RNK-002.** Revenue share não pode ser fator oculto de superioridade.

**RN-RNK-003.** Se existir promoção patrocinada, ela deve ser separada visual e logicamente da recomendação de melhor rota.

**RN-RNK-004.** Banvia deve ser capaz de reproduzir por que Route A venceu Route B.

**RN-RNK-005.** Alterações de pesos/regras precisam de auditoria e rollback.

**RN-RNK-006.** O sistema deve medir win rate por venue para detectar viés ou degradação.

### 13.1 Monetização, microfee e vantagem líquida

O modelo inicial preferencial do Banvia Ramp é uma **microfee transacional explícita**, e não uma assinatura B2C obrigatória. O objetivo é capturar uma fração pequena do valor criado pela orquestração sem transformar Banvia em uma camada adicional que destrói a própria economia encontrada.

Para cada rota, o sistema deve distinguir:

```text
UserNetOutcome =
GrossPayout
- DEXFees
- NetworkFees
- ExpectedSlippageOrPriceImpact
- ExitVenueFees
- BanviaFee
```

E, quando houver baseline comparável:

```text
RouteAdvantage = BestRouteNetBeforeBanviaFee - BaselineNetOutcome
UserNetAdvantage = RouteAdvantage - BanviaFee
```

Exemplo conceitual:

```text
Rota habitual/baseline:      R$ 490,00
Melhor rota Banvia:          R$ 497,00
Route Advantage:             + R$ 7,00
Fee Banvia:                  - R$ 1,00
User Net Advantage:          + R$ 6,00
```

**RN-ECO-001.** A fee Banvia deve ser exibida separadamente e antes da assinatura/commit.

**RN-ECO-002.** A fee pode ser percentual em basis points, valor fixo pequeno ou combinação com piso/teto, desde que a política seja simples, versionada e auditável.

**RN-ECO-003.** A recomendação econômica não deve depender de uma quote bruta; deve usar o resultado all-in após a própria fee Banvia.

**RN-ECO-004.** Se a diferença entre rotas não pagar a fee Banvia, o sistema deve indicar que a baseline/rota atual continua competitiva.

**RN-ECO-005.** Revenue share recebido de providers não reduz artificialmente a fee mostrada nem pode ser contabilizado como economia do usuário sem disclosure apropriado.

**RN-ECO-006.** Shared savings pode ser testado futuramente: Banvia captura somente parte da vantagem econômica mensurada, preservando benefício líquido ao usuário.

**RN-ECO-007.** Tickets pequenos devem possuir política de viabilidade para evitar que custos fixos, suporte ou fee mínima tornem o produto economicamente irracional.

**RN-ECO-008.** A política deve permitir fee zero ou reduzida quando necessário para testes, aquisição, ausência de vantagem econômica ou decisão comercial explicitamente versionada.

## 14. Identidade, KYC e compliance orchestration

**KYC (Know Your Customer)** é o processo de identificação e verificação do cliente. **KYT (Know Your Transaction)** é a análise de risco da transação/endereço. **AML (Anti-Money Laundering)** reúne controles de prevenção à lavagem de dinheiro. No Banvia Ramp, esses controles não são tratados como uma única caixa-preta: é necessário registrar **quem executou cada verificação, para qual venue, em qual momento e sob qual política**.

O problema de produto é que a rota economicamente melhor pode exigir que o usuário se cadastre ou faça KYC em um novo venue. Essa fricção pode destruir a simplicidade da experiência. Por isso, identidade deve ser parte do Route Graph e do Route Engine.

### 14.1 Estados normalizados de identidade

**RF-CMP-001.** Estado normalizado por venue: `NOT_REQUIRED`, `NOT_STARTED`, `REQUIRED`, `PENDING`, `APPROVED`, `REJECTED`, `REVIEW`, `REVERIFICATION_REQUIRED`, `EXPIRED`.

**RF-CMP-002.** O estado deve ser por **usuário + venue + produto/escopo**, e não um booleano global “KYC=true”.

**RF-CMP-003.** Registrar, quando disponível, data de aprovação, validade, nível/limite, referência externa e ação pendente, sem armazenar documento bruto por padrão.

### 14.2 Três modelos de KYC

O Banvia deve distinguir três modelos, porque eles possuem responsabilidades e UX diferentes:

1. **Provider-owned KYC (KYC pertencente ao venue).** O usuário é verificado diretamente pelo exit venue. Banvia conhece o status e conduz a jornada, mas não deve fingir que a verificação pertence ao Banvia. É o modelo mais simples para o MVP.
2. **Embedded KYC (KYC embutido).** A interface de verificação aparece dentro da experiência Banvia, mas a coleta/processamento pode continuar sendo do provider ou de um fornecedor especializado. “Embutido” descreve a UX, não transfere automaticamente a responsabilidade regulatória.
3. **Reusable/delegated KYC (KYC reutilizável/delegado).** Uma verificação pode ser aceita por múltiplos venues ou executada por Banvia/terceiro em nome deles. Esse modelo só pode existir com acordos formais sobre padrão de verificação, compartilhamento de dados, responsabilidade, auditoria, LGPD e regras regulatórias.

**RF-CMP-004.** Quando KYC for hospedado pelo venue, Banvia deve redirecionar ou embutir a jornada sem copiar documentos desnecessariamente.

**RF-CMP-005.** Quando houver embedded KYC, a interface deve informar quem é o controlador/recebedor dos dados e obter consentimentos aplicáveis.

**RF-CMP-006.** Reuso/delegação de KYC não pode ser habilitado por inferência técnica; requer capability explícita e contrato correspondente.

### 14.3 KYC Graph

**KYC Graph** é a representação lógica das relações de verificação entre um usuário e os exit venues. Ele não é um repositório de documentos. Seu objetivo é responder rapidamente: **“quais saídas este usuário já pode utilizar sem nova verificação?”**

Exemplo conceitual:

```text
USER-123
  ├── Foxbit      APPROVED
  ├── BRL0        NOT_STARTED
  └── Onramper
       ├── Venue X APPROVED
       └── Venue Y REQUIRED
```

**RF-CMP-007.** Persistir um `VenueIdentityStatus` por relação relevante, com status, escopo, validade, referência externa e timestamps.

**RF-CMP-008.** O Route Engine deve consultar o KYC Graph antes do ranking final.

**RF-CMP-009.** Uma mudança de status recebida por webhook/API deve atualizar o KYC Graph de modo idempotente e auditável.

**RF-CMP-010.** O usuário deve poder ver quais venues já estão verificados e quais exigiriam nova ação, sem expor detalhes internos desnecessários.

### 14.4 Fricção como variável de decisão

**Fricção** é qualquer esforço adicional que não aparece apenas como preço: novo cadastro, envio de documento, selfie, espera de aprovação, redirecionamento, assinatura adicional ou repetição de dados.

**RF-CMP-011.** Cada RouteCandidate deve expor as ações adicionais de identidade necessárias antes do commit irreversível.

**RF-CMP-012.** O ranking deve distinguir “maior payout” de “recomendado”. Exemplo: uma rota pode pagar R$ 1,90 a mais e ainda não ser a recomendada se exigir nova jornada de KYC e houver uma rota já verificada quase equivalente.

**RF-CMP-013.** O usuário pode optar explicitamente por maximizar payout e aceitar o novo KYC; a decisão deve ser registrada na Order.

**RF-CMP-014.** Nunca apresentar estimativa de tempo de KYC como SLA garantido quando ela for apenas histórico/estimativa.

### 14.5 Compliance e privacidade

**RF-CMP-015.** Restrições KYT devem bloquear ou encaminhar a rota para revisão conforme política.

**RF-CMP-016.** Coletar somente dados mínimos compatíveis com LGPD, contrato e finalidade declarada.

**RF-CMP-017.** Registrar qual entidade executou cada checagem e qual política estava vigente.

**RF-CMP-018.** Usar provider regulado não deve ser documentado como isenção automática de obrigações do Banvia.

**RF-CMP-019.** Dados de identidade não podem ser utilizados para ranking comercial além do necessário para elegibilidade, fricção e execução da rota.

**RF-CMP-020.** A visão de longo prazo pode perseguir “uma wallet, um KYC, qualquer rota”, mas a produção só pode divulgar reuso de identidade para venues que efetivamente aceitem esse modelo.

## 15. Ordem e state machine

```text
CREATED
  ↓
PORTFOLIO_DISCOVERED
  ↓
ROUTE_QUOTED
  ↓
AWAITING_USER_ACTION
  ↓
AWAITING_SIGNATURE          (quando aplicável)
  ↓
ONCHAIN_SUBMITTED           (quando aplicável)
  ↓
ONCHAIN_CONFIRMED           (quando aplicável)
  ↓
EXIT_VENUE_ACCEPTED
  ↓
COMPLIANCE_REVIEW           (opcional)
  ↓
OFFRAMP_PROCESSING
  ↓
PIX_SENT
  ↓
PIX_SETTLED
  ↓
COMPLETED
```

Estados alternativos:

- `EXPIRED`
- `USER_CANCELLED`
- `REJECTED`
- `FAILED_REVERSIBLE`
- `FAILED_IRREVERSIBLE_REVIEW`
- `REFUND_PENDING`
- `REFUNDED`
- `MANUAL_REVIEW`

**RF-ORD-001.** Toda ordem possui `orderId` Banvia imutável.

**RF-ORD-002.** Toda ordem referencia `routeId` e versão do ranking.

**RF-ORD-003.** IDs externos e transaction signatures são correlacionados sem substituir IDs internos.

**RF-ORD-004.** Eventos duplicados não duplicam efeitos financeiros.

**RF-ORD-005.** Após leg irreversível, falha entra em recuperação/review, não fallback cego.

**RF-ORD-006.** Toda transição registra timestamp, origem, motivo e evidência.

## 16. Assinatura e execução

**RF-EXE-001.** Antes da assinatura, mostrar resumo econômico da rota.

**RF-EXE-002.** A wallet apresenta a transação para aprovação.

**RF-EXE-003.** Rejeição pelo usuário não é falha sistêmica.

**RF-EXE-004.** Após assinatura, tracking é automático.

**RF-EXE-005.** Não prometer atomicidade entre blockchain e PIX.

**RF-EXE-006.** A UI deve informar quando a próxima etapa se torna irreversível.

## 17. Tracking e UX

**RF-TRK-001.** Exibir timeline ponta a ponta.

**RF-TRK-002.** Traduzir estados externos para estado Banvia.

**RF-TRK-003.** Exibir “onde estão os fundos” somente com evidência confirmada.

**RF-TRK-004.** Exibir latência histórica apenas com amostra suficiente e rotular como estimativa.

**RF-TRK-005.** Expor detalhes técnicos sob demanda: mint, tx signature, venue order ID, PIX E2E.

**RF-TRK-006.** Atualizações em tempo real preferencialmente via SSE/WebSocket.

**RF-TRK-007.** A experiência deve priorizar a intenção “quanto quero receber?” em vez de exigir seleção manual de provider.

## 18. Recibo e evidências

**RF-RCP-001.** Gerar recibo unificado Banvia Ramp.

**RF-RCP-002.** Correlacionar wallet, ativo/mint, Route Graph, transações on-chain, DEX, exit venue, valores, taxas, BRL liquidado e PIX E2E quando disponível.

**RF-RCP-003.** Permitir exportação legível e estruturada em fase posterior.

**RF-RCP-004.** Separar comprovante operacional de qualquer cálculo fiscal.

## 19. Reconciliação

**RF-REC-001.** Reconciliar `Order ↔ RouteLegs ↔ blockchain ↔ DEX ↔ ExitVenue ↔ PIX`.

**RF-REC-002.** Criar fila de divergências.

**RF-REC-003.** Detectar `COMPLETED` sem evidência de settlement.

**RF-REC-004.** Detectar settlement externo sem estado correspondente.

**RF-REC-005.** Correção manual gera novo evento, nunca reescrita destrutiva.

## 20. Transaction Journal imutável

**RF-LED-001.** A Banvia é 100% não-custodial e possui `immutable transaction journal` como fonte de verdade probatória.

**RF-LED-002.** A Banvia não mantém saldos de clientes, custódia de fundos, prefunding nem tesouraria própria. O modelo financeiro é estritamente o Transaction Journal probatório por payout.

**RF-LED-003.** Fees Banvia e revenue share devem ser contabilmente conciliáveis desde o início.

## 21. Backoffice

**RF-OPS-001.** Busca por orderId, routeId, tx signature, external order, wallet e PIX E2E quando permitido.

**RF-OPS-002.** Exibir Route Graph e decisão de ranking.

**RF-OPS-003.** Exibir eventos e divergências.

**RF-OPS-004.** Exibir health/circuit breakers por venue.

**RF-OPS-005.** Ações manuais com RBAC, MFA, motivo obrigatório e auditoria.

**RF-OPS-006.** Histórico não pode ser apagado/regravado para esconder divergência.

## 22. B2B — Banvia Connect / Banvia API

**RF-B2B-001.** API wallet-to-PIX quote/orchestration.

**RF-B2B-002.** Hosted checkout Banvia Ramp.

**RF-B2B-003.** Webhooks assinados do lifecycle.

**RF-B2B-004.** SDK web para conectar wallet e iniciar fluxo.

**RF-B2B-005.** `Idempotency-Key` obrigatório para criação de ordem.

**RF-B2B-006.** Multi-tenant e isolamento lógico.

**RF-B2B-007.** Fee/revenue share por parceiro quando permitido.

## 23. Requisitos não funcionais

### 23.1 Segurança

- TLS obrigatório;
- segredos de integrations em secret manager;
- nenhuma private key de usuário;
- webhook signatures verificadas;
- RBAC/MFA no backoffice;
- proteção contra replay;
- rate limiting;
- CSP e frontend hardening;
- supply-chain/dependency monitoring;
- audit trail imutável;
- segregação sandbox/produção;
- PII criptografada e minimizada.

### 23.2 Consistência e idempotência

- criação de ordem idempotente;
- webhook idempotente;
- transactional outbox/inbox;
- locks/optimistic concurrency quando necessário;
- invariantes de state machine no domínio e banco.

### 23.3 Resiliência

- timeout por integração;
- retry somente em operações seguras;
- circuit breaker por venue;
- bulkhead quando aplicável;
- fallback somente antes de ponto irreversível;
- health externo separado da saúde interna.

### 23.4 Observabilidade

- OpenTelemetry;
- logs estruturados com `orderId`, `routeId`, `legId`, `venue`, `txSignature`, `correlationId`;
- métricas de quote/ranking/settlement;
- dashboards por venue;
- alertas de mismatch;
- PII mínima em logs.

### 23.5 Performance

Metas iniciais são internas e devem ser medidas:

- portfolio discovery responsivo;
- quote multi-venue em poucos segundos quando dependências respondem;
- atualização de webhook para UI em baixa latência;
- processamento robusto de duplicatas.

Nenhum SLA público antes de dados reais.

### 23.6 Disponibilidade

Falha de um venue não deve derrubar o Banvia quando houver outra rota segura. Se não houver alternativa, a UI deve degradar com clareza, não inventar fallback.


### 23.7 Fundamentos de System Design e correção distribuída

O Banvia Ramp deve ser projetado como **um sistema financeiro distribuído**, ainda que seja implementado inicialmente como monólito modular. O desafio central não é apenas integrar APIs: é preservar correção quando blockchain, DEX/router, exit venues, webhooks e PIX evoluem e falham de forma independente.

**Referência técnica principal:** Martin Kleppmann e Chris Riccomini, *Designing Data-Intensive Applications*, 2ª edição (O'Reilly, 2026).

#### 23.7.1 System of Record x Derived Data

O sistema deve distinguir explicitamente **qual fonte possui autoridade sobre cada fato**:

| Fato | Fonte de autoridade esperada |
|---|---|
| intenção do usuário, Order, Route Graph, ranking e journal Banvia | PostgreSQL/Banvia |
| confirmação de transação on-chain | blockchain/RPC validado |
| execução de swap | blockchain + evidência do DEX/router |
| aceitação/compliance/status do exit venue | exit venue |
| liquidação final em PIX | evidência de settlement/PIX disponível pelo venue |
| timeline mostrada ao usuário | **derived data** calculado a partir das fontes acima |

**RN-SD-001.** O banco Banvia não pode declarar como fato externo algo que não possua evidência correspondente.

**RN-SD-002.** Estado derivado deve ser reconstruível a partir de eventos, referências e evidências persistidas sempre que tecnicamente possível.

**RN-SD-003.** `COMPLETED` exige evidência suficiente de settlement; ausência de erro não equivale a sucesso.

#### 23.7.2 Falhas parciais e estados desconhecidos

Falha parcial é comportamento normal. Uma dependência pode concluir a operação e ainda assim Banvia receber timeout, perder a resposta ou reiniciar antes de persistir o resultado.

**RN-SD-004.** Toda integração deve diferenciar `KNOWN_SUCCESS`, `KNOWN_FAILURE` e `UNKNOWN` quando a semântica externa permitir.

**RN-SD-005.** Timeout não autoriza repetir movimentação financeira automaticamente.

**RN-SD-006.** Estado `UNKNOWN` deve ser resolvido por polling, webhook, lookup por idempotency key/external ID ou reconciliação antes de nova ação irreversível.

#### 23.7.3 Idempotência e ausência de exactly-once global

Não existe transação exactly-once global entre browser, Banvia, Solana, DEX, exit venue e PIX.

**RN-SD-007.** Criação de Order deve aceitar `Idempotency-Key` e associá-la ao hash/escopo da intenção para impedir reutilização incompatível.

**RN-SD-008.** Repetir a mesma intenção idempotente deve retornar/referenciar a mesma operação financeira, nunca criar uma nova por acidente.

**RN-SD-009.** Webhooks devem passar por inbox/deduplicação antes de provocar efeitos de domínio.

**RN-SD-010.** Consumers/handlers devem ser reentrantes: processar novamente um evento já aplicado não pode duplicar payout, funding ou transição financeira.

**RN-SD-011.** Após qualquer leg irreversível, fallback ou retry que mova fundos exige prova explícita de segurança.

#### 23.7.4 Limites transacionais, Outbox e Inbox

Banvia pode garantir ACID **dentro do próprio PostgreSQL**, mas não sobre sistemas externos.

**RN-SD-012.** Mudança de estado crítica + evento interno correspondente devem ser persistidos na mesma transação local quando necessário, usando Transactional Outbox.

**RN-SD-013.** Eventos externos relevantes devem ser persistidos/identificados antes de sua aplicação ao domínio, usando Webhook Inbox ou mecanismo equivalente.

**RN-SD-014.** Não adotar 2PC/transaction manager distribuído para tentar incluir Solana, DEX, provider ou PIX em uma única transação ACID.

**RN-SD-015.** Ações compensatórias devem ser modeladas explicitamente; compensação não significa rollback real de uma transação blockchain já finalizada.

#### 23.7.5 Workflow durável e State Machine

A execução de uma Order deve sobreviver a restart/deploy/crash.

**RN-SD-016.** O próximo passo do fluxo deve ser derivado de estado persistido, não da memória de thread, sessão HTTP, `Future`, cache ou scheduler efêmero.

**RN-SD-017.** Toda transição deve validar `fromState → toState`, versão da Order e pré-condições de domínio.

**RN-SD-018.** Atualizações concorrentes devem usar optimistic locking/versionamento e constraints de banco quando aplicável.

**RN-SD-019.** Uma Order deve poder ser retomada/reconciliada após reinício sem repetir efeitos externos já confirmados.

#### 23.7.6 Tempo, TTL e ordenação

**RN-SD-020.** Quotes possuem TTL persistido e devem ser revalidadas imediatamente antes do primeiro ponto irreversível.

**RN-SD-021.** Timestamps de sistemas externos não devem ser tratados como relógio global perfeitamente ordenado.

**RN-SD-022.** Eventos Banvia devem usar `Instant`/UTC para persistência; métricas de duração/timeout devem usar mecanismos monotônicos quando disponíveis.

**RN-SD-023.** Webhook fora de ordem deve ser aceito como cenário de teste e não pode retroceder a Order de forma inválida.

#### 23.7.7 Dinheiro e precisão numérica

**RN-SD-024.** Payout, fee, spread, exchange rate e reconciliação devem usar `BigDecimal`/tipo monetário equivalente com escala e `RoundingMode` explícitos.

**RN-SD-025.** `double`/`float` não podem representar valores financeiros autoritativos.

**RN-SD-026.** Toda conversão deve registrar moeda/ativo, escala, taxa usada e regra de arredondamento suficiente para reproduzir o cálculo.

#### 23.7.8 Timeouts, retries, circuit breakers e bulkheads

**RN-SD-027.** Toda chamada externa possui timeout finito e configurável.

**RN-SD-028.** Retry usa backoff + jitter e somente é habilitado para operação sabidamente segura/idempotente.

**RN-SD-029.** Cada exit venue possui circuit breaker/health independente, consumido pelo Route Engine.

**RN-SD-030.** Falha de um venue não pode esgotar threads/conexões necessárias aos demais; aplicar bulkhead/limites de concorrência onde houver risco real.

#### 23.7.9 Reconciliação como mecanismo de correção

Reconciliação é parte do design de consistência, não apenas atividade financeira posterior.

**RN-SD-031.** Job/processo de reconciliação deve procurar divergências entre Order, RouteLegs, blockchain, DEX, exit venue e PIX.

**RN-SD-032.** Divergências criam `ReconciliationCase` auditável e nunca são corrigidas apagando eventos anteriores.

**RN-SD-033.** O sistema deve detectar ao menos:

- on-chain confirmado sem progressão interna;
- exit venue aceitou/processou sem estado Banvia equivalente;
- PIX/settlement externo sem Order concluída;
- Order `COMPLETED` sem evidência de settlement;
- valores/taxas divergentes além da tolerância definida.

#### 23.7.10 Observabilidade orientada à recuperação

**RN-SD-034.** `orderId`, `routeId`, `legId`, `txSignature`, `externalOrderId`, `correlationId` e PIX E2E devem ser correlacionáveis.

**RN-SD-035.** Métricas e alertas devem monitorar invariantes financeiros e divergências, não apenas CPU/RAM/HTTP 5xx.

**RN-SD-036.** Deve ser possível responder operacionalmente: **“qual é a última evidência confirmada e onde os fundos provavelmente estão?”** sem depender de leitura manual de logs desconectados.

#### 23.7.11 Evolução de contratos externos

**RN-SD-037.** Payloads de provider são convertidos em DTOs versionados do adapter antes de atingir o domínio.

**RN-SD-038.** Mudança de schema/webhook externo deve possuir testes de compatibilidade/regressão.

**RN-SD-039.** Evento desconhecido não deve ser silenciosamente tratado como sucesso; deve ser persistido, observado e encaminhado para política segura.

#### 23.7.12 System Design não significa microservices

O MVP deve privilegiar **correção com baixa complexidade operacional**.

**RN-SD-040.** Arquitetura inicial recomendada: Java 25 + Spring Boot + Spring Modulith + PostgreSQL, usando transações locais, outbox/inbox e workers controlados.

**RN-SD-041.** Kafka, Kubernetes, microservices ou bancos distribuídos não são requisitos do MVP.

**RN-SD-042.** Um componente só deve ser distribuído quando existir necessidade mensurável de escala, isolamento de falha, disponibilidade, throughput ou ownership que compense a complexidade adicionada.

> **Regra de engenharia:** a sofisticação inicial do Banvia Ramp deve estar na semântica de correção — idempotência, estados, evidência, recuperação e reconciliação — e não na quantidade de serviços implantados.


## 24. Modelo de dados conceitual

Agregados/entidades:

- `Customer`
- `WalletConnection`
- `WalletAddress`
- `Asset`
- `AssetHoldingSnapshot`
- `EligibilityAssessment`
- `RouteCandidate`
- `RouteGraph`
- `RouteLeg`
- `Quote`
- `RankingDecision`
- `Order`
- `OnChainExecution`
- `ExitVenue`
- `ExitVenueCapability`
- `ExitExecution`
- `PixSettlement`
- `RiskDecision`
- `ComplianceReference`
- `ReconciliationCase`
- `AuditEvent`
- `FeeRecord`

Holdings são snapshots observados on-chain, não saldo Banvia.

## 25. Critérios de aceite — MVP com um Exit Venue

1. usuário conecta Wallet Standard suportada;
2. sistema lê SOL/USDC sem chave privada;
3. patrimônio indicativo e elegível são distintos;
4. usuário solicita saída em ativo/quantidade ou BRL alvo;
5. Banvia obtém quote real de um venue;
6. Route Graph é criado;
7. payout líquido/TTL são apresentados;
8. usuário autoriza a transação necessária;
9. Banvia acompanha blockchain;
10. venue aceita/processa;
11. webhooks atualizam state machine idempotentemente;
12. PIX settlement possui evidência;
13. ordem é reconciliada;
14. timeline é compreensível;
15. nenhuma chave privada é coletada.

Com um único venue, o produto não pode dizer que encontrou a melhor rota multi-provider.

## 26. Critérios de aceite — Orquestração real com dois ou mais venues

1. pelo menos dois venues reais estão contratados/integrados;
2. ambos podem cotar o mesmo cenário comparável;
3. quotes são normalizadas;
4. Route Graph é produzido para cada candidato;
5. score usa política versionada;
6. ranking é reproduzível;
7. health/circuit breakers interferem na elegibilidade;
8. comissão não distorce silenciosamente a escolha;
9. UI informa “melhor rota integrada” ou “recomendada”, sem claim de mercado universal;
10. dados realizados permitem comparar payout, tempo e sucesso por venue.

## 27. Critérios de aceite — Long-tail

Antes de memecoins/long-tail em produção:

1. provider/venue confirma aceitação do fluxo self-custody/DEX conforme política;
2. Jupiter quote/execution está integrado;
3. Eligibility limita price impact/liquidez;
4. simulation evita ativos sem execução segura;
5. UX informa swap necessário;
6. custos all-in são estimados;
7. há procedimento para `swap confirmado + exit rejeitado`;
8. limites são conservadores/configuráveis;
9. mint correto é validado;
10. rotas são auditáveis por leg.

## 28. Fora do escopo inicial

- promessa de “qualquer token”;
- promessa de “qualquer wallet” sem protocolo suportado;
- “sempre mais barato que Binance”;
- route splitting multi-provider;
- uma assinatura universal;
- PIX em menos de X segundos sem dados;
- multichain completo;
- custódia Banvia;
- liquidity desk/market maker Banvia;
- conta de pagamento Banvia;
- cartão/crédito/staking;
- on-ramp como prioridade;
- declaração fiscal completa;
- IA/ML para ranking antes de dataset suficiente.

## 29. Perguntas comerciais obrigatórias aos Exit Venues

1. Banvia pode manter marca e UX?
2. Quem é customer of record?
3. Aceita self-custody?
4. Aceita stablecoin recém-resultante de DEX? Sob quais critérios KYT?
5. Como funciona Travel Rule?
6. Quem executa KYC?
7. Quais dados Banvia precisa armazenar?
8. Ativos/redes suportados no B2B?
9. Como funciona funding/deposit address e TTL?
10. Há prefunding?
11. Como é preço/spread?
12. Fee e volume mínimo?
13. Minimum monthly commitment?
14. Webhooks possuem assinatura/retry/ordering semantics?
15. SLA/P50/P95 real de PIX?
16. Sandbox end-to-end?
17. Rejeição/refund/review manual?
18. PIX exige mesma titularidade?
19. Restrições por DEX/origem/token?
20. Responsabilidades regulatórias/fiscais de cada parte?

Para CEX/aggregator adicionalmente:

21. Existe programa/API partner para criar e acompanhar a saída sem obrigar experiência manual fora do Banvia?
22. O KYC do usuário pode ser reutilizado ou precisa de nova jornada?
23. É possível obter quote e status determinísticos por API?
24. Há payout direto em PIX ou exige saldo fiat/interação adicional?

## 30. Concorrência e benchmarks obrigatórios

### 30.1 Onramper

É o benchmark/concorrente mais próximo na categoria de ramp aggregation e smart routing. Banvia Ramp não pode usar “agregar providers” como diferencial suficiente. A validação precisa demonstrar valor adicional em:

- começar pelo portfólio já existente na wallet;
- asset eligibility;
- composição DEX + exit venue;
- foco BRL/PIX;
- tracking/reconciliação end-to-end;
- inteligência sobre a rota completa.

### 30.2 Binance

É concorrente indireta/substituto verticalmente integrado. Deve ser benchmark de experiência porque o usuário pode optar pelo caminho wallet→Binance→BRL/PIX.

**Regra:** Binance só vira `ExitVenue` operacional se houver integração comercial/API compatível. Até lá, é benchmark, não capability.

### 30.3 Foxbit Infra / BRL0

Podem ser simultaneamente concorrentes em fluxos próprios e rails do Banvia. A arquitetura deve preservar essa coopetição.

## 31. KPIs

### Funil

- `WalletConnectionSuccessRate`
- `PortfolioReadSuccessRate`
- `EligiblePortfolioRatio`
- `QuoteSuccessRate`
- `QuoteToSignatureConversion`
- `SignatureToCompletionRate`

### Roteamento

- `MultiVenueCoverageRatio`
- `ProviderWinRate`
- `RouteGenerationLatency`
- `RouteSelectionChangeRate`
- `RouteFallbackRateBeforeCommit`
- `RankingReproducibilityRate`
- `RouteAdvantageRateVsBaseline`

`RouteAdvantageRateVsBaseline` mede, em cenários comparáveis, quantas vezes a rota Banvia melhora payout, tempo ou fricção contra uma rota de referência definida, sem afirmar superioridade universal.

### Identidade e fricção

- `KycAlreadyVerifiedRate`: percentual de rotas recomendadas em venues onde o usuário já estava verificado;
- `NewKycRequiredRate`: percentual de intents elegíveis que exigiriam ao menos um novo KYC para acessar a rota de maior payout;
- `KycAvoidedByRoutingRate`: percentual de intents em que a recomendação evitou novo KYC com perda econômica abaixo do limiar/política configurada;
- `KycCompletionRate`: percentual de jornadas de KYC iniciadas que chegam a `APPROVED`;
- `KycMedianTimeToApproval`: mediana do tempo de aprovação quando mensurável;
- `RouteChoiceAfterKycDisclosure`: distribuição entre recomendado, maior payout, mais rápido e menor fricção depois que o trade-off é mostrado.

### Operação

- `WalletToPixP50/P95/P99`
- `VenueAcceptanceRate`
- `VenueFailureRate`
- `SettlementMismatchRate`
- `AutoReconciliationRate`
- `SupportContactsPer1000Orders`

### Economia

- `NetTakeRate`
- `ContributionMarginPerOrder`
- `AverageAllInCost`
- `RevenueShareByVenue`
- `RouteAdvantageAverage`
- `RouteAdvantageMedian`
- `UserNetAdvantageAverage`
- `UserNetAdvantagePositiveRate`
- `BanviaFeePerOrder`
- `FeeToRouteAdvantageRatio`
- `BaselineStillBestRate`

`UserNetAdvantagePositiveRate` é um KPI central para testar se a microfee se paga economicamente para o usuário. Se a taxa frequentemente elimina a vantagem descoberta pelo Route Engine, o modelo de monetização ou a própria tese de roteamento precisa ser revisto.

### North Star

> **GMV elegível liquidado via PIX pela rota recomendada, dentro do SLA, reconciliado, sem incidente de segurança e com resultado líquido coerente com a política de fee apresentada ao usuário.**

## 32. Gates de produção

- parecer jurídico sobre papel do Banvia Ramp;
- contrato com pelo menos um exit venue;
- responsabilidades KYC/KYT/AML/Travel Rule definidas;
- modelo de KYC por venue classificado como provider-owned, embedded ou reusable/delegated;
- política de KYC Graph, retenção mínima e expiração/reverificação definida;
- ranking testado com cenário “maior payout exige novo KYC”;
- sandbox validado;
- runbooks de incidentes;
- reconciliação operacional;
- observabilidade ponta a ponta;
- threat model revisado;
- LGPD/privacy review;
- limites conservadores;
- suporte/escalonamento com venues;
- política de ranking aprovada;
- disclosure de fees/revenue share compatível com produto e jurídico.
- política de microfee versionada, testada e incluída no cálculo all-in;
- baseline e metodologia de `RouteAdvantage`/`UserNetAdvantage` definidas antes de claims econômicos.

## 33. Roadmap recomendado

### Fase 0 — Viabilidade comercial/jurídica

Foxbit Infra, BRL0 e terceiro candidato. Mapear Onramper e Binance. Validar contratos, APIs, self-custody, DEX-origin, KYC/KYT e economia.

### Fase 1 — Banvia Ramp sem dinheiro real

Wallet Standard, portfolio discovery, eligible balance, Route Graph, timeline mockada e KYC Graph simulado para testar fricção sem coletar documentos.

### Fase 2 — Primeiro Exit Venue

SOL/USDC→venue→PIX com usuários controlados, tracking e reconciliação reais. Começar com provider-owned KYC e registrar o estado por venue; não tentar KYC universal.

### Fase 3 — Segundo Exit Venue

Quote normalization, capability matrix, score, health/circuit breaker, KYC Graph e ranking reproduzível. Validar UX de trade-off entre maior payout e menor fricção.

### Fase 4 — Benchmark competitivo

Comparar Banvia Ramp com fluxo manual/Binance onde possível: payout, tempo, passos, KYC, confiança e taxa de conclusão. O benchmark deve calcular `RouteAdvantage` antes da fee e `UserNetAdvantage` depois da microfee. Uma rota só deve ser divulgada como economicamente superior quando a vantagem permanecer positiva após todos os custos.

### Fase 5 — Long-tail

Jupiter + Eligibility + allowlist + limites conservadores + recuperação pós-swap.

### Fase 6 — B2B

Banvia Connect/API, hosted checkout, SDK e distribuição em wallets/dApps. Somente depois de acordos comerciais/jurídicos, avaliar embedded KYC e reutilização/delegação entre venues.

## 34. Definição operacional da tese

O Banvia Ramp está cumprindo sua proposta quando o usuário consegue expressar uma intenção simples:

> **“Quero transformar parte do patrimônio desta wallet em reais no meu PIX.”**

E o sistema consegue, sem exigir que ele selecione manualmente DEX, stablecoin, exchange ou provider:

1. descobrir o patrimônio;
2. identificar o que é liquidável;
3. gerar rotas elegíveis;
4. calcular o resultado all-in de cada rota;
5. aplicar a política de microfee e calcular o `UserNetOutcome`;
6. comparar a vantagem frente a uma baseline quando aplicável;
7. selecionar a melhor rota integrada conforme política auditável;
8. pedir somente as autorizações necessárias;
9. acompanhar a execução;
10. provar a liquidação;
11. reconciliar toda a cadeia.

Essa é a fronteira do produto:

```text
WALLET
  ↓
BANVIA RAMP
  ↓
MELHOR ROTA INTEGRADA E EXECUTÁVEL
  ↓
PIX
```

**Banvia Ramp não precisa vencer os grandes players em liquidez. Precisa usar a competição entre eles — quando integráveis — para reduzir a complexidade e melhorar a saída do usuário.**

**A microfee Banvia só é defensável quando o valor líquido entregue ao usuário permanece competitivo. Se a nossa taxa consumir a vantagem que o Route Engine encontrou, a ferramenta deixou de justificar economicamente sua própria existência naquela operação.**

## 35. Checklist de System Design antes de considerar um fluxo pronto

Um fluxo que movimenta ou coordena dinheiro **não está pronto** apenas porque o happy path funciona. Antes de produção/controlled pilot, responder e testar:

1. Se `POST /orders` for enviado duas vezes, nasce uma ou duas operações financeiras?
2. Se o provider executar e a resposta HTTP der timeout, como Banvia descobre o resultado sem executar novamente?
3. Se o mesmo webhook chegar 10 vezes, o efeito continua único?
4. Se webhooks chegarem fora de ordem, as invariantes da state machine continuam válidas?
5. Se o backend cair imediatamente depois da confirmação on-chain, a Order retoma sem repetir o swap/transferência?
6. Se duas threads/processos tentarem avançar a mesma Order, somente uma vence legitimamente?
7. Se a quote expirar enquanto o usuário assina, o commit irreversível é bloqueado/revalidado?
8. Se Banvia e exit venue discordarem, existe estado de divergência e reconciliação em vez de sobrescrita silenciosa?
9. Se a Order estiver `COMPLETED`, existe evidência reproduzível do settlement?
10. Se a operação ficar em `UNKNOWN`, existe runbook de recuperação?
11. Cada retry externo foi classificado como seguro/idempotente?
12. Todos os cálculos monetários possuem escala e arredondamento explícitos?
13. É possível reconstruir a linha do tempo por `orderId` sem depender de memória humana?
14. Existe teste automatizado para timeout, duplicate webhook, out-of-order, crash/restart e provider indisponível?
15. O sistema degrada sem inventar uma “melhor rota” quando venues ficam indisponíveis?

**Definition of Done financeira:** happy path + duplicate path + timeout path + unknown-result path + crash/recovery path + reconciliation path precisam estar cobertos antes de aumentar limites.

### Checklist econômico adicional

Antes de considerar o fluxo comercialmente pronto, responder e testar:

- A fee Banvia está presente em todas as quotes, telas e recibos relevantes?
- O mesmo `feePolicyVersion` pode reproduzir o valor cobrado posteriormente?
- A baseline usada para justificar economia é realmente comparável?
- A recomendação econômica permanece melhor depois da fee?
- O sistema consegue dizer “sua rota atual continua melhor” sem forçar execução Banvia?
- Revenue share e rebates estão separados da pontuação objetiva?
- Arredondamento monetário consegue alterar falsamente o sinal de `UserNetAdvantage`?
- Tickets pequenos continuam com contribution margin aceitável sem destruir valor para o usuário?

### 35.1 Checklist de identidade antes de considerar o routing pronto

1. O sistema sabe diferenciar “usuário já verificado” de “venue exige novo KYC”?
2. O status é por venue/escopo e possui validade quando aplicável?
3. A rota de maior payout e a rota recomendada podem ser diferentes e a UI explica por quê?
4. O Route Engine registra a ação de KYC necessária antes de qualquer commit irreversível?
5. O usuário consegue escolher conscientemente uma rota com maior payout mesmo que exija novo KYC?
6. O sistema evita copiar documentos entre providers sem base contratual/jurídica?
7. Embedded KYC informa claramente quem recebe/controla os dados?
8. Reuso de KYC só aparece quando o venue efetivamente o aceita?
9. `APPROVED` pode expirar ou exigir reverificação sem corromper ordens antigas?
10. O KYC Graph pode ser reconstruído/auditado sem guardar mais dados pessoais do que o necessário?

**North Star de identidade (aspiracional):** “uma wallet, um KYC, qualquer rota”. Não tratar como requisito do MVP nem como promessa comercial até existir suporte contratual real.

## 36. Guia de estudo técnico aplicado ao Banvia Ramp

### Livro principal

**KLEPPMANN, Martin; RICCOMINI, Chris. _Designing Data-Intensive Applications_. 2ª ed. O'Reilly Media, 2026.**

Leitura prioritária em paralelo ao código:

| Ordem | Capítulo | Aplicação direta no Banvia |
|---|---|---|
| 1 | 1 — Trade-Offs in Data Systems Architecture | system of record, derived data, escolha consciente da arquitetura |
| 2 | 2 — Defining Nonfunctional Requirements | confiabilidade, latência, disponibilidade e operabilidade |
| 3 | 5 — Encoding and Evolution | APIs, webhooks, DTOs e evolução de contratos |
| 4 | 8 — Transactions | ACID local, isolamento, concorrência e exactly-once como problema |
| 5 | 9 — The Trouble with Distributed Systems | falhas parciais, timeouts, redes e relógios |
| 6 | 10 — Consistency and Consensus | consistência e limites das garantias distribuídas |
| 7 | 12 — Stream Processing | eventos, outbox/inbox, consumers e processamento contínuo |
| 8 | 13 — A Philosophy of Streaming Systems | correção end-to-end e raciocínio sobre estado derivado |

**Método recomendado:** cada capítulo lido deve gerar pelo menos uma decisão de arquitetura, teste de falha, ADR, runbook ou melhoria de código no Banvia. Não tratar DDIA como leitura abstrata.

### Princípio para o desenvolvimento com agentes

Claude/Codex podem acelerar implementação, geração de testes e revisão, mas código de fluxo financeiro deve ser validado contra as invariantes deste documento. **“Compila” e “o teste feliz passou” não são critérios suficientes para movimentação financeira.**



## 37. Glossário técnico e de produto

> **Regra de manutenção:** este glossário não substitui a explicação na primeira ocorrência. Toda nova sigla ou termo relevante introduzido em código, ADR, requisito, runbook ou documentação deve ganhar definição curta em português.

- **ACID** — conjunto clássico de propriedades de uma transação local: atomicidade, consistência, isolamento e durabilidade. Não implica transação global entre Banvia, blockchain, provider e PIX.
- **Adapter** — implementação que traduz um sistema externo para um contrato interno estável do Banvia.
- **Aggregator (agregador)** — serviço que reúne múltiplos fornecedores/rotas sob uma única integração.
- **All-in** — visão de custo/resultado que soma todos os componentes materiais conhecidos da rota, em vez de olhar apenas uma taxa isolada.
- **Allowlist** — lista explícita de itens permitidos por política.
- **AML (Anti-Money Laundering)** — controles de prevenção à lavagem de dinheiro.
- **AMM (Automated Market Maker)** — mecanismo de mercado automatizado que usa pools e fórmulas/curvas para permitir swaps sem um livro de ofertas tradicional.
- **API (Application Programming Interface)** — contrato de software usado para integrar sistemas.
- **Asset Discovery** — processo de descobrir e normalizar os ativos existentes em uma wallet.
- **At-least-once** — modelo de entrega em que uma mensagem pode chegar mais de uma vez; exige consumidores idempotentes.
- **Baseline** — rota/cenário de referência usado para comparar se Banvia criou vantagem real.
- **Basis point (bp/bps)** — centésimo de ponto percentual; 100 bps = 1%.
- **B2B (business-to-business)** — produto vendido/integrado para outra empresa.
- **B2C (business-to-consumer)** — produto oferecido diretamente ao usuário final.
- **Best Executable Route** — melhor rota que é de fato integrada, disponível e elegível, não a melhor rota teórica do mercado inteiro.
- **Best User Outcome / User Net Outcome** — resultado final relevante para o usuário depois de custos e condições da rota, incluindo a fee Banvia.
- **Blockchain** — infraestrutura distribuída que registra estado/transações segundo regras de consenso.
- **Bridge** — protocolo que transfere/representa ativos ou mensagens entre blockchains; adiciona riscos próprios.
- **BRL** — código monetário do real brasileiro.
- **Bulkhead** — padrão de resiliência que isola recursos/falhas para impedir que um componente indisponível derrube todo o sistema.
- **Capability Matrix** — registro das capacidades, limites, ativos, redes, KYC e estado operacional de cada exit venue.
- **CEX (Centralized Exchange)** — exchange centralizada, operada por uma entidade que mantém infraestrutura e regras de negociação/custódia.
- **Circuit Breaker** — padrão que interrompe temporariamente chamadas a uma dependência com falhas repetidas para evitar cascata e recuperação insegura.
- **Commit irreversível** — ponto após o qual uma etapa não pode ser simplesmente desfeita, como uma transferência on-chain já finalizada.
- **Contribution Margin** — receita da operação menos custos variáveis diretamente atribuíveis àquela operação.
- **Correlation ID** — identificador usado para ligar logs, traces e eventos de uma mesma operação.
- **Custodial** — modelo em que uma entidade controla temporariamente ou permanentemente chaves/ativos/saldos em nome do usuário.
- **dApp (decentralized application)** — aplicação que interage com blockchain/smart contracts e, em geral, usa wallet para autorização do usuário.
- **Depeg** — perda relevante da referência de preço que uma stablecoin pretende manter.
- **Derived Data** — estado calculado a partir de fatos de uma ou mais fontes; pode ser reconstruído e não substitui a evidência original.
- **DEX (Decentralized Exchange)** — mecanismo/protocolo de negociação on-chain sem a custódia central típica de uma CEX.
- **DEX Router** — serviço que encontra/constrói rotas de swap entre fontes de liquidez; Jupiter é a primeira implementação prevista na Solana.
- **Eligibility Engine** — componente que decide se um ativo/rota pode ser usado e sob quais condições.
- **Embedded KYC** — jornada de KYC apresentada dentro da UX Banvia, embora a verificação possa continuar pertencendo a um provider/terceiro.
- **Exactly-once** — ideal de um efeito ocorrer exatamente uma vez. Em sistemas distribuídos externos, Banvia não deve assumir garantia global; deve combinar idempotência, evidência e reconciliação.
- **Exit Venue** — entidade externa integrada que executa uma etapa de saída para fiat, como off-ramp direto, CEX ou agregador.
- **Fee Policy** — regra versionada que define como a taxa Banvia é calculada, arredondada, limitada e exibida.
- **Finality (finalização)** — grau em que uma transação blockchain é considerada definitiva segundo a rede.
- **Friction (fricção)** — esforço/custo não necessariamente monetário, como cadastro, KYC, espera, redirecionamento ou assinaturas adicionais.
- **Health** — visão operacional da saúde/disponibilidade de um provider/venue usada pelo roteamento.
- **Hosted Checkout** — fluxo de checkout hospedado por um fornecedor e aberto/embutido pelo integrador.
- **Idempotência** — propriedade de repetir a mesma intenção identificada sem produzir efeitos financeiros adicionais.
- **Idempotency Key** — chave única enviada numa operação para permitir que retries sejam reconhecidos como a mesma intenção.
- **Irreversibilidade** — característica de uma etapa que, depois de executada, não pode ser simplesmente cancelada.
- **Journal** — registro imutável/append-only de eventos econômicos e operacionais relevantes para auditoria/reconciliação.
- **KPI (Key Performance Indicator)** — indicador usado para medir desempenho do produto/operação.
- **KYC (Know Your Customer)** — identificação e verificação do cliente.
- **KYC Graph** — mapa lógico do status de verificação do usuário em cada venue/escopo; não é um depósito de documentos.
- **KYB (Know Your Business)** — identificação e verificação de uma empresa/parceiro.
- **KYT (Know Your Transaction)** — análise de risco de transações, endereços e origem/destino de ativos.
- **Journal de Transações** — registro estruturado e imutável de eventos e payouts probatórios da Banvia (modelo 100% não-custodial).
- **Leg** — etapa individual de uma rota, como swap, transferência, off-ramp ou payout PIX.
- **LGPD** — Lei Geral de Proteção de Dados Pessoais brasileira.
- **Liquidity (liquidez)** — capacidade de comprar/vender um ativo sem perda excessiva de preço ou dificuldade de execução.
- **Long-tail asset** — ativo fora dos pares de maior liquidez/capitalização, normalmente com execução e risco mais incertos.
- **Market cap** — preço unitário multiplicado pela oferta circulante; não equivale ao dinheiro que poderia ser sacado sem impacto.
- **MEV (Maximal Extractable Value)** — valor que participantes da cadeia podem extrair alterando inclusão/ordem de transações.
- **Microfee** — taxa transacional pequena e explícita cobrada pelo Banvia e incluída no cálculo all-in.
- **Mint / mint address** — endereço que identifica tecnicamente um token na Solana.
- **Moat** — vantagem competitiva difícil de reproduzir; no Banvia, dados operacionais podem virar moat futuro, mas não são promessa inicial.
- **Non-custodial** — modelo em que Banvia não controla a chave privada do usuário.
- **Off-chain** — processamento que ocorre fora da blockchain.
- **Off-ramp** — processo de converter criptoativo em moeda fiduciária.
- **On-chain** — estado/transação registrada ou executada na blockchain.
- **On-ramp** — processo de converter moeda fiduciária em criptoativo.
- **Optimistic Locking** — controle de concorrência que detecta atualização concorrente por versão, evitando sobrescrita silenciosa.
- **Orchestration (orquestração)** — coordenação de múltiplos sistemas/fornecedores para completar uma intenção ponta a ponta.
- **P50/P95/P99** — percentis de uma distribuição de latência/tempo; P95 significa que 95% das observações ficaram até aquele valor.
- **Partial Failure (falha parcial)** — situação em que uma parte do fluxo falha ou fica incerta enquanto outras partes continuam/foram concluídas.
- **Payout** — valor/transferência entregue ao beneficiário na saída, aqui normalmente em BRL via PIX.
- **PIX E2E** — identificador end-to-end associado à transação PIX e útil como evidência de liquidação.
- **Pool de liquidez** — reserva de ativos usada por um protocolo para permitir swaps/negociação.
- **Prefunding** — saldo previamente disponibilizado para permitir liquidação rápida antes de receber/compensar integralmente o fluxo correspondente.
- **Price Impact** — alteração do preço causada pelo tamanho da própria ordem em relação à liquidez disponível.
- **Provider** — fornecedor externo especializado; no Banvia, pode ser um subtipo de Exit Venue.
- **Provider-neutral** — princípio de não favorecer um fornecedor por acoplamento técnico ou comissão escondida.
- **Provider-owned KYC** — KYC executado/pertencente ao próprio exit venue, ainda que Banvia conduza a jornada.
- **Quote (cotação)** — proposta temporária com valor, custos, validade e condições de execução.
- **Rail** — infraestrutura/canal usado para mover ou liquidar valor, como PIX ou um rail cripto/fiat de provider.
- **Reconciliation (reconciliação)** — comparação sistemática entre registros Banvia e fontes externas para provar, corrigir ou escalar divergências.
- **Reusable/delegated KYC** — KYC aceito/reutilizado por outros venues ou executado por uma parte delegada mediante acordos formais.
- **Retry** — nova tentativa de uma operação após falha; só é segura quando a semântica/idempotência é conhecida.
- **Revenue Share** — divisão de receita entre parceiros; não deve alterar ocultamente o ranking.
- **Route / RouteCandidate** — rota é o caminho completo; RouteCandidate é uma alternativa concreta avaliada pelo motor antes da seleção.
- **Route Advantage** — diferença entre o resultado líquido da melhor rota integrada antes da fee Banvia e uma baseline comparável.
- **Route Engine** — componente que gera, filtra, pontua e seleciona rotas executáveis.
- **Route Graph** — representação estruturada da rota completa por legs, custos, estados, TTLs e pontos de irreversibilidade.
- **RPC (Remote Procedure Call)** — interface/chamada remota usada, entre outros casos, para consultar/enviar dados a nós blockchain.
- **Sandbox** — ambiente de teste fornecido por um parceiro, separado da produção e normalmente sem dinheiro real.
- **SDK (Software Development Kit)** — conjunto de bibliotecas/ferramentas oferecido para facilitar uma integração.
- **Self-custody** — custódia em que o usuário controla suas próprias chaves/credenciais.
- **Settlement (liquidação)** — conclusão financeira de uma obrigação/transferência segundo o rail aplicável.
- **Shared Savings** — modelo em que Banvia captura parte da economia mensurada que ajudou a criar.
- **Slippage** — diferença entre preço esperado no momento da cotação e preço efetivamente executado.
- **SLA (Service Level Agreement)** — compromisso contratual de nível de serviço; não deve ser confundido com uma mera estimativa histórica.
- **Spread** — diferença/margem entre preços de compra e venda ou entre referência de mercado e cotação oferecida.
- **Stablecoin** — token desenhado para acompanhar uma referência relativamente estável, como uma moeda fiduciária; estabilidade não significa ausência de risco.
- **State Machine (máquina de estados)** — modelo que limita quais transições de estado de uma Order são válidas.
- **System Design** — disciplina de projetar arquitetura, dados, falhas, consistência, escalabilidade e operação do sistema como um todo.
- **System of Record** — fonte autoritativa de determinado fato dentro de sua fronteira.
- **Ticker** — símbolo curto de um ativo, como SOL; não é identificador técnico suficiente para decisões financeiras.
- **Threat Model** — análise estruturada de ativos, atacantes, fronteiras de confiança, ameaças e controles.
- **Tokenomics** — regras econômicas de emissão, oferta, incentivos, distribuição e fluxo de um token.
- **Travel Rule** — conjunto de obrigações de compartilhamento de determinadas informações entre participantes em transferências de ativos virtuais, conforme regulação aplicável.
- **TTL (Time To Live)** — tempo de validade de uma quote/rota/dado antes de precisar ser revalidado.
- **Unit Economics** — economia de uma unidade de negócio/transação: receitas, custos variáveis e margem.
- **User Net Advantage** — vantagem que permanece para o usuário depois de descontar a fee Banvia da vantagem de rota.
- **Wallet** — software/carteira que gerencia contas/chaves e apresenta autorizações ao usuário; os tokens permanecem registrados na blockchain.
- **Wallet Standard** — padrão de interoperabilidade/discovery de wallets no ecossistema Solana.
- **Wallet-to-PIX** — jornada que começa no patrimônio de uma wallet e termina em liquidação via PIX.
- **Webhook** — chamada HTTP enviada por um sistema externo para notificar eventos/estados de forma assíncrona.
- **Webhook Inbox** — registro de entrada usado para deduplicar e processar webhooks de forma idempotente.
- **Workflow durável** — processo persistente que consegue retomar após crash/restart sem esquecer etapas já executadas.
- **Customer of Record** — entidade que mantém a relação formal/comercial principal com o cliente em determinada operação, conforme contrato; não deve ser inferida apenas pela UX.
- **DTO (Data Transfer Object)** — estrutura usada para transportar dados entre fronteiras/APIs; DTO de provider não deve vazar para o domínio central.
- **EVM (Ethereum Virtual Machine)** — ambiente de execução usado por Ethereum e redes compatíveis; é citado apenas para contraste arquitetural com Solana.
- **Funding / Funding Instructions** — etapa e instruções para enviar/associar os criptoativos ao exit venue que executará a saída.
- **HMAC (Hash-based Message Authentication Code)** — mecanismo criptográfico usado para validar autenticidade/integridade de mensagens, comum em assinatura de webhooks.
- **HTTP** — protocolo de comunicação utilizado por APIs web.
- **LP (Liquidity Provider)** — participante que fornece ativos a uma pool/venue para permitir negociação/liquidez.
- **MFA (Multi-Factor Authentication)** — autenticação que exige dois ou mais fatores independentes.
- **MPC (Multi-Party Computation)** — técnica criptográfica que distribui uma computação/segredo entre múltiplas partes sem reunir o segredo completo em um único ponto.
- **MVP (Minimum Viable Product)** — produto mínimo viável: menor versão capaz de testar hipóteses centrais com usuários/integrações reais ou controladas.
- **Onboarding** — jornada de entrada/cadastro/ativação do usuário ou parceiro, podendo incluir KYC/KYB.
- **Order** — entidade interna Banvia que representa uma intenção de saída e seu workflow persistente; não é sinônimo da ordem externa do provider.
- **PII (Personally Identifiable Information)** — informação capaz de identificar uma pessoa; deve receber controles de privacidade e segurança compatíveis.
- **PSP (Payment Service Provider)** — prestador de serviços de pagamento.
- **Ranking / Score** — ranking é a ordenação das RouteCandidates; score é a pontuação calculada por uma política versionada para ajudar nessa ordenação.
- **REST** — estilo arquitetural comum para APIs HTTP orientadas a recursos.
- **RFQ (Request for Quote)** — solicitação de cotação a um ou mais participantes/venues para obter preço executável.
- **SPL / Token Program** — conjunto de padrões/programas do ecossistema Solana usados para representar e movimentar tokens fungíveis e outros ativos.
- **SOR (Smart Order Routing)** — lógica que distribui/seleciona caminhos de execução entre múltiplas fontes de liquidez ou venues.
- **TAM (Total Addressable Market)** — estimativa do mercado total endereçável; não deve ser confundida com volume realmente capturável pelo Banvia.
- **TLS (Transport Layer Security)** — protocolo de segurança que protege comunicações em trânsito, normalmente via HTTPS.
- **UI / UX** — UI (User Interface) é a interface visual/interativa; UX (User Experience) é a experiência completa do usuário ao realizar uma tarefa.
- **WebSocket** — protocolo de comunicação bidirecional persistente usado para receber/enviar atualizações em tempo real.
- **RBAC (Role-Based Access Control)** — controle de acesso baseado em papéis/funções.
- **CSP (Content Security Policy)** — política de segurança do navegador que restringe fontes de scripts/conteúdo e reduz certas classes de ataque.
- **CRUD** — operações básicas de Create, Read, Update e Delete; Banvia não deve ser modelado apenas como CRUD porque coordena workflows distribuídos e efeitos externos.
