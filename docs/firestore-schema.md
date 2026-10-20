# Firestore schema

M2M/GPS trackers speak binary TCP/UDP protocols (GT06, Suntech, Teltonika…) and cannot write to
Firestore directly. A **gateway** decodes them and writes with the Admin SDK:

```
Tracker ──TCP/UDP──▶ Gateway (e.g. Traccar + position forwarding → Cloud Function) ──Admin SDK──▶ Firestore ──realtime──▶ App
```

## `vehicles/{vehicleId}`

| Field        | Type          | Notes                                     |
|--------------|---------------|-------------------------------------------|
| `name`       | string        | Required. Display name / plate.           |
| `uniqueId`   | string        | Device IMEI.                              |
| `ownerIds`   | array<string> | Firebase Auth UIDs allowed to see it.     |
| `status`     | string        | `online` \| `offline` \| `unknown`.       |
| `lastUpdate` | timestamp     | Last communication with the device.       |
| `category`   | string        | Optional (car, truck, motorcycle…).       |
| `position`   | map           | Last fix (see below). Optional.           |

### `position`

| Field        | Type      | Notes                         |
|--------------|-----------|-------------------------------|
| `location`   | geopoint  | Required.                     |
| `fixTime`    | timestamp | Required. GPS fix time.       |
| `speedKmh`   | number    | km/h.                         |
| `course`     | number    | Heading, degrees from north.  |
| `address`    | string    | Reverse-geocoded, optional.   |
| `ignition`   | boolean   | Optional.                     |
| `motion`     | boolean   | Optional.                     |
| `powerVolts` | number    | Vehicle battery, optional.    |
| `valid`      | boolean   | GPS fix validity.             |

Security rules: [`firebase/firestore.rules`](../firebase/firestore.rules) — clients are read-only and
only see documents whose `ownerIds` contains their UID.
