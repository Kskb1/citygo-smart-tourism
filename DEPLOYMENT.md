# CityGo Cloud Deployment Guide

## Recommended Server

- Ubuntu 22.04 LTS or 24.04 LTS
- 2 CPU / 4 GB RAM minimum, 4 CPU / 8 GB RAM recommended
- 40 GB SSD or larger
- Open inbound ports: 80, 443
- Do not expose MySQL 3306 or Spring Boot 8080 to the public internet

## Install Docker

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Log out and back in after adding the Docker group.

## Upload Project

Upload or clone the project to the server, for example:

```bash
git clone <your-repo-url> citygo-smart-tourism
cd citygo-smart-tourism
```

## Configure Environment

```bash
cp .env.production.example .env.production
nano .env.production
```

Required values:

```env
MYSQL_ROOT_PASSWORD=change_me_root_password
MYSQL_DATABASE=citygo
MYSQL_USER=citygo_user
MYSQL_PASSWORD=change_me_mysql_password
SPRING_DATASOURCE_PASSWORD=change_me_mysql_password
AMAP_API_KEY=your_amap_web_service_key
JWT_SECRET=change_me_to_a_long_random_string_at_least_32_chars
CITYGO_ADMIN_USERNAME=admin
CITYGO_ADMIN_PASSWORD=change_me_admin_password
CITYGO_ADMIN_EMAIL=admin@example.com
CITYGO_DEMO_USERS_ENABLED=false
```

Never commit `.env.production`.

## First Start

```bash
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml build
docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Or:

```bash
chmod +x scripts/*.sh
bash scripts/deploy.sh
```

## Database Initialization And Migration

Production uses Flyway migrations in:

```text
backend/src/main/resources/db/migration/
```

On a new server, Spring Boot runs migrations automatically when the backend starts with `SPRING_PROFILES_ACTIVE=prod`.

For an existing database, Flyway uses `baseline-on-migrate=true` to avoid deleting data. Do not run `DROP DATABASE` for daily upgrades. `database/schema.sql` and `database/init.sql` remain reference scripts for local development only.

## Check Status

```bash
docker compose --env-file .env.production ps
docker compose --env-file .env.production logs -f backend
bash scripts/health-check.sh
```

Health endpoint:

```text
http://your-domain/api/health
```

It returns service status, database status, AMap configured status, and timestamp. It never returns database passwords, AMap keys, or JWT secrets.

## Verify AMap

After deployment:

```text
/api/system/provider-status
/api/weather?city=成都
/api/spots/search?city=成都&keyword=景点
```

If AMap is not configured, the API returns a clear provider message instead of generated fake data.

## Verify Trip Persistence

1. Log in as the configured admin or a registered user.
2. Generate a plan.
3. Save the trip.
4. Open `/my-trips` and confirm the trip exists.
5. Restart backend:

```bash
docker compose --env-file .env.production restart backend
```

6. Log in again and confirm the trip is still visible.

## Backup Database

```bash
bash scripts/backup-db.sh
```

Backups are written to `backups/citygo-YYYYMMDD-HHMMSS.sql`.

## Restore Database

```bash
bash scripts/restore-db.sh backups/citygo-YYYYMMDD-HHMMSS.sql
```

The restore script asks for confirmation and never deletes Docker volumes.

## Update Project

```bash
git pull
bash scripts/update.sh
```

Flyway applies new migrations on backend startup.

## HTTPS

Use a reverse proxy such as Nginx Proxy Manager, Caddy, or host-level Nginx with Certbot.

Basic host-level Nginx flow:

1. Point your domain A record to the server.
2. Install Certbot.
3. Proxy `https://your-domain` to `http://127.0.0.1:80`.
4. Keep container MySQL and backend unexposed.

## Change Admin Password

Set a new value in `.env.production`:

```env
CITYGO_ADMIN_PASSWORD=new_strong_password
```

Restart backend:

```bash
docker compose --env-file .env.production restart backend
```

When `CITYGO_ADMIN_PASSWORD` is provided, startup updates the configured admin password hash.

## Stop Safely

```bash
docker compose --env-file .env.production stop
```

Avoid `docker compose down -v` unless you intentionally want to delete database volumes.

## Common Issues

- `database DOWN` in `/api/health`: check MySQL container health and database password.
- Login works but saving trip fails: database is not writable or `user_trip` migration failed.
- `/planner` refresh returns 404: check `frontend/nginx.conf` and `try_files`.
- AMap APIs return not configured: set `AMAP_API_KEY` in `.env.production` and restart backend.
- Admin login fails: confirm `CITYGO_ADMIN_USERNAME` and `CITYGO_ADMIN_PASSWORD`, then restart backend.
