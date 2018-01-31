#!/bin/bash

set -e  # Bail if something fails

LOOPBACK_IP=
while getopts l:h OPT; do
    case "$OPT" in
	l)
	    LOOPBACK_IP="$OPTARG"
	    ;;
	h)
	    echo "$0 [-l LOOPBACK_IP]"
	    exit 0
	    ;;
	[?])
	    exit 1
	    ;;
    esac
done

# This script tries to create a .env file in the current working directory for 
# use with docker-compose. The file contains variables that include info on the 
# user's docker setup like the IP address of the Host and VM.

ENV_FILE_PATH="$PWD/.env"

# TODO: Don't overwrite file if it already exists?

# Relative path that contains this script
SCRIPT_PATH=${BASH_SOURCE%/*}

if [ -z "$LOOPBACK_IP" ] ; then
    # Create an alias for the loopback adapter so that the Mac and Docker VM can communicate using that IP
    LOOPBACK_IP='10.0.75.1'
    if [ `uname` = "Darwin" ] ; then
	LOOPBACK_NAME=lo0
    else
	LOOPBACK_NAME=lo
    fi
    echo "We need to create an alias for the loopback adapter ($LOOPBACK_NAME) using sudo"
    echo 'so your OS and the Docker VM can communicate. It will be created using'
    echo "IP $LOOPBACK_IP. You will be prompted for your password."
    if [ `uname` = "Darwin" ] ; then
	sudo ifconfig $LOOPBACK_NAME alias $LOOPBACK_IP
    else
	sudo ifconfig ${LOOPBACK_NAME}:0 $LOOPBACK_IP/24
    fi
fi
export LOOPBACK_IP


# Should use compose file relative to this script, followed by a compose file relative to the
# working directory (i.e. where the .env file is going to be created)
COMPOSE_FILE="$SCRIPT_PATH/docker-compose.yaml:./docker-compose.yaml"
COMPOSE_PROJECT_NAME='killrvideo'

# Get other variables from the get-environment.sh script
GET_ENV_OUTPUT=$(exec $SCRIPT_PATH/get-environment.sh)

# Write to .env file in current working directory
echo "COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT_NAME
COMPOSE_FILE=$COMPOSE_FILE
$GET_ENV_OUTPUT" > $ENV_FILE_PATH
