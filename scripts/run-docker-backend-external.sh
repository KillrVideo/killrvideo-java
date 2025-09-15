#!/bin/bash

# setting environment variable to the IP address of the host
# does not ALWAYS WORK...
# export KILLRVIDEO_BACKEND=`ipconfig getifaddr en0`
#export KILLRVIDEO_BACKEND='docker run --rm alpine nslookup host.docker.internal > ip.log 2>/dev/null && KILLRVIDEO_BACKEND=`tail -1 ip.log | cut -b 12-30` && rm ip.log && echo "IP for Backend is set to : ${KILLRVIDEO_BACKEND}"'
#export KILLRVIDEO_BACKEND=`docker run --rm alpine nslookup host.docker.internal 2>/dev/null | cut -b 12-28 | tail -1`
export KILLRVIDEO_BACKEND=`ipconfig getifaddr en0`

# the compose file swaps in the value of `KILLRVIDEO_BACKEND` in several places
docker-compose -p killrvideo-java -f docker-compose-backend-external.yaml up -d
 