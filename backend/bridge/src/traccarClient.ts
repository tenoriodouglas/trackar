/** Minimal client for the Traccar REST API, reached only on localhost by the bridge. */
export type SendOutcome = "sent" | "queued";

export interface TraccarApi {
  findDeviceId(uniqueId: string): Promise<number | null>;
  supportedCommands(deviceId: number): Promise<string[]>;
  sendCommand(deviceId: number, type: string): Promise<SendOutcome>;
}

export class TraccarHttpError extends Error {
  constructor(
    readonly status: number,
    body: string,
  ) {
    super(`Traccar responded ${status}: ${body.slice(0, 200)}`);
  }
}

type Fetch = typeof fetch;

export class TraccarClient implements TraccarApi {
  private readonly authorization: string;

  constructor(
    private readonly baseUrl: string,
    private readonly email: string,
    private readonly password: string,
    private readonly fetchFn: Fetch = fetch,
  ) {
    this.authorization = `Basic ${Buffer.from(`${email}:${password}`).toString("base64")}`;
  }

  /**
   * Makes sure the bridge account exists. On a fresh server the first user becomes administrator,
   * after which public registration is closed.
   */
  async ensureAdmin(): Promise<void> {
    const session = await this.fetchFn(`${this.baseUrl}/api/session`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ email: this.email, password: this.password }),
    });
    if (session.ok) return;
    if (session.status !== 401) throw new TraccarHttpError(session.status, await session.text());

    const created = await this.fetchFn(`${this.baseUrl}/api/users`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: "Trackar bridge", email: this.email, password: this.password }),
    });
    if (!created.ok) throw new TraccarHttpError(created.status, await created.text());

    const server = await this.request<Record<string, unknown>>("GET", "/api/server");
    await this.request("PUT", "/api/server", { ...server, registration: false });
  }

  async findDeviceId(uniqueId: string): Promise<number | null> {
    const devices = await this.request<{ id: number }[]>("GET", `/api/devices?uniqueId=${encodeURIComponent(uniqueId)}`);
    return devices[0]?.id ?? null;
  }

  async supportedCommands(deviceId: number): Promise<string[]> {
    const types = await this.request<{ type: string }[]>("GET", `/api/commands/types?deviceId=${deviceId}`);
    return types.map((t) => t.type);
  }

  /** 200 = delivered to the connected device; 202 = queued until the device reconnects. */
  async sendCommand(deviceId: number, type: string): Promise<SendOutcome> {
    const response = await this.fetchFn(`${this.baseUrl}/api/commands/send`, {
      method: "POST",
      headers: this.headers(),
      body: JSON.stringify({ deviceId, type, attributes: {} }),
    });
    if (response.status === 202) return "queued";
    if (response.ok) return "sent";
    throw new TraccarHttpError(response.status, await response.text());
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const response = await this.fetchFn(`${this.baseUrl}${path}`, {
      method,
      headers: this.headers(),
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!response.ok) throw new TraccarHttpError(response.status, await response.text());
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }

  private headers() {
    return { Authorization: this.authorization, "Content-Type": "application/json", Accept: "application/json" };
  }
}
