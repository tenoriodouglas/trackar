import { useMemo, useState } from "react";
import { adminApi, errorMessage, useServices } from "../api";
import { commandLabel } from "../lib/format";
import { blockConfirmation } from "../lib/moving";
import { CommandType, VehicleDoc } from "../lib/types";

/** Sends a block/unblock after confirmation; the backend enforces ownership and a short cooldown. */
export function useCommand(vehicle: VehicleDoc) {
  const services = useServices();
  const api = useMemo(() => adminApi(services), [services]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inProgress = vehicle.lastCommand?.status === "pending" || vehicle.lastCommand?.status === "processing";

  const send = async (type: CommandType) => {
    const question = type === "engineStop" ? blockConfirmation(vehicle) : `Desbloquear ${vehicle.name}?`;
    if (!window.confirm(question)) return;
    setBusy(true);
    setError(null);
    try {
      await api.sendCommand(vehicle.id, type);
    } catch (e) {
      const text = errorMessage(e);
      setError(/few seconds/.test(text) ? "Aguarde alguns segundos antes de enviar outro comando." : text);
    } finally {
      setBusy(false);
    }
  };
  return { send, busy: busy || inProgress, error };
}

/** Compact button for lists: one tap to block (or unblock when already blocked). */
export function BlockButton({ vehicle }: { vehicle: VehicleDoc }) {
  const { send, busy, error } = useCommand(vehicle);
  const blocked = vehicle.engineBlocked === true;
  return (
    <span className="block-button" onClick={(e) => e.stopPropagation()} title={error ?? undefined}>
      <button
        className={blocked ? "small" : "small danger"}
        disabled={busy}
        onClick={() => send(blocked ? "engineResume" : "engineStop")}
      >
        {busy ? "Enviando…" : blocked ? "🔓 Desbloquear" : "🔒 Bloquear"}
      </button>
      {error && <span className="error small-text">{error}</span>}
    </span>
  );
}

/** Full block card: state, last command and action. */
export function CommandControls({ vehicle }: { vehicle: VehicleDoc }) {
  const { send, busy, error } = useCommand(vehicle);
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
          <button disabled={busy} onClick={() => send("engineResume")}>
            Desbloquear
          </button>
        ) : (
          <button className="danger" disabled={busy} onClick={() => send("engineStop")}>
            Bloquear agora
          </button>
        )}
      </div>
      {error && <span className="error">{error}</span>}
    </div>
  );
}
