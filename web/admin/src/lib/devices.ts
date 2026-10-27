/**
 * Setup reference for common trackers sold in Brazil. Ports are Traccar defaults; SMS syntax varies by
 * firmware, so the manual of the exact model always wins. "000000"/"123456" are factory passwords.
 */
export interface DeviceProfile {
  id: string;
  name: string;
  examples: string;
  port: number;
  smsSetup: (ip: string, apn: string) => string[];
  blockSupport: string;
}

export const DEVICE_PROFILES: DeviceProfile[] = [
  {
    id: "gt06",
    name: "GT06 / Concox / clones chineses",
    examples: "GT06N, GT02, TK100, J16, ST-901, a maioria dos 'rastreadores com relé'",
    port: 5023,
    smsSetup: (ip, apn) => [`APN,${apn}#`, `SERVER,0,${ip},5023,0#`, "TIMER,30,180#", "GPRSON,1#"],
    blockSupport: "Sim (Relay,1# / Relay,0#)",
  },
  {
    id: "h02",
    name: "H02 / Sinotrack",
    examples: "ST-901, ST-906, ST-907 (Sinotrack)",
    port: 5013,
    smsSetup: (ip, apn) => [`8030000 ${apn}`, `8040000 ${ip} 5013`, "8050000 30"],
    blockSupport: "Sim (comando de corte)",
  },
  {
    id: "tk103",
    name: "Coban / TK103",
    examples: "TK103A/B, TK303, GPS103",
    port: 5001,
    smsSetup: (ip, apn) => [`apn123456 ${apn}`, `adminip123456 ${ip} 5001`, "gprs123456", "fix030s***n123456"],
    blockSupport: "Sim (stop123456 / resume123456)",
  },
  {
    id: "suntech",
    name: "Suntech",
    examples: "ST310U, ST340, ST4300",
    port: 5011,
    smsSetup: () => ["Configure pelo SyncTrack/SMS conforme o manual: servidor = IP abaixo, porta 5011"],
    blockSupport: "Sim (saída 1)",
  },
  {
    id: "teltonika",
    name: "Teltonika",
    examples: "FMB920, FMB120, FMC130",
    port: 5027,
    smsSetup: (ip, apn) => [`  setparam 2001:${apn};2004:${ip};2005:5027`],
    blockSupport: "Sim (setdigout 1)",
  },
  {
    id: "osmand",
    name: "Celular (Traccar Client)",
    examples: "App Traccar Client para testes",
    port: 5055,
    smsSetup: (ip) => [`No app, URL do servidor: http://${ip}:5055`],
    blockSupport: "Não",
  },
];

export const APNS: { carrier: string; apn: string }[] = [
  { carrier: "Vivo", apn: "zap.vivo.com.br" },
  { carrier: "Claro", apn: "claro.com.br" },
  { carrier: "TIM", apn: "timbrasil.br" },
  { carrier: "Oi", apn: "gprs.oi.com.br" },
  { carrier: "Chip M2M (ex.: Arqia, Algar)", apn: "consulte a operadora do chip" },
];
