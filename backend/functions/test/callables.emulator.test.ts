import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { beforeEach, describe, expect, it } from "vitest";
import { requestCommand } from "../src/commands.js";
import { VEHICLES } from "../src/ingest.js";
import { adminSetOwners, claimVehicle, renameVehicle } from "../src/vehicles.js";

// Runs only against the emulators (npm run test:emulator).
const enabled = Boolean(process.env.FIRESTORE_EMULATOR_HOST && process.env.FIREBASE_AUTH_EMULATOR_HOST);
const app = enabled ? (getApps()[0] ?? initializeApp({ projectId: "demo-trackar" })) : undefined!;
const db = enabled ? getFirestore(app) : undefined!;
const auth = enabled ? getAuth(app) : undefined!;

const alice = { uid: "alice", isAdmin: false };
const bob = { uid: "bob", isAdmin: false };
const admin = { uid: "root", isAdmin: true };

describe.runIf(enabled)("callables", () => {
  beforeEach(async () => {
    await db.recursiveDelete(db.collection(VEHICLES));
  });

  it("claims an unknown tracker and protects it from other users", async () => {
    await claimVehicle(db, alice, { imei: "359339075123456", name: "Hilux" });

    const vehicle = await db.collection(VEHICLES).doc("359339075123456").get();
    expect(vehicle.get("ownerIds")).toEqual(["alice"]);
    expect(vehicle.get("name")).toBe("Hilux");
    await expect(claimVehicle(db, bob, { imei: "359339075123456" })).rejects.toThrow(/already linked/);
  });

  it("claims a reporting, unowned tracker without losing its data", async () => {
    await db.collection(VEHICLES).doc("imei-2").set({ name: "imei-2", ownerIds: [], status: "online" });
    await claimVehicle(db, bob, { imei: "imei-2", name: "Moto" });

    const vehicle = await db.collection(VEHICLES).doc("imei-2").get();
    expect(vehicle.get("ownerIds")).toEqual(["bob"]);
    expect(vehicle.get("status")).toBe("online");
  });

  it("queues a command and mirrors it on the vehicle", async () => {
    await db.collection(VEHICLES).doc("imei-3").set({ ownerIds: ["alice"] });
    const { commandId } = await requestCommand(db, alice, { vehicleId: "imei-3", type: "engineStop" });

    const vehicle = await db.collection(VEHICLES).doc("imei-3").get();
    expect(vehicle.get("lastCommand.id")).toBe(commandId);
    expect(vehicle.get("lastCommand.status")).toBe("pending");
    const command = await vehicle.ref.collection("commands").doc(commandId).get();
    expect(command.get("requestedBy")).toBe("alice");
  });

  it("blocks immediately even at speed, and rejects unknown command types", async () => {
    await db.collection(VEHICLES).doc("imei-4").set({
      ownerIds: ["alice"],
      position: { speedKmh: 80, fixTime: Timestamp.now() },
    });
    await expect(requestCommand(db, alice, { vehicleId: "imei-4", type: "engineStop" })).resolves.toHaveProperty("commandId");
    await expect(requestCommand(db, alice, { vehicleId: "imei-4", type: "reboot" })).rejects.toThrow(/Unsupported/);
  });

  it("lets owners rename their vehicles only", async () => {
    await db.collection(VEHICLES).doc("imei-6").set({ name: "old", ownerIds: ["alice"] });
    await renameVehicle(db, alice, { vehicleId: "imei-6", name: "Hilux" });
    expect((await db.collection(VEHICLES).doc("imei-6").get()).get("name")).toBe("Hilux");
    await expect(renameVehicle(db, bob, { vehicleId: "imei-6", name: "x" })).rejects.toThrow(/Not your vehicle/);
  });

  it("lets admins set owners by e-mail", async () => {
    const user = await auth.createUser({ email: `carol${Date.now()}@example.com`, password: "secret123" });
    await db.collection(VEHICLES).doc("imei-5").set({ ownerIds: ["alice"] });

    await adminSetOwners(db, auth, admin, { vehicleId: "imei-5", emails: [user.email] });
    expect((await db.collection(VEHICLES).doc("imei-5").get()).get("ownerIds")).toEqual([user.uid]);

    await expect(adminSetOwners(db, auth, admin, { vehicleId: "imei-5", emails: ["nobody@example.com"] })).rejects.toThrow(/not found/);
    await expect(adminSetOwners(db, auth, alice, { vehicleId: "imei-5", emails: [] })).rejects.toThrow(/Admin only/);
  });
});
