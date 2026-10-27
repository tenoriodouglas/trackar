import { collection, limit, onSnapshot, orderBy, query, Timestamp } from "firebase/firestore";
import { useEffect, useState } from "react";
import { useServices } from "../api";
import { commandLabel, relativeTime } from "../lib/format";

interface CommandEntry {
  id: string;
  type: string;
  status: string;
  errorCode?: string | null;
  requestedAt?: Timestamp;
}

/** Last commands of a vehicle (who/when/result), for audit and peace of mind. */
export function CommandLog({ vehicleId }: { vehicleId: string }) {
  const { db } = useServices();
  const [entries, setEntries] = useState<CommandEntry[]>([]);

  useEffect(
    () =>
      onSnapshot(query(collection(db, "vehicles", vehicleId, "commands"), orderBy("requestedAt", "desc"), limit(5)), (s) =>
        setEntries(s.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<CommandEntry, "id">) }))),
      ),
    [db, vehicleId],
  );

  if (entries.length === 0) return <p className="muted">Nenhum comando enviado ainda.</p>;
  return (
    <ul className="log">
      {entries.map((c) => (
        <li key={c.id}>
          <b>{c.type === "engineStop" ? "Bloquear" : "Desbloquear"}</b> · {relativeTime(c.requestedAt?.toDate())}
          <br />
          <span className="muted">{commandLabel(c.status, c.errorCode)}</span>
        </li>
      ))}
    </ul>
  );
}
