# Trackar

Rastreamento veicular em tempo real para rastreadores M2M/GPS: **app Android**, **portal web** (clientes e
administração) e **backend no Google Cloud/Firebase**.

> **Trackar** (com *k*) é este produto: app, portal e backend.
> **Traccar** (com *cc*) é o servidor open source de terceiros ([traccar.org](https://www.traccar.org)) que roda
> na VM e decodifica os protocolos dos rastreadores. "Traccar Client" é o app deles, usado aqui só para testes.

## Sumário

1. [Endereços](#endereços)
2. [Arquitetura](#arquitetura)
3. [Funcionalidades](#funcionalidades)
4. [Estrutura do repositório](#estrutura-do-repositório)
5. [Stack](#stack)
6. [Configuração inicial](#configuração-inicial)
7. [Operação do dia a dia](#operação-do-dia-a-dia)
8. [CI/CD](#cicd)
9. [Custos](#custos)
10. [Desenvolvimento local](#desenvolvimento-local)
11. [Decisões e limitações](#decisões-e-limitações)
12. [Documentação detalhada](#documentação-detalhada)

## Endereços

| O quê | Onde |
|---|---|
| Portal web (clientes e admin) | https://trackar-e3945.web.app |
| APK de teste (link fixo) | `https://github.com/tenoriodouglas/trackar/releases/download/apk-latest/trackar-latest-debug.apk` |
| Servidor dos rastreadores | IP no portal → Configurar rastreador (hoje `34.139.236.244`), portas 5001–5250 |
| Projeto Firebase / GCP | `trackar-e3945` |
| Pacote Android | `app.trackar` (debug: `app.trackar.debug`) |

## Arquitetura

```
                        ┌──────────── Google Cloud (projeto trackar-e3945) ─────────────┐
Rastreador ─TCP/UDP─▶   │ VM e2-micro (us-east1, cota grátis)                            │
 (chip M2M)             │  ├─ Traccar  ── posição (HTTPS + token) ──▶ Cloud Function      │
      ▲                 │  └─ Ponte    ◀── comandos pendentes ───┐      ingestPosition    │
      └── bloqueio ─────┤                                         │            │           │
                        │ Cloud Functions (southamerica-east1)    │            ▼           │
                        │  sendCommand · claimVehicle · admin* ──▶ Firestore (tempo real)  │
                        │  markOffline (a cada 5 min)                          │           │
                        └──────────────────────────────────────────────────────┼───────────┘
                                                                               ▼
                                         App Android  ·  Portal web (Firebase Hosting)
```

- O rastreador **não fala com o Firebase**: o Traccar decodifica o protocolo e repassa cada posição à
  function `ingestPosition`, que grava no Firestore (posição atual + histórico).
- Comandos (bloqueio) seguem o caminho inverso: app/portal → `sendCommand` (valida) → Firestore →
  **ponte** na VM → API local do Traccar → rastreador. Nenhuma porta de administração fica exposta.
- Firestore é a **fonte única da verdade**; app e portal só leem dele (com cache offline) e escrevem via functions.

## Funcionalidades

**App Android**
- Mapa ao vivo estilo navegação: setas que giram conforme o rumo e deslizam entre posições, "seguir veículo",
  bolha de velocidade, busca e direção por extenso ("Indo para Nordeste").
- Histórico por dia: rota, reprodução do percurso, distância, velocidade máxima, tempo rodando e paradas.
- Bloqueio/desbloqueio remoto imediato, com confirmação (alerta se em movimento) e status em tempo real.
- Adicionar rastreador pelo IMEI e remover veículo da conta.
- Atualização automática da APK de teste (ver [limitações](#decisões-e-limitações)).

**Portal web — clientes** (qualquer usuário)
- Meus veículos: lista com botão **Bloquear/Desbloquear direto em cada veículo**, mapa, status, endereço, "como chegar".
- Painel do veículo: renomear, bloqueio, últimos comandos (quando e resultado), histórico, remover da conta.
- Histórico por dia com rota, paradas e resumo (mesmo cálculo do app).
- Adicionar rastreador pelo IMEI.
- Minha conta: trocar senha ou receber link de redefinição.

**Portal web — administração** (usuários com papel admin)
- Frota completa: mapa, totais (online, bloqueados, sem dono), editar, definir donos por e-mail, excluir.
- Usuários: criar conta de cliente com senha gerada, dar/tirar admin, desativar.
- Configurar rastreador: IP do servidor, comandos SMS por modelo, APNs, ligação do relé.

**Bloqueio imediato:** aceito em qualquer velocidade (decisão do dono do produto). A confirmação avisa
explicitamente quando o veículo está em movimento (motor desliga andando). Intervalo mínimo de 10 s entre comandos,
expira em 2 min sem entrega, registro de quem pediu. Se o rastreador estiver offline, o comando fica na fila.
Alguns modelos (ex.: vários GT06) só acionam o relé abaixo de ~20 km/h por trava no próprio firmware.

## Estrutura do repositório

```
app/                  Activity, tema/splash, navegação raiz, atualização automática
feature/auth          login
feature/map           mapa ao vivo
feature/vehicles      lista, detalhe, bloqueio, adicionar rastreador
feature/history       histórico do dia
core/model            modelos de domínio (Kotlin puro) + resumo de viagem
core/data             repositórios (fonte única da verdade)
core/firebase         Auth, Firestore e callables (data sources + mapeamento)
core/designsystem     tema e componentes Compose
core/common           dispatchers, escopos, relógio
core/testing          fakes e regras de teste
build-logic/          convention plugins do Gradle
backend/functions/    Cloud Functions (TypeScript): ingestão, comandos, vínculo, admin, scripts
backend/bridge/       ponte na VM: comandos do Firestore → API local do Traccar (Docker)
backend/infra/        Terraform: VM, IP fixo, firewall, secrets, Artifact Registry
web/admin/            portal web (React + Vite + Leaflet), clientes e admin
firebase/             regras e índices do Firestore
fastlane/             ficha da Play Store
docs/                 documentação detalhada
.github/              workflows de CI/CD e Dependabot
```

Dependências do app apontam sempre para dentro: `feature → data → firebase → model`.

## Stack

| Parte | Tecnologia |
|---|---|
| App | Kotlin 2.4, Jetpack Compose + Material 3, Navigation 3, Hilt, Coroutines/Flow, Google Maps Compose |
| Build Android | Gradle Kotlin DSL, version catalog, convention plugins, AGP 9, R8 |
| Backend | Firebase Auth, Cloud Firestore, Cloud Functions 2ª geração (Node 22, TypeScript) |
| Gateway | Traccar 6 (Docker) + ponte Node em VM Compute Engine, provisionada por Terraform |
| Portal | React 19, Vite, Leaflet/OpenStreetMap, Firebase JS SDK, Firebase Hosting |
| Qualidade | ktlint (Spotless), Android Lint, JUnit/Turbine, Vitest, emuladores Firebase, actionlint |
| CI/CD | GitHub Actions + Dependabot |

## Configuração inicial

Já feito para o projeto `trackar-e3945`; repita só para um ambiente novo.

1. **Firebase:** projeto no plano **Blaze**; apps Android `app.trackar` e `app.trackar.debug`;
   **Authentication → E-mail/senha**; **Firestore** em `southamerica-east1`.
2. **Conta de serviço de deploy** (`github-deployer`) com papel **Proprietário** (ou: Editor, Secret Manager Admin,
   Project IAM Admin, Cloud Functions Admin, Service Account User, Firebase Admin). Chave JSON direto no secret.
3. **Chave do Maps SDK for Android** restrita a apps Android e à API Maps SDK.
4. **Secrets do GitHub** (Settings → Secrets and variables → Actions):

| Secret | Uso | Obrigatório |
|---|---|---|
| `GOOGLE_SERVICES_JSON_BASE64` | `base64 -w0 google-services.json` (contém os dois pacotes) | sim |
| `FIREBASE_SERVICE_ACCOUNT` | JSON da conta de serviço de deploy | sim |
| `MAPS_API_KEY` | chave do Maps SDK for Android | sim |
| `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | assinatura do AAB | só Play Store |
| `PLAY_SERVICE_ACCOUNT_JSON` | envio à Play Store | só Play Store |

5. Rode **Actions → Backend CI/CD** (cria VM, ponte, functions) e **Actions → Set admin** com o seu e-mail.

Sem secrets, todos os builds e testes rodam (com Firebase placeholder); só os deploys são pulados com aviso.

## Operação do dia a dia

| Tarefa | Como |
|---|---|
| Criar conta de cliente | Portal → Usuários → Novo usuário (senha gerada, mostrada uma vez) |
| Dar acesso de admin | Portal → Usuários → admin, ou **Actions → Set admin** |
| Configurar um rastreador | Portal → Configurar rastreador (IP, comandos SMS, APN) · [docs/devices.md](docs/devices.md) |
| Vincular rastreador a um cliente | Cliente no app/portal (Adicionar rastreador), ou admin em Frota → Donos, ou **Actions → Register vehicle** |
| Bloquear/desbloquear | App (detalhe do veículo) ou portal (painel do veículo) |
| Testar sem rastreador | App **Traccar Client** no celular com `http://<IP>:5055` e vincular o identificador |
| Abrir o Traccar (admin técnico) | `gcloud compute ssh traccar --zone us-east1-b --tunnel-through-iap -- -N -L 8082:localhost:8082` e acessar `http://localhost:8082` |
| Publicar na Play Store | tag `v*` (`git tag v0.1.0 && git push --tags`) ou execução manual (`status=draft` na primeira vez) |

## CI/CD

| Workflow | Gatilho | O que faz |
|---|---|---|
| `android.yml` | todo push/PR | spotless, testes, lint, APK debug |
| | push na `main` | publica APK + `latest.json` na release `apk-latest` (link fixo) |
| | tag `v*` ou manual | AAB assinado → Google Play |
| `backend.yml` | `backend/**` | testes (emuladores), imagem da ponte, `terraform apply`, deploy das functions, IP no Firestore |
| `admin-web.yml` | `web/admin/**` | testes + build + deploy do portal no Firebase Hosting |
| `firestore-rules.yml` | `firebase/**` | regras, índices e TTL do histórico |
| `register-vehicle.yml` | manual | vincula IMEI a um usuário |
| `set-admin.yml` | manual | dá/tira papel de admin |
| `update-store-listing.yml` | manual | ficha da Play Store (fastlane) |
| `cleanup-storage.yml` | semanal | apaga artifacts antigos |

`versionCode` = número da execução do workflow, sempre crescente. A VM baixa novas imagens da ponte sozinha a cada 5 min.

## Custos

Meta: **até R$ 50/mês**. Estimativa atual: **≈ R$ 20/mês**.

| Item | Custo |
|---|---|
| VM `e2-micro` + disco 30 GB (us-east1) | grátis (cota: 1 por conta de faturamento) |
| IP fixo IPv4 | ≈ US$ 3,65/mês (único custo fixo) |
| Functions, Firestore, Scheduler, Hosting, Artifact Registry, Secret Manager | cota grátis para frotas pequenas (~30 veículos reportando por minuto) |

Mantenha um **alerta de orçamento de R$ 40** em Faturamento → Orçamentos e alertas.
O histórico expira em 90 dias (TTL), limitando o armazenamento.

## Desenvolvimento local

```bash
# App Android (JDK 17+, Android SDK 37)
./gradlew spotlessApply testDebugUnitTest lintDebug assembleDebug

# Functions (Node 22) — testes unitários e de integração com emuladores (precisa de Java)
cd backend/functions && npm ci && npm run typecheck && npm run test:emulator

# Ponte
cd backend/bridge && npm ci && npm test

# Portal web
cd web/admin && npm ci && npm test && npm run build
# Rodar contra os emuladores: firebase emulators:start --only auth,firestore,functions
VITE_USE_EMULATORS=true VITE_FIREBASE_CONFIG='{"apiKey":"demo","projectId":"demo-trackar","appId":"1:1:web:1"}' npm run dev

# Infra
cd backend/infra && terraform fmt -check && terraform init -backend=false && terraform validate
```

Arquivos locais (fora do git): `app/google-services.json` (ou copie o `.placeholder`) e `local.properties`
com `MAPS_API_KEY=...`.

## Decisões e limitações

- **Rastreador → Traccar → Function:** rastreadores usam TCP/UDP próprio; Cloud Functions só aceitam HTTP.
  O Traccar cobre 200+ protocolos e roda na VM grátis.
- **Portal no Firebase Hosting, não na VM:** HTTPS e domínio grátis, sem disputar a memória de 1 GB com o Traccar.
- **Ponte em vez de expor o Traccar:** a VM só abre as portas dos rastreadores; comandos chegam pelo Firestore.
- **Bloqueio imediato em qualquer velocidade**, com confirmação que alerta sobre corte em movimento.
- **Atualização automática da APK** depende de o repositório ser público (o app não guarda token do GitHub).
  Com o repo privado ela fica inativa sem erro; alternativa: publicar a APK num bucket público.
- **Recriar a VM** (mudança no script de inicialização) mantém o IP; perde-se só o cadastro interno do Traccar,
  que se refaz sozinho. Posições ficam no Firestore.
- **Convenções:** comentários de código em inglês e curtos; commits sem referência a ferramentas de IA;
  CI/CD completo em todo projeto. Detalhes em [AGENTS.md](AGENTS.md).

## Documentação detalhada

- [docs/devices.md](docs/devices.md) — configurar rastreadores, comandos SMS, APN, ligação elétrica e bloqueio
- [docs/gateway.md](docs/gateway.md) — gateway no Google Cloud, Terraform, ponte, acesso ao Traccar
- [docs/firestore-schema.md](docs/firestore-schema.md) — coleções, campos, comandos e regras
- [AGENTS.md](AGENTS.md) — contexto, decisões e convenções para quem mantém o projeto
