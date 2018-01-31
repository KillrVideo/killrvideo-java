#!/bin/bash
. ./.env
export KILLRVIDEO_DOCKER_IP
export KILLRVIDEO_HOST_IP
# use everything under ~/.m2 ... good luck
java -cp target/classes:`find /home/bsmith/.m2/ -name \*.jar|grep -v guava-10| grep -v guava-18|tr '\n' ':'` killrvideo.KillrVideoServer |& tee killrvideo.out
