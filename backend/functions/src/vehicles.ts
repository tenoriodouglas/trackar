import { Auth } from "firebase-admin/auth";
import { FieldValue, Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/https";
import { VEHICLES } from "./ingest.js";
import { Caller, optionalText, requireAdmin, requireEmail, requireUniqueId } from "./validation.js";

export const CATEGORIES = ["car", "truck", "motorcycle", "van", "bus", "other"] as const;

/**
 * Links a tracker to the caller. Allowed when the device is unknown (pre-registration before installing)
 * or reported but not yet owned. Devices owned by someone else can only be reassigned by an admin.
 */
export async function claimVehicle(db: Firestore, caller: Caller, data: unknown) {
  const input = (data ?? {}) as { imei?: unknown; name?: unknown };
  const imei = requireUniqueId(input.imei, "imei");
  const name = optionalText(input.name, "name");
  const ref = db.collection(VEHICLES).doc(imei);

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    const owners = (snapshot.get("ownerIds") as string[] | undefined) ?? [];
    if (owners.length > 0 && !owners.includes(caller.uid)) {
      throw new HttpsError("already-exists", "This tracker is already linked to another account.");
    }
    const update: Record<string, unknown> = { uniqueId: imei, ownerIds: FieldValue.arrayUnion(caller.uid) };
    if (name) update.name = name;
    if (!snapshot.exists) {
      update.name = name ?? imei;
      update.status = "unknown";
      update.createdAt = FieldValue.serverTimestamp();
    }
    tx.set(ref, update, { merge: true });
  });
  return { vehicleId: imei };
}

/** Removes the caller from a vehicle's owners (the device keeps reporting, unassigned). */
export async function releaseVehicle(db: Firestore, caller: Caller, data: unknown) {
  const vehicleId = requireUniqueId((data as { vehicleId?: unknown } | undefined)?.vehicleId);
  await db.collection(VEHICLES).doc(vehicleId).update({ ownerIds: FieldValue.arrayRemove(caller.uid) });
  return { vehicleId };
}

// ---------- Admin ----------

export async function adminUpdateVehicle(db: Firestore, caller: Caller, data: unknown) {
  requireAdmin(caller);
  const input = (data ?? {}) as { vehicleId?: unknown; name?: unknown; category?: unknown };
  const vehicleId = requireUniqueId(input.vehicleId);
  const update: Record<string, unknown> = {};
  const name = optionalText(input.name, "name");
  if (name) update.name = name;
  if (input.category !== undefined) {
    if (!CATEGORIES.includes(input.category as (typeof CATEGORIES)[number])) {
      throw new HttpsError("invalid-argument", "Invalid category.");
    }
    update.category = input.category;
  }
  await db.collection(VEHICLES).doc(vehicleId).set(update, { merge: true });
  return { vehicleId };
}

/** Replaces the owners of a vehicle by e-mail; every e-mail must belong to an existing user. */
export async function adminSetOwners(db: Firestore, auth: Auth, caller: Caller, data: unknown) {
  requireAdmin(caller);
  const input = (data ?? {}) as { vehicleId?: unknown; emails?: unknown };
  const vehicleId = requireUniqueId(input.vehicleId);
  if (!Array.isArray(input.emails) || input.emails.length > 20) {
    throw new HttpsError("invalid-argument", "emails must be a list (max 20).");
  }
  const emails = [...new Set(input.emails.map(requireEmail))];
  const result = emails.length > 0 ? await auth.getUsers(emails.map((email) => ({ email }))) : undefined;
  if (result && result.notFound.length > 0) {
    const missing = result.notFound.map((id) => ("email" in id ? id.email : "")).join(", ");
    throw new HttpsError("not-found", `Users not found: ${missing}`);
  }
  await db.collection(VEHICLES).doc(vehicleId).set({ ownerIds: result?.users.map((u) => u.uid) ?? [] }, { merge: true });
  return { vehicleId };
}

export async function adminDeleteVehicle(db: Firestore, caller: Caller, data: unknown) {
  requireAdmin(caller);
  const vehicleId = requireUniqueId((data as { vehicleId?: unknown } | undefined)?.vehicleId);
  await db.recursiveDelete(db.collection(VEHICLES).doc(vehicleId));
  return { vehicleId };
}

export async function adminListUsers(auth: Auth, caller: Caller) {
  requireAdmin(caller);
  const { users } = await auth.listUsers(1000);
  return {
    users: users.map((u) => ({
      uid: u.uid,
      email: u.email ?? "",
      disabled: u.disabled,
      admin: u.customClaims?.admin === true,
      lastSignIn: u.metadata.lastSignInTime ?? null,
      createdAt: u.metadata.creationTime,
    })),
  };
}

export async function adminCreateUser(auth: Auth, caller: Caller, data: unknown) {
  requireAdmin(caller);
  const input = (data ?? {}) as { email?: unknown; password?: unknown };
  const email = requireEmail(input.email);
  const password = typeof input.password === "string" ? input.password : "";
  if (password.length < 8) throw new HttpsError("invalid-argument", "Password must have at least 8 characters.");
  const user = await auth.createUser({ email, password }).catch((error: { code?: string }) => {
    if (error.code === "auth/email-already-exists") throw new HttpsError("already-exists", "E-mail already in use.");
    throw error;
  });
  return { uid: user.uid };
}

export async function adminSetUserAdmin(auth: Auth, caller: Caller, data: unknown) {
  requireAdmin(caller);
  const input = (data ?? {}) as { uid?: unknown; admin?: unknown };
  if (typeof input.uid !== "string" || typeof input.admin !== "boolean") {
    throw new HttpsError("invalid-argument", "uid and admin are required.");
  }
  if (input.uid === caller.uid && !input.admin) throw new HttpsError("failed-precondition", "You cannot remove your own admin role.");
  const user = await auth.getUser(input.uid);
  await auth.setCustomUserClaims(input.uid, { ...user.customClaims, admin: input.admin });
  return { uid: input.uid };
}

export async function adminSetUserDisabled(auth: Auth, caller: Caller, data: unknown) {
  requireAdmin(caller);
  const input = (data ?? {}) as { uid?: unknown; disabled?: unknown };
  if (typeof input.uid !== "string" || typeof input.disabled !== "boolean") {
    throw new HttpsError("invalid-argument", "uid and disabled are required.");
  }
  if (input.uid === caller.uid) throw new HttpsError("failed-precondition", "You cannot disable yourself.");
  await auth.updateUser(input.uid, { disabled: input.disabled });
  return { uid: input.uid };
}
