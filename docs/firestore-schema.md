# Firestore schema

M2M/GPS trackers speak binary TCP/UDP protocols (GT06, Suntech, Teltonika…) and cannot write to
Firestore directly. A **gateway** decodes them and writes with the Admin SDK:

```
Tracker ──TCP/UDP──▶ Traccar (GCE VM) ──HTTPS──▶ Cloud Function ingestPosition ──Admin SDK──▶ Firestore ──realtime──▶ App
```

## `vehicles/{imei}`

Document id = tracker unique id (IMEI). Created automatically on first fix with empty `ownerIds`;
linked to users by the **Register vehicle** workflow.

<!-- fields -->

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

## `vehicles/{vehicleId}/positions/{autoId}` (history)

One document per GPS fix, with the same fields as `position` above (`location` and `fixTime` required).
The gateway appends here on every fix and also overwrites `vehicles/{id}.position` with the latest one.
The app queries one day at a time (`fixTime` range, ascending, max 5,000 points); the automatic
single-field index on `fixTime` is enough. Consider a TTL policy on `fixTime` to cap storage (e.g. 90 days).

Security rules: [`firebase/firestore.rules`](../firebase/firestore.rules) — clients are read-only and
only see documents whose `ownerIds` contains their UID.
