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
- JUnit 5, Mockito, MockMvc (testes)

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
| `JPA_DDL_AUTO` | `update` | Estratégia do Hibernate (`ddl-auto`) |
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
./gradlew test        # roda apenas os testes
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
| GET | `/usuarios` | JWT | Lista usuários |
| GET | `/usuarios/{id}` | JWT | Busca por id |
| GET | `/usuarios/buscar?nome=` | JWT | Busca por nome |
| PUT | `/usuarios/{id}` | JWT | Atualiza usuário |
| DELETE | `/usuarios/{id}` | JWT | Remove usuário |

### Produtos

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `/produtos` | Não | Lista com filtros (`nome`, `categoria`, `precoMin`, `precoMax`) e paginação |
| GET | `/produtos/{id}` | Não | Busca por id |
| GET | `/produtos/categoria/{categoria}` | Não | Lista por categoria, paginado |
| POST | `/produtos` | JWT | Cria produto |
| PUT | `/produtos/{id}` | JWT | Atualiza produto |
| PATCH | `/produtos/{id}/estoque` | JWT | Atualiza estoque |
| DELETE | `/produtos/{id}` | JWT | Inativa produto (soft delete) |

Categorias (`ProdutoCategoria`): `ELETRONICO`, `VESTUARIO`, `ALIMENTACAO`, `LIVRO`, `ESPORTE`, `CASA_E_JARDIM`, `OUTRO`.

### Pedidos

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| POST | `/pedidos` | JWT | Cria pedido e baixa estoque |
| GET | `/pedidos` | JWT | Lista pedidos do usuário autenticado (filtro por `status`, paginado) |
| GET | `/pedidos/{id}` | JWT | Busca pedido (isolado por usuário) |
| PATCH | `/pedidos/{id}/status` | JWT | Transiciona status do pedido |
| DELETE | `/pedidos/{id}` | JWT | Cancela pedido `PENDENTE` e devolve estoque |

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

O token é obtido em `/auth/login` ou `/auth/registro`. No Swagger UI, use o botão **Authorize** informando `Bearer <token>`.

## Códigos de status padronizados

`200`, `201`, `204`, `400`, `401`, `404`, `409`, `422` — detalhados por endpoint no Swagger.
