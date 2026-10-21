# criandoAPI

API REST de estudo, construída com **Spring Boot 3.5** e **Java 21**, para prática de testes de API. Implementa autenticação via JWT e CRUDs de Usuários, Produtos e Pedidos, com regras de negócio de estoque e máquina de estados de pedido.

> Documentação interativa (Swagger UI): http://localhost:8087/criandoAPI/v1/swagger-ui.html *(com a API em execução localmente, portas/paths padrão)*

## Stack

- Java 21
- Spring Boot 3.5.7 (Web, Data JPA, Validation, Actuator, DevTools)
- Spring Security (filtro JWT customizado, sem `spring-boot-starter-security`)
- MySQL (via `mysql-connector-j`)
- JJWT 0.12.6 (geração/validação de tokens)
- springdoc-openapi 2.8.13 (Swagger UI)
- Lombok
- Gradle (wrapper incluído)
- Flyway (migrations versionadas; Hibernate apenas valida o schema)
- JUnit 5, Mockito, MockMvc, H2 e Testcontainers/MySQL (testes)

## Pré-requisitos

- JDK 21
- MySQL em execução (local ou remoto)

## Configuração

A aplicação carrega opcionalmente um arquivo `.env` na raiz do projeto (`spring.config.import: optional:file:.env[.properties]`). Crie um `.env` com as variáveis abaixo conforme necessário — todas têm valor padrão, exceto quando indicado:

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/apiParaEstudoDeAutomacao?createDatabaseIfNotExist=true&useTimezone=true&serverTimezone=UTC` | URL de conexão do MySQL |
| `DB_USERNAME` | `root` | Usuário do banco |
| `DB_PASSWORD` | *(vazio)* | Senha do banco |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` | Adoção explícita de banco existente compatível com V1 (primeira migração) |
| `API_MANUTENCAO_ENABLED` | `false` | Habilita limpeza do banco, exclusiva para ADMIN e ambientes isolados |
| `SERVER_PORT` | `8087` | Porta HTTP |
| `SERVER_SERVLET_CONTEXT_PATH` | `/criandoAPI` | Context path da aplicação |
| `API_VERSION_PREFIX` | `/v1` | Prefixo de versão usado em todas as rotas |
| `JWT_SECRET` | chave de exemplo embutida | Segredo usado para assinar os tokens JWT |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Tempo de expiração do token, em ms |

> Para qualquer uso além de estudo local, defina `JWT_SECRET` com um valor próprio — o padrão embutido não é seguro para produção.

O banco `apiParaEstudoDeAutomacao` é criado automaticamente na primeira conexão (`createDatabaseIfNotExist=true`).

## Como executar

```bash
./gradlew bootRun
```

A API sobe em `http://localhost:8087/criandoAPI` (ajustável via `SERVER_PORT`/`SERVER_SERVLET_CONTEXT_PATH`).

- Swagger UI: http://localhost:8087/criandoAPI/v1/swagger-ui.html
- OpenAPI JSON: http://localhost:8087/criandoAPI/v1/api-docs

## Build e testes

```bash
./gradlew build      # compila, roda os testes e empacota o WAR
./gradlew test        # testes unitarios e integracao com H2 isolado
./gradlew mysqlTest   # mesmos cenarios de integracao em MySQL via Docker (requer daemon ativo)
```

O artefato gerado é um WAR (`build/libs/criandoAPI.war`), pronto para deploy em servlet container ou execução standalone via `bootRun`/`java -jar`.

## Endpoints

Todas as rotas abaixo são prefixadas por `{context-path}{api.version-prefix}` (por padrão `/criandoAPI/v1`).

