import { HttpsError } from "firebase-functions/https";

/** Same identifier rule as the ingest endpoint (IMEI or Traccar unique id). */
const UNIQUE_ID = /^[\w.-]{4,64}$/;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export interface Caller {
  uid: string;
  isAdmin: boolean;
}

/** Extracts the signed-in caller or rejects the call. */
export function requireCaller(auth: { uid: string; token: Record<string, unknown> } | undefined): Caller {
  if (!auth) throw new HttpsError("unauthenticated", "Sign in first.");
  return { uid: auth.uid, isAdmin: auth.token.admin === true };
}

export function requireAdmin(caller: Caller): void {
  if (!caller.isAdmin) throw new HttpsError("permission-denied", "Admin only.");
}

export function requireUniqueId(value: unknown, field = "vehicleId"): string {
  const id = typeof value === "string" ? value.trim() : "";
  if (!UNIQUE_ID.test(id)) throw new HttpsError("invalid-argument", `${field} is invalid.`);
  return id;
}

export function requireEmail(value: unknown): string {
  const email = typeof value === "string" ? value.trim().toLowerCase() : "";
  if (!EMAIL.test(email)) throw new HttpsError("invalid-argument", `Invalid e-mail: ${String(value)}`);
  return email;
}

export function optionalText(value: unknown, field: string, maxLength = 60): string | undefined {
  if (value === undefined || value === null || value === "") return undefined;
  if (typeof value !== "string" || value.trim().length > maxLength) {
    throw new HttpsError("invalid-argument", `${field} must be up to ${maxLength} characters.`);
  }
  return value.trim();
}
