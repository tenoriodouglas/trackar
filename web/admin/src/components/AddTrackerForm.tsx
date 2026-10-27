import { FormEvent, useMemo, useState } from "react";
import { adminApi, errorMessage, useServices } from "../api";

/** Links a tracker (IMEI from the label) to the signed-in account. */
export function AddTrackerForm({ hint }: { hint?: string }) {
  const services = useServices();
  const api = useMemo(() => adminApi(services), [services]);
  const [imei, setImei] = useState("");
  const [name, setName] = useState("");
  const [message, setMessage] = useState<{ text: string; error?: boolean } | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setMessage(null);
    try {
      await api.claimVehicle(imei.trim(), name.trim());
      setMessage({ text: hint ?? "Rastreador adicionado." });
      setImei("");
      setName("");
    } catch (e) {
      const text = errorMessage(e);
      setMessage({ text: /already linked/.test(text) ? "Este rastreador já está vinculado a outra conta." : text, error: true });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit}>
      <div className="row">
        <input placeholder="IMEI (15 dígitos da etiqueta)" value={imei} onChange={(e) => setImei(e.target.value.replace(/\D/g, ""))} required minLength={4} />
        <input placeholder="Nome (ex.: Hilux ABC1D23)" value={name} onChange={(e) => setName(e.target.value)} maxLength={40} />
        <button type="submit" disabled={busy}>
          Adicionar
        </button>
      </div>
      {message && <p className={message.error ? "error" : "ok"}>{message.text}</p>}
    </form>
  );
}
