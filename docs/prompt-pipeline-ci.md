# Prompt: pipeline CI/CD, Jenkinsfile e proteção de branches

Cole o bloco abaixo no agente (Claude Code) na raiz do projeto novo. Substitua os
`{{placeholders}}` ou deixe o agente descobrir os valores na etapa 0.

Pré-requisitos no ambiente: `gh` autenticado com permissão de admin no repositório,
`git`, JDK da versão do projeto e Docker (se houver testes com Testcontainers).

---

````text
Você vai configurar a esteira de CI/CD, o Jenkinsfile e a proteção de branches deste
projeto, no mesmo modelo do projeto criandoAPI (Spring Boot + Gradle + GitHub Actions
+ Jenkins). Responda sempre em pt-br com acentuação correta.

## Regras de conduta
- Faça por etapas, na ordem abaixo. Ao fim de cada etapa, diga o que mudou e o que foi
  verificado de fato. Nunca declare sucesso sem ter rodado a verificação.
- Antes de qualquer ação externa (git push, abrir PR, criar ruleset, alterar
  configuração do repositório), diga o que vai fazer. Nunca faça merge de PR por conta
  própria: o merge é do usuário, pelo botão do GitHub.
- Algumas configurações de segurança do repositório podem ser negadas ao agente. Se uma
  chamada for negada, NÃO tente contornar: pare, explique o que precisa e dê o caminho
  na interface (ou o comando `gh api`) para o usuário executar.
- Valide todo YAML (`python3 -c "import yaml;yaml.safe_load(open(...))"`) e rode os
  testes localmente antes de subir.
- Não versione imagens, `.env` nem a pasta `.idea`. Use `git add` por arquivo.

## Etapa 0 — Descobrir o projeto (somente leitura)
Leia `build.gradle` (ou `pom.xml`), `README.md`, a árvore de `src/test` e o `Jenkinsfile`
e `.github/workflows` existentes. Levante e me mostre:
- build tool, versão do Java, versão do Spring Boot, plugin `war`/`jar`;
- pacotes de teste (ex.: `{{pacote.base}}.controller`, `.services`, `.integration`);
- se existe teste de repository dedicado (`@DataJpaTest`); se não, diga que a suíte
  "repository" usará os testes de integração (JPA + Flyway + H2);
- como o app é publicado (WAR em Tomcat? JAR?), URL de health check (actuator);
- remoto Git, branch padrão e se já existe a branch `develop`.
Rode `./gradlew clean test` e registre o total de testes e a cobertura atual de linhas.
Pare e confirme comigo se algo divergir do modelo (ex.: Maven em vez de Gradle).

## Etapa 1 — Modelo de branches
Duas branches permanentes: `main` e `develop`. Regra: **somente a `develop` chega à
`main`**, e de forma automática depois do CI verde. Todas as demais branches
(`feature/*`, `bugfix/*`, `hotfix/*`, `release/*`) abrem PR para a `develop`.
Se a `develop` não existir, crie a partir da `main`. Trabalhe sempre em uma branch nova
a partir da `origin/main` e abra PR para a `develop`.

## Etapa 2 — Workflow do GitHub Actions (`.github/workflows/gradle.yml`)
Nome `Pipeline CI`. Gatilhos:
- `pull_request` para `main` e `develop` (types: opened, synchronize, reopened);
- `push` SOMENTE em `main` e `develop` (nunca em `feature/**`: evita execução em
  duplicata, uma por push e outra por PR).
`concurrency` por `github.ref` com `cancel-in-progress: true`; `permissions: contents: read`.

Jobs (o nome exibido é `"<stage> · <job>"`, e esses nomes exatos serão os checks
obrigatórios dos rulesets, então não os renomeie depois sem atualizar os rulesets):

1. `validate · validate-git-constraints`
   - nome da branch válido: `main|develop|(feature|bugfix|hotfix|release)/.+`;
   - em PR com destino `main`, a origem só pode ser `develop` (mensagem de erro
     orientando a abrir o PR para a `develop`);
   - falhar se houver `.env` versionado ou marcadores de conflito (`<<<<<<<`, `>>>>>>>`);
   - `gradle/actions/wrapper-validation@v4`.
2. `build · build` (needs validate): `./gradlew clean build bootWar -x test`; publica o
   WAR como artefato. Usa `gradle/actions/setup-gradle@v4` com
   `dependency-graph: generate-and-submit` e `permissions: contents: write` (alimenta o
   dependency review).
