#!/bin/bash
#
# Start the killrvideo business logic app

. ./.env
export KILLRVIDEO_DOCKER_IP
export KILLRVIDEO_HOST_IP
java -cp target/classes:`cat classpath-jars| grep -v ^# | sed "s@^@$HOME/@g"| tr '\n' ':'` killrvideo.KillrVideoServer |& tee killrvideo.out
