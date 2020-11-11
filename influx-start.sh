#!/bin/bash

TOKEN=my-token
ORG=my-org
BUCKET=my-bucket
USER=user
PASS=password

influxd run &
sleep 12 # wait for influxd to start
influx setup -f -b $BUCKET -o $ORG -u $USER -p $PASS -t $TOKEN
influx auth create -o $ORG -u $USER --read-buckets --write-buckets --read-tasks --write-tasks --read-user -t $TOKEN
echo "Finished influx setup"
sleep infinity