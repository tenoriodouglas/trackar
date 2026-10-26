import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { beforeEach, describe, expect, it } from "vitest";
import { HISTORY, markStaleVehiclesOffline, savePosition, VEHICLES } from "../src/ingest.js";
import { parseForward } from "../src/traccar.js";

// Runs only against the Firestore emulator (npm run test:emulator).
const enabled = Boolean(process.env.FIRESTORE_EMULATOR_HOST);
const db = enabled ? getFirestore(initializeApp({ projectId: "demo-trackar" })) : undefined!;

const forward = (fixTime: string, latitude = -23.55) =>
  parseForward({ position: { fixTime, latitude, longitude: -46.63, speed: 20 }, device: { uniqueId: "imei-1", name: "Hilux" } });

describe.runIf(enabled)("savePosition", () => {
  beforeEach(async () => {
    await db.recursiveDelete(db.collection(VEHICLES));
  });

  it("creates unknown vehicles without owners and stores history", async () => {
    await savePosition(db, forward("2026-01-01T12:00:00Z"), 90);

    const vehicle = await db.collection(VEHICLES).doc("imei-1").get();
    expect(vehicle.get("name")).toBe("Hilux");
    expect(vehicle.get("ownerIds")).toEqual([]);
    expect(vehicle.get("status")).toBe("online");
    const history = await vehicle.ref.collection(HISTORY).get();
    expect(history.size).toBe(1);
    expect((history.docs[0].get("expireAt") as Timestamp).toMillis()).toBe(Date.UTC(2026, 3, 1, 12));
  });

  it("keeps the newest fix as latest and deduplicates retries", async () => {
    await savePosition(db, forward("2026-01-01T12:05:00Z", -23.6), 90);
    await savePosition(db, forward("2026-01-01T12:00:00Z", -23.5), 90); // late, out of order
    await savePosition(db, forward("2026-01-01T12:00:00Z", -23.5), 90); // retry

    const vehicle = await db.collection(VEHICLES).doc("imei-1").get();
    expect(vehicle.get("position.location").latitude).toBe(-23.6);
    expect((await vehicle.ref.collection(HISTORY).get()).size).toBe(2);
  });

  it("does not overwrite name or owners of existing vehicles", async () => {
    await db.collection(VEHICLES).doc("imei-1").set({ name: "Frota 01", ownerIds: ["u1"] });
    await savePosition(db, forward("2026-01-01T12:00:00Z"), 90);

    const vehicle = await db.collection(VEHICLES).doc("imei-1").get();
    expect(vehicle.get("name")).toBe("Frota 01");
    expect(vehicle.get("ownerIds")).toEqual(["u1"]);
  });

  it("marks silent vehicles offline", async () => {
    const now = new Date("2026-01-01T12:00:00Z");
    await savePosition(db, forward("2026-01-01T11:00:00Z"), 90, new Date("2026-01-01T11:00:00Z"));

    expect(await markStaleVehiclesOffline(db, 10, now)).toBe(1);
    expect((await db.collection(VEHICLES).doc("imei-1").get()).get("status")).toBe("offline");
  });
});
