#!/usr/bin/env bash

BASEDIR=$(dirname "$0")
cd "$BASEDIR"/..

docker-compose -f docker-compose-for-front.yml down
docker-compose -f docker-compose-for-front.yml build --no-cache
docker rmi $(docker images --filter "dangling=true" -q --no-trunc)
docker-compose -f docker-compose-for-front.yml up -d

