# Trackar

Rastreamento veicular em tempo real para dispositivos M2M/GPS. Android nativo.

## Funcionalidades

- **Mapa ao vivo estilo navegação:** carros como setas que giram conforme o rumo e deslizam entre as
  posições, modo "seguir veículo", bolha de velocidade, busca rápida e direção por extenso ("Indo para Nordeste").
- **Histórico de trajetos:** escolha o dia, veja a rota no mapa, reproduza o percurso e confira
  distância, velocidade máxima, tempo rodando e paradas.
- **Frota:** lista com busca, status online/offline, ignição, bateria e endereço.
- **Offline:** últimas posições ficam em cache.

## Stack

| Camada        | Tecnologia                                                        |
|---------------|-------------------------------------------------------------------|
| Linguagem     | Kotlin 2.4 (100%)                                                 |
| UI            | Jetpack Compose + Material 3 + layout adaptativo (Navigation Suite) |
| Navegação     | Navigation 3 (type-safe, `NavKey` serializável)                   |
| Arquitetura   | Clean Architecture + MVVM / UDF (`StateFlow` imutável)            |
| DI            | Hilt (KSP)                                                        |
| Backend       | Firebase Auth + Cloud Firestore (listeners em tempo real, cache offline) |
| Mapa          | Google Maps Compose                                               |
| Assíncrono    | Coroutines + Flow                                                 |
| Build         | Gradle (Kotlin DSL), version catalog, convention plugins, AGP 9   |
| Qualidade     | ktlint (Spotless), Android Lint, testes unitários (JUnit, Turbine) |
| CI/CD         | GitHub Actions (build, testes, APK de teste, Play Store, regras Firebase) + Dependabot |

## Módulos

```
app                 -> Activity, tema/splash, navegação raiz
feature/auth        -> login (e-mail/senha)
feature/map         -> mapa com a frota em tempo real
feature/vehicles    -> lista com busca + detalhe do veículo
feature/history     -> histórico do dia: rota, reprodução e resumo
core/data           -> repositórios (fonte única da verdade)
core/firebase       -> Auth e Firestore (data sources + mapeamento)
core/model          -> modelos de domínio (Kotlin puro)
core/designsystem   -> tema e componentes Compose
core/common         -> dispatchers, escopos, utilitários
core/testing        -> fakes e regras de teste
build-logic         -> convention plugins do Gradle
```

Dependências apontam sempre para dentro: `feature -> data -> firebase -> model`.

## Fluxo dos dados

```
Rastreador ─TCP/UDP─▶ Gateway (ex.: Traccar → Cloud Function) ─▶ Firestore ─tempo real─▶ App
```

O rastreador não fala com o Firebase diretamente; um gateway decodifica o protocolo e grava no
Firestore. Schema e regras: [docs/firestore-schema.md](docs/firestore-schema.md).

## Configuração

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com), adicione os apps
   Android `app.trackar` e `app.trackar.debug` e ative **Authentication → E-mail/senha** e **Firestore**.
2. Baixe o `google-services.json` do projeto e salve em `app/google-services.json` (ignorado pelo git;
   sem ele, copie `app/google-services.placeholder.json` só para compilar).
3. Publique as regras: `firebase deploy --only firestore`.
4. Gere uma chave do Maps SDK for Android e adicione em `local.properties`:
   ```properties
   MAPS_API_KEY=sua_chave
   ```

## CI/CD (GitHub Actions)

| Workflow | Gatilho | O que faz |
|---|---|---|
| `android.yml` · Build & Test | todo push e PR | spotless, testes, lint, APK debug |
| `android.yml` · rolling APK | push na `main` | publica `trackar-latest-debug.apk` na release `apk-latest` (link fixo) |
| `android.yml` · Release | tag `v*` ou manual | AAB assinado → Google Play (track à escolha; manual = `draft`) |
| `firestore-rules.yml` | mudança em `firebase/**` na `main` ou manual | deploy de regras e índices |
| `update-store-listing.yml` | manual | ficha da loja via fastlane (`fastlane/metadata`) |
| `cleanup-storage.yml` | semanal ou manual | apaga artifacts antigos |

`versionCode` = número da execução (`VERSION_CODE`), sempre crescente.

Link fixo da APK de teste:
`https://github.com/tenoriodouglas/trackar/releases/download/apk-latest/trackar-latest-debug.apk`

### Secrets (Settings → Secrets and variables → Actions)

| Secret | Uso |
|---|---|
| `GOOGLE_SERVICES_JSON_BASE64` | `base64 -w0 app/google-services.json` (obrigatório no release) |
| `MAPS_API_KEY` | chave do Maps SDK for Android |
| `KEYSTORE_BASE64` | `base64 -w0 upload-keystore.jks` |
| `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` | dados do keystore |
| `PLAY_SERVICE_ACCOUNT_JSON` | JSON da service account com acesso ao Play Console |
| `FIREBASE_SERVICE_ACCOUNT` | JSON da service account do Firebase (deploy de regras) |

Sem os secrets, o build/teste roda normalmente (Firebase placeholder) e só o deploy falha.

Gerar keystore de upload:
```bash
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
```

Primeiro release: rode o workflow manualmente com `status=draft`, crie o app `app.trackar` no Play
Console e conclua a primeira publicação por lá; depois use tags `v*` (`git tag v0.1.0 && git push --tags`).

## Comandos

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # testes
./gradlew lintDebug            # lint
./gradlew spotlessApply        # formatação
```

Requisitos: JDK 17+, Android SDK 37.
