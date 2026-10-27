import { useMemo, useState } from "react";
import { adminApi, errorMessage, useServices } from "../api";
import { AddTrackerForm } from "../components/AddTrackerForm";
import { BlockButton, CommandControls } from "../components/CommandControls";
import { CommandLog } from "../components/CommandLog";
import { FleetMap } from "../components/FleetMap";
import { relativeTime } from "../lib/format";
import { VehicleDoc } from "../lib/types";

const STATUS_LABEL: Record<string, string> = { online: "Online", offline: "Offline", unknown: "Aguardando sinal" };

/** Client view: own vehicles on the map, live status, remote block and adding trackers. */
export function MyVehicles({ vehicles, onOpenHistory }: { vehicles: VehicleDoc[]; onOpenHistory: (vehicleId: string) => void }) {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const selected = vehicles.find((v) => v.id === selectedId) ?? null;

  return (
    <div className="vehicles">
      <section className="split">
        <div className="list">
          {vehicles.length === 0 && (
            <div className="card">
              <h2>Bem-vindo ao Trackar</h2>
              <p>Você ainda não tem veículos. Adicione o seu rastreador pelo IMEI impresso na etiqueta:</p>
            </div>
          )}
          {vehicles.map((v) => (
            <button key={v.id} className={`card vehicle-card ${selectedId === v.id ? "selected" : ""}`} onClick={() => setSelectedId(v.id)}>
              <span className={`dot ${v.status ?? "unknown"}`} />
              <span className="grow">
                <b>{v.name}</b>
                <span className="muted">{v.position?.address ?? STATUS_LABEL[v.status ?? "unknown"]}</span>
              </span>
              <span className="end">
                {v.position && <b>{Math.round(v.position.speedKmh)} km/h</b>}
                <span className="muted">{relativeTime(v.lastUpdate?.toDate())}</span>
                {v.engineBlocked && <span className="badge danger">bloqueado</span>}
              </span>
              <BlockButton vehicle={v} />
            </button>
          ))}
          <div className="card">
            <h3>Adicionar rastreador</h3>
            <AddTrackerForm />
          </div>
        </div>
        <div className="card map-card">
          <FleetMap vehicles={vehicles} onSelect={(v) => setSelectedId(v.id)} />
        </div>
      </section>

      {selected && <VehicleSheet vehicle={selected} onClose={() => setSelectedId(null)} onOpenHistory={onOpenHistory} />}
    </div>
  );
}

function VehicleSheet({
  vehicle: selected,
  onClose,
  onOpenHistory,
}: {
  vehicle: VehicleDoc;
  onClose: () => void;
  onOpenHistory: (vehicleId: string) => void;
}) {
  const services = useServices();
  const api = useMemo(() => adminApi(services), [services]);
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(selected.name);
  const [error, setError] = useState<string | null>(null);

  const rename = async () => {
    try {
      await api.renameVehicle(selected.id, name.trim());
      setEditing(false);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const release = async () => {
    if (!window.confirm(`Remover ${selected.name} da sua conta? O rastreador continua funcionando e pode ser adicionado de novo pelo IMEI.`)) return;
    try {
      await api.releaseVehicle(selected.id);
      onClose();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <aside className="panel card">
      <div className="panel-head">
        {editing ? (
          <span className="row">
            <input value={name} onChange={(e) => setName(e.target.value)} maxLength={40} autoFocus />
            <button className="small" onClick={rename} disabled={!name.trim()}>
              Salvar
            </button>
          </span>
        ) : (
          <h2>
            {selected.name}{" "}
            <button className="ghost small" onClick={() => setEditing(true)} title="Renomear">
              ✎
            </button>
          </h2>
        )}
        <button className="ghost" onClick={onClose}>
          ✕
        </button>
      </div>
      <p>
        <span className={`dot ${selected.status ?? "unknown"}`} /> {STATUS_LABEL[selected.status ?? "unknown"]} ·{" "}
        {relativeTime(selected.lastUpdate?.toDate())}
      </p>
      {selected.position ? (
        <p>
          <b>{Math.round(selected.position.speedKmh)} km/h</b>
          {selected.position.ignition != null && <> · ignição {selected.position.ignition ? "ligada" : "desligada"}</>}
          <br />
          {selected.position.address ??
            `${selected.position.location.latitude.toFixed(5)}, ${selected.position.location.longitude.toFixed(5)}`}
          <br />
          <a
            href={`https://www.google.com/maps/dir/?api=1&destination=${selected.position.location.latitude},${selected.position.location.longitude}`}
            target="_blank"
            rel="noreferrer"
          >
            Como chegar ↗
          </a>
        </p>
      ) : (
        <p className="muted">Ainda sem posição. Confira se o rastreador está configurado.</p>
      )}
      <CommandControls vehicle={selected} />
      <button onClick={() => onOpenHistory(selected.id)}>Ver histórico</button>
      <h3>Últimos comandos</h3>
      <CommandLog vehicleId={selected.id} />
      {error && <p className="error">{error}</p>}
      <p className="muted mono">IMEI {selected.id}</p>
      <button className="danger ghost" onClick={release}>
        Remover da minha conta
      </button>
    </aside>
  );
}
