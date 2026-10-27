import { Firestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/https";
import { VEHICLES } from "./ingest.js";
import { Caller, requireUniqueId } from "./validation.js";

export const COMMANDS = "commands";
export const COMMAND_TYPES = ["engineStop", "engineResume"] as const;
export type CommandType = (typeof COMMAND_TYPES)[number];

/** Prevents accidental double taps and command floods to the tracker. */
export const COMMAND_COOLDOWN_MS = 10_000;

export type CommandStatus = "pending" | "processing" | "sent" | "queued" | "failed";

interface VehicleData {
  ownerIds?: string[];
  lastCommand?: { requestedAt?: Timestamp };
}

export interface Rejection {
  code: "permission-denied" | "resource-exhausted";
  message: string;
}

/**
 * Pure policy check for a command request; null means allowed.
 * Blocking is immediate at any speed (owner's decision); clients warn when the vehicle is moving.
 */
export function checkCommand(vehicle: VehicleData, caller: Caller, now: Date): Rejection | null {
  if (!caller.isAdmin && !(vehicle.ownerIds ?? []).includes(caller.uid)) {
    return { code: "permission-denied", message: "Not your vehicle." };
  }
  const lastRequest = vehicle.lastCommand?.requestedAt?.toMillis();
  if (lastRequest && now.getTime() - lastRequest < COMMAND_COOLDOWN_MS) {
    return { code: "resource-exhausted", message: "Wait a few seconds before sending another command." };
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

    const rejection = checkCommand(snapshot.data() as VehicleData, caller, now);
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
