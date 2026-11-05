# Banvia — MVP v1

**Status:** documento canônico do MVP — agosto de 2026
**Relação com a v4.0:** a [v4.0](requisitos-banvia-v4.0.md) permanece válida como
**arquitetura-alvo**. Este documento define o que entra na **primeira versão**.
**Decisão que o originou:** [ADR 0007](adr/0007-reducao-de-escopo-do-mvp.md)

---

## 1. Objetivo

A Banvia se define primeiro pelo que recusa ser. Não somos corretora, não somos
custodiante, não somos emissora de stablecoin e muito menos uma exchange
descentralizada. Essa recusa não é modéstia de escopo nem etapa a ser superada com o
amadurecimento do produto — ela é constitutiva. A decisão registrada na
[ADR 0005](adr/0005-offramp-router-provider-first.md) estabelece que a atividade

regulada — conhecimento do cliente, análise de risco de transação, prevenção à lavagem,
custódia, provisão de liquidez e liquidação em PIX — permanece integralmente com o venue
contratado.

Portanto, somos uma camada de orquestração. Expomos uma interface programática com
quatro operações — cotar, criar um payout, consultar o seu estado e receber notificações
— por meio da qual uma empresa envia stablecoin e determina que um pagamento em reais
seja realizado no Brasil via PIX.

> Permitir que uma empresa envie stablecoin e realize um payout via PIX no Brasil
> através de uma única API, sem precisar integrar diretamente com cada provedor de
> off-ramp.

Por baixo dessa interface operam dois provedores regulados, e o cliente não precisa
saber qual deles executou a operação — nem que existem dois. O nosso produto é um só:
oferecer uma interface estável enquanto os provedores por baixo podem mudar.

### 1.1 Contextualização

Uma stablecoin é uma criptomoeda criada para manter o preço relativamente estável,
normalmente acompanhando o valor de algum ativo real, como o dólar. Como exemplo, temos
duas stablecoins atreladas ao dólar, que possuem por trás uma ideia simples: 1 token se
manter próximo do valor de US$ 1. São elas a **USDT** (Tether USD), emitida pela empresa
Tether e uma das stablecoins mais usadas no mundo, especialmente em exchanges e mercados
de cripto — podemos pensar nela como um "dólar digital" usado dentro de blockchains; e a
**USDC** (USD Coin), emitida pela Circle, que busca manter o valor de US$ 1 por token e é
muito utilizada em fintechs, DeFi, pagamentos e integrações institucionais.

Vale sempre lembrar que o valor do Bitcoin pode variar bastante, enquanto uma stablecoin
tenta permanecer próxima do valor de 1 dólar. Stablecoins circulam em blockchains.

O MVP opera com **USDC apenas** — ver seção 5. A USDT aparece aqui como contexto de
mercado, não como escopo.

---

## 2. O problema

Uma wallet, fintech, exchange ou plataforma Web3 que queira oferecer `USDC → BRL → PIX`
precisa integrar diretamente com um provedor de off-ramp. Isso significa lidar com:

API específica · autenticação própria · formato próprio de cotação · formato próprio de
payout · webhooks específicos · códigos de erro diferentes · estados diferentes ·
idempotência · reconciliação · comprovante de pagamento · disponibilidade do provedor ·
manutenção da integração

E, se um dia quiser trocar de provedor, boa parte desse trabalho se repete.

A Banvia remove esse acoplamento.

---

## 3. A proposta

```text
Cliente
   │
   │ API Banvia
   ▼
BANVIA
   │
   ├── Provider A
   │
   └── Provider B
          │
          ▼
         PIX
```

O cliente não precisa conhecer a API que roda por baixo. Para ele existe somente:

```text
USDC → PIX
```

---

## 4. Caso de uso

Um fluxo principal: **receber stablecoin e entregar reais via PIX a um beneficiário
brasileiro.**

```text
Wallet
   │
   │ 1000 USDC
   ▼
Banvia
   │
   ▼
Provider
   │
   ▼
BRL
   │
   ▼
PIX
```

---

## 5. Escopo

| | |
|---|---|
| Stablecoin | **USDC** |
| Rede | **uma só** — Polygon ou Solana |
| Moeda de destino | **BRL** |
| Método de payout | **PIX** |
| Mercado | **Brasil** |

A escolha da rede é determinada por **quais redes os primeiros provedores integrados
suportam** — não por preferência técnica.

---

## 6. API

Quatro operações.

### Cotação

```http
POST /v1/quotes
```

```json
{ "asset": "USDC", "network": "polygon", "amount": "1000" }
```

