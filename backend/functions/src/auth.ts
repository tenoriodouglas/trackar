import { timingSafeEqual } from "node:crypto";

/** Constant-time check of an `Authorization: Bearer <token>` header. */
export function isAuthorized(header: string | undefined, token: string): boolean {
  if (!header?.startsWith("Bearer ") || token.length === 0) return false;
  const given = Buffer.from(header.slice("Bearer ".length));
  const expected = Buffer.from(token);
  return given.length === expected.length && timingSafeEqual(given, expected);
}
