import { describe, expect, it } from "vitest";
import { isAuthorized } from "../src/auth.js";
import { InvalidPayloadError, parseForward } from "../src/traccar.js";

const payload = () => ({
  position: {
    fixTime: "2026-01-01T12:00:00.000+00:00",
    valid: true,
    latitude: -23.55,
    longitude: -46.63,
    speed: 10,
    course: 90,
    address: "Av. Paulista",
    attributes: { ignition: true, power: 12.6, motion: "yes" },
  },
  device: { uniqueId: "359339075123456", name: "Hilux" },
});

describe("parseForward", () => {
  it("maps a Traccar payload to the app schema", () => {
    const parsed = parseForward(payload());
    expect(parsed.uniqueId).toBe("359339075123456");
    expect(parsed.deviceName).toBe("Hilux");
    expect(parsed.position.speedKmh).toBe(18.5);
    expect(parsed.position.location.latitude).toBe(-23.55);
    expect(parsed.position.fixTime.toMillis()).toBe(Date.UTC(2026, 0, 1, 12));
    expect(parsed.position.ignition).toBe(true);
    expect(parsed.position.powerVolts).toBe(12.6);
    expect(parsed.position.motion).toBeNull();
  });

  it("falls back to the unique id as name", () => {
    const body = payload();
    body.device.name = " ";
    expect(parseForward(body).deviceName).toBe("359339075123456");
  });

  it.each([
    ["missing device", { position: payload().position }],
    ["bad unique id", { ...payload(), device: { uniqueId: "../etc" } }],
    ["out of range latitude", { ...payload(), position: { ...payload().position, latitude: 91 } }],
    ["bad fix time", { ...payload(), position: { ...payload().position, fixTime: "nope" } }],
    ["empty body", undefined],
  ])("rejects %s", (_, body) => {
    expect(() => parseForward(body)).toThrow(InvalidPayloadError);
  });
});

describe("isAuthorized", () => {
  it("accepts only the exact bearer token", () => {
    expect(isAuthorized("Bearer s3cret", "s3cret")).toBe(true);
    expect(isAuthorized("Bearer wrong!", "s3cret")).toBe(false);
    expect(isAuthorized("s3cret", "s3cret")).toBe(false);
    expect(isAuthorized(undefined, "s3cret")).toBe(false);
    expect(isAuthorized("Bearer ", "")).toBe(false);
  });
});
