# Guia de manutenção

Contexto do projeto para quem continua o desenvolvimento (pessoas ou assistentes de código).
Visão geral e operação estão no [README](README.md).

## Fatos do projeto

| Item | Valor |
|---|---|
| Repositório | `tenoriodouglas/trackar` (privado), branch `main` |
| Firebase / GCP | projeto `trackar-e3945`, número `726109189400` |
| Regiões | Firestore e Functions: `southamerica-east1` · VM e Artifact Registry: `us-east1` (zona `us-east1-b`) |
| VM | `traccar`, `e2-micro`, Debian 12, IP fixo `traccar-ip` (hoje `34.139.236.244`) |
| Pacotes Android | `app.trackar` / `app.trackar.debug` |
| Portal | https://trackar-e3945.web.app (Firebase Hosting, pasta `web/admin`) |
| Conta de deploy | `github-deployer@trackar-e3945.iam.gserviceaccount.com` (secret `FIREBASE_SERVICE_ACCOUNT`) |
| Secrets no Secret Manager | `INGEST_TOKEN` (Traccar → function), `TRACCAR_ADMIN_PASSWORD` (ponte → Traccar) |

## Regras do dono do projeto

- **Orçamento: no máximo R$ 50/mês** (hoje ≈ R$ 20). Antes de adicionar recurso pago, estimar custo e preferir cota grátis.
- **CI/CD completo** em toda mudança: testes, build e deploy automatizados; secrets nunca no código; workflows
  pulam deploy com aviso quando falta secret.
- **Sem referências a ferramentas de IA** em código, comentários, commits ou PRs.
- Comentários de código **em inglês**, curtos e explicativos. Textos de interface em pt-BR (com `values/` em inglês).
- Código bem estruturado e nos padrões de cada plataforma (Clean Architecture/MVVM no app, funções puras testáveis no backend).
- Respostas e explicações ao dono: em português, breves e diretas.
- **Tudo no Google** (Firebase/GCP) para backend e hospedagem.
- Repositório **privado** por enquanto (consequência: atualização automática da APK inativa).

## Decisões de arquitetura (e por quê)

| Decisão | Motivo |
|---|---|
| Traccar na VM em vez de decodificar protocolos à mão | 200+ protocolos prontos; rastreadores usam TCP/UDP, que Functions não aceitam |
| VM `e2-micro` em `us-east1` | cota grátis; latência irrelevante para GPS |
| Firestore como fonte única da verdade | listeners em tempo real e cache offline para app e portal |
| Escritas só via Functions/Admin SDK | regras do Firestore negam escrita de clientes; validação no servidor |
| Ponte na VM puxando comandos do Firestore | nada exposto além das portas dos rastreadores; sem VPC connector (custo) |
| `metadata_startup_script` na VM | mudança no script recria a VM e garante que ele rode; IP fixo é preservado |
| Portal no Firebase Hosting | grátis, HTTPS, sem consumir memória da VM; mesmo site para cliente e admin |
| Papel admin por custom claim `admin` | simples, verificado nas regras e nas functions |
| Bloqueio imediato em qualquer velocidade | decisão do dono; a UI alerta quando o veículo está em movimento |
| Confirmação, cooldown 10 s, expiração 2 min, log de comandos | evita toques acidentais, flood ao rastreador e comandos "atrasados" |
| Documento do veículo = IMEI | casa direto com o `uniqueId` do Traccar |
| Histórico com id = `fixTime` em ms e TTL de 90 dias | idempotente para reenvios do Traccar; armazenamento limitado |

## Fluxos principais

- **Posição:** rastreador → Traccar (`forward.url`) → `ingestPosition` (`backend/functions/src/ingest.ts`) →
  `vehicles/{imei}` (atual, só se mais nova) + `vehicles/{imei}/positions/{ms}`.
- **Comando:** `sendCommand` (`commands.ts`) cria `vehicles/{imei}/commands/{id}` com `status=pending` e espelha em
  `lastCommand` → ponte (`backend/bridge/src/main.ts`) reivindica, chama `POST /api/commands/send` no Traccar
  (200 = enviado, 202 = na fila) → atualiza status e `engineBlocked`. Resposta do aparelho chega em `lastCommandResult`.
- **Vínculo:** `claimVehicle` (`vehicles.ts`) aceita IMEI desconhecido ou sem dono; com dono só o admin transfere.
- **Offline:** `markOffline` a cada 5 min marca `offline` após 10 min sem dados.

## Como validar antes de enviar

```bash
./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug assembleRelease
(cd backend/functions && npm run typecheck && npm run test:emulator)   # emuladores auth + firestore
(cd backend/bridge && npm run typecheck && npm test)
(cd web/admin && npm run typecheck && npm test && npm run build)
(cd backend/infra && terraform fmt -check -recursive && terraform validate)
actionlint
```

Ao mexer na interface do portal, conferir visualmente com os emuladores (`VITE_USE_EMULATORS=true`).
Depois do push, acompanhar os workflows até ficarem verdes.

## Armadilhas conhecidas

- Projeto GCP novo vem com APIs desligadas: o workflow habilita as APIs base (`iam`, `cloudresourcemanager`,
  `serviceusage`, `storage`) antes do Terraform.
- O `google-services.json` precisa conter **os dois pacotes** (release e `.debug`).
- A APK de teste é assinada com `app/debug.keystore` (versionado de propósito). Sem chave fixa, cada build do CI
  teria assinatura nova e o Android recusaria a atualização ("conflito com pacote existente").
- APK instalada antes dos secrets usava Firebase placeholder: desinstalar e instalar de novo resolve.
- Strings Android com apóstrofo precisam de escape (`don\'t`).
- `ktlint` (estilo android_studio) remove vírgulas finais; rode `spotlessApply` antes de editar trechos copiados.
- Mudanças no `startup.sh.tftpl` recriam a VM (cerca de 1 min sem receber posições; o Traccar reenvia depois).
- A `e2-micro` tem 1 GB: Traccar com heap 320 MB, ponte limitada a 192 MB, swap de 1 GB.

## Pendências e próximos passos

- Configurar o rastreador físico do dono (modelo ainda não identificado; conector de 4 pinos, sem slot de chip visível).
- Validar o bloqueio ponta a ponta num rastreador com relé.
- Play Store: criar o app, política de privacidade (dados de localização), segurança dos dados, keystore e primeiro envio.
- Ideias: alertas push (ignição, cerca virtual, excesso de velocidade) via FCM; Crashlytics; atualização automática
  com repo privado via bucket público.