3. `testes · testeController`, `testes · testeService`, `testes · testeRepository`
   (needs build, em paralelo): cada um roda `./gradlew test --tests '<pacote>.*'` do seu
   pacote, publica `build/test-results/test/*.xml` como `test-results-<suite>` e
   `build/jacoco/test.exec` como `jacoco-<suite>` (`if: always()`,
   `if-no-files-found: error`).
4. `gates · gate-qualidade` (needs os 3 testes): baixa `test-results-*` e `jacoco-*`
   (este em `build/jacoco-merged`, uma subpasta por artefato para não haver colisão de
   `test.exec`); script Python lê os XMLs e reprova se alguma suíte tiver 0 testes,
   falhas ou testes ignorados, e escreve tabela em `GITHUB_STEP_SUMMARY`; depois roda
   `./gradlew jacocoMergedReport jacocoMergedVerification` e publica o relatório HTML.
5. `gates · gate-seguranca-codigo` (needs build): CodeQL (`languages: java-kotlin`,
   `build-mode: manual`, `./gradlew compileJava --no-build-cache`),
   `permissions: security-events: write`.
6. `gates · gate-seguranca-dependencias` (needs build): `actions/dependency-review-action@v4`,
   `fail-on-severity: high`, com
   `if: github.event_name == 'pull_request' && github.base_ref == 'develop'`
   (a `main` não recebe snapshot do dependency graph, então no PR de promoção o review
   trataria tudo como "novo").
7. `promover · merge-automatico-main` (needs gate-qualidade e gate-seguranca-codigo;
   `if: github.event_name == 'push' && github.ref == 'refs/heads/develop'`;
   `permissions: contents: write, pull-requests: write`; `GH_TOKEN: ${{ github.token }}`):
   - `gh api repos/$GH_REPO/compare/main...develop -q .ahead_by`; se for 0, sair sem erro;
   - abrir (ou reaproveitar) o PR `develop` → `main`;
   - `gh pr merge "$PR" --merge --auto || gh pr merge "$PR" --merge`
     (merge commit; o `--auto` falha quando o PR já está liberado, por isso o fallback).
8. `notificar · notificar-falha` (needs todos os anteriores;
   `if: always() && push && develop && contains(needs.*.result, 'failure')`;
   `permissions: issues: write`): abre uma issue "CI falhou na develop (<sha7>)" com o
   link da execução, sem duplicar issue já aberta para o mesmo commit.

## Etapa 3 — Cobertura (JaCoCo) no `build.gradle`
- plugin `jacoco`, `toolVersion = '0.8.13'` (ou a mais recente compatível);
- tarefas `jacocoMergedReport` (JacocoReport, xml e html) e `jacocoMergedVerification`
  (JacocoCoverageVerification) lendo `build/jacoco-merged/**/*.exec`, com `sourceSets
  sourceSets.main`;
- mínimo de linhas = cobertura medida na etapa 0 menos ~5 pontos (arredonde para baixo).
  Informe o valor escolhido e por quê.

## Etapa 4 — Dependências seguras
Depois do primeiro dependency review, trate os avisos assim:
- prefira atualizar o Spring Boot para a última 3.5.x/da linha usada;
- se um componente do BOM ainda vier vulnerável, fixe a versão com
  `ext['tomcat.version']`, `ext['jackson-bom.version']` etc., consultando a versão
  corrigida na API de advisories (`gh api advisories/<GHSA>`);
- o plugin do Spring Boot traz o próprio Jackson no classpath do build: se o review
  apontar versão antiga que `./gradlew buildEnvironment` mostra vir do plugin, fixe com
  `buildscript { dependencies { constraints { classpath('...:<versão corrigida>') } } }`
  no topo do `build.gradle`, antes de `plugins {}`;
- só use `allow-ghsas` para avisos SEM versão corrigida na linha adotada, listando os IDs
  explicitamente, com comentário do motivo e uma issue de acompanhamento. Isso é aceitar
  risco: peça meu OK antes. Nunca libere por severidade.
Revalide com `./gradlew clean test bootWar`, e `mysqlTest` se existir.

## Etapa 5 — Jenkinsfile
Mantenha/crie o `Jenkinsfile` com agente `any`, `timestamps()`, `disableConcurrentBuilds()`
e `skipDefaultCheckout(true)`, suporte a Windows (`bat`) e macOS/Linux (`sh`) via
`isUnix()`, e variáveis no bloco `environment` (`API_HEALTH_URL`, caminho do Tomcat,
`WAR_NAME`). Stages:
1. Checkout; 2. Validar Java; 3. Testes e build (`./gradlew clean test bootWar --no-daemon`);
4. Localizar WAR (falha se não houver); 5. Deploy no Tomcat (parar, remover versão
anterior, copiar WAR, iniciar); 6. Aguardar API iniciar (loop de ~24 tentativas contra
o health check, falhando ao final); 7. Disparar a pipeline de testes automatizados
(`build job: '<nome>', wait: false, propagate: false`, com try/catch que apenas avisa).
`post`: `junit(testResults: 'build/test-results/test/*.xml', allowEmptyResults: true)` e
`archiveArtifacts` do WAR. Corrija o laço do health check em `sh` (`do ... done`).
Não invente caminhos: pergunte os valores de Tomcat, porta e nome do job de testes.

