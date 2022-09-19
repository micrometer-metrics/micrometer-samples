#!/bin/bash

set -o errexit

ROOT="${ROOT:-`pwd`}"

echo "Sets up docker plugins"
docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions || echo "Already installed"

echo "Sets up IPs for datasources"
"${ROOT}"/scripts/replace_ip.sh
