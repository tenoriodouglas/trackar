/**
 * Trackar bridge: runs next to Traccar on the gateway VM. Listens for pending commands in Firestore and
 * delivers them through Traccar's local API. Nothing is exposed to the internet; credentials come from
 * the VM service account (Firestore) and Secret Manager (Traccar password, injected as env).
 */
import { initializeApp } from "firebase-admin/app";
import { DocumentSnapshot, getFirestore, Timestamp } from "firebase-admin/firestore";
import { executeCommand } from "./executor.js";
import { TraccarClient } from "./traccarClient.js";

const env = (name: string, fallback?: string) => {
  const value = process.env[name] ?? fallback;
  if (!value) throw new Error(`Missing env ${name}`);
  return value;
};

const traccar = new TraccarClient(
  env("TRACCAR_URL", "http://127.0.0.1:8082"),
  env("TRACCAR_EMAIL", "bridge@trackar.local"),
  env("TRACCAR_PASSWORD"),
);

initializeApp();
const db = getFirestore();
const log = (message: string, extra: Record<string, unknown> = {}) =>
  console.log(JSON.stringify({ severity: "INFO", message, ...extra }));

/** Traccar takes a while to boot; retry until its API answers. */
async function waitForTraccar() {
  for (let attempt = 1; ; attempt++) {
    try {
      await traccar.ensureAdmin();
      log("Traccar ready");
      return;
    } catch (error) {
      log("Waiting for Traccar", { attempt, error: String(error) });
      await new Promise((resolve) => setTimeout(resolve, Math.min(attempt * 5_000, 30_000)));
    }
  }
}

/** Claims a pending command atomically so a restarted bridge never sends it twice. */
async function claim(snapshot: DocumentSnapshot): Promise<boolean> {
  return db.runTransaction(async (tx) => {
    const fresh = await tx.get(snapshot.ref);
    if (fresh.get("status") !== "pending") return false;
    tx.update(snapshot.ref, { status: "processing", updatedAt: Timestamp.now() });
    return true;
  });
}

async function handle(snapshot: DocumentSnapshot) {
  if (!(await claim(snapshot))) return;
  const vehicleRef = snapshot.ref.parent.parent!;
  const type = snapshot.get("type") as string;
  const requestedAt = snapshot.get("requestedAt") as Timestamp;

  const outcome = await executeCommand({ uniqueId: vehicleRef.id, type, requestedAtMs: requestedAt.toMillis() }, traccar);
  const update = {
    status: outcome.status,
    errorCode: outcome.status === "failed" ? outcome.errorCode : null,
    message: outcome.status === "failed" ? outcome.message : null,
    updatedAt: Timestamp.now(),
  };
  log("Command processed", { vehicle: vehicleRef.id, type, ...update });

  await db.runTransaction(async (tx) => {
    const vehicle = await tx.get(vehicleRef);
    tx.update(snapshot.ref, update);
    // Only mirror on the vehicle if no newer command replaced it meanwhile.
    if (vehicle.get("lastCommand.id") !== snapshot.id) return;
    const vehicleUpdate: Record<string, unknown> = {
      "lastCommand.status": update.status,
      "lastCommand.errorCode": update.errorCode,
      "lastCommand.message": update.message,
      "lastCommand.updatedAt": update.updatedAt,
    };
    if (outcome.status !== "failed") vehicleUpdate.engineBlocked = type === "engineStop";
    tx.update(vehicleRef, vehicleUpdate);
  });
}

await waitForTraccar();
db.collectionGroup("commands")
  .where("status", "==", "pending")
  .onSnapshot(
    (snapshot) => {
      for (const change of snapshot.docChanges()) {
        if (change.type === "added") void handle(change.doc).catch((error) => log("Command failed", { error: String(error) }));
      }
    },
    (error) => {
      // Let Docker restart the container and re-subscribe.
      console.error(JSON.stringify({ severity: "ERROR", message: "Listener failed", error: String(error) }));
      process.exit(1);
    },
  );
log("Listening for commands");
