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

# --- ASSERT VARS TUNNEL ---
: "${SSH_KEY_BASE64:?missing}"
: "${SSH_HOST:?missing}"
: "${SSH_USER:?missing}"
: "${SSH_PORT:?missing}"
: "${SSH_REMOTE_HOST:?missing}"
: "${SSH_REMOTE_PORT:?missing}"
: "${LOCAL_TUNNEL_PORT:?missing}"
# --- END ASSERT ---

# --- TUNNEL KNOWN_HOSTS ---
mkdir -p /home/spring/.ssh
chmod 700 /home/spring/.ssh
ssh-keyscan -p "${SSH_PORT}" "${SSH_HOST}" > /home/spring/.ssh/known_hosts 2>/dev/null
chmod 644 /home/spring/.ssh/known_hosts
# --- END ---

# --- DECODE SSH KEY FOR TUNNEL ---
echo "$SSH_KEY_BASE64" | base64 -d > /tmp/ssh_key_db
chmod 600 /tmp/ssh_key_db
# --- END ---

# --- START TUNNEL (STRICT) ---
ssh -i /tmp/ssh_key_db \
    -o UserKnownHostsFile=/home/spring/.ssh/known_hosts \
    -o StrictHostKeyChecking=yes \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -o ExitOnForwardFailure=yes \
    -NT -p "${SSH_PORT}" \
    -L "127.0.0.1:${LOCAL_TUNNEL_PORT}:${SSH_REMOTE_HOST}:${SSH_REMOTE_PORT}" \
    "${SSH_USER}@${SSH_HOST}" &
TUNNEL_PID=$!
trap 'kill -TERM "$TUNNEL_PID" 2>/dev/null || true; wait "$TUNNEL_PID" 2>/dev/null || true' EXIT INT TERM

for i in {1..20}; do
  nc -z 127.0.0.1 "${LOCAL_TUNNEL_PORT}" && break
  sleep 1
done
nc -z 127.0.0.1 "${LOCAL_TUNNEL_PORT}" || { echo "SSH tunnel failed to start"; exit 1; }

exec java ${JAVA_OPTS:-} -jar /app/app.jar
