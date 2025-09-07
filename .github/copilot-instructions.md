# Copilot Instructions for cljgae-template

## Project Overview
This is a **Leiningen template** for creating Google App Engine applications in Clojure using the GAE Java SDK (supports Java 11/17 runtimes with bundled services). It's NOT a regular Clojure application - it's a meta-project that generates other Clojure GAE projects.

## Architecture & Key Components

### Template Structure
- **`src/leiningen/new/cljgae_template.clj`**: Main template definition with file mappings
- **`resources/leiningen/new/cljgae_template/`**: Template files with `{{variable}}` placeholders
- **`freshexample01/`**: Example generated project for testing template output
- **`project.clj`**: Template's own project definition (`:eval-in-leiningen true`)

### Generated Project Architecture
Templates create GAE applications with this structure:
- **Ring/Compojure web framework** with GAE-specific middleware
- **Custom Datastore DSL** (`gaeclj.ds`) for entity modeling and querying
- **Integrated GAE services**: Cloud Storage, Push Queues, User Service, App Identity
- **Hiccup templates** for HTML generation

## Development Workflows

### Template Development
```bash
# Install template locally for testing
lein install

# Generate test project (requires 5 parameters) - must run from template directory
lein new cljgae-template project-name org-id zone billing-account java-runtime

# Example with fake values (from template directory)
lein new cljgae-template myapp 123456789 us-central 4D2-1D73A5-012F81-6E5 17
```

### Generated Project Workflows
```bash
# Run development server (in generated project)
./run-dev.sh

# Build deployable WAR
lein ring uberwar

# Deploy to GAE
./deploy.sh

# Run tests
lein test
```

## Project-Specific Patterns

### Template Variable System
- Uses Leiningen's `{{name}}`, `{{sanitized}}` pattern for file/namespace generation
- Custom variables: `{{org-id}}`, `{{zone}}`, `{{billing-account}}`, `{{java-runtime}}`, `{{year}}`
- Binary files (like JPGs) use `raw-copy` function instead of `render`

### Datastore Entity DSL
Generated projects include a custom DSL in `model.clj`:
```clojure
(defentity FileUpload [key transfer-date])  ; Define entity
(create-FileUpload filename (t/now))        ; Create instance
(query-FileUpload [:key = "somekey"])       ; Query with custom syntax
```

### GAE Integration Patterns
- **Environment detection**: `gaeclj.env/environment` (dev vs production)
- **GCS integration**: `gaeclj.gcs/with-gcs-output-stream` for file uploads
- **Push queues**: `gaeclj.push-queue` for background processing
- **User service**: Built-in authentication via GAE User API

### Template File Organization
- **Handler templates**: Web routes and request processing
- **Model templates**: Entity definitions using custom DSL
- **View templates**: Hiccup-based HTML generation
- **Config templates**: GAE-specific XML files (appengine-web.xml, etc.)
- **Infrastructure**: Terraform for project setup

## Critical Dependencies
- **App Engine Java SDK**: Required for local development and deployment
- **Leiningen**: Template system and build tool
- **Custom GAE libraries**: `gaeclj.*` namespaces for GAE service integration
- **Ring ecosystem**: Web framework with GAE-specific adaptations

## Common Gotchas
- Template files must exist in `resources/leiningen/new/cljgae_template/` or generation fails
- Generated projects require GAE Java SDK in PATH for `run-dev.sh`
- Binary files need `raw-copy`, not `render` to avoid encoding issues
- Template requires exactly 5 parameters: name, org-id, zone, billing-account, java-runtime

## Key Files for Template Modification
- **Template definition**: `src/leiningen/new/cljgae_template.clj` (line 41+ for file mappings)
- **Handler template**: `resources/.../handler.clj` (main web application logic)
- **Project template**: `resources/.../project.clj` (dependencies and build config)
- **Infrastructure**: `resources/.../create_project_and_enable_appengine.tf` (GCP setup)
- **VS Code templates**: `resources/.../vscode_settings.json` and `vscode_workspace.json` (Calva configuration)

## VS Code/Calva Integration
Generated projects include optimized Calva configuration:
- **`.vscode/settings.json`**: Calva REPL settings, file associations, formatting preferences
- **`projectname.code-workspace`**: VS Code workspace configuration with extension recommendations
- **Recommended extensions**: Calva, clojure-lsp, XML support for GAE configuration files
- **REPL configuration**: Pre-configured for Leiningen with dev profile
- **File exclusions**: Hides build artifacts and cache directories from VS Code explorer

## Google has changed its API and libraries for App Engine
- The existing libraries may not be compatible with the latest App Engine features.
- Developers should consult the [Google Cloud documentation](https://cloud.google.com/appengine/docs) for the most up-to-date information on API changes.
- It's recommended to regularly update dependencies and test applications against the latest App Engine SDK.

