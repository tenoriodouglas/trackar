import { onAuthStateChanged, signOut, User } from "firebase/auth";
import { useEffect, useState } from "react";
import { ServicesContext, useServices } from "./api";
import { initServices, Services } from "./firebase";
import { useVehicles } from "./lib/useVehicles";
import { Account } from "./pages/Account";
import { History } from "./pages/History";
import { Login } from "./pages/Login";
import { MyVehicles } from "./pages/MyVehicles";
import { Setup } from "./pages/Setup";
import { Users } from "./pages/Users";
import { Vehicles } from "./pages/Vehicles";

type AuthState = { status: "loading" } | { status: "signedOut" } | { status: "signedIn"; user: User; admin: boolean };

export function App() {
  const [services, setServices] = useState<Services | null>(null);
  const [initError, setInitError] = useState<string | null>(null);
  const [auth, setAuth] = useState<AuthState>({ status: "loading" });

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
      {auth.status === "signedOut" ? <Login /> : <Shell email={auth.user.email ?? ""} admin={auth.admin} />}
    </ServicesContext.Provider>
  );
}

type Tab = "mine" | "fleet" | "history" | "users" | "setup" | "account";

/** Same site for everyone: clients see their own vehicles, admins also get fleet management. */
function Shell({ email, admin }: { email: string; admin: boolean }) {
  const services = useServices();
  const [tab, setTab] = useState<Tab>(admin ? "fleet" : "mine");
  const [historyVehicle, setHistoryVehicle] = useState<string | undefined>();
  const mine = useVehicles("mine");
  const all = useVehicles(admin ? "all" : "mine");

  const tabs: [Tab, string][] = admin
    ? [
        ["fleet", "Frota"],
        ["mine", "Meus veículos"],
        ["history", "Histórico"],
        ["users", "Usuários"],
        ["setup", "Configurar rastreador"],
        ["account", "Minha conta"],
      ]
    : [
        ["mine", "Meus veículos"],
        ["history", "Histórico"],
        ["account", "Minha conta"],
      ];

  const openHistory = (vehicleId: string) => {
    setHistoryVehicle(vehicleId);
    setTab("history");
  };

  return (
    <div className="layout">
      <header className="topbar">
        <div className="brand">
          <img src="/icon.svg" alt="" /> Trackar {admin && <span>Admin</span>}
        </div>
        <nav>
          {tabs.map(([id, label]) => (
            <button key={id} className={tab === id ? "tab active" : "tab"} onClick={() => setTab(id)}>
              {label}
            </button>
          ))}
        </nav>
        <div className="who">
          {email}
          <button className="ghost" onClick={() => signOut(services.auth)}>
            Sair
          </button>
        </div>
      </header>
      <main>
        {!mine || !all ? (
          <div className="muted">Carregando veículos…</div>
        ) : (
          <>
            {tab === "mine" && <MyVehicles vehicles={mine} onOpenHistory={openHistory} />}
            {tab === "history" && <History key={historyVehicle} vehicles={all} initialVehicleId={historyVehicle} />}
            {admin && tab === "fleet" && <Vehicles vehicles={all} onOpenHistory={openHistory} />}
            {admin && tab === "users" && <Users />}
            {admin && tab === "setup" && <Setup />}
            {tab === "account" && <Account />}
          </>
        )}
      </main>
    </div>
  );
}
