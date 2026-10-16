#!/bin/bash
# Tells the History Server its public address. In Codespaces, port 18080 is reached through
# https://<codespace>-18080.<domain>, while the server sees the request as localhost:18080 and
# would redirect the browser there. docker-compose.yml restarts the server with this URL as
# spark.ui.proxyRedirectUri. Outside Codespaces the file stays empty and nothing changes.
if [ -n "${CODESPACE_NAME:-}" ]; then
    echo "https://${CODESPACE_NAME}-18080.${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN:-app.github.dev}"
fi > /data/spark-events/history-url
