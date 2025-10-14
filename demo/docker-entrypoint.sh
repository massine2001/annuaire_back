#!/usr/bin/env bash
set -euo pipefail

# --- SFTP PRIVATE KEY MATERIALIZATION ---
: "${SFTP_PRIVATE_KEY_B64:?missing}"

KEY_DIR="${SFTP_KEY_DIR:-/home/spring/.keys}"
mkdir -p "$KEY_DIR"
chmod 700 "$KEY_DIR"

echo "$SFTP_PRIVATE_KEY_B64" | base64 -d > "$KEY_DIR/sftp_id_rsa"
chmod 600 "$KEY_DIR/sftp_id_rsa"
export SFTP_PRIVATE_KEY_PATH="$KEY_DIR/sftp_id_rsa"

if [ -n "${SFTP_KNOWN_HOSTS_HOST:-}" ]; then
  : "${SFTP_KNOWN_HOSTS_PORT:=5022}"
  ssh-keyscan -p "$SFTP_KNOWN_HOSTS_PORT" "$SFTP_KNOWN_HOSTS_HOST" > "$KEY_DIR/known_hosts" 2>/dev/null || true
  chmod 644 "$KEY_DIR/known_hosts"
  export SFTP_KNOWN_HOSTS_PATH="$KEY_DIR/known_hosts"
fi
# --- END SFTP KEY ---


if [[ -n "${SSH_KEY_BASE64:-}" ]]; then
  echo "$SSH_KEY_BASE64" | base64 -d > /tmp/ssh_key_db
  chmod 600 /tmp/ssh_key_db
else
  exit 1
fi

ssh -i /tmp/ssh_key_db \
    -o StrictHostKeyChecking=yes \
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
  exit 1
fi

echo "Starting Spring Boot application..."
exec java ${JAVA_OPTS:-} -jar /app/app.jar
