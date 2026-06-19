#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

HTTP_PORT="${HTTP_PORT:-80}"
if [[ -f ".env.production" ]]; then
  set -a
  source .env.production
  set +a
fi

echo "[CityGo] Docker service status:"
docker compose --env-file .env.production ps

echo "[CityGo] Checking frontend..."
curl -fsS "http://localhost:${HTTP_PORT}/" >/dev/null

echo "[CityGo] Checking backend health through Nginx..."
curl -fsS "http://localhost:${HTTP_PORT}/api/health"
echo
echo "[CityGo] Health check finished."
