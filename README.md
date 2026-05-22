# Checkmate+

A multiplayer chess platform built as a set of Spring Boot
microservices (Kotlin/Java), a Vite + React 19 frontend. Production runs on
Kubernetes behind a Cloudflared tunnel at
[checkmateplus.org](https://checkmateplus.org).

## Documentation

- **[documentation/INSTALL.md](documentation/INSTALL.md)** — local
  development: prerequisites, running services via `./gradlew bootRun`,
  the frontend via `npm run dev`, the full stack via
  `docker compose`, and building images with
  `bootBuildImage` / `javaBootBuildImage`.
- **[documentation/DEPLOYMENT.md](documentation/DEPLOYMENT.md)** —
  Kubernetes deployment: manual Helm install of the `k8s/checkmateplus`
  umbrella chart, and the production GitOps setup using Flux,
  Bitnami sealed-secrets, and Cloudflared.
- **[documentation/Report.pdf](documentation/Report.pdf)** — project
  report covering design, architecture, and evaluation.

## Quick start (local dev)

```bash
# 1. Postgres + Redis
docker run -d --name redis -p 6379:6379 redis
docker run -d --name postgres \
  -e POSTGRES_USER=checkmate -e POSTGRES_PASSWORD=checkmate \
  -e POSTGRES_DB=checkmate -p 5432:5432 postgres

# 2. Backend services (each in its own terminal, from the repo root)
./gradlew :GameServerManager:bootRun
./gradlew :PlayerService:bootRun
./gradlew :StockfishEngine:bootRun
./gradlew :GameServer:bootRun
./gradlew :MatchmakingService:bootRun
./gradlew :VoiceCommandService:bootRun

# 3. Frontend
cd frontend
npm install
npm run dev
```

The app is then available at `http://localhost:5173`. See
[INSTALL.md](documentation/INSTALL.md) for prerequisites, the
docker-compose path, image-build tasks, and troubleshooting.

## Repo layout

```
ChessLibrary/            Shared chess logic (Gradle subproject)
MatchmakingService/      Player queue → GameServer assignment
GameServer/              Match runtime (WebSocket + gRPC)
GameServerManager/       GameServer fleet manager (gRPC)
PlayerService/           Accounts, JWT, GraphQL, Postgres + Flyway
StockfishEngine/         GraalVM-native Stockfish analysis service
VoiceCommandService/     OpenAI-powered voice move input
frontend/                Vite + React 19 client
k8s/checkmateplus/       Helm umbrella chart (one sub-chart per service)
clusters/prod-clust-1/   Flux GitOps config for the prod cluster
docker-compose.yml       Full-stack local run via published images
documentation/           Install / deployment guides + project report
```

## Licence

See [LICENCE](LICENCE).