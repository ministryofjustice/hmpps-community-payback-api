#!/usr/bin/env bash

##### Overview
#
# Synchronises secrets stored in Kubernetes with github secrets
#
# Pre-requisites
#
# 1. Install the github cli (brew install gh) and authenticate
#
# Base 64 handling
#
# Values in k8s secrets are base64 encoded. By default these values will be decoded
# before being written to github. The base64 encoding will be retained if the secret
# key ends with 'B64'

set -e
set -o pipefail

K8S_NAMESPACE=hmpps-community-payback-dev
K8S_SECRET=hmpps-community-payback-e2e-config

sync_to_github() {
  key=$1
  value=$2

  echo "Syncing github for ${key}"
  echo ""

  gh secret set --repo ministryofjustice/hmpps-community-payback-api "${key}" --body "$value"
  gh secret set --repo ministryofjustice/hmpps-community-payback-ui "${key}" --body "$value"
  gh secret set --repo ministryofjustice/hmpps-community-payback-supervisors-ui "${key}" --body "$value"
}

secrets_json=$(kubectl get secrets $K8S_SECRET --namespace $K8S_NAMESPACE -o json | jq ".data")

secret_keys=$(echo "$secrets_json" | jq -r "to_entries[] | .key")
for key in $secret_keys; do
  value=$(echo "$secrets_json" | jq -r "to_entries[] | select(.key == \"""${key}""\") | .value")

  if [[ "$key" == *B64 ]]
  then
    sync_to_github "$key" $value
  else
    decoded_value=$(echo $value | base64 -d)
    sync_to_github "$key" $decoded_value
  fi

done
