import { signInWithEmailAndPassword } from "firebase/auth";
import { FormEvent, useState } from "react";
import { useServices } from "../api";

export function Login() {
  const { auth } = useServices();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await signInWithEmailAndPassword(auth, email.trim(), password);
    } catch {
      setError("E-mail ou senha inválidos.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="center">
      <form className="card narrow login" onSubmit={submit}>
        <img src="/icon.svg" alt="" width={56} />
        <h1>Trackar</h1>
        <p className="muted">Rastreamento veicular em tempo real</p>
        <input type="email" placeholder="E-mail" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required />
        <input type="password" placeholder="Senha" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={busy}>
          {busy ? "Entrando…" : "Entrar"}
        </button>
      </form>
    </div>
  );
}