## Etapa 6 — Proteção de branches (rulesets via `gh api`)
Só depois de a pipeline ter rodado ao menos uma vez (os checks precisam existir) e do
workflow estar na `main`/`develop`. Peça meu OK e então:
- Ruleset `protecao-main` (alvo `refs/heads/main`) e `protecao-develop`
  (alvo `refs/heads/develop`), `enforcement: active`, `bypass_actors: []`, com as regras:
  `deletion`, `non_fast_forward`, `pull_request` (0 aprovações exigidas, para trabalho
  solo), `required_status_checks` com `strict_required_status_checks_policy: false`
  (a promoção cria merge commit na `main`; com "branch atualizada" ligado travaria) e
  `code_scanning` (CodeQL, `security_alerts_threshold: high_or_higher`,
  `alerts_threshold: errors`).
- Checks obrigatórios (nomes exatos): na `main` — validate-git-constraints, build,
  testeController, testeService, testeRepository, gate-qualidade,
  gate-seguranca-codigo; na `develop` — esses mais gate-seguranca-dependencias
  (que na `main` NÃO entra, pois só roda em PR para a `develop`).
- Use `gh api -X POST repos/<owner>/<repo>/rulesets --input arquivo.json`
  (arquivo JSON temporário fora do repositório) e confira depois com `gh api` listando
  regras e contextos.
- Habilite `allow_auto_merge` no repositório (`gh api -X PATCH repos/<owner>/<repo> -F
  allow_auto_merge=true`).

## Etapa 7 — Passos manuais do usuário (o agente pode ser impedido de fazê-los)
Entregue estas instruções ao usuário e aguarde confirmação antes de seguir:
1. Settings → Actions → General → Workflow permissions → marcar
   "Allow GitHub Actions to create and approve pull requests" (sem isso o job
   `merge-automatico-main` falha com "GitHub Actions is not permitted to create or
   approve pull requests"). Alternativa:
   `gh api -X PUT repos/<owner>/<repo>/actions/permissions/workflow -f
   default_workflow_permissions=read -F can_approve_pull_request_reviews=true`.
2. Settings → Advanced Security → habilitar o Dependency graph (sem isso o dependency
   review falha com "Dependency graph is not enabled").
3. Se a IDE reclamar de versão do Java/Gradle, ajustar Settings → Build Tools → Gradle →
   Gradle JVM para o JDK do projeto.

## Etapa 8 — Entrega e verificação ponta a ponta
1. Commits pequenos e descritivos; push da branch e PR para a `develop` (nunca direto
   para a `main`). Acompanhe os checks com `gh pr checks <n>` (use laço com espera, não
   `sleep` encadeado) e me reporte o resultado real de cada job.
2. Depois que EU fizer o merge na `develop`, confira: o CI do push na `develop` passou, o
   job `merge-automatico-main` abriu e mergeou o PR `develop` → `main`, a `main` recebeu
   o merge commit e nenhuma issue de falha foi aberta.
3. Teste das barreiras com PRs descartáveis (peça autorização, nunca faça merge deles,
   feche com `gh pr close --delete-branch` ao final):
   - uma feature direto para a `main` deve falhar no `validate` e ficar `BLOCKED`;
   - um PR saudável para a `develop` deve ficar `BLOCKED` enquanto a pipeline roda e
     `CLEAN` quando os checks passarem.
4. Relatório final: tabela do que foi criado, o que foi verificado de fato, o que NÃO foi
   testado (ex.: falha real de teste, alerta real do CodeQL, `notificar-falha`) e as
   decisões de risco que dependem de mim.
````

---

## Observações de uso

- O prompt segue a ordem em que aprendemos as dependências: workflow → cobertura →
  dependências → Jenkinsfile → rulesets (só depois de os checks existirem) → passos manuais.
- Os nomes dos jobs são o contrato com os rulesets. Se mudar um nome, atualize o ruleset
  junto, ou o merge trava esperando um check que nunca mais aparece.
- Para projetos Maven, peça ao agente na etapa 0 para adaptar: `jacoco-maven-plugin`,
  `mvn -Dtest=...` no lugar de `--tests` e `dependency-submission` do Maven.
