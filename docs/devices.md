# Configurar um rastreador

## Como funciona

```
Rastreador ─(chip M2M, TCP/UDP)─▶ Traccar (VM, IP fixo) ─▶ Cloud Function ─▶ Firestore ─▶ App / Painel
                     ▲                                                              │
                     └── comando (bloqueio) ◀── ponte (VM) ◀── fila "commands" ◀───┘
```

O Traccar entende 200+ protocolos. Cada protocolo escuta numa porta própria (5001–5250).
O IP e os comandos prontos para o seu modelo estão no painel admin → **Configurar rastreador**.

## Passo a passo

1. **Chip:** o rastreador precisa de um chip com dados (M2M ou pré/pós comum). Anote o número da linha
   e a operadora (APN).
2. **Identifique o modelo:** etiqueta com marca, modelo e **IMEI** (15 dígitos).
3. **Configure por SMS** (do seu celular para o número do chip do rastreador):

| Protocolo | Exemplos | Porta | Comandos (troque IP e APN) | Bloqueio |
|---|---|---|---|---|
| GT06 / Concox | GT06N, J16, "rastreador com relé" | 5023 | `APN,zap.vivo.com.br#` · `SERVER,0,IP,5023,0#` · `TIMER,30,180#` | sim |
| H02 / Sinotrack | ST-901, ST-906 | 5013 | `8030000 APN` · `8040000 IP 5013` · `8050000 30` | sim |
| Coban / TK103 | TK103A/B, TK303 | 5001 | `apn123456 APN` · `adminip123456 IP 5001` · `gprs123456` | sim |
| Suntech | ST310U, ST340 | 5011 | software/SMS do fabricante (servidor = IP, porta 5011) | sim |
| Teltonika | FMB920, FMB120 | 5027 | `  setparam 2001:APN;2004:IP;2005:5027` | sim |
| Celular (teste) | Traccar Client | 5055 | URL `http://IP:5055` | não |

Senhas de fábrica comuns: `123456` ou `0000`. A sintaxe muda entre firmwares: o manual do modelo prevalece.

APNs: Vivo `zap.vivo.com.br` · Claro `claro.com.br` · TIM `timbrasil.br` · Oi `gprs.oi.com.br` · chips M2M: consulte a operadora.

4. **Vincule à conta** (qualquer um):
   - App → Veículos → **Adicionar rastreador** (IMEI + nome);
   - Painel admin → Configurar rastreador → Vincular (e depois Veículos → Donos para passar a um cliente);
   - GitHub → Actions → **Register vehicle**.

   Um rastreador já vinculado a outra conta só pode ser transferido por um administrador.

## Instalação elétrica (conector de 4 pinos típico)

| Fio | Ligação |
|---|---|
| Vermelho | +12 V direto da bateria, com fusível de 1–2 A |
| Preto | negativo / terra |
| Amarelo ou branco | pós-chave (ignição) |
| 4º fio (saída) | bobina do **relé de bloqueio** |

**Bloqueio:** a saída do rastreador aciona um relé 12 V que interrompe a bomba de combustível ou a linha da
ignição. Use o contato **normalmente fechado (NF)** do relé: se o rastreador desligar, o carro continua funcionando.
Recomenda-se instalação por profissional.

## Segurança do bloqueio (padrão de mercado)

- Confirmação explícita no app e no painel.
- Recusado acima de **20 km/h** (velocidade dos últimos 10 min).
- Intervalo mínimo de 10 s entre comandos; comandos não entregues em 2 min expiram.
- Se o rastreador estiver offline, o Traccar guarda o comando e entrega quando ele reconectar
  (status "aguardando o rastreador conectar").
- Cada comando fica registrado em `vehicles/{imei}/commands` com quem pediu e o resultado.
