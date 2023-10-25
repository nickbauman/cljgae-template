# do not commit to source code control
resource "google_project" "{{name}}" {
  name       = "{{name}}"
  project_id = "{{name}}"
  org_id     = "{{org-id}}"
  billing_account = "{{billing-account}}"
  auto_create_network = true
}

resource "google_app_engine_application" "app" {
  project     = google_project.{{name}}.project_id
  location_id = "{{zone}}"
}
