#!/bin/bash

set -o errexit

export ROOT

pushd ..
  ROOT="$( pwd )"
popd

../scripts/setup.sh

docker-compose up
