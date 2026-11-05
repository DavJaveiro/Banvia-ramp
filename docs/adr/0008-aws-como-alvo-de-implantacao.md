# ADR 0008 — AWS como alvo de implantação

**Data:** 26/10/2026
**Status:** Aceita
**Relação com a [ADR 0007](0007-reducao-de-escopo-do-mvp.md):** independente; não muda
o escopo do MVP
**Etapas definidas em:** [`evolucao-aws.md`](../evolucao-aws.md)

---

## Contexto

O Banvia só roda localmente. O `spring-boot-docker-compose` monta a datasource a partir
do `compose.yaml`, e não existe imagem, perfil de produção, IaC nem ambiente fora da
máquina do desenvolvedor.

A stack é portável: Spring Boot, PostgreSQL, Redis, Flyway, OpenTelemetry e Angular
não prendem o projeto a nenhum fornecedor. Falta escolher o destino.

Há também um objetivo de formação: exercitar os serviços AWS que o mercado de Java
sênior pede (Lambda, SNS, SQS, S3, DynamoDB, RDS, ElastiCache, EKS) num sistema com
semântica financeira real, e não num exemplo de tutorial.

O risco é conhecido. A v4.0 §18 diz que Kafka, Kubernetes e microsserviços não são
requisito do MVP, e que Redis só entra com necessidade concreta. Uma trilha de
treinamento que espalhe serviços pelo core contraria essa regra e piora o produto.

## Decisão

**A AWS é o alvo de implantação, num ambiente `treino` efêmero, sem dado real e fora do
caminho crítico do MVP.**

- **ECS Fargate** executa a API e os workers. EKS existe só como laboratório.
- **RDS PostgreSQL continua sendo a única fonte de verdade financeira.** Payout,
  journal, idempotência e outbox ficam na mesma transação. SQS e SNS transportam, o S3
  guarda cópia probatória e o DynamoDB deduplica na borda serverless.
- **Todo serviço AWS fica atrás de uma porta** que já tem implementação local. O
  domínio não importa AWS SDK.
- **Todo teste roda local**, com Testcontainers e emulador. A conta AWS valida IAM,
  rede, limites e latência.
- **Terraform** descreve tudo. O que foi criado pelo console não existe.
- **Região `sa-east-1`**, por paridade com uma implantação brasileira real (latência
  até os providers de PIX e expectativa de residência de dados de instituições
  financeiras). A região é variável do Terraform. Se o custo pesar, `us-east-1` é
  aceitável enquanto não houver dado real.
- A trilha tem **milestone próprio** e nenhuma de suas issues é P0. Uma etapa que
  depende de issue de produto espera essa issue fechar.

## Consequências

### Positivas

- O projeto passa a ter um caminho verificável da máquina local até um ambiente real.
- Cada serviço AWS entra amarrado a uma necessidade do domínio, o que torna a trilha
  defensável numa revisão técnica.
- A disciplina de adapter é testada de verdade: trocar o transporte de eventos, a
  cifragem ou o destino da telemetria não toca o domínio.

### Negativas

- Custo recorrente enquanto o ambiente existe. A mitigação é o destroy como padrão e
  um alarme no AWS Budgets.
- Mais superfície para errar: IAM, rede e cifragem são novas fontes de incidente.
- Dependências novas (AWS SDK v2, Spring Cloud AWS, Encryption SDK) e a
  compatibilidade delas com o Boot 4.1, que precisa ser confirmada em cada etapa.

### Neutras

- O ambiente `treino` não é produção. Levar a integração com provider real (#47) para a
  AWS exige uma etapa própria de *hardening*: Multi-AZ, PITR, WAF, revisão de IAM e
  teste de intrusão.

## Ponto de atenção: o ambiente instalado e a #55

A #55 recomenda reescrever a `V1__baseline.sql` em vez de criar uma migration nova,
justificando que "não há ambiente instalado". Um RDS criado antes de a #55 fechar torna
essa justificativa falsa. Por isso a etapa A1 só sobe banco depois da #55.

## Alternativas consideradas

**EKS como alvo principal.** Rejeitada: o *control plane* cobra parado e a operação de
Kubernetes é maior que o problema do MVP. Fica como laboratório (A9).

**Lambda para a API inteira.** Rejeitada: o core tem outbox, workers agendados, pool de
conexão e transação. Lambda entra só na borda de webhook (A6).

**DynamoDB para idempotência.** Rejeitada: separa a chave de idempotência da transação
que cria o payout e reabre a janela de pagamento duplo que a #11 fecha.

**AWS CDK em Java em vez de Terraform.** Viável e coerente com a stack. Rejeitada por
o Terraform ser o padrão mais comum em times de infraestrutura e funcionar igual para
qualquer linguagem.

**Não ir para nuvem antes do MVP.** Rejeitada: a trilha não toca o caminho crítico, e o
custo de adiar é perder o treinamento justamente na fase em que o sistema ainda é
pequeno o bastante para ser entendido inteiro.
