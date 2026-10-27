import { describe, expect, it } from "vitest";
import { DEVICE_PROFILES } from "../src/lib/devices";
import { commandLabel, generatePassword, relativeTime } from "../src/lib/format";

describe("relativeTime", () => {
  const now = new Date("2026-01-01T12:00:00Z");
  it("formats recent and old dates in Portuguese", () => {
    expect(relativeTime(undefined, now)).toBe("—");
    expect(relativeTime(new Date("2026-01-01T11:59:40Z"), now)).toBe("agora");
    expect(relativeTime(new Date("2026-01-01T11:55:00Z"), now)).toBe("há 5 min");
    expect(relativeTime(new Date("2025-12-31T12:00:00Z"), now)).toBe("há 1 dia");
  });
});

describe("commandLabel", () => {
  it("explains failures", () => {
    expect(commandLabel("sent")).toBe("Entregue ao rastreador");
    expect(commandLabel("failed", "unsupported")).toBe("Falhou: modelo não suporta bloqueio");
  });
});

describe("generatePassword", () => {
  it("creates long passwords without ambiguous characters", () => {
    const password = generatePassword();
    expect(password).toHaveLength(14);
    expect(password).not.toMatch(/[0O1lI]/);
  });
});

describe("device profiles", () => {
  it("fill the server IP into setup commands", () => {
    const gt06 = DEVICE_PROFILES.find((p) => p.id === "gt06")!;
    expect(gt06.smsSetup("1.2.3.4", "zap.vivo.com.br")).toContain("SERVER,0,1.2.3.4,5023,0#");
    expect(new Set(DEVICE_PROFILES.map((p) => p.port)).size).toBe(DEVICE_PROFILES.length);
  });
});
