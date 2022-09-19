#!/bin/bash

set -o errexit

ROOT="${ROOT:-`pwd`}"
PROJECT_DIR="${PROJECT_DIR:-`pwd`}"
echo "ROOT dir [${ROOT}]"

my_ip="$( "${ROOT}"/scripts/whats_my_ip.sh )"

echo "Replacing files with your ip [${my_ip}]"

rm -rf "${PROJECT_DIR}"/build/docker/
mkdir -p "${PROJECT_DIR}"/build/docker/config/grafana/provisioning/datasources/
cp "${PROJECT_DIR}"/docker/config/grafana/provisioning/datasources/datasource.yml "${PROJECT_DIR}"/build/docker/config/grafana/provisioning/datasources/datasource.yml
sed -i -e "s/host.docker.internal/$my_ip/g" "${PROJECT_DIR}"/build/docker/config/grafana/provisioning/datasources/datasource.yml
mkdir -p "${PROJECT_DIR}"/build/docker/config/prometheus/
cp "${PROJECT_DIR}"/docker/config/prometheus/prometheus.yml "${PROJECT_DIR}"/build/docker/config/prometheus/prometheus.yml
sed -i -e "s/host.docker.internal/$my_ip/g" "${PROJECT_DIR}"/build/docker/config/prometheus/prometheus.yml
mkdir -p "${PROJECT_DIR}"/build/docker/config/grafana/provisioning/dashboards/
cp "${PROJECT_DIR}"/docker/config/grafana/provisioning/dashboards/* "${PROJECT_DIR}"/build/docker/config/grafana/provisioning/dashboards/