```json
{
  "quoteId": "qt_123",
  "sourceAmount": "1000",
  "sourceAsset": "USDC",
  "destinationCurrency": "BRL",
  "destinationAmount": "5312.20",
  "fee": "31.80",
  "expiresAt": "2026-08-27T14:05:00Z"
}
```

O cliente não precisa saber qual provider gerou a cotação.

### Criar payout

```http
POST /v1/payouts
Idempotency-Key: 6df712...
```

```json
{
  "quoteId": "qt_123",
  "beneficiary": {
    "name": "Maria Silva",
    "document": "12345678900",
    "pixKey": "maria@email.com"
  }
}
```

```json
{
  "payoutId": "po_456",
  "status": "AWAITING_FUNDS",
  "deposit": { "asset": "USDC", "network": "polygon", "address": "0x..." }
}
```

### Consultar payout

```http
GET /v1/payouts/{payoutId}
```

```json
{
  "payoutId": "po_456",
  "status": "SETTLED",
  "destinationAmount": "5312.20",
  "currency": "BRL"
}
```

### Webhook

```text
payout.awaiting_funds
payout.funds_received
payout.processing
payout.settled
payout.failed
```

```json
{
  "event": "payout.settled",
  "payoutId": "po_456",
  "timestamp": "2026-08-27T14:03:21Z"
}
```

### 6.1 Regras da API

Valores monetários trafegam como **string decimal**, nunca número JSON — precisão morre
na fronteira com JavaScript.

Documento e chave PIX do beneficiário **nunca** voltam em claro na resposta.

---

## 7. Modelo de estados

A Banvia normaliza os estados dos diferentes provedores. Externamente:

```text
CREATED
   ↓
AWAITING_FUNDS
   ↓
FUNDS_RECEIVED
   ↓
PROCESSING
   ↓
SETTLED
```

Excepcionais:

```text
FAILED
EXPIRED
CANCELLED
UNKNOWN
```

Mesmo que um provider use `pending / approved / paid / completed` e outro use
`created / crypto_received / payout_processing / done`, a Banvia converte ambos para o
mesmo modelo. **Essa normalização já é parte relevante do produto.**

### 7.1 `UNKNOWN` — por que ele existe

Quando uma chamada ao provider expira sem resposta, três coisas podem ter acontecido: a
requisição não chegou, chegou e falhou, ou chegou e teve sucesso com a resposta perdida
no caminho. As três são indistinguíveis do nosso lado.

Sem um estado para isso, o sistema é obrigado a escolher entre `FAILED` e `SETTLED` — e
errar significa pagar duas vezes ou não pagar.

`UNKNOWN` é resolvido por **consulta ao provider**, nunca por nova tentativa cega.

### 7.2 O ponto irreversível

Assim que o endereço de depósito é emitido, o payout está comprometido com aquele
provider. **Não existe failover depois disso** — os fundos vão para o endereço dele.

Consequência de desenho: a escolha de provider acontece na **criação do payout**, antes
de emitir o endereço. Nunca depois.

### 7.3 `SETTLED` exige comprovante

O comprovante de pagamento está na lista de dores da seção 2 — é uma das coisas que o
cliente teria de resolver sozinho. Então ele é entregável, não detalhe.

`SETTLED` só é declarado com o **E2E do PIX** registrado. Webhook de sucesso do provider
sem E2E mantém o payout em `PROCESSING`.

---

## 8. Idempotência

Payout é operação financeira. `Idempotency-Key` é suportada desde a primeira versão.

```http
Idempotency-Key: 6df712...
```

Se o cliente chamar duas vezes por timeout ou retry, a Banvia **não** realiza dois PIX.

- mesma chave, payload equivalente → devolve o mesmo payout
- mesma chave, payload diferente → `409`, nunca um segundo payout
- a garantia **persiste além de restart e deploy** — a chave vive no banco, não em
  memória

Isso faz parte do core do MVP.

---

## 9. Providers

Dois. Não dez.

Com um só:

```text
Cliente → Banvia → Provider A
```

somos praticamente um wrapper. Com dois:

```text
                   ┌→ Provider A
Cliente → Banvia ──┤
                   └→ Provider B
```

já demonstramos desacoplamento — que é a tese.

Cada provider fica atrás de um **adapter**. Nenhum objeto ou biblioteca de fornecedor
entra no domínio. Evento de provider que não reconhecemos é **persistido para análise**,
nunca tratado como sucesso.

---

## 10. Routing

Sem smart routing. Regra de disponibilidade:

```text
Provider A disponível?
       │
      sim
       │
       ▼
usar Provider A
```

```text
Provider A indisponível
       │
       ▼
Provider B
```

