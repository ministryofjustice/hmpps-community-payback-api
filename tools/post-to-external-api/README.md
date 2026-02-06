# External API - Manual testing

This README and the files in this folder cover how to send messages to the [HMPPS External API](https://github.com/ministryofjustice/hmpps-integration-api).

You will need the following files to run the commands in this directory:
- `dev-community-payback-dev-client.pem`: The client certificate for authentication.
- `dev_private_key.pem`: The private key corresponding to the client certificate.
- `dev-community-payback-dev-api-key`: A file containing the API key for authentication.

These are in the MoJ community payback 1pass vault under "External API *". Save these files in the same directory as this README to run the commands below.

## Course completion

Send a course completion (ete) message to the external API and get it onto the queue.

```shell
curl \
    -q \
    -X POST \
    --cert dev-community-payback-dev-client.pem \
    --key dev_private_key.pem \
    -H "x-api-key: $(cat ./dev-community-payback-dev-api-key)" \
    -H "Content-Type: application/json" \
    -d @ete_course_completion_payload_01.json \
    https://dev.integration-api.hmpps.service.justice.gov.uk/v1/education/course-completion | jq "." 
```

## Test external API connectivity

```shell
API_KEY="$(cat ./dev-community-payback-dev-api-key)"
curl \
    --cert dev-community-payback-dev-client.pem \
    --key dev_private_key.pem \
    -H "x-api-key: $API_KEY" \
    https://dev.integration-api.hmpps.service.justice.gov.uk/v1/status
```
