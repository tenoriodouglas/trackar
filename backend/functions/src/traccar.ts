import { GeoPoint, Timestamp } from "firebase-admin/firestore";

/** Subset of the JSON body Traccar POSTs when position forwarding is enabled. */
export interface TraccarForward {
  position?: {
    fixTime?: string;
    valid?: boolean;
    latitude?: number;
    longitude?: number;
    speed?: number; // knots
    course?: number;
    address?: string | null;
    attributes?: Record<string, unknown>;
  };
  device?: {
    uniqueId?: string;
    name?: string;
    category?: string | null;
  };
}

/** Position document as stored in Firestore (see docs/firestore-schema.md). */
export interface PositionDoc {
  location: GeoPoint;
  fixTime: Timestamp;
  speedKmh: number;
  course: number;
  address: string | null;
  ignition: boolean | null;
  motion: boolean | null;
  powerVolts: number | null;
  valid: boolean;
}

export interface ParsedForward {
  uniqueId: string;
  deviceName: string;
  category: string | null;
  position: PositionDoc;
}

export class InvalidPayloadError extends Error {}

const KNOTS_TO_KMH = 1.852;

/** Validates a Traccar payload and maps it to the app's Firestore schema. */
export function parseForward(body: unknown): ParsedForward {
  const payload = (body ?? {}) as TraccarForward;
  const { position, device } = payload;
  const uniqueId = device?.uniqueId?.trim();
  if (!uniqueId || !/^[\w.-]{1,64}$/.test(uniqueId)) throw new InvalidPayloadError("device.uniqueId missing or invalid");
  if (!position) throw new InvalidPayloadError("position missing");

  const { latitude, longitude } = position;
  if (!isFiniteIn(latitude, -90, 90) || !isFiniteIn(longitude, -180, 180)) {
    throw new InvalidPayloadError("latitude/longitude invalid");
  }
  const fixTime = Date.parse(position.fixTime ?? "");
  if (Number.isNaN(fixTime)) throw new InvalidPayloadError("fixTime invalid");

  const attributes = position.attributes ?? {};
  return {
    uniqueId,
    deviceName: device?.name?.trim() || uniqueId,
    category: device?.category ?? null,
    position: {
      location: new GeoPoint(latitude, longitude),
      fixTime: Timestamp.fromMillis(fixTime),
      speedKmh: round((position.speed ?? 0) * KNOTS_TO_KMH, 1),
      course: position.course ?? 0,
      address: position.address ?? null,
      ignition: typeof attributes.ignition === "boolean" ? attributes.ignition : null,
      motion: typeof attributes.motion === "boolean" ? attributes.motion : null,
      powerVolts: typeof attributes.power === "number" ? attributes.power : null,
      valid: position.valid ?? true,
    },
  };
}

function isFiniteIn(value: unknown, min: number, max: number): value is number {
  return typeof value === "number" && Number.isFinite(value) && value >= min && value <= max;
}

function round(value: number, decimals: number): number {
  const factor = 10 ** decimals;
  return Math.round(value * factor) / factor;
}
