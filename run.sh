#!/bin/bash
. ./.env
export KILLRVIDEO_DOCKER_IP
export KILLRVIDEO_HOST_IP
# use everything under ~/.m2 ... good luck
java -cp target/classes:`cat classpath-jars| sed "s@^@$HOME/@g"| tr '\n' ':'` killrvideo.KillrVideoServer |& tee killrvideo.out
