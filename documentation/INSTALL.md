# Checkmate++ — Installation Guide

This guide gets a local development environment running. For cluster
deployment see [DEPLOYMENT.md](DEPLOYMENT.md).

## Architecture at a glance

Checkmate++ is a multi-module Gradle build plus a Vite/React frontend.

| Module / app           | Type            | HTTP   | gRPC   | Depends on                       |
| ---------------------- | --------------- | ------ | ------ | -------------------------------- |
| `frontend`             | Vite + React 19 | 5173   | —      | MatchmakingService, PlayerService, GameServer, VoiceCommandService |
| `MatchmakingService`   | Spring Boot     | 8080   | —      | Redis, GameServerManager         |
| `GameServer`           | Spring Boot     | 8090   | 9091   | Redis, GameServerManager, PlayerService |
| `GameServerManager`    | Spring Boot     | 8081   | 9090   | Redis                            |
| `PlayerService`        | Spring Boot     | 8095   | 9092   | Postgres, Redis, StockfishEngine |
| `StockfishEngine`      | Spring Boot     | 8085   | 9093   | — (bundles Stockfish binary)     |
| `VoiceCommandService`  | Spring Boot     | 9100   | 9101   | OpenAI API                       |
| `ChessLibrary`         | Library         | —      | —      | shared by services               |

Postgres holds player data; Redis carries cache, session, matchmaking
queue, and GameServer ↔ Manager state.

## Prerequisites

- **JDK 25** (Spring Boot 4 / Kotlin 2.3 toolchain — set `JAVA_HOME`).
- **Node.js 25.x** and **npm**.
- **Docker** (only needed for the Postgres + Redis containers, or for
  the full docker-compose path below).
- **Git**.

Optional, depending on path:
- **GraalVM 25** if you want to build native images locally (the
  `bootBuildImage` task enables `BP_NATIVE_IMAGE`).
- **OpenAI API key** if you want voice-command features to work end to
  end — set `openai.api-key` / `OPENAI_API_KEY` for VoiceCommandService.

## Path A — local dev (recommended)

Each service runs straight from Gradle; the frontend runs from Vite.
This is the fastest inner loop — hot reload on frontend, Spring DevTools
on services.

### 1. Start Postgres and Redis

```bash
docker run -d --name redis -p 6379:6379 redis
docker run -d --name postgres \
  -e POSTGRES_USER=checkmate \
  -e POSTGRES_PASSWORD=checkmate \
  -e POSTGRES_DB=checkmate \
  -p 5432:5432 postgres
```

PlayerService runs Flyway migrations on startup against the `checkmate`
database — no manual schema setup needed.

### 2. Run the backend services

From the repo root. Each command blocks; run them in separate terminals
(or background them).

```bash
./gradlew :GameServerManager:bootRun
./gradlew :PlayerService:bootRun
./gradlew :StockfishEngine:bootRun
./gradlew :GameServer:bootRun
./gradlew :MatchmakingService:bootRun
./gradlew :VoiceCommandService:bootRun
```

Order matters on first start:
1. `GameServerManager` (others register with it).
2. `PlayerService` and `StockfishEngine`.
3. `GameServer`, `MatchmakingService`, `VoiceCommandService`.

> If Gradle gets stuck on a rerun, `./gradlew clean` and try again. On
> Windows use `gradlew.bat`.

Default ports and connection details come from each service's
`src/main/resources/application.properties`. Override any of them with
environment variables — for example to point GameServer at a non-local
GameServerManager:

```bash
GAMESERVERMANAGER_HOST=gsm.local GAMESERVERMANAGER_PORT=9090 \
  ./gradlew :GameServer:bootRun
```

The JWT secret in the checked-in `application.properties` files is a
shared, **dev-only** value (`QDTNT7u+qrEDSRmwTYsPawyGevb6Jpm+9j4DsJcJ/aU=`). Never reuse it in a real deployment — generate a fresh one with `openssl rand -base64 32`. All
services that issue or validate tokens must use the same value — keep
it in sync if you override it.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Vite serves on `http://localhost:5173`. Backend URLs come from
`frontend/.env`:

```dotenv
VITE_BASE_URL='http://localhost:8080/'
VITE_PLAYER_SERVICE_URL='http://localhost:8095'
VITE_HTTP_PROTOCOL='http'
VITE_VOICE_URL='http://localhost:9100/api/voice/move'
```

Change these if you're running the backend on different hosts or ports.
`.env.production` is used by the production build (`npm run build`) and
points at the public `checkmateplus.org` domains.

### 4. Sanity checks

- `http://localhost:5173` loads the app.
- `http://localhost:8095/actuator/health` returns `UP` (PlayerService).
- `http://localhost:8095/graphiql` opens the GraphQL playground in dev.
- Register a player, queue for a match — confirms the
  Matchmaking → Manager → GameServer chain is wired up.

## Path B — full stack via docker-compose

Use this when you want the whole system running locally without
building anything — every service is pulled as a pre-built image from
the Kent GitLab registry.

### 1. Log in to the GitLab container registry

The images live at `git.cs.kent.ac.uk:5050/jhl28/checkmate/*` and the
registry requires authentication. Create a personal access token in
GitLab (`read_registry` scope is enough), then:

