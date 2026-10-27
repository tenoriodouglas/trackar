import { doc, onSnapshot } from "firebase/firestore";
import { useEffect, useState } from "react";
import { useServices } from "../api";
import { AddTrackerForm } from "../components/AddTrackerForm";
import { APNS, DEVICE_PROFILES } from "../lib/devices";

export function Setup() {
  const services = useServices();
  const [ip, setIp] = useState("…");
  const [profileId, setProfileId] = useState(DEVICE_PROFILES[0].id);
  const [apn, setApn] = useState(APNS[0].apn);

  useEffect(
    () => onSnapshot(doc(services.db, "config", "server"), (snapshot) => setIp((snapshot.get("trackerIp") as string) ?? "(publicado no próximo deploy)")),
    [services.db],
  );
  const profile = DEVICE_PROFILES.find((p) => p.id === profileId)!;

  return (
    <div className="setup">
      <div className="card">
        <h2>1. Servidor</h2>
        <p>
          IP do servidor: <b className="mono big">{ip}</b> — cada modelo usa uma porta (tabela abaixo).
        </p>
      </div>

      <div className="card">
        <h2>2. Comandos de configuração</h2>
        <div className="row">
          <label>
            Modelo
            <select value={profileId} onChange={(e) => setProfileId(e.target.value)}>
              {DEVICE_PROFILES.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Operadora do chip (APN)
            <select value={apn} onChange={(e) => setApn(e.target.value)}>
              {APNS.map((a) => (
                <option key={a.carrier} value={a.apn}>
                  {a.carrier} — {a.apn}
                </option>
              ))}
            </select>
          </label>
        </div>
        <p className="muted">
          Exemplos: {profile.examples}. Porta <b>{profile.port}</b>. Bloqueio remoto: {profile.blockSupport}.
        </p>
        <p>Envie por SMS para o número do chip do rastreador, um por vez, aguardando a resposta:</p>
        <pre>{profile.smsSetup(ip, apn).join("\n")}</pre>
        <p className="muted">
          Senhas de fábrica comuns: 123456 ou 0000. A sintaxe varia por firmware; em caso de dúvida, siga o manual do modelo.
        </p>
      </div>

      <div className="card">
        <h2>3. Vincular à conta</h2>
        <p>O IMEI (15 dígitos) está na etiqueta do rastreador. O cliente também pode fazer isso no app ou no site (Meus veículos → Adicionar rastreador).</p>
        <AddTrackerForm hint="Rastreador vinculado à sua conta. Para passar a outro cliente, use a aba Frota → Donos." />
      </div>

      <div className="card">
        <h2>4. Instalação e bloqueio</h2>
        <ul>
          <li>
            <b>Alimentação:</b> vermelho no +12 V direto da bateria (fusível 1–2 A), preto no negativo, amarelo/branco no pós-chave
            (ignição).
          </li>
          <li>
            <b>Bloqueio:</b> a saída do rastreador aciona um relé 12 V que interrompe a bomba de combustível ou a ignição. Use o relé
            no contato <b>normalmente fechado</b> para o carro funcionar se o rastreador desligar.
          </li>
          <li>
            O bloqueio é imediato em qualquer velocidade, com confirmação que avisa se o veículo está andando. Alguns
            modelos só cortam abaixo de ~20 km/h por trava no firmware. Prefira instalação por profissional.
          </li>
        </ul>
      </div>
    </div>
  );
}
