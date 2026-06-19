#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f ".env.production" ]]; then
  echo "[CityGo] .env.production not found."
  exit 1
fi

echo "[CityGo] Pull latest code before running this script if your server uses Git."
echo "[CityGo] Rebuilding and restarting services without deleting volumes..."
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml up -d
echo "[CityGo] Update finished."
