import { httpsCallable } from "firebase/functions";
import { createContext, useContext } from "react";
import { Services } from "./firebase";
import { AdminUser, CommandType } from "./lib/types";

export const ServicesContext = createContext<Services | null>(null);

export function useServices(): Services {
  const services = useContext(ServicesContext);
  if (!services) throw new Error("Services not initialised");
  return services;
}

/** Typed wrappers over the callable functions (backend/functions/src/index.ts). */
export function adminApi({ functions }: Services) {
  const call = async <T,>(name: string, data?: unknown): Promise<T> =>
    (await httpsCallable<unknown, T>(functions, name)(data)).data;
  return {
    listUsers: () => call<{ users: AdminUser[] }>("adminListUsers").then((r) => r.users),
    createUser: (email: string, password: string) => call("adminCreateUser", { email, password }),
    setUserAdmin: (uid: string, admin: boolean) => call("adminSetUserAdmin", { uid, admin }),
    setUserDisabled: (uid: string, disabled: boolean) => call("adminSetUserDisabled", { uid, disabled }),
    updateVehicle: (vehicleId: string, name: string, category: string) =>
      call("adminUpdateVehicle", { vehicleId, name, category }),
    setOwners: (vehicleId: string, emails: string[]) => call("adminSetOwners", { vehicleId, emails }),
    deleteVehicle: (vehicleId: string) => call("adminDeleteVehicle", { vehicleId }),
    sendCommand: (vehicleId: string, type: CommandType) => call("sendCommand", { vehicleId, type }),
    claimVehicle: (imei: string, name: string) => call("claimVehicle", { imei, name }),
    renameVehicle: (vehicleId: string, name: string) => call("renameVehicle", { vehicleId, name }),
    releaseVehicle: (vehicleId: string) => call("releaseVehicle", { vehicleId }),
  };
}

/** Human-readable message from a callable error. */
export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
