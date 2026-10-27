import { collection, getDocs, GeoPoint, orderBy, query, Timestamp, where } from "firebase/firestore";
import L from "leaflet";
import { useEffect, useMemo, useState } from "react";
import { CircleMarker, MapContainer, Marker, Polyline, Popup, TileLayer, useMap } from "react-leaflet";
import { useServices } from "../api";
import { formatDistance, formatDuration, summarize, TripPoint } from "../lib/trip";
import { VehicleDoc } from "../lib/types";

const MAX_POINTS = 5000;

function todayIso(): string {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

function cursorIcon(course: number): L.DivIcon {
  return L.divIcon({
    className: "vehicle-pin",
    iconSize: [36, 36],
    iconAnchor: [18, 18],
    html: `<div style="background:#12B5F0"><svg viewBox="0 0 24 24" style="transform:rotate(${course}deg)"><path fill="#fff" d="M12 2 4.5 20.3l.7.7L12 18l6.8 3 .7-.7z"/></svg></div>`,
  });
}

function FitRoute({ points }: { points: [number, number][] }) {
  const map = useMap();
  useEffect(() => {
    if (points.length === 1) map.setView(points[0], 16);
    else if (points.length > 1) map.fitBounds(points, { padding: [40, 40] });
  }, [points, map]);
  return null;
}

/** Day route for one vehicle: polyline, stops, summary and a scrubber to replay the trip. */
export function History({ vehicles, initialVehicleId }: { vehicles: VehicleDoc[]; initialVehicleId?: string }) {
  const { db } = useServices();
  const [vehicleId, setVehicleId] = useState(initialVehicleId ?? vehicles[0]?.id ?? "");
  const [day, setDay] = useState(todayIso());
  const [points, setPoints] = useState<TripPoint[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cursor, setCursor] = useState(0);

  useEffect(() => {
    if (!vehicleId && vehicles[0]) setVehicleId(vehicles[0].id);
  }, [vehicles, vehicleId]);

  useEffect(() => {
    if (!vehicleId) return;
    let cancelled = false;
    setPoints(null);
    setError(null);
    const start = new Date(`${day}T00:00:00`);
    const end = new Date(start.getTime() + 86_400_000);
    getDocs(
      query(
        collection(db, "vehicles", vehicleId, "positions"),
        where("fixTime", ">=", Timestamp.fromDate(start)),
        where("fixTime", "<", Timestamp.fromDate(end)),
        orderBy("fixTime"),
      ),
    )
      .then((snapshot) => {
        if (cancelled) return;
        const loaded = snapshot.docs.slice(0, MAX_POINTS).map((d) => {
          const data = d.data();
          const location = data.location as GeoPoint;
          return {
            lat: location.latitude,
            lng: location.longitude,
            time: (data.fixTime as Timestamp).toMillis(),
            speedKmh: Number(data.speedKmh ?? 0),
            course: Number(data.course ?? 0),
            valid: data.valid !== false,
          };
        });
        setPoints(loaded);
        setCursor(Math.max(loaded.length - 1, 0));
      })
      .catch((e) => !cancelled && setError(String(e)));
    return () => {
      cancelled = true;
    };
  }, [db, vehicleId, day]);

  const route = useMemo(() => (points ?? []).map((p) => [p.lat, p.lng] as [number, number]), [points]);
  const summary = useMemo(() => summarize(points ?? []), [points]);
  const current = points?.[cursor];
  const time = (ms: number) => new Date(ms).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });

  if (vehicles.length === 0) return <div className="card">Nenhum veículo na sua conta ainda.</div>;

  return (
    <div className="history">
      <div className="card toolbar">
        <select value={vehicleId} onChange={(e) => setVehicleId(e.target.value)}>
          {vehicles.map((v) => (
            <option key={v.id} value={v.id}>
              {v.name}
            </option>
          ))}
        </select>
        <input type="date" value={day} max={todayIso()} onChange={(e) => e.target.value && setDay(e.target.value)} />
      </div>

      <section className="stats">
        <div className="card stat">
          <span>Distância</span>
          <b>{formatDistance(summary.distanceKm)}</b>
        </div>
        <div className="card stat">
          <span>Vel. máxima</span>
          <b>{Math.round(summary.maxSpeedKmh)} km/h</b>
        </div>
        <div className="card stat">
          <span>Tempo rodando</span>
          <b>{formatDuration(summary.movingMs)}</b>
        </div>
        <div className="card stat">
          <span>Paradas</span>
          <b>{summary.stops.length}</b>
        </div>
      </section>

      <div className="card map-card">
        <MapContainer center={[-14.235, -51.925]} zoom={4} className="map">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <FitRoute points={route} />
          {route.length > 1 && (
            <>
              <Polyline positions={route} pathOptions={{ color: "#1B2A40", weight: 9, opacity: 0.9 }} />
              <Polyline positions={route} pathOptions={{ color: "#12B5F0", weight: 5 }} />
              <CircleMarker center={route[0]} radius={8} pathOptions={{ color: "#fff", fillColor: "#2ECC71", fillOpacity: 1 }}>
                <Popup>Início {time(points![0].time)}</Popup>
              </CircleMarker>
              <CircleMarker center={route[route.length - 1]} radius={8} pathOptions={{ color: "#fff", fillColor: "#FF5A5F", fillOpacity: 1 }}>
                <Popup>Fim {time(points![points!.length - 1].time)}</Popup>
              </CircleMarker>
            </>
          )}
          {summary.stops.map((s) => (
            <CircleMarker key={s.start} center={[s.lat, s.lng]} radius={7} pathOptions={{ color: "#fff", fillColor: "#FFC93C", fillOpacity: 1 }}>
              <Popup>
                Parado das {time(s.start)} às {time(s.end)} ({formatDuration(s.end - s.start)})
              </Popup>
            </CircleMarker>
          ))}
          {current && <Marker position={[current.lat, current.lng]} icon={cursorIcon(current.course)} />}
        </MapContainer>
      </div>

      <div className="card playback">
        {error && <p className="error">Não foi possível carregar o histórico.</p>}
        {!points && !error && <p className="muted">Carregando trajeto…</p>}
        {points && points.length === 0 && <p className="muted">Nenhum trajeto neste dia. O veículo não enviou posições.</p>}
        {points && points.length > 0 && current && (
          <>
            <div className="row">
              <b>{time(current.time)}</b>
              <b className="right">{Math.round(current.speedKmh)} km/h</b>
            </div>
            <input type="range" min={0} max={points.length - 1} value={cursor} onChange={(e) => setCursor(Number(e.target.value))} />
          </>
        )}
      </div>
    </div>
  );
}
