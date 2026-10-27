import L from "leaflet";
import { useEffect } from "react";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import { VehicleDoc } from "../lib/types";

const STATUS_COLORS: Record<string, string> = { online: "#2ECC71", offline: "#9AA5B1", unknown: "#FFB020" };

/** Heading arrow marker, same visual language as the app. */
function arrowIcon(vehicle: VehicleDoc): L.DivIcon {
  const color = vehicle.engineBlocked ? "#FF5A5F" : (STATUS_COLORS[vehicle.status ?? "unknown"] ?? "#FFB020");
  const course = vehicle.position?.course ?? 0;
  return L.divIcon({
    className: "vehicle-pin",
    iconSize: [36, 36],
    iconAnchor: [18, 18],
    html: `<div style="background:${color}"><svg viewBox="0 0 24 24" style="transform:rotate(${course}deg)"><path fill="#fff" d="M12 2 4.5 20.3l.7.7L12 18l6.8 3 .7-.7z"/></svg></div>`,
  });
}

function FitBounds({ vehicles }: { vehicles: VehicleDoc[] }) {
  const map = useMap();
  const key = vehicles.map((v) => v.id).join(",");
  useEffect(() => {
    const points = vehicles.map((v) => [v.position!.location.latitude, v.position!.location.longitude] as [number, number]);
    if (points.length === 1) map.setView(points[0], 15);
    else if (points.length > 1) map.fitBounds(points, { padding: [40, 40] });
    // Refit only when the set of vehicles changes, not on every position update.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, map]);
  return null;
}

export function FleetMap({ vehicles, onSelect }: { vehicles: VehicleDoc[]; onSelect: (v: VehicleDoc) => void }) {
  const plotted = vehicles.filter((v) => v.position);
  return (
    <MapContainer center={[-14.235, -51.925]} zoom={4} className="map">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <FitBounds vehicles={plotted} />
      {plotted.map((v) => (
        <Marker
          key={v.id}
          position={[v.position!.location.latitude, v.position!.location.longitude]}
          icon={arrowIcon(v)}
          eventHandlers={{ click: () => onSelect(v) }}
        >
          <Popup>
            <b>{v.name}</b>
            <br />
            {Math.round(v.position!.speedKmh)} km/h
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
