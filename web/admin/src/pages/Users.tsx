import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { adminApi, errorMessage, useServices } from "../api";
import { generatePassword, relativeTime } from "../lib/format";
import { AdminUser } from "../lib/types";

export function Users() {
  const services = useServices();
  const api = useMemo(() => adminApi(services), [services]);
  const me = services.auth.currentUser?.uid;
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [email, setEmail] = useState("");
  const [created, setCreated] = useState<{ email: string; password: string } | null>(null);
  const [message, setMessage] = useState<{ text: string; error?: boolean } | null>(null);

  const reload = useCallback(() => api.listUsers().then(setUsers, (e) => setMessage({ text: errorMessage(e), error: true })), [api]);
  useEffect(() => {
    void reload();
  }, [reload]);

  const run = async (action: () => Promise<unknown>) => {
    setMessage(null);
    try {
      await action();
      await reload();
    } catch (error) {
      setMessage({ text: errorMessage(error), error: true });
    }
  };

  const create = (event: FormEvent) => {
    event.preventDefault();
    const password = generatePassword();
    void run(async () => {
      await api.createUser(email.trim(), password);
      setCreated({ email: email.trim(), password });
      setEmail("");
    });
  };

  return (
    <div className="users">
      <form className="card toolbar" onSubmit={create}>
        <b>Novo usuário</b>
        <input type="email" placeholder="e-mail do cliente" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <button type="submit">Criar com senha gerada</button>
      </form>
      {created && (
        <div className="card notice">
          Conta criada. Envie ao cliente (a senha não será mostrada de novo):
          <pre>
            Login: {created.email}
            {"\n"}Senha: {created.password}
          </pre>
          <button onClick={() => navigator.clipboard.writeText(`Login: ${created.email}\nSenha: ${created.password}`)}>Copiar</button>
          <button className="ghost" onClick={() => setCreated(null)}>
            Fechar
          </button>
        </div>
      )}
      {message && <p className={message.error ? "error" : "ok"}>{message.text}</p>}
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>E-mail</th>
              <th>Último acesso</th>
              <th>Papel</th>
              <th>Situação</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.uid}>
                <td>{u.email}</td>
                <td>{relativeTime(u.lastSignIn ? new Date(u.lastSignIn) : undefined)}</td>
                <td>
                  <label className="check">
                    <input type="checkbox" checked={u.admin} disabled={u.uid === me} onChange={(e) => run(() => api.setUserAdmin(u.uid, e.target.checked))} />{" "}
                    admin
                  </label>
                </td>
                <td>
                  <button className="ghost" disabled={u.uid === me} onClick={() => run(() => api.setUserDisabled(u.uid, !u.disabled))}>
                    {u.disabled ? "Reativar" : "Desativar"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
