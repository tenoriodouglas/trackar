# Trackar

Rastreamento veicular em tempo real para dispositivos M2M/GPS. Android nativo.

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
| CI            | GitHub Actions + Dependabot                                       |

## Módulos

```
app                 -> Activity, tema/splash, navegação raiz
feature/auth        -> login (e-mail/senha)
feature/map         -> mapa com a frota em tempo real
feature/vehicles    -> lista com busca + detalhe do veículo
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
2. Substitua `app/google-services.json` (placeholder) pelo arquivo real do seu projeto.
3. Publique as regras: `firebase deploy --only firestore`.
4. Gere uma chave do Maps SDK for Android e adicione em `local.properties`:
   ```properties
   MAPS_API_KEY=sua_chave
   ```

## Comandos

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # testes
./gradlew lintDebug            # lint
./gradlew spotlessApply        # formatação
```

Requisitos: JDK 17+, Android SDK 37.
