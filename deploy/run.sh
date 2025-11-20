#!/bin/bash

if [ "$CONTAINER_TIMEZONE" = "" ]
then
	echo "Using Europe/Copenhagen timezone"
	CONTAINER_TIMEZONE="Europe/Copenhagen"
fi

TZFILE="/usr/share/zoneinfo/$CONTAINER_TIMEZONE"
if [ ! -e "$TZFILE" ]
then
	echo "requested timezone $CONTAINER_TIMEZONE doesn't exist"
else
	cp /usr/share/zoneinfo/$CONTAINER_TIMEZONE /etc/localtime
	echo "$CONTAINER_TIMEZONE" > /etc/timezone
	echo "using timezone $CONTAINER_TIMEZONE"
fi

java -Dloader.path=config/* -Xmx256m -jar medcom-mailbox-1.0.0.jar
