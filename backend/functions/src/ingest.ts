import { FieldValue, Firestore, Timestamp } from "firebase-admin/firestore";
import { ParsedForward } from "./traccar.js";

export const VEHICLES = "vehicles";
export const HISTORY = "positions";

/**
 * Stores one fix: appends it to the history (idempotent per fix time, so Traccar retries are safe)
 * and updates the vehicle's latest position only when the fix is newer.
 * Unknown devices are created without owners, ready to be assigned.
 */
export async function savePosition(db: Firestore, data: ParsedForward, retentionDays: number, now = new Date()) {
  const vehicleRef = db.collection(VEHICLES).doc(data.uniqueId);
  const historyRef = vehicleRef.collection(HISTORY).doc(String(data.position.fixTime.toMillis()));
  const expireAt = Timestamp.fromMillis(data.position.fixTime.toMillis() + retentionDays * 86_400_000);

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(vehicleRef);
    const latest = snapshot.get("position.fixTime") as Timestamp | undefined;
    const isNewer = !latest || latest.toMillis() < data.position.fixTime.toMillis();

    const update: Record<string, unknown> = {
      uniqueId: data.uniqueId,
      status: "online",
      lastUpdate: Timestamp.fromDate(now),
    };
    if (isNewer) update.position = data.position;
    if (!snapshot.exists) {
      update.name = data.deviceName;
      update.category = data.category;
      update.ownerIds = [];
      update.createdAt = FieldValue.serverTimestamp();
    }

    tx.set(vehicleRef, update, { merge: true });
    tx.set(historyRef, { ...data.position, expireAt });
  });
}

/** Marks vehicles offline when they stop reporting. Returns how many changed. */
export async function markStaleVehiclesOffline(db: Firestore, maxSilenceMinutes: number, now = new Date()) {
  const cutoff = Timestamp.fromMillis(now.getTime() - maxSilenceMinutes * 60_000);
  const stale = await db.collection(VEHICLES).where("status", "==", "online").where("lastUpdate", "<", cutoff).get();
  const writer = db.bulkWriter();
  stale.docs.forEach((doc) => writer.update(doc.ref, { status: "offline" }));
  await writer.close();
  return stale.size;
}