### Health

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/actuator/health` | Não | Status da aplicação |

### Auth

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | `/auth/registro` | Não | Registra usuário e retorna token JWT |
| POST | `/auth/login` | Não | Autentica e retorna token JWT |

### Usuários

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | `/usuarios` | Não | Cria usuário |
| GET | `/usuarios` | ADMIN | Lista usuários |
| GET | `/usuarios/{id}` | Próprio usuário ou ADMIN | Busca por id |
| GET | `/usuarios/buscar?nome=` | ADMIN | Busca por nome (404 quando sem resultado) |
| PUT | `/usuarios/{id}` | Próprio usuário ou ADMIN | Atualiza usuário |
| DELETE | `/usuarios/{id}` | Próprio usuário ou ADMIN | Remove usuário (409 se houver vínculos) |

### Produtos

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/produtos` | JWT | Lista com filtros (`nome`, `categoria`, `precoMin`, `precoMax`) e paginação |
| GET | `/produtos/{id}` | JWT | Busca por id |
| GET | `/produtos/categoria/{categoria}` | JWT | Lista por categoria, paginado |
| POST | `/produtos` | ADMIN | Cria produto |
| PUT | `/produtos/{id}` | ADMIN | Atualiza produto |
| PATCH | `/produtos/{id}/estoque` | ADMIN | Atualiza estoque |
| DELETE | `/produtos/{id}` | ADMIN | Inativa produto (soft delete) |

Categorias (`ProdutoCategoria`): `ELETRONICO`, `VESTUARIO`, `ALIMENTACAO`, `LIVRO`, `ESPORTE`, `CASA_E_JARDIM`, `OUTRO`.

### Pedidos

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | `/pedidos` | JWT | Cria pedido e baixa estoque |
| GET | `/pedidos` | JWT | Lista pedidos do usuário autenticado (filtro por `status`, paginado) |
| GET | `/pedidos/{id}` | JWT | Busca pedido (isolado por usuário) |
| PATCH | `/pedidos/{id}/status` | JWT | Transiciona status do pedido |
| DELETE | `/pedidos/{id}` | JWT | Cancela pedido `PENDENTE` ou `CONFIRMADO` e devolve estoque uma única vez |

Máquina de estados (`PedidoStatus`):

```
PENDENTE → CONFIRMADO → EM_PREPARO → ENVIADO → ENTREGUE
PENDENTE → CANCELADO
CONFIRMADO → CANCELADO
```

`ENTREGUE` e `CANCELADO` são estados finais.

Paginação (aplicável a `produtos` e `pedidos`): `page` (inicia em 0), `size`, `sort` no formato `campo,direcao` (ex.: `nome,asc`, `criadoEm,desc`).

## Autenticação

Rotas protegidas exigem o header:

```
Authorization: Bearer <token>
```

O token é obtido em `/auth/login` ou `/auth/registro`. No Swagger UI, use o botão **Authorize** e cole apenas o token: o esquema HTTP Bearer adiciona o prefixo automaticamente.

## Códigos de status padronizados

`200`, `201`, `204`, `400`, `401`, `403`, `404`, `405`, `409`, `415`, `422`, `500` — detalhados por endpoint no Swagger.

## Integridade de pedidos e estoque

Criação, atualização de produtos e cancelamento usam transações e bloqueios pessimistas de escrita. Produtos são bloqueados em ordem de ID para reduzir disputas em pedidos com vários itens. Cancelamento por PATCH e DELETE aceita pedidos PENDENTE ou CONFIRMADO; estados finais não permitem nova devolução. Falhas revertem toda a operação. Disputas de bloqueio retornam 409 e podem ser reenviadas.

`POST /pedidos` aceita o header opcional `Idempotency-Key`: 1 a 128 caracteres entre letras, números, `.`, `_`, `:`, `-`. A chave é sensível a maiúsculas/minúsculas e pertence ao usuário autenticado. Mesmo usuário, chave e payload retornam 201 com o mesmo pedido, em seu estado atual, sem nova baixa de estoque. A ordem dos itens faz parte do payload. Alterar o payload com a mesma chave retorna 409. Sem header, cada chamada cria um pedido. As chaves permanecem armazenadas junto aos pedidos, sem expiração automática.

O bloqueio do usuário serializa criações daquele usuário, inclusive reenvios simultâneos. Isso favorece integridade; medir a contenção antes de aumentar a carga.

## Permissões e manutenção

Contas públicas são sempre criadas com perfil USER; o payload não permite atribuir ADMIN. Alterações de produtos, listagem/busca de usuários, métricas e limpeza do banco exigem ADMIN. Pedidos continuam isolados por proprietário. A identidade autenticada é consultada no banco a cada requisição, incluindo o perfil atual.

