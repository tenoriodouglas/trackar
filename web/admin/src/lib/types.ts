import { Timestamp } from "firebase/firestore";

export type CommandType = "engineStop" | "engineResume";

export interface VehicleDoc {
  id: string;
  name: string;
  uniqueId: string;
  category?: string | null;
  status?: "online" | "offline" | "unknown";
  ownerIds?: string[];
  lastUpdate?: Timestamp;
  engineBlocked?: boolean;
  lastCommand?: { type: CommandType; status: string; errorCode?: string | null; requestedAt?: Timestamp };
  position?: {
    location: { latitude: number; longitude: number };
    fixTime: Timestamp;
    speedKmh: number;
    course: number;
    address?: string | null;
    ignition?: boolean | null;
  };
}

export interface AdminUser {
  uid: string;
  email: string;
  disabled: boolean;
  admin: boolean;
  lastSignIn: string | null;
  createdAt: string;
}
