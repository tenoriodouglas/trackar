import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { setGlobalOptions } from "firebase-functions";
import { logger } from "firebase-functions";
import { onRequest } from "firebase-functions/https";
import { defineInt, defineSecret, defineString } from "firebase-functions/params";
import { onSchedule } from "firebase-functions/scheduler";
import { isAuthorized } from "./auth.js";
import { markStaleVehiclesOffline, savePosition } from "./ingest.js";
import { InvalidPayloadError, parseForward } from "./traccar.js";

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