A criação do primeiro administrador é uma operação administrativa no banco, fora dos endpoints públicos: após cadastrar a conta, um responsável pode atribuir `role = 'ADMIN'` ao usuário escolhido. Não há promoção automática do primeiro usuário.

`DELETE /manutencao/banco` só existe com `API_MANUTENCAO_ENABLED=true` e exige ADMIN. A configuração padrão mantém o endpoint ausente. A limpeza também remove usuários e pedidos com suas chaves de idempotência; use apenas em ambientes isolados de testes.

## Validação e erros

`page >= 0`; `size` entre 1 e 100. `sort` exige exatamente `campo,asc` ou `campo,desc`.

- Produtos: `id`, `nome`, `preco`, `estoque`, `categoria`, `criadoEm`, `atualizadoEm`.
- Pedidos: `id`, `criadoEm`, `atualizadoEm`, `status`, `valorTotal`.

Filtros de preço não aceitam negativos e exigem `precoMin <= precoMax`. Preços de produtos aceitam até 8 dígitos inteiros e 2 decimais. IDs de produtos nos itens devem ser positivos e itens nulos são rejeitados.

Erros preservam `status`, `erro`, `mensagem`, `path`, `timestamp`, acrescentando `codigo`, `requestId` e `campos`. O timestamp é UTC. `requestId` corresponde ao header `X-Request-ID`, presente também nas respostas de sucesso. `campos` contém detalhes de validação ou uma lista vazia. Códigos estáveis incluem `REQUISICAO_INVALIDA`, `NAO_AUTENTICADO`, `ACESSO_NEGADO`, `RECURSO_NAO_ENCONTRADO`, `CONFLITO` e `REGRA_DE_NEGOCIO`. Detalhes SQL não são devolvidos ao cliente.

## Migrations e adoção de banco existente

Bancos novos recebem V1 (schema inicial), V2 (idempotência e índices) e V3 (unicidade de nomes de produtos). Hibernate usa `ddl-auto: validate`; alterações futuras devem ter novas migrations. O deploy desta versão não deve usar `ddl-auto: update`.

Para um banco já criado por versões anteriores, faça backup e confira a correspondência com V1, incluindo tipos, precisão, enums e chaves estrangeiras. Verifique nomes de produtos duplicados conforme a collation do banco antes da V3; a migration falha se encontrar duplicatas, sem escolher ou remover registros automaticamente. O nome de produto continua reservado após soft delete.

Depois dessa conferência, habilite `FLYWAY_BASELINE_ON_MIGRATE=true` apenas na primeira inicialização do banco existente. O baseline versão 1 registra o schema legado e aplica V2/V3 sem recriar as tabelas de V1. Remova a opção após a adoção. Bancos com schema divergente precisam de reconciliação antes do baseline. Nenhuma alteração foi aplicada a banco externo durante a implementação.

Referência: [baseline do Flyway](https://documentation.red-gate.com/flyway/reference/commands/baseline).

## Observabilidade e verificação

Logs HTTP registram método, status, duração e identificador de requisição; não registram corpos, tokens ou parâmetros de query. Logs SQL e bind de parâmetros estão desativados por padrão. Health público não expõe detalhes internos. `/v1/actuator/metrics` exige ADMIN e oferece métricas do servidor HTTP, JVM e pool de conexões; o prefixo acompanha `API_VERSION_PREFIX`.

`./gradlew test` executa integração com contexto Spring, filtros JWT, migrations e banco H2 em modo MySQL, incluindo concorrência, rollback, idempotência, permissões e contratos HTTP. `./gradlew mysqlTest` valida os mesmos cenários em um MySQL descartável, sem acessar o banco da aplicação; falha se Docker não estiver disponível. H2 não substitui a validação das características específicas do MySQL. As configurações de testes não importam `.env` e usam bancos isolados.

O Jenkins executa `clean test bootWar` antes do deploy e publica resultados JUnit. A suíte externa `testeCriandoAPI` permanece independente. A validação MySQL deve ser executada em um agente com Docker usando `./gradlew mysqlTest`.
