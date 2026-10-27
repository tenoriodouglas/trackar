import { Timestamp } from "firebase-admin/firestore";
import { describe, expect, it } from "vitest";
import { checkCommand, COMMAND_COOLDOWN_MS } from "../src/commands.js";

const now = new Date("2026-01-01T12:00:00Z");
const owner = { uid: "u1", isAdmin: false };
const vehicle = { ownerIds: ["u1"] };

describe("checkCommand", () => {
  it("allows the owner", () => {
    expect(checkCommand(vehicle, owner, now)).toBeNull();
  });

  it("rejects non-owners but allows admins", () => {
    expect(checkCommand(vehicle, { uid: "u2", isAdmin: false }, now)?.code).toBe("permission-denied");
    expect(checkCommand(vehicle, { uid: "u2", isAdmin: true }, now)).toBeNull();
  });

  it("enforces a cooldown between commands", () => {
    const recent = { ...vehicle, lastCommand: { requestedAt: Timestamp.fromMillis(now.getTime() - COMMAND_COOLDOWN_MS / 2) } };
    expect(checkCommand(recent, owner, now)?.code).toBe("resource-exhausted");
  });
});
