#!/bin/bash

set -o errexit

docker-compose kill
docker-compose rm -v -f
