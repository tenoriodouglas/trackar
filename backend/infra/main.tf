locals {
  services = [
    "iam.googleapis.com",
    "compute.googleapis.com",
    "iap.googleapis.com",
    "secretmanager.googleapis.com",
    "cloudfunctions.googleapis.com",
    "cloudbuild.googleapis.com",
    "run.googleapis.com",
    "artifactregistry.googleapis.com",
    "eventarc.googleapis.com",
    "cloudscheduler.googleapis.com",
  ]
  # Same URL scheme Firebase uses for HTTPS functions.
  ingest_url   = "https://${var.region}-${var.project_id}.cloudfunctions.net/ingestPosition"
  bridge_image = "${var.vm_region}-docker.pkg.dev/${var.project_id}/trackar/bridge:latest"
}

resource "google_project_service" "services" {
  for_each           = toset(local.services)
  service            = each.value
  disable_on_destroy = false
}

# ---------- Shared ingest secret (Traccar -> Cloud Function) ----------

resource "random_password" "ingest_token" {
  length  = 48
  special = false
}

resource "google_secret_manager_secret" "ingest_token" {
  # Name must match defineSecret("INGEST_TOKEN") in the functions.
  secret_id = "INGEST_TOKEN"
  replication {
    auto {}
  }
  depends_on = [google_project_service.services]
}

resource "google_secret_manager_secret_version" "ingest_token" {
  secret      = google_secret_manager_secret.ingest_token.id
  secret_data = random_password.ingest_token.result
}

# ---------- Traccar admin account (used by the bridge on localhost) ----------

resource "random_password" "traccar_admin" {
  length  = 32
  special = false
}

resource "google_secret_manager_secret" "traccar_admin" {
  secret_id = "TRACCAR_ADMIN_PASSWORD"
  replication {
    auto {}
  }
  depends_on = [google_project_service.services]
}

resource "google_secret_manager_secret_version" "traccar_admin" {
  secret      = google_secret_manager_secret.traccar_admin.id
  secret_data = random_password.traccar_admin.result
}

# ---------- Bridge image registry (free tier: 0.5 GB) ----------

resource "google_artifact_registry_repository" "trackar" {
  repository_id = "trackar"
  location      = var.vm_region
  format        = "DOCKER"

  cleanup_policies {
    id     = "keep-recent"
    action = "KEEP"
    most_recent_versions {
      keep_count = 3
    }
  }
  cleanup_policies {
    id     = "delete-old"
    action = "DELETE"
    condition {
      older_than = "604800s"
    }
  }
  depends_on = [google_project_service.services]
}

# ---------- Traccar VM ----------

resource "google_service_account" "traccar" {
  account_id   = "traccar-vm"
  display_name = "Traccar gateway VM"
  depends_on   = [google_project_service.services]
}

resource "google_secret_manager_secret_iam_member" "traccar_reads_token" {
  secret_id = google_secret_manager_secret.ingest_token.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.traccar.email}"
}

resource "google_secret_manager_secret_iam_member" "traccar_reads_admin" {
  secret_id = google_secret_manager_secret.traccar_admin.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.traccar.email}"
}

resource "google_artifact_registry_repository_iam_member" "traccar_pulls_bridge" {
  location   = google_artifact_registry_repository.trackar.location
  repository = google_artifact_registry_repository.trackar.name
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.traccar.email}"
}

# Bridge reads pending commands and writes their status.
resource "google_project_iam_member" "traccar_firestore" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.traccar.email}"
}

resource "google_project_iam_member" "traccar_observability" {
  for_each = toset(["roles/logging.logWriter", "roles/monitoring.metricWriter"])
  project  = var.project_id
  role     = each.value
  member   = "serviceAccount:${google_service_account.traccar.email}"
}

resource "google_compute_address" "traccar" {
  name       = "traccar-ip"
  region     = var.vm_region
  depends_on = [google_project_service.services]
}

resource "google_compute_firewall" "trackers" {
  name          = "traccar-allow-trackers"
  network       = "default"
  direction     = "INGRESS"
  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["traccar"]

  allow {
    protocol = "tcp"
    ports    = var.tracker_port_ranges
  }
  allow {
    protocol = "udp"
    ports    = var.tracker_port_ranges
  }
  depends_on = [google_project_service.services]
}

# SSH only through Identity-Aware Proxy; the Traccar UI is reachable via an IAP tunnel, never publicly.
resource "google_compute_firewall" "iap_ssh" {
  name          = "traccar-allow-iap-ssh"
  network       = "default"
  direction     = "INGRESS"
  source_ranges = ["35.235.240.0/20"]
  target_tags   = ["traccar"]

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
  depends_on = [google_project_service.services]
}

resource "google_compute_instance" "traccar" {
  name                      = "traccar"
  machine_type              = var.machine_type
  zone                      = var.zone
  tags                      = ["traccar"]
  allow_stopping_for_update = true

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-12"
      size  = 30
      type  = "pd-standard" # Free-tier disk type.
    }
  }

  network_interface {
    network = "default"
    access_config {
      nat_ip = google_compute_address.traccar.address
    }
  }

  shielded_instance_config {
    enable_secure_boot          = true
    enable_vtpm                 = true
    enable_integrity_monitoring = true
  }

  metadata = {
    enable-oslogin = "TRUE"
  }

  # A changed startup script recreates the VM so it always runs (static IP is kept; positions live in
  # Firestore, Traccar only holds its device registry, which is rebuilt automatically).
  metadata_startup_script = templatefile("${path.module}/startup.sh.tftpl", {
    traccar_image = var.traccar_image
    heap_mb       = var.traccar_heap_mb
    bridge_image  = local.bridge_image
    admin_secret  = google_secret_manager_secret.traccar_admin.secret_id
    registry_host = "${var.vm_region}-docker.pkg.dev"
    ingest_url    = local.ingest_url
    secret_name   = google_secret_manager_secret.ingest_token.secret_id
  })

  service_account {
    email  = google_service_account.traccar.email
    scopes = ["cloud-platform"]
  }

  depends_on = [
    google_secret_manager_secret_version.ingest_token,
    google_secret_manager_secret_version.traccar_admin,
    google_secret_manager_secret_iam_member.traccar_reads_token,
    google_secret_manager_secret_iam_member.traccar_reads_admin,
  ]
}