Depois podemos adicionar melhor preço, taxa de sucesso, latência, limites, liquidez e
rede. Isso é V3, não faz parte da primeira validação.

Uma coisa que **entra desde já**, porque é barata agora e cara depois: registrar qual
provider foi escolhido e por quê. Mesmo que o "porquê" seja sempre "A estava
disponível", o campo precisa existir — é o que torna a evolução para V3 possível sem
reescrita.

---

## 11. Fora do MVP

marketplace público de venues · dezenas de providers · smart routing sofisticado · IA
para roteamento · múltiplas stablecoins · múltiplas moedas fiduciárias · pagamentos
internacionais · cartões · boleto · on-ramp · carteira própria · custódia complexa ·
blockchain própria · token Banvia · protocolo descentralizado · dashboard avançado ·
analytics sofisticado · engine de liquidez · otimização automática de spread · múltiplos
países

**Também fora, e vale dizer em voz alta:** ledger de dupla entrada e custódia.
A Banvia é 100% não-custodial e nunca manterá saldo de cliente nem tesouraria
própria. Um registro sequencial e imutável de eventos por payout (journal
probatório) basta.

Isso não é afrouxamento de disciplina, é mudança de natureza dela. A correção que nos
governa é **probatória, não contábil** — precisamos demonstrar o que aconteceu com cada
payout, não conciliar posições patrimoniais. Partidas dobradas resolvem o segundo
problema, e nós não o temos.

---

## 12. O verdadeiro produto

O produto não é "converter USDC para reais" — providers já fazem isso.

O produto é:

> oferecer uma interface estável enquanto os provedores por baixo podem mudar.

Hoje:

```text
Wallet → Provider A
```

Amanhã:

```text
Wallet → BANVIA → Provider A
                → Provider B
```

E depois:

```text
Wallet → BANVIA → Provider A
                → Provider B
                → Provider C
                → Provider D
                → Provider E
```

**A API do cliente permanece a mesma.**

### 12.1 O valor não aparece em um payout isolado

Considerado um pagamento único, a integração direta com o provider é equivalente ou
superior à nossa: tem menos saltos, menos partes e nenhuma taxa intermediária. Quem
comparar as duas em um payout só não vai encontrar diferença a nosso favor — e essa
comparação é a primeira que um cliente técnico faz.

O valor da abstração é **diacrônico**: realiza-se ao longo do tempo, à medida que
providers são adicionados, substituídos, degradam ou encerram operação, enquanto o
código do cliente permanece o mesmo. Mede-se na derivada, não no ponto.

Daí decorre a objeção comercial que a
[ADR 0007](adr/0007-reducao-de-escopo-do-mvp.md) já registra: sem seleção por preço, a
fee da seção 15 aparece na proposta como custo puro, sem contrapartida imediatamente
mensurável. O argumento a favor é o **custo de integração evitado** — real, e difícil de
colocar numa planilha. Convém saber disso antes da primeira conversa comercial, não
durante.

### 12.2 O núcleo técnico é o tratamento da incerteza

Não há cálculo relevante no MVP. O routing é um `if` (seção 10) e a cotação vem do
provider. O que exige engenharia é outra coisa: **o que o sistema faz quando não sabe o
que aconteceu.**

Quatro compromissos sustentam isso:

| Compromisso | Onde | O que impede |
|---|---|---|
| Idempotência que sobrevive a restart e deploy | seção 8 | retry virar PIX duplicado |
| Estado de resultado desconhecido | 7.1 | o sistema chutar entre pagar duas vezes e não pagar |
| Evidência para concluir | 7.3 | declarar liquidado por inferência |
| Fronteira de adapters | seção 9 | acoplamento ao fornecedor e evento estranho lido como sucesso |

Nenhum dos quatro acelera o MVP se for retirado, e cada um custa reescrita depois. Foi
por isso que sobreviveram ao corte de escopo.

---

## 13. Hipótese a validar

> Empresas que precisam realizar payouts stablecoin → PIX valorizam suficientemente a
> abstração dos provedores para integrar com uma camada intermediária como a Banvia.

Não precisamos provar ainda que conseguimos construir uma rede global. Precisamos
descobrir se alguém diz:

> "Sim. Eu prefiro integrar uma vez com vocês em vez de manter essas integrações
> sozinho."

---

## 14. Cliente ideal

Empresas que já têm usuários cripto e precisam enviar dinheiro para o Brasil:

wallets · fintechs cripto · exchanges menores · plataformas Web3 · empresas de remessas ·
payroll em stablecoin · plataformas internacionais pagando contractors brasileiros

Um caso especialmente interessante:

```text
Empresa estrangeira
        │
        │ USDC
        ▼
      Banvia
        │
        │ PIX
        ▼
Desenvolvedor brasileiro
```

