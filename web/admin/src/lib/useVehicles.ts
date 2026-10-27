import { collection, onSnapshot, query, where } from "firebase/firestore";
import { useEffect, useState } from "react";
import { useServices } from "../api";
import { VehicleDoc } from "./types";

/**
 * Live vehicle list. "all" requires the admin role; "mine" is what Firestore rules allow any user to read
 * (vehicles whose ownerIds contain them).
 */
export function useVehicles(scope: "all" | "mine"): VehicleDoc[] | null {
  const { db, auth } = useServices();
  const [vehicles, setVehicles] = useState<VehicleDoc[] | null>(null);
  const uid = auth.currentUser?.uid;

  useEffect(() => {
    if (!uid) return;
    const source = collection(db, "vehicles");
    const target = scope === "all" ? source : query(source, where("ownerIds", "array-contains", uid));
    return onSnapshot(
      target,
      (snapshot) =>
        setVehicles(
          snapshot.docs
            .map((d) => ({ id: d.id, ...(d.data() as Omit<VehicleDoc, "id">) }))
            .sort((a, b) => (a.name ?? a.id).localeCompare(b.name ?? b.id)),
        ),
      () => setVehicles([]),
    );
  }, [db, scope, uid]);

  return vehicles;
}
