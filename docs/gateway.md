# Gateway (Google Cloud)

```
Tracker ─TCP/UDP─▶ Traccar (Compute Engine VM, static IP) ─HTTPS + token─▶ Cloud Function ingestPosition ─▶ Firestore ─▶ App
                                                                            Cloud Scheduler → markOffline (every 5 min)
```

- **Traccar** decodes 200+ tracker protocols (GT06, Suntech, Teltonika, H02, Coban…). Each protocol has its own
  port (5001-5250). Unknown devices are auto-registered.
- **ingestPosition** validates the token, writes the latest fix to `vehicles/{imei}` (only if newer) and appends
  it to `vehicles/{imei}/positions/{fixTimeMs}` (idempotent, safe for retries). History expires via TTL (`expireAt`, 90 days).
- **markOffline** flips vehicles to `offline` after 10 min without data.
- **Bridge** (`backend/bridge`, container on the VM) listens to pending `commands` in Firestore and sends them
  through Traccar's localhost API (no inbound port is opened). The VM pulls new bridge images every 5 minutes.
- **Callables** (`sendCommand`, `claimVehicle`, `admin*`) validate ownership/admin role server-side.
- Everything is provisioned by Terraform (`backend/infra`) and deployed by `.github/workflows/backend.yml`.

## One-time setup

1. Firebase project on the **Blaze** plan, Firestore created in `southamerica-east1`, Email/Password auth enabled.
2. Service account for CI (secret `FIREBASE_SERVICE_ACCOUNT`) with roles:
   `Editor`, `Secret Manager Admin`, `Project IAM Admin`, `Cloud Functions Admin`, `Service Account User`,
   `Firebase Admin`.
3. Secrets `GOOGLE_SERVICES_JSON_BASE64` and `FIREBASE_SERVICE_ACCOUNT` set → run **Backend CI/CD** (or push to main).
   The run summary shows the **tracker server IP**.

## Connecting a tracker

Configure the tracker (usually by SMS, see its manual) to send to `<tracker server IP>:<protocol port>`.
Common ports: GT06 `5023`, Coban/TK103 `5001`/`5002`, H02 `5013`, Suntech `5011`, Teltonika `5027`, OsmAnd app `5055`.
Full list: https://www.traccar.org/devices/

Then link it to a user: **Actions → Register vehicle** (IMEI + user e-mail). The vehicle shows up in the app.

## Traccar admin UI

Not exposed publicly. Open a tunnel and browse http://localhost:8082:

```bash
gcloud compute ssh traccar --zone us-east1-b --tunnel-through-iap -- -N -L 8082:localhost:8082
```

## Cost (estimate)

The VM runs on the GCP **free tier** (e2-micro + 30 GB standard disk in `us-east1`). The only fixed cost is the
static IPv4 (≈ US$ 3.65/month). Functions, Firestore and Scheduler stay within the free quota for small fleets
(e.g. ~20k Firestore writes/day ≈ 30 vehicles reporting every minute). Set a budget alert in Billing.