---

## 15. Receita

Modelo simples, validado comercialmente.

**Recomendado — fee explícita:**

```text
Provider cobra: 0,60%
Banvia cobra:   0,15%
Custo total:    0,75%
```

**Alternativa — spread:** possível, com um custo que precisa ser considerado agora. A
partir da V3, quando o routing passar a escolher por preço, o cliente vai perguntar como
sabemos que escolhemos a melhor rota. Spread cobrado hoje enfraquece essa resposta
amanhã, porque cria incentivo para preferir o provider que permite mais margem.

Fee explícita mantém as duas histórias compatíveis. É a recomendação.

Futuro possível: fee fixa + percentual.

---

## 16. Métrica principal

Não é número de usuários, downloads ou contas criadas.

> **TPV — volume processado através da Banvia.**

Complementares:

```text
payout success rate
tempo médio de liquidação
provider failure rate
volume por provider
número de clientes B2B ativos
```

Uma métrica de disciplina: **taxa de resultado desconhecido** — quantos payouts pararam
em `UNKNOWN`. Se subir, é sinal de que a integração com algum provider está frágil.

Sobre tempo de liquidação: medir internamente desde já, **não publicar SLA** antes de ter
número de produção. A liquidação é do provider, não nossa.

---

## 17. Critério de sucesso

```text
3–5 clientes B2B
       ↓
realizando payouts reais
       ↓
através da mesma API
       ↓
com pelo menos 2 providers
```

Nesse momento começamos a ter evidência de que existe um produto.

---

## 18. Evolução

```text
MVP → USDC → PIX, 2 providers, regras simples
 ▼
V2  → mais providers, health checks, failover automático
 ▼
V3  → routing por preço, liquidez, success rate
 ▼
V4  → políticas customizadas por cliente
 ▼
V5  → marketplace / orchestration network
```

O smart routing é **consequência do crescimento, não premissa do produto**.

A [v4.0](requisitos-banvia-v4.0.md) descreve o destino dessa curva — `RouteScore`,
`RoutingPolicy` versionada, reconciliação multi-venue, multi-tenancy plena. Continua
válida como arquitetura-alvo, e é o que a V3 e a V4 implementam.

---

## 19. Posicionamento

Evitar:

> "A Banvia é uma plataforma revolucionária de smart liquidity routing para
> stablecoins."

Preferir:

> **Banvia é uma API para empresas enviarem stablecoin e realizarem payouts via PIX no
> Brasil.**

E, quando perguntarem o diferencial:

> **Você integra uma vez. Nós lidamos com os provedores por baixo.**

---

## 20. A pergunta que guia o projeto

Antes de adicionar qualquer funcionalidade:

> Isso torna mais simples para uma empresa realizar stablecoin → PIX sem depender
> diretamente de um único provider?

Se a resposta for não, provavelmente não pertence ao MVP.

---

## 21. Fronteira regulatória

Não é escopo de produto, é gate de produção — e por isso fica registrado aqui.

A Banvia **não** custodia ativo, não mantém saldo de cliente, não faz KYC, KYT ou AML e
não é PSP. Toda atividade regulada é executada pelo provider, sob a autorização dele.

Dois itens em aberto, ambos anteriores à primeira linha de código:

- **parecer jurídico sobre a Resolução BCB 561/2026**, vigente em 1º de outubro de 2026,
  que restringe ativo virtual como instrumento de liquidação no eFX
- **apetite de compliance de cada provider** quanto à origem dos fundos que pretendemos
  processar

Um "não" em qualquer dos dois muda o produto — e é mais barato descobrir agora.

A fronteira desta seção é a mesma recusa da seção 1, vista do lado operacional. Lá ela
define o que somos; aqui ela vira condição para ligar o sistema.

---

## 22. Estado de validação

Registrado para que o documento não seja lido como descrição do presente.

Nada do que está acima foi validado. A hipótese da seção 13 permanece por testar,
**nenhum provider foi contratado**, e a rede da seção 5 será determinada por quais redes
os primeiros providers suportarem — não por preferência técnica.

Os números de fee, prazo e latência que aparecem neste documento são **ilustrativos**.
Nenhum é medição. A seção 16 proíbe publicar SLA antes de haver número de produção.

Levantamentos sobre capacidade de terceiros vieram de material público e **não foram
verificados de forma independente**. Revalidar antes de decisão contratual ou
arquitetural irreversível.

Neste momento, a Banvia é uma tese documentada e um andaime de código. O estado real do
código está no [`README.md`](README.md) da documentação, e a distância entre as duas
coisas é exatamente o trabalho que falta.
