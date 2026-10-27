import { onAuthStateChanged, signOut, User } from "firebase/auth";
import { useEffect, useState } from "react";
import { ServicesContext } from "./api";
import { initServices, Services } from "./firebase";
import { Login } from "./pages/Login";
import { Setup } from "./pages/Setup";
import { Users } from "./pages/Users";
import { Vehicles } from "./pages/Vehicles";

type Tab = "vehicles" | "users" | "setup";
type AuthState = { status: "loading" } | { status: "signedOut" } | { status: "signedIn"; user: User; admin: boolean };

export function App() {
  const [services, setServices] = useState<Services | null>(null);
  const [initError, setInitError] = useState<string | null>(null);
  const [auth, setAuth] = useState<AuthState>({ status: "loading" });
  const [tab, setTab] = useState<Tab>("vehicles");

  useEffect(() => {
    initServices().then(setServices, (e) => setInitError(String(e)));
  }, []);

  useEffect(() => {
    if (!services) return;
    return onAuthStateChanged(services.auth, async (user) => {
      if (!user) return setAuth({ status: "signedOut" });
      // Force refresh so a role granted after the last sign-in is picked up.
      const token = await user.getIdTokenResult(true);
      setAuth({ status: "signedIn", user, admin: token.claims.admin === true });
    });
  }, [services]);

  if (initError) return <div className="center error">Falha ao carregar: {initError}</div>;
  if (!services || auth.status === "loading") return <div className="center">Carregando…</div>;

  return (
    <ServicesContext.Provider value={services}>
      {auth.status === "signedOut" ? (
        <Login />
      ) : !auth.admin ? (
        <div className="center card narrow">
          <h2>Acesso restrito</h2>
          <p>
            A conta <b>{auth.user.email}</b> não é administradora. No GitHub, rode <b>Actions → Set admin</b> com
            este e-mail e entre de novo.
          </p>
          <button onClick={() => signOut(services.auth)}>Sair</button>
        </div>
      ) : (
        <div className="layout">
          <header className="topbar">
            <div className="brand">
              <img src="/icon.svg" alt="" /> Trackar <span>Admin</span>
            </div>
            <nav>
              {(
                [
                  ["vehicles", "Veículos"],
                  ["users", "Usuários"],
                  ["setup", "Configurar rastreador"],
                ] as [Tab, string][]
              ).map(([id, label]) => (
                <button key={id} className={tab === id ? "tab active" : "tab"} onClick={() => setTab(id)}>
                  {label}
                </button>
              ))}
            </nav>
            <div className="who">
              {auth.user.email}
              <button className="ghost" onClick={() => signOut(services.auth)}>
                Sair
              </button>
            </div>
          </header>
          <main>
            {tab === "vehicles" && <Vehicles />}
            {tab === "users" && <Users />}
            {tab === "setup" && <Setup />}
          </main>
        </div>
      )}
    </ServicesContext.Provider>
  );
}
