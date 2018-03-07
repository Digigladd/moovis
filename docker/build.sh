#!/bin/bash

docker build --tag "moovis/grafana" --no-cache=true - < grafana/Dockerfile