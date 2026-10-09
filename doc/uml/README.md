# DEWECS UML (PlantUML source)

Plain-text diagrams that match the code (entities, service rules, flash messages and status codes were read from the
source). Edit the `.puml` files and re-render; no image is committed.

| File | Diagram | What it shows |
|---|---|---|
| `usecase.puml` | Use case | Actors (citizen, DMC officer, rescue coordinator, relief coordinator) and the five use cases UC-01 to UC-05 with include and extend |
| `class-domain.puml` | Class | The JPA entities, their enums and relationships (joined `User` / `Citizen`, post-event report and metrics) |
| `class-layers.puml` | Class | Backend layers: controllers, service interfaces and implementations, repositories, the S3 / folder photo stores, the JSON filter and interceptor |
| `class-mobile.puml` | Class | The Flutter app: API interface with the HTTP and demo implementations, the offline queue and sync service, controllers, screens |
| `sequence-issue-warning.puml` | Sequence | UC-01 draft, publish, retract, lazy expiry |
| `sequence-citizen-report.puml` | Sequence | UC-02 offline queue, retry with back-off, replay safe submit, photo upload |
| `sequence-officer-review.puml` | Sequence | UC-02 verify, action with note, dismiss, citizen sees the outcome |
| `sequence-shelter-rescue.puml` | Sequence | UC-03 check-in and out, rescue request assign, complete, cancel |
| `sequence-relief-distribution.puml` | Sequence | UC-04 dispatch (stock deducted), deliver, cancel (stock restored) |
| `sequence-post-event-report.puml` | Sequence | UC-05 generate the report and its four metrics, then view it |
| `sequence-officer-json.puml` | Sequence | An officer page answering in JSON (Accept header, JSON body, problem+json) |

## How to render

- **Online, nothing to install:** paste a file into https://www.plantuml.com/plantuml (class and use case diagrams
  need no Graphviz there).
- **VS Code:** install the "PlantUML" extension (jebbs), open a `.puml`, press Alt+D. It needs Java; class and use
  case diagrams also need Graphviz (`choco install graphviz`) unless you use the server render.
- **Command line:** `java -jar plantuml.jar -tpng doc/uml/*.puml` (or `-tsvg`). Sequence diagrams render with Java
  alone; class and use case diagrams need Graphviz installed (`dot`).
- **IntelliJ:** the "PlantUML integration" plugin.

For a Word report, export SVG or PNG at a large size (`-tpng -Sdpi=200`) and insert the image.

## Checked

All files were parsed with PlantUML 1.2025.4 (`-checkonly`: no errors). The sequence diagrams were rendered and
read; the class and use case diagrams were only syntax-checked here because Graphviz is not installed on the
build machine.
