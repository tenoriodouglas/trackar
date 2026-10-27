import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { logger, setGlobalOptions } from "firebase-functions";
import { CallableRequest, onCall, onRequest } from "firebase-functions/https";
import { defineInt, defineSecret, defineString } from "firebase-functions/params";
import { onSchedule } from "firebase-functions/scheduler";
import { isAuthorized } from "./auth.js";
import { requestCommand } from "./commands.js";
import { markStaleVehiclesOffline, savePosition } from "./ingest.js";
import { InvalidPayloadError, parseForward } from "./traccar.js";
import { requireCaller } from "./validation.js";
import * as vehicles from "./vehicles.js";

const region = defineString("REGION", { default: "southamerica-east1" });
const retentionDays = defineInt("HISTORY_RETENTION_DAYS", { default: 90 });
const offlineAfterMinutes = defineInt("OFFLINE_AFTER_MINUTES", { default: 10 });
// Shared with the Traccar VM; created by Terraform (backend/infra).
const ingestToken = defineSecret("INGEST_TOKEN");

initializeApp();
setGlobalOptions({ region, maxInstances: 10 });

/** Receives Traccar position forwarding (one fix per request). */
export const ingestPosition = onRequest({ secrets: [ingestToken], concurrency: 80 }, async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }
  if (!isAuthorized(req.get("authorization"), ingestToken.value())) {
    res.status(401).send("Unauthorized");
    return;
  }
  try {
    await savePosition(getFirestore(), parseForward(req.body), retentionDays.value());
    res.status(204).end();
  } catch (error) {
    if (error instanceof InvalidPayloadError) {
      // 4xx: Traccar must not retry malformed data.
      logger.warn("Rejected payload", { reason: error.message });
      res.status(400).send(error.message);
      return;
    }
    logger.error("Failed to store position", error);
    res.status(500).send("Internal Error");
  }
});

/** Flips vehicles to offline when they go silent. */
export const markOffline = onSchedule("every 5 minutes", async () => {
  const count = await markStaleVehiclesOffline(getFirestore(), offlineAfterMinutes.value());
  if (count > 0) logger.info(`Marked ${count} vehicle(s) offline`);
});

// ---------- Callable API (app and admin panel) ----------

const callable = <T>(handler: (request: CallableRequest) => Promise<T>) => onCall({ enforceAppCheck: false }, handler);
const caller = (request: CallableRequest) => requireCaller(request.auth);

export const sendCommand = callable((r) => requestCommand(getFirestore(), caller(r), r.data));
export const claimVehicle = callable((r) => vehicles.claimVehicle(getFirestore(), caller(r), r.data));
export const releaseVehicle = callable((r) => vehicles.releaseVehicle(getFirestore(), caller(r), r.data));

export const adminUpdateVehicle = callable((r) => vehicles.adminUpdateVehicle(getFirestore(), caller(r), r.data));
export const adminSetOwners = callable((r) => vehicles.adminSetOwners(getFirestore(), getAuth(), caller(r), r.data));
export const adminDeleteVehicle = callable((r) => vehicles.adminDeleteVehicle(getFirestore(), caller(r), r.data));
export const adminListUsers = callable((r) => vehicles.adminListUsers(getAuth(), caller(r)));
export const adminCreateUser = callable((r) => vehicles.adminCreateUser(getAuth(), caller(r), r.data));
export const adminSetUserAdmin = callable((r) => vehicles.adminSetUserAdmin(getAuth(), caller(r), r.data));
export const adminSetUserDisabled = callable((r) => vehicles.adminSetUserDisabled(getAuth(), caller(r), r.data));
