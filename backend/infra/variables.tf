variable "project_id" {
  description = "Firebase/GCP project id."
  type        = string
}

variable "region" {
  description = "Region of the Cloud Functions (São Paulo, close to Firestore and users)."
  type        = string
  default     = "southamerica-east1"
}

# Free tier: one e2-micro + 30 GB standard disk per billing account in us-east1/us-central1/us-west1.
# Tracker latency to the US is irrelevant (fixes are sent every few seconds).
variable "vm_region" {
  description = "Region for the Traccar VM and its static IP."
  type        = string
  default     = "us-east1"
}

variable "zone" {
  description = "Zone for the Traccar VM (must be inside vm_region)."
  type        = string
  default     = "us-east1-b"
}

variable "machine_type" {
  description = "VM size. e2-micro (1 GB) is free-tier eligible; use e2-small for large fleets."
  type        = string
  default     = "e2-micro"
}

variable "traccar_heap_mb" {
  description = "JVM heap for Traccar; keep well below VM memory."
  type        = number
  default     = 384
}

variable "traccar_image" {
  description = "Pinned Traccar container image."
  type        = string
  default     = "traccar/traccar:6.16.0"
}

variable "tracker_port_ranges" {
  description = "Ports open to trackers (Traccar listens on 5001-5250, one per protocol)."
  type        = list(string)
  default     = ["5001-5250"]
}
