import { Firestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/https";
import { VEHICLES } from "./ingest.js";
import { Caller, requireUniqueId } from "./validation.js";

export const COMMANDS = "commands";
export const COMMAND_TYPES = ["engineStop", "engineResume"] as const;
export type CommandType = (typeof COMMAND_TYPES)[number];

/** Market practice: cutting fuel/ignition is refused while the vehicle is clearly moving. */
export const MAX_BLOCK_SPEED_KMH = 20;
/** Speed older than this says nothing about the current state, so it is not enforced. */
const SPEED_FRESHNESS_MS = 10 * 60_000;
/** Prevents accidental double taps and command floods to the tracker. */
export const COMMAND_COOLDOWN_MS = 10_000;

export type CommandStatus = "pending" | "processing" | "sent" | "queued" | "failed";

interface VehicleData {
  ownerIds?: string[];
  position?: { speedKmh?: number; fixTime?: Timestamp };
  lastCommand?: { requestedAt?: Timestamp };
}

export interface Rejection {
  code: "permission-denied" | "failed-precondition" | "resource-exhausted";
  message: string;
}

/** Pure policy check for a command request; null means allowed. */
export function checkCommand(vehicle: VehicleData, caller: Caller, type: CommandType, now: Date): Rejection | null {
  if (!caller.isAdmin && !(vehicle.ownerIds ?? []).includes(caller.uid)) {
    return { code: "permission-denied", message: "Not your vehicle." };
  }
  const lastRequest = vehicle.lastCommand?.requestedAt?.toMillis();
  if (lastRequest && now.getTime() - lastRequest < COMMAND_COOLDOWN_MS) {
    return { code: "resource-exhausted", message: "Wait a few seconds before sending another command." };
  }
  const fixTime = vehicle.position?.fixTime?.toMillis();
  const speed = vehicle.position?.speedKmh ?? 0;
  const fresh = fixTime !== undefined && now.getTime() - fixTime < SPEED_FRESHNESS_MS;
  if (type === "engineStop" && fresh && speed > MAX_BLOCK_SPEED_KMH) {
    return {
      code: "failed-precondition",
      message: `Vehicle is moving (${Math.round(speed)} km/h). Blocking is allowed below ${MAX_BLOCK_SPEED_KMH} km/h.`,
    };
  }
  return null;
}

/**
 * Queues a command for the Traccar bridge running on the gateway VM.
 * The vehicle mirrors it in `lastCommand`, so apps follow progress with their existing listener.
 */
export async function requestCommand(db: Firestore, caller: Caller, data: unknown, now = new Date()) {
  const input = (data ?? {}) as { vehicleId?: unknown; type?: unknown };
  const vehicleId = requireUniqueId(input.vehicleId);
  const type = input.type as CommandType;
  if (!COMMAND_TYPES.includes(type)) throw new HttpsError("invalid-argument", "Unsupported command.");

  const vehicleRef = db.collection(VEHICLES).doc(vehicleId);
  return db.runTransaction(async (tx) => {
    const snapshot = await tx.get(vehicleRef);
    if (!snapshot.exists) throw new HttpsError("not-found", "Vehicle not found.");

    const rejection = checkCommand(snapshot.data() as VehicleData, caller, type, now);
    if (rejection) throw new HttpsError(rejection.code, rejection.message);

    const commandRef = vehicleRef.collection(COMMANDS).doc();
    const timestamp = Timestamp.fromDate(now);
    const command = {
      type,
      status: "pending" as CommandStatus,
      requestedBy: caller.uid,
      requestedAt: timestamp,
      updatedAt: timestamp,
      message: null,
    };
    tx.set(commandRef, command);
    tx.update(vehicleRef, { lastCommand: { id: commandRef.id, ...command } });
    return { commandId: commandRef.id };
  });
}
