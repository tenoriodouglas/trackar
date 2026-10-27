/** Mirrors the app's TripSummary (core/model/TripSummary.kt) so web and app show the same numbers. */
export interface TripPoint {
  lat: number;
  lng: number;
  time: number; // epoch ms
  speedKmh: number;
  course: number;
  valid: boolean;
}

export interface Stop {
  lat: number;
  lng: number;
  start: number;
  end: number;
}

export interface TripSummary {
  distanceKm: number;
  maxSpeedKmh: number;
  movingMs: number;
  stops: Stop[];
}

const MOVING_THRESHOLD_KMH = 3;
const MIN_STOP_MS = 5 * 60_000;
const EARTH_RADIUS_KM = 6371;

export function distanceKm(a: { lat: number; lng: number }, b: { lat: number; lng: number }): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
}

/** Summarizes chronologically sorted points; invalid GPS fixes are ignored. */
export function summarize(all: TripPoint[]): TripSummary {
  const points = all.filter((p) => p.valid);
  let distance = 0;
  let moving = 0;
  for (let i = 1; i < points.length; i++) {
    distance += distanceKm(points[i - 1], points[i]);
    if (points[i - 1].speedKmh >= MOVING_THRESHOLD_KMH) moving += points[i].time - points[i - 1].time;
  }

  const stops: Stop[] = [];
  let stillSince: TripPoint | null = null;
  const closeStop = (from: TripPoint, until: number) => {
    if (until - from.time >= MIN_STOP_MS) stops.push({ lat: from.lat, lng: from.lng, start: from.time, end: until });
  };
  for (const point of points) {
    const still = point.speedKmh < MOVING_THRESHOLD_KMH;
    if (still && !stillSince) stillSince = point;
    if (!still && stillSince) {
      closeStop(stillSince, point.time);
      stillSince = null;
    }
  }
  if (stillSince) closeStop(stillSince, points[points.length - 1].time);

  return {
    distanceKm: distance,
    maxSpeedKmh: points.reduce((max, p) => Math.max(max, p.speedKmh), 0),
    movingMs: moving,
    stops,
  };
}

export function formatDistance(km: number): string {
  return km < 1 ? `${Math.round(km * 1000)} m` : `${km.toFixed(1).replace(".", ",")} km`;
}

export function formatDuration(ms: number): string {
  const minutes = Math.round(ms / 60_000);
  const hours = Math.floor(minutes / 60);
  return hours > 0 ? `${hours}h ${String(minutes % 60).padStart(2, "0")}min` : `${minutes} min`;
}
