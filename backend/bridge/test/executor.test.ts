import { describe, expect, it, vi } from "vitest";
import { COMMAND_TTL_MS, executeCommand } from "../src/executor.js";
import { TraccarApi } from "../src/traccarClient.js";

const now = 1_000_000;
const request = { uniqueId: "imei-1", type: "engineStop", requestedAtMs: now - 1_000 };

const fakeTraccar = (overrides: Partial<TraccarApi> = {}): TraccarApi => ({
  findDeviceId: vi.fn(async () => 7),
  supportedCommands: vi.fn(async () => ["engineStop", "engineResume"]),
  sendCommand: vi.fn(async () => "sent" as const),
  ...overrides,
});

describe("executeCommand", () => {
  it("sends supported commands to known devices", async () => {
    const traccar = fakeTraccar();
    expect(await executeCommand(request, traccar, now)).toEqual({ status: "sent" });
    expect(traccar.sendCommand).toHaveBeenCalledWith(7, "engineStop");
  });

  it("reports queued delivery for offline devices", async () => {
    const traccar = fakeTraccar({ sendCommand: vi.fn(async () => "queued" as const) });
    expect(await executeCommand(request, traccar, now)).toEqual({ status: "queued" });
  });

  it.each([
    ["expired", { ...request, requestedAtMs: now - COMMAND_TTL_MS - 1 }, fakeTraccar()],
    ["device_not_connected", request, fakeTraccar({ findDeviceId: vi.fn(async () => null) })],
    ["unsupported", request, fakeTraccar({ supportedCommands: vi.fn(async () => ["positionPeriodic"]) })],
    ["gateway_error", request, fakeTraccar({ sendCommand: vi.fn(async () => Promise.reject(new Error("boom"))) })],
  ])("fails with %s", async (code, input, traccar) => {
    const outcome = await executeCommand(input, traccar, now);
    expect(outcome).toMatchObject({ status: "failed", errorCode: code });
  });
});
