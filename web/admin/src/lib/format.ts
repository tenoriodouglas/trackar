/** Relative time in Portuguese ("há 5 min"), or a dash when unknown. */
export function relativeTime(date: Date | undefined, now = new Date()): string {
  if (!date) return "—";
  const seconds = Math.round((now.getTime() - date.getTime()) / 1000);
  if (seconds < 60) return "agora";
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `há ${minutes} min`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `há ${hours} h`;
  const days = Math.round(hours / 24);
  return `há ${days} ${days === 1 ? "dia" : "dias"}`;
}

const COMMAND_STATUS: Record<string, string> = {
  pending: "Enviando…",
  processing: "Enviando…",
  sent: "Entregue ao rastreador",
  queued: "Aguardando o rastreador conectar",
  failed: "Falhou",
};

const COMMAND_ERRORS: Record<string, string> = {
  unsupported: "modelo não suporta bloqueio",
  device_not_connected: "rastreador nunca conectou",
  expired: "expirou",
  gateway_error: "erro no servidor",
};

export function commandLabel(status: string, errorCode?: string | null): string {
  const base = COMMAND_STATUS[status] ?? status;
  return status === "failed" && errorCode ? `${base}: ${COMMAND_ERRORS[errorCode] ?? errorCode}` : base;
}

/** Strong random password for new users (shown once to the admin). */
export function generatePassword(length = 14): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
  const bytes = crypto.getRandomValues(new Uint32Array(length));
  return Array.from(bytes, (b) => alphabet[b % alphabet.length]).join("");
}
