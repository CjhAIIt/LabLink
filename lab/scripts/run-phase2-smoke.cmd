@echo off
setlocal

if not exist logs mkdir logs

set "DB_URL=jdbc:mysql://localhost:3307/lab_recruitment?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%%2B8&allowPublicKeyRetrieval=true"
set "DB_USERNAME=root"
set "DB_PASSWORD=LabLinkLocalRoot!2026"
set "JWT_SECRET=LabLinkLocalJwtSecretForPhase2Wave1!2026"
set "LABLINK_CACHE_REDIS_ENABLED=true"
set "LABLINK_MQ_RABBIT_ENABLED=true"
set "REDIS_HOST=localhost"
set "REDIS_PORT=6379"
set "RABBITMQ_HOST=localhost"
set "RABBITMQ_PORT=5672"
set "RABBITMQ_USERNAME=guest"
set "RABBITMQ_PASSWORD=guest"
set "RABBITMQ_VHOST=/"
set "SERVER_PORT=8081"

call mvn spring-boot:run >> logs\phase2-smoke.out.log 2>> logs\phase2-smoke.err.log
