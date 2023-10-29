# QUICKSTART ONLY; do not commit to source code control
resource "google_project" "{{name}}" {
  name       = "{{name}}"
  project_id = "{{name}}"
  org_id     = "{{org-id}}"
  billing_account = "{{billing-account}}"
  auto_create_network = true
}

locals {
  enable_services = ["iam.googleapis.com","cloudscheduler.googleapis.com","datastore.googleapis.com","cloudresourcemanager.googleapis.com", "cloudtasks.googleapis.com", "cloudidentity.googleapis.com"]
}

resource "google_project_service" "{{name}}" {
  project = "{{name}}"
  for_each = toset(local.enable_services)
  service = each.key
}

resource "google_app_engine_application" "app" {
  project     = google_project.{{name}}.project_id
  location_id = "{{zone}}"
}
