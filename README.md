
# TeamGraph

A small internal tool for engineering orgs: given the people, skills, and
projects at a company, answer questions like *"who else knows what I know"*,
*"what's missing from this project's skill set"*, and *"what should someone
learn next to bridge two skills"*. Backed by **CognoDB** (a managed graph
database speaking openCypher over Bolt) via the official Neo4j Java driver.

## Why a graph database?

The interesting questions in this domain are all about **paths and
neighborhoods**, not rows:

- *"Who shares a skill with me?"* is one join in SQL. Fine.
- *"Who worked on a project that needed a skill I have, even though we've
  never worked together directly?"* is a 3–4 table join with no natural stopping
  point, and it only gets worse as you add more relationship types
  (mentorship, skill adjacency, etc.).
- *"What's the shortest chain of related skills connecting Python and
  Kubernetes?"* is a **variable-length path** query. In SQL this needs a
  recursive CTE with a manually bounded depth, re-written for every new
  relationship type you add. In Cypher it's one line:
  `shortestPath((a)-[:RELATED_TO*1..6]-(b))`.

None of this data is naturally tabular — it's a small, densely connected
mesh of people, skills, and projects where the value is in the connections,
not the entities. That's exactly the case a graph database is built for:
relationships are stored as first-class, traversable edges instead of
being reconstructed at query time through foreign keys.

## Data model

```
 (:Person {id, name, role, email})
      │
      ├─[:HAS_SKILL {proficiency, since}]──────▶ (:Skill {id, name, category})
      │
      ├─[:CONTRIBUTED_TO {role, since}]────────▶ (:Project {id, title, status, description})
      │
      └─[:MENTORS]─────────────────────────────▶ (:Person)

 (:Project)─[:REQUIRES {priority}]──────────────▶ (:Skill)

 (:Skill)───[:RELATED_TO {strength}]────────────▶ (:Skill)
```

| Node      | Properties                                  |
|-----------|----------------------------------------------|
| `Person`  | `id`, `name`, `role`, `email`                 |
| `Skill`   | `id`, `name`, `category`                      |
| `Project` | `id`, `title`, `status`, `description`        |

| Relationship    | Direction              | Properties            |
|-----------------|-------------------------|------------------------|
| `HAS_SKILL`     | Person → Skill           | `proficiency`, `since` |
| `CONTRIBUTED_TO`| Person → Project         | `role`, `since`        |
| `REQUIRES`      | Project → Skill          | `priority`             |
| `RELATED_TO`    | Skill → Skill            | `strength`             |
| `MENTORS`       | Person → Person          | —                      |

## Queries

All queries are parameterised through the Neo4j driver (`Values.parameters(...)`)
— no string concatenation of user input into Cypher. See `GraphService.java`.

| Endpoint                              | What it does                                                                 | Hops |
|----------------------------------------|-------------------------------------------------------------------------------|------|
| `GET /api/collaborators?name=`        | People who directly share a skill with this person                            | 2 |
| `GET /api/project-network?name=`      | People connected via a shared project's skill requirements                     | 3 |
| `GET /api/skill-path?from=&to=`       | Shortest chain of `RELATED_TO` skills between two skills — variable-length path, the query a relational schema handles awkwardly | 1–6 |
| `GET /api/projects/{id}/gap`          | Required skills nobody on the project team currently has (anti-join via `NOT EXISTS`) | 2 |
| `GET /api/projects/{id}/suggestions`  | People not yet on the project, ranked by overlap with required skills          | 2 |

Example — the skill path query:

```cypher
MATCH path = shortestPath((a:Skill {name:$from})-[:RELATED_TO*1..6]-(b:Skill {name:$to}))
RETURN [n IN nodes(path) | n.name] AS steps, length(path) AS hops
```

## Project structure

```
graphed/
├── pom.xml
├── cypher/seed.cypher                  # same seed data as GraphService, for manual inspection
├── src/main/java/com/wexa/graphed/
│   ├── GraphedApplication.java
│   ├── config/Neo4jConfig.java         # Driver bean, reads env vars only
│   ├── controller/GraphController.java # REST endpoints
│   ├── service/GraphService.java       # all Cypher lives here
│   └── exception/GlobalExceptionHandler.java  # maps driver failures → clean JSON errors
└── src/main/resources/
    ├── application.properties
    └── static/                         # vanilla HTML/CSS/JS frontend
        ├── index.html
        ├── css/style.css
        └── js/app.js
```

