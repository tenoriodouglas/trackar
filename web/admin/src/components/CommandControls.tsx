import { useMemo, useState } from "react";
import { adminApi, errorMessage, useServices } from "../api";
import { commandLabel } from "../lib/format";
import { CommandType, VehicleDoc } from "../lib/types";

/** Remote block/unblock with confirmation; the backend enforces ownership and the speed limit. */
export function CommandControls({ vehicle }: { vehicle: VehicleDoc }) {
  const services = useServices();
  const api = useMemo(() => adminApi(services), [services]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inProgress = vehicle.lastCommand?.status === "pending" || vehicle.lastCommand?.status === "processing";

  const send = async (type: CommandType) => {
    const question =
      type === "engineStop"
        ? `Bloquear ${vehicle.name}?\n\nO rastreador vai cortar combustível/ignição. Só é aceito abaixo de 20 km/h. Use com o veículo em local seguro.`
        : `Desbloquear ${vehicle.name}?`;
    if (!window.confirm(question)) return;
    setBusy(true);
    setError(null);
    try {
      await api.sendCommand(vehicle.id, type);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={`block-card ${vehicle.engineBlocked ? "blocked" : ""}`}>
      <b>{vehicle.engineBlocked ? "🔒 Veículo bloqueado" : vehicle.engineBlocked === false ? "🔓 Veículo liberado" : "Bloqueio remoto"}</b>
      {vehicle.lastCommand && (
        <span className="muted">
          Último comando ({vehicle.lastCommand.type === "engineStop" ? "bloquear" : "desbloquear"}):{" "}
          {commandLabel(vehicle.lastCommand.status, vehicle.lastCommand.errorCode)}
        </span>
      )}
      <div className="row">
        {vehicle.engineBlocked ? (
          <button disabled={busy || inProgress} onClick={() => send("engineResume")}>
            Desbloquear
          </button>
        ) : (
          <button className="danger" disabled={busy || inProgress} onClick={() => send("engineStop")}>
            Bloquear
          </button>
        )}
      </div>
      {error && <span className="error">{error}</span>}
    </div>
  );
}
