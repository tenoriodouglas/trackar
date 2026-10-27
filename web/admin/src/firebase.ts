import { FirebaseApp, initializeApp } from "firebase/app";
import { connectAuthEmulator, getAuth } from "firebase/auth";
import { connectFirestoreEmulator, getFirestore } from "firebase/firestore";
import { connectFunctionsEmulator, getFunctions } from "firebase/functions";

/** Must match the functions' REGION parameter (backend/functions/.env). */
const FUNCTIONS_REGION = "southamerica-east1";

/**
 * Firebase Hosting serves the web app config at a reserved URL, so nothing is hard-coded or committed.
 * For local development, set VITE_FIREBASE_CONFIG to the JSON config of the web app.
 */
async function loadConfig(): Promise<Record<string, string>> {
  const local = import.meta.env.VITE_FIREBASE_CONFIG as string | undefined;
  if (local) return JSON.parse(local);
  const response = await fetch("/__/firebase/init.json");
  if (!response.ok) throw new Error("Firebase config unavailable");
  return response.json();
}

export interface Services {
  app: FirebaseApp;
  auth: ReturnType<typeof getAuth>;
  db: ReturnType<typeof getFirestore>;
  functions: ReturnType<typeof getFunctions>;
}

export async function initServices(): Promise<Services> {
  const app = initializeApp(await loadConfig());
  const services = { app, auth: getAuth(app), db: getFirestore(app), functions: getFunctions(app, FUNCTIONS_REGION) };
  // Local development against `firebase emulators:start` (ports from firebase.json).
  if (import.meta.env.VITE_USE_EMULATORS === "true") {
    connectAuthEmulator(services.auth, "http://127.0.0.1:9099", { disableWarnings: true });
    connectFirestoreEmulator(services.db, "127.0.0.1", 8080);
    connectFunctionsEmulator(services.functions, "127.0.0.1", 5001);
  }
  return services;
}
