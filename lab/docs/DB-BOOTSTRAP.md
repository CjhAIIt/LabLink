# DB Bootstrap

## Purpose

The database delivery is now split into three clear paths:

- `src/main/resources/db/migration`
  Incremental Flyway migrations. These files are now self-contained for fresh schema creation and must be treated as frozen once applied.
- `src/main/resources/db/bootstrap`
  Generated SQL bundles for clean provisioning and manual recovery.
- `src/main/resources/init.sql`
  Legacy docker bootstrap script for local demo data. It is no longer the authoritative schema source for Flyway.

## Generate bootstrap SQL

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-phase2-bootstrap.ps1
```

Generated files:

- `src/main/resources/db/bootstrap/phase2-wave1-bootstrap.sql`
  Fresh database bootstrap: `V1-V9`
- `src/main/resources/db/bootstrap/phase2-wave1-migrations-only.sql`
  Migration-only bundle: `V1-V9`

## Which file to use

- Use `phase2-wave1-bootstrap.sql` when the database is empty and you want one SQL file that creates the full schema and restores all required foreign keys in order.
- Use `phase2-wave1-migrations-only.sql` when you want the Flyway migration content in one ordered file.
- Keep Flyway as the normal runtime path for local/dev/prod upgrades.

## Repair existing local databases

If an old local database already ran earlier copies of `V3`, `V4`, or `V5`, you can repair checksum history with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/repair-phase2-flyway.ps1
```

Default target:

- `jdbc:mysql://localhost:3307/lab_recruitment`

You can override connection info:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/repair-phase2-flyway.ps1 `
  -DbUrl "jdbc:mysql://localhost:3306/lab_recruitment" `
  -DbUser "root" `
  -DbPassword "your-password"
```

## Verified startup path

The currently verified local startup path is:

1. `docker compose --env-file .env.local -f docker-compose.local.yml up -d mysql redis rabbitmq`
2. if needed, `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/repair-phase2-flyway.ps1`
3. start backend with the IntelliJ run config:
   `LabRecruitmentApplication (Local Middleware)`

On this path:

- Flyway `V1` to `V9` validates and migrates successfully
- backend starts on `8081`
- Redis and RabbitMQ both connect successfully

## Important note

Do not keep editing already-applied versioned migrations. If schema adjustments are needed after a migration has been shared, add a new migration version instead of rewriting the old one.
