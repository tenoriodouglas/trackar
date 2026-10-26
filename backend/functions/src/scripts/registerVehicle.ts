/**
 * Registers a tracker and links it to a user (by e-mail). Run with admin credentials:
 *   GOOGLE_APPLICATION_CREDENTIALS=sa.json npm run register-vehicle -- --imei 123 --name "Hilux" --owner a@b.com
 */
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { parseArgs } from "node:util";
import { VEHICLES } from "../ingest.js";

const { values } = parseArgs({
  options: {
    imei: { type: "string" },
    name: { type: "string" },
    owner: { type: "string" },
    category: { type: "string" },
  },
});

if (!values.imei || !/^[\w.-]{1,64}$/.test(values.imei)) throw new Error("--imei is required (letters, digits, . _ -)");
if (!values.owner) throw new Error("--owner (user e-mail) is required");

initializeApp();
const user = await getAuth().getUserByEmail(values.owner).catch(() => {
  throw new Error(`User ${values.owner} not found. Create it in Firebase Console > Authentication first.`);
});

const update: Record<string, unknown> = {
  uniqueId: values.imei,
  ownerIds: FieldValue.arrayUnion(user.uid),
};
if (values.name) update.name = values.name;
if (values.category) update.category = values.category;

await getFirestore().collection(VEHICLES).doc(values.imei).set(update, { merge: true });
console.log(`Vehicle ${values.imei} linked to ${values.owner}.`);
