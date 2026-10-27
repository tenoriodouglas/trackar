import { EmailAuthProvider, reauthenticateWithCredential, sendPasswordResetEmail, updatePassword } from "firebase/auth";
import { FormEvent, useState } from "react";
import { useServices } from "../api";

/** Self-service account settings: change password (re-authenticated) or receive a reset e-mail. */
export function Account() {
  const { auth } = useServices();
  const user = auth.currentUser!;
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [message, setMessage] = useState<{ text: string; error?: boolean } | null>(null);
  const [busy, setBusy] = useState(false);

  const change = async (event: FormEvent) => {
    event.preventDefault();
    if (next !== confirm) return setMessage({ text: "As senhas novas não conferem.", error: true });
    if (next.length < 8) return setMessage({ text: "A nova senha precisa de pelo menos 8 caracteres.", error: true });
    setBusy(true);
    setMessage(null);
    try {
      await reauthenticateWithCredential(user, EmailAuthProvider.credential(user.email!, current));
      await updatePassword(user, next);
      setMessage({ text: "Senha alterada." });
      setCurrent("");
      setNext("");
      setConfirm("");
    } catch {
      setMessage({ text: "Senha atual incorreta.", error: true });
    } finally {
      setBusy(false);
    }
  };

  const reset = async () => {
    await sendPasswordResetEmail(auth, user.email!);
    setMessage({ text: `Enviamos um link de redefinição para ${user.email}.` });
  };

  return (
    <div className="setup">
      <div className="card">
        <h2>Minha conta</h2>
        <p>
          E-mail: <b>{user.email}</b>
        </p>
      </div>
      <form className="card" onSubmit={change}>
        <h2>Trocar senha</h2>
        <label>
          Senha atual
          <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} autoComplete="current-password" required />
        </label>
        <label>
          Nova senha
          <input type="password" value={next} onChange={(e) => setNext(e.target.value)} autoComplete="new-password" required />
        </label>
        <label>
          Confirmar nova senha
          <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" required />
        </label>
        <div className="row">
          <button type="submit" disabled={busy}>
            Salvar nova senha
          </button>
          <button type="button" className="ghost" onClick={reset}>
            Esqueci a senha atual
          </button>
        </div>
        {message && <p className={message.error ? "error" : "ok"}>{message.text}</p>}
      </form>
    </div>
  );
}
