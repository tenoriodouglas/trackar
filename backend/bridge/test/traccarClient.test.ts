import { describe, expect, it, vi } from "vitest";
import { TraccarClient } from "../src/traccarClient.js";

const response = (status: number, body: unknown = "") =>
  new Response(typeof body === "string" ? body : JSON.stringify(body), { status });

describe("TraccarClient", () => {
  it("maps 200/202 to sent/queued and uses basic auth", async () => {
    const fetchFn = vi.fn(async () => response(202, {}));
    const client = new TraccarClient("http://t", "a@b", "pw", fetchFn as typeof fetch);

    expect(await client.sendCommand(3, "engineStop")).toBe("queued");
    const [url, init] = fetchFn.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("http://t/api/commands/send");
    expect((init.headers as Record<string, string>).Authorization).toBe(`Basic ${Buffer.from("a@b:pw").toString("base64")}`);
    expect(JSON.parse(init.body as string)).toMatchObject({ deviceId: 3, type: "engineStop" });
  });

  it("creates the first admin and closes registration when login fails", async () => {
    const calls: string[] = [];
    const fetchFn = vi.fn(async (url: string, init?: RequestInit) => {
      calls.push(`${init?.method ?? "GET"} ${url}`);
      if (url.endsWith("/api/session")) return response(401);
      if (url.endsWith("/api/users")) return response(200, { id: 1 });
      if (init?.method === "GET") return response(200, { id: 1, registration: true });
      return response(200, {});
    });
    const client = new TraccarClient("http://t", "a@b", "pw", fetchFn as unknown as typeof fetch);

    await client.ensureAdmin();
    expect(calls).toEqual(["POST http://t/api/session", "POST http://t/api/users", "GET http://t/api/server", "PUT http://t/api/server"]);
    const put = fetchFn.mock.calls[3] as unknown as [string, RequestInit];
    expect(JSON.parse(put[1].body as string)).toMatchObject({ registration: false });
  });

  it("finds devices by unique id", async () => {
    const fetchFn = vi.fn(async () => response(200, [{ id: 42 }]));
    const client = new TraccarClient("http://t", "a@b", "pw", fetchFn as typeof fetch);
    expect(await client.findDeviceId("123")).toBe(42);
    expect(fetchFn.mock.calls[0][0]).toBe("http://t/api/devices?uniqueId=123");
  });
});
