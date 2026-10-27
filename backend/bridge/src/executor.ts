import { TraccarApi } from "./traccarClient.js";

/** Commands older than this are dropped: blocking a car minutes after the request is surprising. */
export const COMMAND_TTL_MS = 2 * 60_000;

/** Error codes shown to users (the app/admin panel translate them). */
export type CommandErrorCode = "expired" | "device_not_connected" | "unsupported" | "gateway_error";

export interface CommandRequest {
  uniqueId: string;
  type: string;
  requestedAtMs: number;
}

export type CommandOutcome =
  | { status: "sent" | "queued" }
  | { status: "failed"; errorCode: CommandErrorCode; message: string };

/** Delivers one command through Traccar. Never throws: every failure becomes an outcome. */
export async function executeCommand(request: CommandRequest, traccar: TraccarApi, nowMs = Date.now()): Promise<CommandOutcome> {
  if (nowMs - request.requestedAtMs > COMMAND_TTL_MS) {
    return { status: "failed", errorCode: "expired", message: "Command expired before delivery." };
  }
  try {
    const deviceId = await traccar.findDeviceId(request.uniqueId);
    if (deviceId === null) {
      return { status: "failed", errorCode: "device_not_connected", message: "Tracker has never connected to the server." };
    }
    const supported = await traccar.supportedCommands(deviceId);
    if (!supported.includes(request.type)) {
      return { status: "failed", errorCode: "unsupported", message: `Tracker model does not support ${request.type}.` };
    }
    return { status: await traccar.sendCommand(deviceId, request.type) };
  } catch (error) {
    return { status: "failed", errorCode: "gateway_error", message: String(error instanceof Error ? error.message : error) };
  }
}
