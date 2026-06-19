# CityGo Final Regression Report

Date: 2026-06-19

## Executed Checks

| Check | Result | Notes |
| --- | --- | --- |
| Backend `mvnw.cmd test` | Passed | 4 MockMvc tests: login/profile, register, 401/403, save trip and user isolation |
| Backend `mvnw.cmd -DskipTests package` | Passed | Spring Boot jar repackaged successfully |
| Frontend `npm ci` | Passed | Completed after elevated permissions; npm reported audit findings, no force fix executed |
| Frontend `npm run build` | Passed | Vite build succeeded; existing chunk size warning remains |
| Source keyword scan | Passed | No project-source usage of random/fake price keywords; third-party `node_modules` and `dist` excluded |
| Docker CLI availability | Not executed | `docker` command is not available on this machine |
| Docker Compose config/build/up | Not executed | Blocked by missing Docker CLI |
| MySQL persistence restart test | Not executed | Local MySQL port 3306 was not reachable in this environment |
| Browser E2E/manual viewport test | Not executed | No browser automation environment was started for this run |

## Implemented Readiness Changes

- Authentication now uses MySQL-backed `users` table as the user source.
- Passwords are encoded with BCrypt; legacy development passwords are upgraded after successful login.
- Bearer tokens are JWTs signed with `JWT_SECRET` and include expiration.
- User trip saves derive `userId` from the current JWT, not from frontend input.
- `user_trip` stores `price_mode` and `price_rule_version` snapshots.
- Flyway production migrations were added under `backend/src/main/resources/db/migration/`.
- Docker, Nginx, Compose, production env template, deployment scripts, and deployment guide were added.
- `/api/health` was added for container health checks.

## Remaining Manual Verification Required

Run these on a machine with Docker and MySQL available:

1. `docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml config`
2. `docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml build`
3. `docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml up -d`
4. Verify `/api/health`.
5. Register a user, save a trip, restart backend, and confirm the trip persists.
6. Confirm a second user cannot view the first user's trip.
7. Confirm normal user receives 403 for `/api/admin/dashboard`.
8. Refresh `/login`, `/planner`, `/weather`, `/my-trips`, and `/admin` through Nginx.
