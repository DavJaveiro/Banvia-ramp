# ADR 0007 — Redução de escopo do MVP e adiamento do smart routing

**Data:** 27/08/2026
**Status:** Aceita
**Relação com a [ADR 0006](0006-inversao-b2b-api-first.md):** complementa, não reverte
**Escopo definido em:** [`requisitos-banvia-mvp-v1.md`](../requisitos-banvia-mvp-v1.md)

---

## Contexto

A ADR 0006 inverteu o cliente primário para a empresa que integra a API, e a
[v4.0](../requisitos-banvia-v4.0.md) detalhou o produto correspondente: normalização
multi-venue, `RouteScore` com seis fatores ponderados, `RoutingPolicy` versionada,
decisão reproduzível, reconciliação em cinco fontes, multi-tenancy plena.

Esse desenho está correto como destino. Como **primeira versão**, tem um problema que a
própria `product-strategy.md` §8.2 já registrava e que ficou sem resposta:

> O valor do Route Engine só existe com N ≥ 2 venues contratados e ativos. No dia 1
> teremos um. Com um venue, roteamento é adapter.

Ou seja: a v4.0 propunha construir, como núcleo do MVP, exatamente a peça cujo valor não
pode ser demonstrado no MVP. E a hipótese que ela pedia para validar — que a seleção de
rota entrega mais do que a integração direta — exige volume e histórico que só existem
depois.

Há uma hipótese anterior, mais barata e ainda não testada.

## Decisão

**A hipótese do MVP passa a ser a abstração, não a otimização.**

> Empresas que precisam realizar payouts stablecoin → PIX valorizam suficientemente a
> abstração dos provedores para integrar com uma camada intermediária como a Banvia.

Com isso:

**Escopo reduzido a um caminho.** USDC, uma rede, BRL, PIX, Brasil. Quatro operações de
API: cotação, criar payout, consultar payout, webhook.

**Routing passa a ser por disponibilidade.** Provider A disponível, usa A; indisponível,
usa B. Sem `RouteScore`, sem pesos, sem política versionada. Isso vai para a **V3**.

**Dois providers continuam sendo requisito** — mas para provar desacoplamento, não
melhor execução. Com um só, somos um wrapper.

**A v4.0 permanece vigente como arquitetura-alvo.** Ela descreve V3 e V4, não o MVP.

## O que permanece obrigatório mesmo no escopo reduzido

Quatro disciplinas não foram cortadas, e a razão de cada uma é a mesma: custam pouco
agora e custam reescrita depois.

- **Idempotência** com chave persistida além de restart. Sem ela, retry vira PIX
  duplicado.
- **Estado `UNKNOWN`** para timeout com resultado indeterminado. Sem ele, o sistema
  chuta entre pagar duas vezes e não pagar.
- **Evidência de liquidação** — `SETTLED` exige o E2E do PIX. O comprovante está na
  lista de dores que justificam o produto; entregá-lo por inferência seria não
  entregá-lo.
- **Adapter boundary** — nenhum DTO ou SDK de provider entra no domínio, e evento
  desconhecido é persistido, nunca tratado como sucesso.

Também permanece o registro de **qual provider foi escolhido e por quê**, mesmo quando o
porquê for sempre "estava disponível". É o campo que torna a V3 possível sem reescrita.

## Consequências

### Positivas

- A hipótese fica testável com dois providers e nenhum histórico de execução.
- O caminho até o primeiro payout real encurta de forma significativa.
- O discurso comercial fica honesto: "você integra uma vez" é verificável no dia 1;
  "escolhemos a melhor rota" não era.
- Remove a dependência circular entre ter volume para gerar dados e precisar de dados
  para justificar o produto.

### Negativas

- Perde-se o diferencial mais chamativo. "Integramos os providers por você" é um
  argumento mais fraco de defensabilidade — e mais fácil de copiar.
- Sem seleção por preço, a fee da Banvia aparece como custo puro para o cliente, sem
  contrapartida mensurável. O argumento passa a ser custo de integração evitado, que é
  real mas difícil de quantificar numa proposta.
- Um provider com escala pode oferecer a mesma abstração. A neutralidade continua sendo
  a defesa estrutural, mas ela fica menos visível quando não há comparação acontecendo.

### Neutras

- O modelo de rota como grafo deixa de ser exercitado no MVP, mas permanece no desenho
  de destino.
- Multi-tenancy plena da v4.0 §17 pode ser simplificada no MVP, desde que `tenantId`
  exista desde a primeira migration. Retrofit de isolamento é reescrita.

## Ponto de atenção: o depósito cria irreversibilidade

O `POST /v1/payouts` devolve um endereço de depósito e o payout aguarda em
`AWAITING_FUNDS`. Esse endereço pertence ao provider escolhido.

Consequência: **a partir da emissão do endereço não existe failover.** Os fundos vão
para aquele provider.

A escolha de provider portanto acontece na criação do payout, antes de emitir o
endereço — nunca depois. Isso é a regra RN-RT-001 da v4.0 aplicada ao fluxo novo, e
precisa estar explícita porque é o tipo de coisa que alguém "otimiza" mais tarde sem
perceber o que quebrou.

## Ponto de atenção: spread

O documento do MVP lista spread como alternativa de receita. A v4.0 §14, a
`product-strategy.md` §7 e a monografia afirmam, todas, que a fee é linha separada e que
não se depende de spread oculto.

Com routing por disponibilidade a contradição é pequena, porque não estamos afirmando
que escolhemos a melhor rota. Mas a partir da V3 ela cresce: spread cria incentivo para
preferir o provider que permite mais margem, o que corrói exatamente a resposta que a V3
vai precisar dar.

**Recomendação registrada: fee explícita**, como no exemplo 0,60% + 0,15% da §15. Mantém
as duas histórias compatíveis.

## Alternativas consideradas

**Construir a v4.0 completa como MVP.** Rejeitada: constrói a peça mais cara antes de
validar a hipótese mais barata, e não consegue demonstrar o próprio valor no dia 1.

**Um provider só, e roteamento depois.** Rejeitada: com um provider não há tese, apenas
uma integração conveniente. Dois é o mínimo que prova desacoplamento.

**Cortar também a idempotência e o `UNKNOWN` para acelerar.** Rejeitada: são as duas
coisas cuja ausência gera perda de dinheiro real, e ambas são baratas agora.

## Validação pendente

Sem alteração em relação à ADR 0006, mais um item novo:

- parecer jurídico sobre a **Resolução BCB 561/2026**, vigente em 1º/10/2026
- apetite de compliance dos providers quanto à origem dos fundos
- viabilidade de contratar dois providers em paralelo
- **quais redes os primeiros providers suportam** — é isso que decide se o MVP roda em
  Polygon ou em Solana, não preferência técnica
