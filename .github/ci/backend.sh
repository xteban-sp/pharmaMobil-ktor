#!/usr/bin/env bash
# Sesion 10: (re)arranca PharmaSoft con el perfil h2 en localhost:8080.
# Uso: backend.sh [minutos_de_vida_del_token]   (por defecto 60, como el backend)
# La flujo de expiracion lo vuelve a levantar con 1 minuto para provocar el 401
# real del servidor sin esperar una hora.
set -u
MINUTOS="${1:-60}"
DIR="${GITHUB_WORKSPACE:-$PWD}/backend"
LOG="${EVID:-/tmp}/backend-token-${MINUTOS}min.log"

pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "PharmaBackendApplication" 2>/dev/null || true
for i in $(seq 1 20); do curl -sf http://localhost:8080/api/health >/dev/null || break; sleep 1; done

cd "$DIR"
nohup ./mvnw -q -DskipTests spring-boot:run -Dspring-boot.run.profiles=h2 \
  "-Dspring-boot.run.arguments=--app.jwt.expiracion-minutos=${MINUTOS}" > "$LOG" 2>&1 &

for i in $(seq 1 120); do
  if curl -sf http://localhost:8080/api/health >/dev/null; then
    echo "PharmaSoft listo (tokens de ${MINUTOS} min)"
    exit 0
  fi
  sleep 2
done
echo "PharmaSoft no arranco"; tail -40 "$LOG"; exit 1
