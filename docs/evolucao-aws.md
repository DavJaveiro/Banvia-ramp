# Trilha AWS — Banvia

**Status:** vigente — outubro de 2026
**Decisão que a originou:** [ADR 0008](adr/0008-aws-como-alvo-de-implantacao.md)
**Relação com o roadmap de produto:** corre em paralelo às Fases 0–7 da
[v4.0 §20](requisitos-banvia-v4.0.md) e ao [MVP v1](requisitos-banvia-mvp-v1.md).
**Não altera o escopo nem o critério de aceite do MVP.**

---

## 1. Objetivo

Levar o Banvia para a AWS, explorando Lambda, SNS, SQS, S3, DynamoDB, RDS,
ElastiCache, KMS, ECS, EKS e observabilidade gerenciada com o mesmo rigor que o
domínio já exige.

A trilha só aceita um serviço AWS quando ele resolve uma **necessidade concreta**
do Banvia. Serviço que entra só para aparecer no diagrama contraria a regra de
engenharia da v4.0 §18:

> a sofisticação inicial deve estar na semântica de correção — idempotência,
> estados, evidência, recuperação e reconciliação — e não na quantidade de
> serviços implantados.

| Serviço | Necessidade concreta no Banvia | Etapa |
|---|---|---|
| ECS Fargate + ALB | executar a API e os workers sem administrar servidor | A1 |
| RDS PostgreSQL | fonte única de verdade: payout, journal, idempotência, outbox | A1 |
| Secrets Manager | credenciais do banco e segredos de provider fora da imagem e do env | A1 |
| X-Ray, CloudWatch, Prometheus gerenciado, Grafana | destino de produção para a telemetria da #69, #70 e #71 | A2 |
| KMS | cifrar documento e chave PIX do beneficiário em repouso (#59) | A3 |
| SNS + SQS | transportar eventos do outbox para mais de um consumidor (#61) | A4 |
| S3 (Object Lock) | cópia probatória WORM do journal e das evidências de liquidação | A5 |
| API Gateway + Lambda | receber webhook de provider mesmo com o core fora do ar | A6 |
| DynamoDB | deduplicar webhook de entrada na borda, onde não há Postgres | A6 |
| ElastiCache (Valkey) | rate limit por tenant compartilhado entre instâncias (#57) | A7 |
| EKS | laboratório: a mesma imagem em Kubernetes | A9 |

---

## 2. Invariantes da trilha

Valem para todas as etapas. Uma etapa que viole qualquer uma delas não fecha.

1. **Produto primeiro.** Nenhuma etapa é P0 nem entra no milestone
   *Fase 1 — Protótipo local*. Etapas que dependem de uma issue de produto
   esperam essa issue ser fechada, sem adiantar o trabalho dela.
2. **AWS atrás de adapter.** O domínio não importa AWS SDK (AGENTS.md §6.6).
   Cada adapter AWS implementa uma porta que já tem uma implementação local.
3. **O Postgres continua sendo a fonte de verdade financeira.** SQS e SNS
   transportam, o S3 guarda cópia probatória e o DynamoDB deduplica na borda.
   Payout, journal, idempotência e outbox ficam no banco relacional, na mesma
   transação (AGENTS.md §6.2 e §6.5; RN-IDM-003).
4. **Todo teste roda local.** A suíte usa Testcontainers com emulador
   (LocalStack, ou ElasticMQ, DynamoDB Local e MinIO). A conta AWS só valida
   o que o emulador não prova: IAM, rede, limites e latência real.
5. **Ambiente efêmero e sem dado real.** O ambiente `treino` é recriável do
   zero, roda com providers simulados ou em sandbox e nunca recebe documento
   ou chave PIX de pessoa real. Destruí-lo é o padrão.
6. **Tudo por IaC.** O que for criado pelo console não existe para a trilha.
7. **A sanitização vale fora da JVM também.** CloudWatch, X-Ray, S3, DynamoDB,
   tags e contexto de cifragem do KMS seguem AGENTS.md §6.7 e §7.11.

---

## 3. Dependências com as issues abertas

Levantamento feito em 26/10/2026 sobre as 33 issues abertas.

```text
#41 comando único ─┐
#42 CI ────────────┼─→ A0 imagem e perfil prod ─→ A1 fundação AWS ─┬─→ A8 carga
#55 schema MVP ────┘                                 │             └─→ A9 EKS
                                                     │
#69 #70 #71 observabilidade ─────────────────────────┼─→ A2 telemetria AWS
#59 beneficiário cifrado ────────────────────────────┼─→ A3 KMS
#26 #61 state machine + webhook de saída ────────────┼─→ A4 SNS/SQS ─┐
#28 #36 #60 journal, auditoria, E2E ─────────────────┼─→ A5 S3 WORM ─┼─→ A6 Lambda ingress
#34 #58 providers simulados + SPI ───────────────────┼───────────────┘
#57 auth + #24 routing ──────────────────────────────┴─→ A7 Valkey
```

| Etapa | Espera fechar | Não pode atropelar |
|---|---|---|
| A0 | — | #41 é dono do `compose.yaml` e do script na raiz. A0 só cria `application-prod.yml` e a imagem. |
| A1 | #42, #55 | **#55 recomenda reescrever a `V1__baseline.sql` porque "não há ambiente instalado".** Um RDS de pé antes dela torna essa exceção falsa. A1 só sobe banco depois de #55. |
| A2 | #69, #70, #71 | #70 escolhe o coletor local e adia a decisão de amostragem de produção. A2 é essa decisão. |
| A3 | #59 | #59 define a porta de cifragem e a implementação local. A3 só adiciona o adapter KMS. |
| A4 | #26, #55, #61 | #61 entrega o webhook de saída com outbox em processo. A4 troca o transporte, sem reescrever a entrega. |
| A5 | #28, #36, #60 | O journal e a auditoria nascem no Postgres. O S3 recebe cópia, não substitui. |
| A6 | #34, #58, A4, A5 | A validação de assinatura continua no `parseWebhook` do adapter (#58). A Lambda não interpreta payload. |
| A7 | #24, #57 | #11 proíbe idempotência em cache. O Redis entra para rate limit, nunca para idempotência. |
| A8 | #11, #23, #27, A1, A4 | Os números medidos são do ambiente `treino`. Não viram SLA (P-010). |
| A9 | A1–A4 estáveis | v4.0 §18: Kubernetes não é requisito do MVP. O EKS é laboratório e o ECS continua sendo o alvo. |

---

## 4. Etapas

### A0 — Imagem OCI e perfil de produção

**Por quê.** Hoje a API só roda com `spring-boot-docker-compose` montando a
datasource. Fora da máquina do desenvolvedor não há como configurá-la.

**Escopo**

- imagem por `./mvnw spring-boot:build-image` (Paketo Buildpacks, JRE 25,
  usuário não-root, camadas do jar separadas). Sem Dockerfile para manter.
- `application-prod.yml`:
  - `spring.docker.compose.enabled: false`
  - datasource e Redis só por variável de ambiente, sem valor padrão
  - `logging.level.com.banvia: INFO`
  - amostragem de trace configurável, não fixa em `1.0`
  - `management.endpoint.health.probes.enabled: true`
- **liveness não verifica o banco.** Com o banco fora, a liveness reprova,
  o orquestrador reinicia todas as tasks e a queda vira cascata. Banco e Redis
  entram só na readiness.
- `spring.lifecycle.timeout-per-shutdown-phase` menor que o `stopTimeout` da
  task ECS. Os dois são alinhados ao *deregistration delay* do ALB.
- JVM: `-XX:MaxRAMPercentage=75`, para o heap respeitar o limite do container

**Aceite**

- a imagem sobe com `docker run` apontando para o Postgres do compose, só com
  variáveis de ambiente
- `/actuator/health/readiness` reprova com o banco parado e
  `/actuator/health/liveness` continua aprovando
- a imagem não roda como root e não contém segredo. O scan da imagem passa.
- o jar não contém `spring-boot-docker-compose` nem `devtools`. Os dois já são
  `optional` no `pom.xml`, e o teste confirma a exclusão.

---

### A1 — Fundação AWS: Terraform, rede, RDS, ECS e deploy

**Por quê.** É o primeiro ambiente real. Toda etapa seguinte pressupõe que ele
sobe e desce por um comando.

**Escopo**

- Terraform em `infra/terraform/`, com `bootstrap/`, `envs/treino/` e
  `modules/`. O estado remoto fica no S3 com lock nativo (`use_lockfile`,
  Terraform ≥ 1.10). O lock em DynamoDB está obsoleto e não é o exercício de
  DynamoDB desta trilha.
- VPC com duas AZs. O ALB fica em sub-rede pública. ECS e RDS ficam em
  sub-rede privada.
- Saída para a internet (sandbox de provider e RPC): um único NAT Gateway, em
  uma AZ. A decisão é aceitável só em `treino` e fica registrada como dívida.
  S3 e DynamoDB usam *gateway endpoints*, que não cobram por hora.
- RDS PostgreSQL:
  - privado e cifrado com KMS
  - `manage_master_user_password`, com a senha gerada e rotacionada pelo
    Secrets Manager
  - `rds.force_ssl=1`
  - Multi-AZ desligado em `treino`, registrado como dívida
  - confirmar se a região já oferece PostgreSQL 18
- ECR com tags imutáveis e scan no push
- serviço ECS Fargate atrás do ALB:
  - health check em `/actuator/health/readiness`
  - *deployment circuit breaker* com rollback
  - *task role* separada de *execution role*, ambas com privilégio mínimo
  - segredos injetados pelo campo `secrets` da task, nunca como env em claro
- **migrations em deploy contínuo.** Com várias tasks, o Flyway serializa a
  aplicação das migrations pelo lock do Postgres. Durante o *rolling deploy*,
  porém, a versão anterior roda contra o schema novo. Toda migration segue
  *expand/contract*: adiciona, migra e só remove numa versão seguinte.
  Complementa a regra de migrations do AGENTS.md §6.
- CI/CD (depois de #42):
  - GitHub Actions assume uma role IAM por **OIDC**, sem chave de longa duração
  - a *trust policy* restringe repositório e branch
  - `terraform plan` em PR
  - `apply` manual, com *environment protection*
  - build da imagem → ECR → deploy no ECS
- frontend opcional: S3 privado + CloudFront com OAC. O caminho `/api` é
  roteado ao ALB pelo próprio CloudFront, o que dispensa CORS. O frontend fica
  fora do caminho crítico (#9).
- HTTPS no ALB exige domínio próprio para o certificado ACM. Sem domínio, o
  ALB fica em HTTP, e o ambiente não recebe nenhuma credencial real.

**Aceite**

- `terraform apply` do zero até `/actuator/health` respondendo pelo ALB
- `terraform destroy` não deixa recurso cobrável. A verificação usa o
  Resource Explorer e o Cost Explorer no dia seguinte.
- um deploy com readiness quebrada é revertido sozinho pelo circuit breaker
- nenhuma credencial AWS de longa duração existe no GitHub

---

### A2 — Telemetria na AWS

**Por quê.** A #70 faz a telemetria sair do processo localmente. Em produção
ela precisa de destino, retenção, alarme e decisão de amostragem.

**Escopo**

- coletor ADOT como *sidecar* da task ECS. A aplicação exporta OTLP para
  `localhost:4317`, o mesmo endereço do coletor local da #70. Se a aplicação
  precisar mudar, o desenho da #70 estava errado.
- destinos:
  - traces → X-Ray
  - métricas → Amazon Managed Prometheus
  - logs em JSON da #69 → CloudWatch Logs, consultáveis por `payoutId` e
    `tenantId` no Logs Insights
- **redação no coletor**, a segunda camada pedida na #70. Processadores
  `attributes`/`transform` removem CPF, CNPJ, chave PIX e segredo, mesmo que
  escapem da aplicação.
- **amostragem por cauda.** Ficam 100% dos traces com erro, dos payouts em
  `UNKNOWN` e das chamadas a provider acima do p99. Do resto, guarda uma
  fração.
- **cardinalidade.** `tenantId` e `provider` podem ser label de métrica.
  `payoutId`, `quoteId` e `requestId` nunca: vão para trace e log.
- dashboards da North Star e das métricas da v4.0 §16 (`PayoutSuccessRate`,
  `UnknownOutcomeRate`, `SettlementP95`, `ReconciliationMismatchRate`),
  versionados como JSON no repositório e reaproveitados do `otel-lgtm` local
- alarmes CloudWatch:
  - `UnknownOutcomeRate` acima de zero por N minutos
  - DLQ com mensagem (A4)
  - 5xx no ALB
  - conexões do RDS perto do limite
- Grafana: Amazon Managed Grafana (custo por usuário ativo e exige IAM
  Identity Center) ou Grafana OSS numa task ECS. A escolha fica registrada.

**Aceite**

- uma chamada aparece no X-Ray com `requestId`, `tenantId` e `payoutId`
- a linha de log correspondente é encontrada no CloudWatch pelo `payoutId`
- um span com CPF injetado em teste não chega ao X-Ray
- o alarme de DLQ dispara num teste provocado

---

### A3 — Cifragem do beneficiário com KMS

**Por quê.** A #59 exige documento e chave PIX cifrados em repouso, com hash
para busca. Fora da máquina local, a chave de cifragem não pode morar na
aplicação.

**Escopo**

- adapter KMS da porta de cifragem definida em #59, com o AWS Encryption SDK
  para Java (*envelope encryption*: a data key cifra o dado e o KMS cifra a
  data key)
- **o contexto de cifragem amarra o ciphertext ao registro**, com
  `tenantId`, `payoutId` e o nome do campo. Um ciphertext copiado para outra
  linha não decifra.
- **o contexto de cifragem nunca carrega PII.** O CloudTrail registra o
  contexto em claro a cada chamada ao KMS.
- índice cego para busca e deduplicação: HMAC-SHA256 com chave **separada** da
  de cifragem (chave HMAC do KMS, ou chave no Secrets Manager mantida em
  memória). O valor é normalizado antes do HMAC, com CPF só em dígitos e
  e-mail em minúsculas. Sem normalização, a deduplicação falha.
- rotação:
  - a rotação automática da chave simétrica do KMS é transparente para
    decifrar
  - chave HMAC não rotaciona sozinha, e trocá-la exige reindexar. A coluna
    guarda a versão da chave.
- IAM:
  - a task role só decifra com a condição
    `kms:EncryptionContextKeys` presente
  - o backoffice só vê a versão mascarada e não recebe `kms:Decrypt`
- trade-off do cache de data key: menos chamadas e menos latência, contra mais
  dado exposto se uma chave em memória vazar. A cota de requisições do KMS é
  por conta e região.

**Aceite**

- um dump da tabela não mostra documento nem chave PIX
- um ciphertext movido para outra linha falha ao decifrar
- com o KMS indisponível, a criação de payout **falha fechada**, sem fallback
  em claro e com Problem Details sem detalhe interno
- nenhuma chamada do CloudTrail traz PII no contexto

---

### A4 — Mensageria: outbox → SNS → SQS

**Por quê.** A #61 publica eventos a partir do outbox. Quando o mesmo evento
precisa chegar a mais de um consumidor fora do processo (entrega de webhook,
gatilho de reconciliação, arquivo em S3), o transporte em processo deixa de
bastar.

**Escopo**

- topologia:

```text
Postgres (estado + event_publication, mesma transação)
   │  externalização depois do commit
   ▼
SNS FIFO  payout-events
   ├─ filtro eventType ─→ SQS FIFO webhook-delivery  ─→ DLQ
   ├─ filtro eventType ─→ SQS FIFO reconciliation    ─→ DLQ
   └─ filtro eventType ─→ SQS FIFO evidence-archive  ─→ DLQ
```

- externalização pelo Spring Modulith (`spring-modulith-events-aws-sns`), com
  `@Externalized` nos eventos de payout. **Antes de adotar, confirmar se o
  Spring Cloud AWS já suporta o Boot 4.1.** Se não suportar, o caminho é um
  adapter próprio com AWS SDK v2, lendo o mesmo registro de publicação.
- FIFO com `MessageGroupId = payoutId` e `MessageDeduplicationId = eventId`.
  Isso reduz entrega fora de ordem do mesmo payout, mas **não muda o contrato
  público**: a #61 diz que ordenação não é garantida, e a documentação
  continua dizendo. O evento carrega a versão do estado, e o consumidor
  descarta versão antiga.
- **o consumidor é idempotente mesmo com FIFO.** A deduplicação do SQS vale por
  5 minutos. Visibilidade expirada e republicação do outbox depois de restart
  reentregam a mensagem. Cada consumidor grava
  `processed_message(consumer, message_id)` no Postgres, na mesma transação
  do efeito (*inbox*).
- entrega de webhook ao tenant:
  - mensagem que falha não é apagada
  - backoff exponencial com *jitter* via `ChangeMessageVisibility`
  - após N tentativas, vai para a DLQ, a `webhook_delivery` fica `FAILED`
    e o alarme dispara
  - o *redrive* é manual e auditado (#36)
- limites que viram regra de produto:
  - visibilidade máxima de 12 h por recebimento
  - retenção máxima de 14 dias
  - o teto de retry é explícito e cabe nesses limites
- consumo com long polling (`WaitTimeSeconds=20`), em lote e com virtual
  threads
- **exactly-once para o tenant não existe.** O contrato é *at-least-once* com
  `eventId` estável para o tenant deduplicar, e a documentação pública diz
  isso.

**Aceite**

- rollback da transação não gera mensagem (AGENTS.md §7.4)
- matar a task entre o commit e a publicação → o evento é publicado no
  restart
- mensagem duplicada → um efeito no consumidor
- mensagem envenenada vai para a DLQ sem travar o grupo
- tudo provado com Testcontainers e emulador antes de rodar na conta

---

### A5 — Arquivo probatório em S3

**Por quê.** O journal (#28) e a auditoria (#36) são append-only no Postgres.
Uma cópia imutável fora do alcance de quem administra o banco transforma
"ninguém alterou" em algo verificável.

**Escopo**

- bucket `evidence`: comprovante E2E do PIX (#60) e webhook bruto de provider
- bucket `journal-archive`: export diário do journal
- nos dois buckets:
  - versionamento e Object Lock
  - SSE-KMS com chave dedicada
  - *Block Public Access*
  - política que nega acesso sem TLS (`aws:SecureTransport`)
- **Object Lock em modo *governance* e retenção curta no `treino`.** Em modo
  *compliance*, nem o root apaga o objeto antes de expirar: o bucket não sai,
  o `terraform destroy` falha e a conta segue cobrando. Em produção o modo é
  *compliance*.
- export em JSON Lines, com chave determinística
  `journal/date=AAAA-MM-DD/part-NNNN.jsonl`, gravada com escrita condicional
  (`If-None-Match: *`). Reexecutar o job não duplica nem sobrescreve.
- **cadeia de hash.** Cada arquivo traz o SHA-256 do anterior, a contagem de
  lançamentos e a soma por moeda. O checksum fica no próprio objeto
  (`x-amz-checksum-sha256`).
- evidência bruta pode conter dado pessoal. O acesso é restrito à role de
  reconciliação, e o acesso a objeto é registrado em *data events* do
  CloudTrail (custo à parte).

**Aceite**

- apagar ou sobrescrever um objeto travado falha
- reexecutar o export do mesmo dia não cria objeto novo
- uma ferramenta de verificação recalcula a cadeia de hash e confere com o
  banco

---

### A6 — Ingress serverless de webhook de provider

**Por quê.** O provider entrega o webhook quando quer, inclusive durante um
deploy ou uma queda do core. A borda serverless recebe, guarda e enfileira,
desacoplando a disponibilidade da recepção da disponibilidade da API.

**Escopo**

```text
provider ─→ API Gateway (HTTP API) ─→ Lambda webhook-ingress
                                         ├─ S3 evidence (corpo + headers brutos)
                                         ├─ DynamoDB  (dedupe de entrega)
                                         └─ SQS provider-inbound ─→ core ─→ parseWebhook (#58)
```

- **a Lambda não interpreta payload nem muda estado.** Ela limita tamanho,
  grava o bruto, deduplica e enfileira um ponteiro para o objeto no S3.
- a validação de assinatura continua no `parseWebhook` do adapter (#58), que é
  a fonte única das regras de cada provider. Validar também na Lambda
  duplicaria lógica por provider.
  - trade-off: lixo não assinado chega ao S3
  - mitigação: *throttling*, limite de tamanho e retenção curta do prefixo
    não validado
- **ordem das operações.** Se a Lambda marcar a entrega como vista no DynamoDB
  e o envio ao SQS falhar, o retry do provider é descartado como duplicata e o
  evento se perde. O registro passa por `IN_PROGRESS` → `COMPLETED` e só
  completa depois do SQS. É o modelo do utilitário de idempotência do
  Powertools for AWS Lambda (Java). Falhou no meio, o registro é removido e a
  Lambda devolve 5xx, para o provider tentar de novo.
- 2xx só depois de S3 e SQS confirmarem
- DynamoDB:
  - sob demanda
  - chave `provider#deliveryId`, com SHA-256 do corpo quando o provider não
    manda id
  - TTL para limpeza
  - **o TTL apaga com atraso**, então a validade é conferida também no código
- o core **não confia** na deduplicação da borda: o *inbox* da A4 deduplica de
  novo
- código da Lambda em `aws/lambda/webhook-ingress/`, com `pom.xml` próprio. O
  `backend/` não muda de estrutura.
- runtime:
  - Java gerenciado mais recente disponível. Se `java25` não existir na
    região, o módulo compila com `--release 21`.
  - SnapStart contra o *cold start*
  - estado restaurado do snapshot (conexões, segredo em cache, semente
    aleatória) é refeito em hook `afterRestore`
- a Lambda nunca loga o corpo

**Aceite**

- o mesmo webhook disparado 10 vezes em paralelo vira uma mensagem na borda e
  uma transição no core
- com o SQS indisponível, o provider recebe 5xx e o retry dele é processado
  depois
- payload acima do limite recebe 413
- o core parado por 10 minutos não perde nenhum webhook

---

### A7 — Valkey no ElastiCache para rate limit por tenant

**Por quê.** Hoje o Redis está no `compose.yaml` e no `pom.xml`, mas nenhum
código o usa. A v4.0 §18 diz *"Redis só com necessidade concreta"*. Ela
aparece quando há duas ou mais tasks: o rate limit e a quota por tenant da
#57 precisam ser compartilhados entre instâncias.

**Escopo**

- *token bucket* atômico (Bucket4j com Lettuce, ou script Lua), com chave por
  tenant
- **comportamento com o Redis fora do ar é decisão registrada.** *Fail open*
  com alarme preserva disponibilidade. *Fail closed* preserva quota
  contratual. A escolha vai na issue.
- **idempotência nunca vai para cache** (#11, RN-IDM-003)
- ElastiCache Valkey com TLS em trânsito e autenticação. *Serverless* ou nó
  pequeno, conforme o custo.
- paridade local: a imagem do compose troca para `valkey/valkey` nesta etapa,
  em acordo com o dono do compose (#41)
- opcional (V2): estado de saúde de provider compartilhado entre instâncias

**Aceite**

- com duas tasks, N requisições distribuídas respeitam o limite global
- com o Valkey parado, o comportamento é o registrado e o alarme dispara

---

### A8 — Carga, concorrência e autoscaling

**Por quê.** A #11 prova idempotência com threads de um processo. Na AWS a
concorrência real é entre tasks, e o gargalo real é o pool de conexões.

**Escopo**

- carga com Gatling (DSL Java) contra o ALB:
  - rajada com a mesma `Idempotency-Key` distribuída entre tasks
  - vazão com chaves distintas
  - cotação expirando no meio da rajada
- virtual threads já estão ligadas. O limite real é o Hikari:
  `tasks × maximumPoolSize` tem de caber no `max_connections` do RDS, com
  folga.
- *bulkhead* por provider (semáforo), para um provider lento não esgotar o pool
  nem as threads de quem chama o outro
- paralelismo onde o MVP já precisa:
  - reconciliação (#48) consultando providers em paralelo, com concorrência
    limitada
  - consumo SQS em lote
- cotação comparativa em paralelo entre providers é V3 e fica fora
- `StructuredTaskScope` ainda é *preview* no Java 25 e não entra em código de
  produção. Em vez dele: executor de virtual threads com timeout explícito.
- autoscaling:
  - API: *target tracking* por CPU
  - workers: *backlog* SQS por task (`ApproximateNumberOfMessagesVisible`
    dividido pelas tasks em execução)

**Aceite**

- relatório em `docs/spikes/` com p50/p95/p99, saturação do pool e **zero
  payout duplicado** sob rajada
- o relatório diz que os números são do `treino` e não são SLA (P-010)

---

### A9 — Laboratório EKS

**Por quê.** Exercitar Kubernetes com a mesma imagem e o mesmo contrato de
saúde, sem trocar o alvo de produção. A v4.0 §18 mantém Kubernetes fora do
MVP.

**Escopo**

- ensaio local com `kind` ou `k3d` antes de pagar o *control plane* do EKS
- Helm ou Kustomize:
  - probes ligadas a `/actuator/health/liveness` e `/readiness`
  - `PodDisruptionBudget`
  - HPA
  - `preStop` e `terminationGracePeriodSeconds` alinhados ao graceful shutdown
- EKS Pod Identity para IAM, AWS Load Balancer Controller e segredos pelo
  Secrets Store CSI ou External Secrets
- ambiente criado, validado e destruído no mesmo dia

**Aceite**

- o aceite da A1 passa no EKS
- comparação ECS × EKS registrada (operação, custo, tempo de deploy)
- `destroy` concluído

---

### A10 — Laboratórios opcionais

Não têm necessidade de produto. Entram só como exercício e só com ADR.

- **RabbitMQ.** Segundo adapter da mesma porta de externalização
  (`spring-modulith-events-amqp`), testado com Testcontainers. O Amazon MQ é
  opcional. Prova que o domínio não conhece o broker e compara a semântica
  com a do SQS: *publisher confirms*, ack/nack e DLX contra visibilidade e
  redrive.
- **Quarkus.** A Lambda da A6 reescrita em Quarkus com imagem nativa,
  comparando *cold start* com SnapStart.
- **IBM MQ.** Só local, com a imagem de desenvolvimento e JMS. Não há serviço
  gerenciado equivalente na AWS.

---

## 5. Fora da trilha, e por quê

| Fora | Motivo |
|---|---|
| DynamoDB como armazenamento de payout ou de idempotência | quebra a atomicidade com a transação do Postgres (#11, RN-IDM-003, AGENTS.md §6.5) |
| Lambda para a API inteira | o core tem outbox, workers, pool de conexão e transação longa. ECS é o encaixe. |
| Kafka / MSK | v4.0 §18. Os volumes e os consumidores do MVP não justificam. |
| Microsserviços | o monólito modular é decisão vigente e o `ModularityTests` a protege |
| Listener de blockchain | #58: quem observa a chain é o provider |
| MySQL e Oracle | o Banvia tem um banco relacional, e trocar não ensina nada que valha o custo |
| Multi-região | sem dado de produção, só custo |
| Dinheiro real ou PII real no `treino` | invariante 5. Levar #47 para a AWS exige uma etapa própria de *hardening*, fora desta trilha. |

---

## 6. Custo e higiene da conta

Os valores mudam por região e com o tempo. **Conferir na AWS Pricing
Calculator antes de cada etapa.**

- AWS Budgets com alarme de valor baixo, criado no `bootstrap/` antes de
  qualquer outro recurso
- tags obrigatórias em todo recurso: `project=banvia`, `env=treino`,
  `stage=A<n>`
- recursos que cobram parados:
  - NAT Gateway
  - ALB
  - RDS, inclusive snapshots manuais
  - *interface endpoints*
  - *control plane* do EKS
  - Amazon Managed Grafana, por usuário
  - chaves KMS, por chave e por mês
  - *data events* do CloudTrail
- armadilhas de remoção:
  - Object Lock em modo *compliance* (A5)
  - chave KMS só some após o período mínimo de espera
  - imagens no ECR sem *lifecycle policy*
- `skip_final_snapshot = true` só em `treino`

---

## 7. Issues propostas

Milestone **Trilha AWS**, criado em 26/10/2026. Rótulo `infra` em todas. Nenhuma é P0.

| Etapa | Issue | Título | Rótulos | Depende de |
|---|---|---|---|---|
| A0 | #73 | `chore(infra): imagem OCI e perfil de produção da API` | tipo: chore, backend, P1 | — |
| A1 | #74 | `chore(infra): fundação AWS com Terraform, RDS e ECS Fargate` | tipo: chore, P2 | #73, #55 |
| A1 | #75 | `ci: deploy na AWS com OIDC a partir do GitHub Actions` | tipo: chore, P2 | #74, #42 |
| A2 | #76 | `chore(infra): telemetria em X-Ray, CloudWatch e Prometheus gerenciado` | tipo: chore, P2 | #74, #69, #70, #71 |
| A3 | #77 | `feat(payout): adapter KMS para cifragem do beneficiário` | tipo: feature, backend, módulo: payout, P2 | #74, #59 |
| A4 | #78 | `feat(webhook): externaliza eventos de payout para SNS e SQS` | tipo: feature, backend, módulo: webhook, P2 | #74, #26, #55, #61 |
| A5 | #79 | `feat(ledger): arquivo probatório do journal e das evidências em S3` | tipo: feature, backend, módulo: ledger, P2 | #74, #28, #36, #60 |
| A6 | #80 | `feat(provider): ingress serverless de webhook com Lambda e DynamoDB` | tipo: feature, módulo: provider, P2 | #78, #79, #34, #58 |
| A7 | #81 | `feat(auth): rate limit por tenant com Valkey no ElastiCache` | tipo: feature, backend, módulo: auth, P2 | #74, #24, #57 |
| A8 | #82 | `test: carga e concorrência do fluxo de payout na AWS` | tipo: test, backend, P2 | #74, #78, #11, #23, #27 |
| A9 | #83 | `chore(infra): laboratório EKS com a mesma imagem da API` | tipo: chore, P2 | #74, #75, #76, #78 |

A10 não vira issue antes da ADR correspondente.

---

## 8. Competências exercitadas

| Competência | Onde |
|---|---|
| Java 25, Spring Boot, APIs REST | todo o core; A0 |
| Lambda, API Gateway | A6 |
| SNS, SQS, DLQ, FIFO | A4, A6 |
| S3, Object Lock, escrita condicional | A5, A6 |
| DynamoDB, escrita condicional, TTL | A6 |
| RDS PostgreSQL, Flyway, *expand/contract* | A1 |
| ElastiCache / Redis | A7 |
| KMS, Secrets Manager, IAM de privilégio mínimo, OIDC | A1, A3 |
| ECS Fargate, ALB, autoscaling | A1, A8 |
| Kubernetes (EKS) | A9 |
| Mensageria multi-broker (RabbitMQ, IBM MQ) | A10 |
| Paralelismo, virtual threads, *bulkhead* | A8 |
| Observabilidade: OpenTelemetry, X-Ray, CloudWatch, Prometheus, Grafana | A2 |
| Quarkus | A10 |
| Terraform, CI/CD | A1 |
| Design patterns: Outbox, Inbox, Adapter/Port, Idempotency Key, Envelope Encryption, Circuit Breaker, Bulkhead | A3, A4, A6, A8 |
