variable "project_id" {
  description = "Firebase/GCP project id."
  type        = string
}

variable "region" {
  description = "Region for the VM and Cloud Functions (São Paulo by default)."
  type        = string
  default     = "southamerica-east1"
}

variable "zone" {
  description = "Zone for the Traccar VM."
  type        = string
  default     = "southamerica-east1-a"
}

variable "machine_type" {
  description = "Traccar runs on the JVM; e2-small (2 GB) is the practical minimum."
  type        = string
  default     = "e2-small"
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
