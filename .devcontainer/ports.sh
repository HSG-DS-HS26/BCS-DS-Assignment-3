#!/bin/bash
# Codespaces forwards only ports that listen inside the dev container, so the web UIs of the
# other Compose services are relayed here. Locally, docker-compose.yml publishes them instead.
[ -n "${CODESPACE_NAME:-}" ] || exit 0
for target in spark-master:8080 history-server:18080 namenode:9870; do
    nohup setsid socat TCP-LISTEN:${target#*:},fork,reuseaddr TCP:$target >/dev/null 2>&1 &
done
