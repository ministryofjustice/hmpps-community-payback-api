#!/bin/bash

# Set environment
env=dev

echo "Getting client credentials from Kubernetes..."
CREDENTIALS=$(kubectl -n hmpps-community-payback-$env get secrets hmpps-community-payback-ui-client-creds -o json | jq ".data | map_values(@base64d)")

# Extract client ID and secret from the credentials
CLIENT_ID=$(echo "$CREDENTIALS" | jq -r '.CLIENT_CREDS_CLIENT_ID')
CLIENT_SECRET=$(echo "$CREDENTIALS" | jq -r '.CLIENT_CREDS_CLIENT_SECRET')

echo "Client ID: $CLIENT_ID"

if [ -z "$CLIENT_ID" ] || [ -z "$CLIENT_SECRET" ]; then
    echo "Error: Failed to extract client credentials"
    exit 1
fi

echo "Getting access token..."
TOKEN=$(curl -s -X "POST" "https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token?grant_type=client_credentials" \
   -H 'Content-Type: application/json' \
   -u "$CLIENT_ID:$CLIENT_SECRET" | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "Error: Failed to get access token"
    exit 1
fi

echo "Token obtained successfully!"
echo
echo "$TOKEN"
echo
echo "Making API request..."
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/references/project-types > /dev/null
if [ $? ]; then
  echo "✅ Token is good"
else
  echo "❌ There was a problem, use set -x to check what is wrong."
fi