```bash
docker login git.cs.kent.ac.uk:5050
# Username: your GitLab username
# Password: the personal access token
```

> **On campus only:** the Kent GitLab registry is reachable from the
> university **Wi-Fi** but not from wired Ethernet (the wired network
> sits behind a different firewall path). If `docker login` or `docker
> pull` hangs or returns a TLS / connection-refused error, switch to
> Wi-Fi (or a VPN that routes via the campus network).

### 2. Bring the stack up

From the repo root:

```bash
docker compose up -d
```

This starts Postgres, Redis, all backend services, and
VoiceCommandService. The frontend is **not** included in
`docker-compose.yml` — run it from `frontend/` with `npm run dev` as in
Path A, or build the production image yourself with
`docker build -t checkmate-frontend frontend/`.

### 3. Service URLs

Once `docker compose ps` reports everything healthy:

- MatchmakingService — `http://localhost:8080`
- GameServer — `http://localhost:8090` (WS), `localhost:9091` (gRPC)
- PlayerService — `http://localhost:8095` (HTTP), `localhost:9092` (gRPC)
- StockfishEngine — `http://localhost:8085` (HTTP), `localhost:9093` (gRPC)
- VoiceCommandService — `http://localhost:9100`
- Postgres — `localhost:5432` (user `checkmate`, password `checkmate`)
- Redis — `localhost:6379`

### 4. Stop / tear down

```bash
docker compose down            # stop containers, keep DB volume
docker compose down -v         # also drop the postgres-data volume
```

## Building images locally

Each Spring Boot module has two image-build tasks defined by its
`build.gradle.kts`:

- **`bootBuildImage`** — builds a GraalVM **native image** via Paketo
  buildpacks (Oracle GraalVM builder, G1GC, `-O3`, UPX compression).
  This is the slow path; expect several minutes per service. CI uses
  this for the services that ship as native binaries.
- **`javaBootBuildImage`** — builds a regular JVM image via Paketo
  (Spring AOT + CDS enabled, UPX-compressed). Much faster, no GraalVM
  toolchain needed.

In both cases the produced tag is
`git.cs.kent.ac.uk:5050/jhl28/checkmate/<service>:<version>` (or
`<service>:<commit-sha>` when CI sets `CI_COMMIT_SHA`). The JVM variant
adds a `-jvm` suffix.

### Which task to use per service

- **Native (`bootBuildImage`)** — `MatchmakingService`, `GameServer`,
  `GameServerManager`, `StockfishEngine`. These build cleanly into a
  GraalVM native image.
- **JVM (`javaBootBuildImage`)** — `PlayerService` and
  `VoiceCommandService`. Their dependencies hit native-image
  incompatibilities, so we ship them as JVM images. Note that the prod
  Helm values reflect this — e.g. `playerservice.image.tag: 2.2.2-jvm`.

### Building a single service

```bash
# JVM image — PlayerService / VoiceCommandService
./gradlew :PlayerService:javaBootBuildImage
./gradlew :VoiceCommandService:javaBootBuildImage

# Native image — everything else
./gradlew :MatchmakingService:bootBuildImage
./gradlew :GameServer:bootBuildImage
./gradlew :GameServerManager:bootBuildImage
./gradlew :StockfishEngine:bootBuildImage
```

> **Workaround for "Unsafe Lock" / configuration-cache errors:** when
> building an *individual* image, Gradle's configuration cache can
> trip when running an individual task. It will refer to an "an unsafe lock."
> Rerun with the configuration cache disabled:
>
> ```bash
> ./gradlew :PlayerService:javaBootBuildImage --no-configuration-cache
> ```
>
> This only seems to bite the per-service invocation — a full-project
> build is generally unaffected.

### Frontend image

```bash
docker build -t checkmate-frontend:dev frontend/
```

The `Dockerfile` is multi-stage (`build` → `development` →
`production`); the default target is `production`, which serves the
Vite build through nginx on port 80.

## Troubleshooting

- **Gradle build hangs or fails partway** — run `./gradlew clean` and
  rerun. The native-image plugin occasionally leaves stale state.
- **`POSTGRES_USER` mismatch errors from PlayerService** — `docker-compose.yml`
  uses `checkmate`/`checkmate`; the Helm chart uses `checkmateuser`. If
  you mix paths, drop the Postgres volume (`docker volume rm
  checkmate_postgres-data`) so init runs again.
- **Frontend WebSocket fails to connect** — confirm `VITE_BASE_URL`
  matches where MatchmakingService is actually listening, and that
  `VITE_HTTP_PROTOCOL` matches (`http` for local, `https` for prod).
- **Voice commands return 401/empty** — VoiceCommandService needs a
  valid `openai.api-key`. The dev `application.properties` ships
  blank.
- **Port already in use** — every service's port can be overridden via
  `SERVER_PORT` (or `server.port=` in the application properties).

## Next steps

To deploy to Kubernetes — manual Helm install, or the full Flux +
sealed-secrets + Cloudflared production setup — see
[DEPLOYMENT.md](DEPLOYMENT.md).