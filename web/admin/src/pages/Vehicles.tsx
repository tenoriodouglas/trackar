import { collection, onSnapshot } from "firebase/firestore";
import { useEffect, useMemo, useState } from "react";
import { adminApi, errorMessage, useServices } from "../api";
import { FleetMap } from "../components/FleetMap";
import { commandLabel, relativeTime } from "../lib/format";
import { AdminUser, CommandType, VehicleDoc } from "../lib/types";

const CATEGORIES: [string, string][] = [
  ["car", "Carro"],
  ["truck", "Caminhão"],
  ["motorcycle", "Moto"],
  ["van", "Van"],
  ["bus", "Ônibus"],
  ["other", "Outro"],
];

export function Vehicles() {
  const services = useServices();
  const api = useMemo(() => adminApi(services), [services]);
  const [vehicles, setVehicles] = useState<VehicleDoc[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [query, setQuery] = useState("");
  const [onlyUnassigned, setOnlyUnassigned] = useState(false);
  const [selected, setSelected] = useState<VehicleDoc | null>(null);

  useEffect(
    () =>
      onSnapshot(collection(services.db, "vehicles"), (snapshot) =>
        setVehicles(
          snapshot.docs
            .map((d) => ({ id: d.id, ...(d.data() as Omit<VehicleDoc, "id">) }))
            .sort((a, b) => (a.name ?? a.id).localeCompare(b.name ?? b.id)),
        ),
      ),
    [services.db],
  );
  useEffect(() => {
    api.listUsers().then(setUsers, () => setUsers([]));
  }, [api]);

  const emailOf = useMemo(() => new Map(users.map((u) => [u.uid, u.email])), [users]);
  const filtered = vehicles.filter((v) => {
    const q = query.trim().toLowerCase();
    const matches = !q || v.name?.toLowerCase().includes(q) || v.id.includes(q);
    return matches && (!onlyUnassigned || (v.ownerIds ?? []).length === 0);
  });
  // Keep the open panel in sync with live updates.
  const current = selected ? (vehicles.find((v) => v.id === selected.id) ?? null) : null;
  const online = vehicles.filter((v) => v.status === "online").length;
  const unassigned = vehicles.filter((v) => (v.ownerIds ?? []).length === 0).length;

  return (
    <div className="vehicles">
      <section className="stats">
        <Stat label="Veículos" value={vehicles.length} />
        <Stat label="Online" value={online} tone="ok" />
        <Stat label="Bloqueados" value={vehicles.filter((v) => v.engineBlocked).length} tone="danger" />
        <Stat label="Sem dono" value={unassigned} tone="warn" />
      </section>
      <section className="split">
        <div className="card list">
          <div className="toolbar">
            <input placeholder="Buscar por nome ou IMEI" value={query} onChange={(e) => setQuery(e.target.value)} />
            <label className="check">
              <input type="checkbox" checked={onlyUnassigned} onChange={(e) => setOnlyUnassigned(e.target.checked)} /> Só sem dono
            </label>
          </div>
          <table>
            <thead>
              <tr>
                <th>Veículo</th>
                <th>Status</th>
                <th>Última comunicação</th>
                <th>Donos</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((v) => (
                <tr key={v.id} className={current?.id === v.id ? "selected" : ""} onClick={() => setSelected(v)}>
                  <td>
                    <b>{v.name}</b>
                    <div className="muted mono">{v.id}</div>
                  </td>
                  <td>
                    <span className={`dot ${v.status ?? "unknown"}`} /> {v.status ?? "—"}
                    {v.engineBlocked && <span className="badge danger">bloqueado</span>}
                  </td>
                  <td>{relativeTime(v.lastUpdate?.toDate())}</td>
                  <td>{(v.ownerIds ?? []).map((uid) => emailOf.get(uid) ?? uid).join(", ") || <span className="badge warn">sem dono</span>}</td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={4} className="muted">
                    Nenhum veículo. Configure um rastreador na aba "Configurar rastreador".
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="card map-card">
          <FleetMap vehicles={filtered} onSelect={setSelected} />
        </div>
      </section>
      {current && <VehiclePanel vehicle={current} emailOf={emailOf} onClose={() => setSelected(null)} api={api} />}
    </div>
  );
}

function Stat({ label, value, tone }: { label: string; value: number; tone?: string }) {
  return (
    <div className={`card stat ${tone ?? ""}`}>
      <span>{label}</span>
      <b>{value}</b>
    </div>
  );
}

function VehiclePanel({
  vehicle,
  emailOf,
  onClose,
  api,
}: {
  vehicle: VehicleDoc;
  emailOf: Map<string, string>;
  onClose: () => void;
  api: ReturnType<typeof adminApi>;
}) {
  const [name, setName] = useState(vehicle.name ?? "");
  const [category, setCategory] = useState(vehicle.category ?? "car");
  const [owners, setOwners] = useState((vehicle.ownerIds ?? []).map((uid) => emailOf.get(uid) ?? "").join(", "));
  const [message, setMessage] = useState<{ text: string; error?: boolean } | null>(null);
  const [busy, setBusy] = useState(false);

  const run = async (action: () => Promise<unknown>, success: string) => {
    setBusy(true);
    setMessage(null);
    try {
      await action();
      setMessage({ text: success });
    } catch (error) {
      setMessage({ text: errorMessage(error), error: true });
    } finally {
      setBusy(false);
    }
  };

  const command = (type: CommandType) => {
    const verb = type === "engineStop" ? "BLOQUEAR" : "desbloquear";
    if (window.confirm(`Confirmar: ${verb} ${vehicle.name}?`)) void run(() => api.sendCommand(vehicle.id, type), "Comando enviado.");
  };

  const position = vehicle.position;
  return (
    <aside className="panel card">
      <div className="panel-head">
        <h2>{vehicle.name}</h2>
        <button className="ghost" onClick={onClose}>
          ✕
        </button>
      </div>
      <p className="muted mono">IMEI {vehicle.id}</p>
      {position && (
        <p>
          {Math.round(position.speedKmh)} km/h · {position.address ?? `${position.location.latitude.toFixed(5)}, ${position.location.longitude.toFixed(5)}`}
          <br />
          <span className="muted">GPS {relativeTime(position.fixTime.toDate())}</span>
        </p>
      )}

      <h3>Bloqueio remoto</h3>
      <p>
        Estado: <b>{vehicle.engineBlocked ? "bloqueado" : vehicle.engineBlocked === false ? "liberado" : "nunca acionado"}</b>
        {vehicle.lastCommand && (
          <>
            <br />
            <span className="muted">
              Último comando: {vehicle.lastCommand.type === "engineStop" ? "bloquear" : "desbloquear"} —{" "}
              {commandLabel(vehicle.lastCommand.status, vehicle.lastCommand.errorCode)}
            </span>
          </>
        )}
      </p>
      <div className="row">
        <button className="danger" disabled={busy} onClick={() => command("engineStop")}>
          Bloquear
        </button>
        <button disabled={busy} onClick={() => command("engineResume")}>
          Desbloquear
        </button>
      </div>

      <h3>Dados</h3>
      <label>
        Nome
        <input value={name} onChange={(e) => setName(e.target.value)} maxLength={60} />
      </label>
      <label>
        Categoria
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          {CATEGORIES.map(([id, label]) => (
            <option key={id} value={id}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <button disabled={busy} onClick={() => run(() => api.updateVehicle(vehicle.id, name, category), "Dados salvos.")}>
        Salvar dados
      </button>

      <h3>Donos (e-mails, separados por vírgula)</h3>
      <textarea value={owners} onChange={(e) => setOwners(e.target.value)} rows={2} />
      <button
        disabled={busy}
        onClick={() =>
          run(
            () =>
              api.setOwners(
                vehicle.id,
                owners.split(",").map((e) => e.trim()).filter(Boolean),
              ),
            "Donos atualizados.",
          )
        }
      >
        Salvar donos
      </button>

      {message && <p className={message.error ? "error" : "ok"}>{message.text}</p>}

      <h3>Zona de perigo</h3>
      <button
        className="danger ghost"
        disabled={busy}
        onClick={() => {
          if (window.confirm(`Excluir ${vehicle.name} e todo o histórico? Isso não pode ser desfeito.`)) {
            void run(() => api.deleteVehicle(vehicle.id).then(onClose), "Excluído.");
          }
        }}
      >
        Excluir veículo e histórico
      </button>
    </aside>
  );
}
