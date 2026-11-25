#!/usr/bin/env bash
# 서버에서 실행: 저장소 루트에서 `bash deploy/deploy.sh`
# 최초 1회 /etc/booking.env 가 없으면 DB_PASSWORD, ADMIN_PASSWORD 환경변수로 만든다.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f /etc/booking.env ]; then
  : "${DB_PASSWORD:?DB_PASSWORD 필요 (~/README-server.txt 참고)}"
  : "${ADMIN_PASSWORD:?ADMIN_PASSWORD 필요}"
  sudo tee /etc/booking.env >/dev/null <<ENV
DB_URL=jdbc:mysql://127.0.0.1:3306/app?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=app
DB_PASSWORD=${DB_PASSWORD}
ADMIN_USERNAME=${ADMIN_USERNAME:-admin}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
ENV
  sudo chmod 600 /etc/booking.env
fi

echo "==> backend build"
(cd backend && chmod +x mvnw && ./mvnw -q -B -DskipTests package)
sudo mkdir -p /opt/booking
sudo cp backend/target/booking-0.0.1.jar /opt/booking/booking.jar

echo "==> frontend build"
(cd frontend && npm ci --no-audit --no-fund && npx next build)
sudo mkdir -p /var/www/booking
sudo rm -rf /var/www/booking/*
sudo cp -r frontend/out/. /var/www/booking/

echo "==> services"
sudo cp deploy/booking.service /etc/systemd/system/booking.service
sudo systemctl daemon-reload
sudo systemctl enable --now booking
sudo systemctl restart booking
sudo cp deploy/nginx.conf /etc/nginx/sites-available/booking
sudo ln -sf /etc/nginx/sites-available/booking /etc/nginx/sites-enabled/booking
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

echo "==> health"
for i in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:8080/api/slots?date=$(TZ=Asia/Seoul date +%F)" >/dev/null; then echo "API OK"; break; fi
  sleep 2
done
