output "tracker_server_ip" {
  description = "Configure trackers to send data to this IP (port depends on the protocol)."
  value       = google_compute_address.traccar.address
}

output "ingest_url" {
  description = "Cloud Function that receives positions from Traccar."
  value       = local.ingest_url
}

output "traccar_ui_tunnel" {
  description = "Opens the Traccar admin UI at http://localhost:8082 through IAP."
  value       = "gcloud compute ssh traccar --zone ${var.zone} --tunnel-through-iap -- -N -L 8082:localhost:8082"
}
