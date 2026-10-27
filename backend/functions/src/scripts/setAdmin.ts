/**
 * Grants (or revokes with --revoke) the admin role used by the admin panel.
 *   GOOGLE_APPLICATION_CREDENTIALS=sa.json npm run set-admin -- --email a@b.com
 */
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { parseArgs } from "node:util";

const { values } = parseArgs({ options: { email: { type: "string" }, revoke: { type: "boolean", default: false } } });
if (!values.email) throw new Error("--email is required");

initializeApp();
const auth = getAuth();
const user = await auth.getUserByEmail(values.email).catch(() => {
  throw new Error(`User ${values.email} not found. Create it in Firebase Console > Authentication first.`);
});
await auth.setCustomUserClaims(user.uid, { ...user.customClaims, admin: !values.revoke });
console.log(`${values.email} is ${values.revoke ? "no longer" : "now"} an admin. Sign out and in again to apply.`);
