#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f ".env.production" ]]; then
  echo "[CityGo] .env.production not found. Copy .env.production.example first."
  exit 1
fi

echo "[CityGo] Validating Docker Compose configuration..."
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml config >/dev/null

echo "[CityGo] Building images..."
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml build

echo "[CityGo] Starting services..."
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml up -d

echo "[CityGo] Deployment command finished. Run scripts/health-check.sh to verify service health."
