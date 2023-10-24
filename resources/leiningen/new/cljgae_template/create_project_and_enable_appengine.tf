resource "google_project" "{{name}}" {
  name       = "{{name}}"
  project_id = "{{name}}"
  org_id     = "{{org-id}}"
}

resource "google_app_engine_application" "app" {
  project     = google_project.{{name}}.project_id
  location_id = "{{zone}}"
}