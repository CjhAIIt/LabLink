# IntelliJ Local Run

## 1. Why your current run fails

The default backend startup in IntelliJ uses the fallback datasource from [application.yml](/C:/Users/cjh/IdeaProjects/labLink/lab/src/main/resources/application.yml):

- `jdbc:mysql://localhost:3306/lab_recruitment`

Your pasted log shows that this local `3306` database already contains a failed Flyway record:

- `Detected failed migration to version 4 (audit notification center)`

That is a stale local database state, not a current code compile issue.

## 2. Preferred way to run locally

Use the checked-in IntelliJ run configuration:

- [.run/LabRecruitmentApplication (Local Middleware).run.xml](/C:/Users/cjh/IdeaProjects/labLink/lab/.run/LabRecruitmentApplication%20%28Local%20Middleware%29.run.xml)

This configuration points the backend to the local middleware stack:

- MySQL: `localhost:3307`
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672`

Before starting it, run:

```powershell
docker compose --env-file .env.local -f docker-compose.local.yml up -d mysql redis rabbitmq
```

Reference:

- [.env.local](/C:/Users/cjh/IdeaProjects/labLink/lab/.env.local)
- [LOCAL-MIDDLEWARE.md](/C:/Users/cjh/IdeaProjects/labLink/lab/docs/LOCAL-MIDDLEWARE.md)

## 3. If you want to keep using localhost:3306

You need to repair or recreate your old `lab_recruitment` schema first.

### Option A: fastest clean reset

Drop and recreate `lab_recruitment`, then start the backend again and let Flyway replay from `V1` to `V8`.

### Option B: keep existing data

Run Flyway repair against your existing local database:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/repair-phase2-flyway.ps1 `
  -DbUrl "jdbc:mysql://localhost:3306/lab_recruitment" `
  -DbUser "root" `
  -DbPassword "your-password"
```

Then restart the backend.

## 4. Current verified state

On the local middleware stack at `3307`, the following has already been verified:

- Flyway migrations `V1` to `V8` all succeed
- backend starts successfully
- Redis connects successfully
- RabbitMQ connects successfully
- profile submit/approve workflow works
- audit log and notification async persistence work
