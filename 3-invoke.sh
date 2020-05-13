#!/bin/bash
set -eo pipefail

while true; do
  aws lambda invoke --function-name $FUNCTION --payload file://event.json out.json
  cat out.json
  echo ""
  sleep 2
done
