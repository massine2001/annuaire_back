#!/usr/bin/env bash
set -euo pipefail

# --- SFTP PRIVATE KEY MATERIALIZATION ---

if [ -z "${SFTP_PRIVATE_KEY_B64:-}" ]; then
  echo "SFTP_PRIVATE_KEY_B64 manquant"; exit 1
fi

install -d -m 700 /opt/keys
echo "$SFTP_PRIVATE_KEY_B64" | base64 -d > /opt/keys/sftp_id_rsa
chmod 600 /opt/keys/sftp_id_rsa
export SFTP_PRIVATE_KEY_PATH=/opt/keys/sftp_id_rsa

# known_hosts (sécurité MITM)
if [ -n "${SFTP_KNOWN_HOSTS_HOST:-}" ]; then
  : "${SFTP_KNOWN_HOSTS_PORT:=22}"
  ssh-keyscan -p "$SFTP_KNOWN_HOSTS_PORT" "$SFTP_KNOWN_HOSTS_HOST" > /opt/keys/known_hosts 2>/dev/null || true
  chmod 644 /opt/keys/known_hosts
  export SFTP_KNOWN_HOSTS_PATH=/opt/keys/known_hosts
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
