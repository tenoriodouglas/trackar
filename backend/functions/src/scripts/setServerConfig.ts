/** Publishes gateway facts (tracker IP) for the admin panel's device setup page. Run by CI after Terraform. */
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { parseArgs } from "node:util";

const { values } = parseArgs({ options: { "tracker-ip": { type: "string" } } });
const ip = values["tracker-ip"];
if (!ip || !/^\d{1,3}(\.\d{1,3}){3}$/.test(ip)) throw new Error("--tracker-ip must be an IPv4 address");

initializeApp();
await getFirestore().doc("config/server").set({ trackerIp: ip, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
console.log(`Tracker server IP published: ${ip}`);
