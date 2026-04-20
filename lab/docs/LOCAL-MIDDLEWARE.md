# Local Middleware

This project can now run Redis and RabbitMQ in the existing local Docker stack.

## 1. Prepare env

```powershell
Copy-Item .env.local.example .env.local
```

If you already have a local `.env.local`, update at least these values:

- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- `LABLINK_CACHE_REDIS_ENABLED=true`
- `LABLINK_MQ_RABBIT_ENABLED=true`

## 2. Start local services

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml up -d mysql redis rabbitmq
```

If you want the full local stack:

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml up -d
```

## 3. Service endpoints

- MySQL: `localhost:3307`
- Redis: `localhost:6379`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Console: `http://localhost:15672`
- Backend: `http://localhost:8081`
- Frontend: `http://localhost`

## 4. Local backend without Docker

You can also start only middleware with Docker and run backend/frontend from source:

```powershell
$env:LABLINK_CACHE_REDIS_ENABLED='true'
$env:LABLINK_MQ_RABBIT_ENABLED='true'
$env:REDIS_HOST='localhost'
$env:RABBITMQ_HOST='localhost'
mvn spring-boot:run
```

## 5. Flyway repair for old local databases

If your local database has stale checksum history from earlier edits to versioned SQL files, repair it before starting the backend:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/repair-phase2-flyway.ps1
```

To repair the default local MySQL on `3306` instead:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/repair-phase2-flyway.ps1 `
  -DbUrl "jdbc:mysql://localhost:3306/lab_recruitment" `
  -DbUser "root" `
  -DbPassword "your-password"
```

## 6. Notes

- When Redis is disabled, the project falls back to local in-memory cache and lock handling.
- When RabbitMQ is disabled, the project falls back to local event publishing.
- Attendance sign-in idempotency lock uses key pattern `attendance:sign:lock:{sessionId}:{userId}`.
- Lab detail cache uses key pattern `labDetailCache::{labId}` with a 30 minute TTL.
