import { VehicleDoc } from "./types";

const FRESHNESS_MS = 10 * 60_000;
const MOVING_THRESHOLD_KMH = 3;

/** Current speed when the last fix is recent and shows movement; null when stopped or stale. */
export function movingSpeedKmh(vehicle: VehicleDoc, now = Date.now()): number | null {
  const position = vehicle.position;
  if (!position) return null;
  const fresh = now - position.fixTime.toMillis() < FRESHNESS_MS;
  const speed = Math.round(position.speedKmh);
  return fresh && speed >= MOVING_THRESHOLD_KMH ? speed : null;
}

/** Confirmation text for a block; warns explicitly when the engine would be cut while moving. */
export function blockConfirmation(vehicle: VehicleDoc, now = Date.now()): string {
  const speed = movingSpeedKmh(vehicle, now);
  const moving = speed
    ? `⚠️ O veículo está a ${speed} km/h. O motor vai desligar EM MOVIMENTO: a direção e o freio podem ficar pesados.\n\n`
    : "";
  return `${moving}Bloquear ${vehicle.name} agora?\n\nO rastreador vai cortar a bomba de combustível ou a ignição imediatamente.`;
}