## Setup

### 1. Create a CognoDB instance

1. Sign up at [console.cognodb.com/signup](https://console.cognodb.com/signup) (no credit card needed for the free tier).
2. Create a free (`c0`) instance and pick a region — it provisions in under a minute.
3. Copy the connection URI (`bolt+s://<instance-id>.databases.cognodb.com:<port>`) and the generated password for user `cognodb`. **The password is shown once** — save it immediately.

### 2. Configure environment variables

Never commit credentials. Set these before running the app:

```bash
export COGNODB_URI="bolt+s://<your-instance>.databases.cognodb.com:7687"
export COGNODB_USERNAME="cognodb"
export COGNODB_PASSWORD="<your-password>"
```

On Windows (PowerShell):

```powershell
$env:COGNODB_URI="bolt+s://<your-instance>.databases.cognodb.com:7687"
$env:COGNODB_USERNAME="cognodb"
$env:COGNODB_PASSWORD="<your-password>"
```

### 3. Run

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Click **Seed sample data**
in the sidebar first — the graph starts empty on a fresh instance.

If CognoDB is unreachable (wrong URI, instance paused, bad password), the API
returns a clean JSON error (`503`/`401`) instead of a stack trace, and the UI
shows an inline error state rather than hanging.

## Deploying

Any host that runs a Spring Boot jar works (Render, Railway, Fly.io, a small
VM). Build with `./mvnw clean package`, run the resulting jar with the three
`COGNODB_*` environment variables set, and expose the `PORT` the platform
assigns (the app already reads `${PORT:8080}`).

## A note on the Explorer visualization

The Explorer canvas is intentionally scoped to the selected person's
neighborhood (their skills, direct collaborators, and project-network
connections) rather than rendering the entire graph — with the seed dataset
that's a deliberate UX choice, not a limitation of the data model. `MENTORS`
and the full `RELATED_TO` skill mesh are queried and used (see the Skill Path
view, and `getAllSkills`/`getSkillLearningPath` in `GraphService`), just not
all drawn on one canvas. Worth saying explicitly in the demo recording so it's
clear this is a design decision.

## Tests

`src/test/java/com/wexa/graphed/controller/GraphControllerTest.java` covers
the REST layer with `@WebMvcTest` + a mocked `GraphService`:

- `GET /api/people`
- `GET /api/skills`
- `GET /api/skill-path` (found and not-found cases)
- `GET /api/projects/{id}/gap`
- `GET /api/projects/{id}/suggestions`

Run with:

```bash
./mvnw test
```

These are controller-level tests (the service is mocked), so they don't need
a live CognoDB instance to run — they verify request routing, parameter
binding, and JSON shape, not the Cypher itself. The Cypher queries are best
validated against the real instance during the demo/walkthrough.

## Demo

- **Deployed URL:** _add your hosted link here before submitting_
- **Screen recording:** _add a link (Loom/YouTube unlisted/etc.) here_

## Screenshots

_Add screenshots of the Explorer, Directory, Projects, and Skill Path views
here before submitting — run the app locally or against the deployed URL,
seed the database, and capture each view._
<img width="1880" height="957" alt="Screenshot 2026-08-23 175900" src="https://github.com/user-attachments/assets/2876b49a-4b2c-489d-9a1a-41a55d65b6dc" />
<img width="1905" height="922" alt="Screenshot 2026-08-23 175911" src="https://github.com/user-attachments/assets/3a803a16-3d7f-494a-b624-3d26a809b379" />
<img width="1790" height="841" alt="Screenshot 2026-08-23 175924" src="https://github.com/user-attachments/assets/5721ac66-24c0-4b6f-bb27-49ac0b70d8c5" />
<img width="1598" height="904" alt="Screenshot 2026-08-23 175944" src="https://github.com/user-attachments/assets/31432014-ca1b-497f-9035-62f3edb1c13c" />


