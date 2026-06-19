#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ $# -ne 1 ]]; then
  echo "Usage: scripts/restore-db.sh /path/to/backup.sql"
  exit 1
fi

BACKUP_FILE="$1"
if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "[CityGo] Backup file not found: $BACKUP_FILE"
  exit 1
fi

if [[ ! -f ".env.production" ]]; then
  echo "[CityGo] .env.production not found."
  exit 1
fi

set -a
source .env.production
set +a

echo "[CityGo] This will restore database '$MYSQL_DATABASE' from: $BACKUP_FILE"
read -r -p "Type RESTORE to continue: " CONFIRM
if [[ "$CONFIRM" != "RESTORE" ]]; then
  echo "[CityGo] Restore cancelled."
  exit 1
fi

docker compose --env-file .env.production exec -T mysql \
  sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < "$BACKUP_FILE"

echo "[CityGo] Restore finished."
