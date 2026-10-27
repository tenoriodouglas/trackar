import { Timestamp } from "firebase-admin/firestore";
import { describe, expect, it } from "vitest";
import { checkCommand, COMMAND_COOLDOWN_MS } from "../src/commands.js";

const now = new Date("2026-01-01T12:00:00Z");
const owner = { uid: "u1", isAdmin: false };
const moving = (speedKmh: number, ageMs = 0) => ({
  ownerIds: ["u1"],
  position: { speedKmh, fixTime: Timestamp.fromMillis(now.getTime() - ageMs) },
});

describe("checkCommand", () => {
  it("allows the owner to block a stopped vehicle", () => {
    expect(checkCommand(moving(0), owner, "engineStop", now)).toBeNull();
  });

  it("rejects non-owners but allows admins", () => {
    expect(checkCommand(moving(0), { uid: "u2", isAdmin: false }, "engineStop", now)?.code).toBe("permission-denied");
    expect(checkCommand(moving(0), { uid: "u2", isAdmin: true }, "engineStop", now)).toBeNull();
  });

  it("refuses blocking while moving fast, but always allows unblocking", () => {
    expect(checkCommand(moving(60), owner, "engineStop", now)?.code).toBe("failed-precondition");
    expect(checkCommand(moving(60), owner, "engineResume", now)).toBeNull();
  });

  it("ignores stale speed", () => {
    expect(checkCommand(moving(60, 30 * 60_000), owner, "engineStop", now)).toBeNull();
  });

  it("enforces a cooldown between commands", () => {
    const vehicle = { ...moving(0), lastCommand: { requestedAt: Timestamp.fromMillis(now.getTime() - COMMAND_COOLDOWN_MS / 2) } };
    expect(checkCommand(vehicle, owner, "engineResume", now)?.code).toBe("resource-exhausted");
  });
});
