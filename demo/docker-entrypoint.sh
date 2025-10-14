#!/usr/bin/env bash
set -euo pipefail


if [[ -n "${SSH_KEY_BASE64:-}" ]]; then
  echo "🔑 Decoding SSH key..."
  echo "$SSH_KEY_BASE64" | base64 -d > /tmp/ssh_key_db
  chmod 600 /tmp/ssh_key_db
  echo "✅ SSH key ready"
else
  echo "❌ SSH_KEY_BASE64 not found!"
  exit 1
fi

ssh -i /tmp/ssh_key_db \
    -o StrictHostKeyChecking=accept-new \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -o ExitOnForwardFailure=yes \
    -f -N -p "${SSH_PORT}" \
    -L "127.0.0.1:${LOCAL_TUNNEL_PORT}:${SSH_REMOTE_HOST}:${SSH_REMOTE_PORT}" \
    "${SSH_USER}@${SSH_HOST}"

for i in {1..15}; do
  if nc -z 127.0.0.1 "${LOCAL_TUNNEL_PORT}"; then
    echo "✅ Tunnel ready!"
    break
  fi
  sleep 1
done

if ! nc -z 127.0.0.1 "${LOCAL_TUNNEL_PORT}"; then
  echo "❌ SSH tunnel failed to start!"
  exit 1
fi

echo "Starting Spring Boot application..."
exec java ${JAVA_OPTS:-} -jar /app/app.jar
