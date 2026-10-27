import { Timestamp } from "firebase/firestore";
import { describe, expect, it } from "vitest";
import { blockConfirmation, movingSpeedKmh } from "../src/lib/moving";
import { VehicleDoc } from "../src/lib/types";

const now = Date.UTC(2026, 0, 1, 12);
const vehicle = (speedKmh: number, ageMs = 0): VehicleDoc => ({
  id: "1",
  name: "Hilux",
  uniqueId: "1",
  position: { location: { latitude: 0, longitude: 0 }, fixTime: Timestamp.fromMillis(now - ageMs), speedKmh, course: 0 },
});

describe("movingSpeedKmh", () => {
  it("reports recent movement only", () => {
    expect(movingSpeedKmh(vehicle(62.4), now)).toBe(62);
    expect(movingSpeedKmh(vehicle(1), now)).toBeNull();
    expect(movingSpeedKmh(vehicle(80, 30 * 60_000), now)).toBeNull();
  });
});

describe("blockConfirmation", () => {
  it("warns explicitly when the engine would be cut while moving", () => {
    expect(blockConfirmation(vehicle(80), now)).toContain("80 km/h");
    expect(blockConfirmation(vehicle(0), now)).not.toContain("km/h");
  });
});
