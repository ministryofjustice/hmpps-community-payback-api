# Community Payback Stack

## Overview

The community payback stack (cp-stack) provides a script to simplify running the Community Payback UI and API projects locally

The tool manages local instances of the following:

* Community Payback API - Either via docker or gradle
* Community Payback UI - Either via docker or node (to run local code)
* Community Payback Supervisor UI - Either via docker or node (to run local code)
* Postgres
* Redis
* Localstack
* Wiremock - Proxies all requests to upstream services, allowing us to selectively intercept and mock responses

All upstream services are provided by the [Cloud Platform's](https://user-guide.cloud-platform.service.justice.gov.uk/) 'Dev' environment, proxied via Wiremock (excluding hmpps-auth, which is not proxied)

## Pre-reqs

* [brew](https://brew.sh/)
* docker desktop (for docker compose)
* kubectl installed and configured to use the [cloud platform](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html#connecting-to-the-cloud-platform-39-s-kubernetes-cluster)

Other pre-reqs will be installed automatically via brew

If you want to run locally checked out API code via gradle, see pre-reqs in the [API project README](../../README.md)

If you want to run locally checked out UI code via npm, see pre-reqs in the [UI project README](https://github.com/ministryofjustice/hmpps-community-payback-ui/blob/main/README.md)

## Running

Add the following to your profile (~/.zshrc or ~/.bashrc), changing the paths to match your local setup:

```shell
export LOCAL_CP_API_PATH=$HOME/Desktop/hmpps/hmpps-community-payback-api
export LOCAL_CP_UI_PATH=$HOME/hmpps/hmpps-community-payback-ui
export LOCAL_CP_SUI_PATH=$HOME/hmpps/hmpps-community-payback-supervisors-ui
export PATH="$PATH:$LOCAL_CP_API_PATH/tools/cp-stack/bin"
```

Then either open a new terminal or run:

```shell
source ~/.zshrc # or source ~/.bashrc
```

You can then start the stack using

``cp-stack start [--local-api] [--local-ui] [--local-sui]``

If using the 'local' options, the UIs and/or API will be run directly from your checked out project. Otherwise, the latest docker image will be pulled and ran instead

To stop the stack, use

``cp-stack stop [--clear-databases]``

The API database will be retained over stop/starts of the stack, unless `--clear-databases` is specified when stopping the stack

## Service Ports

| Tool                             | Port |
|----------------------------------|------|
| Community Payback API            | 8080 |
| Community Payback UI             | 3000 |
| Community Payback Supervisors UI | 3001 |
| Wiremock                         | 9004 |

## Configuration

Any overrides to the default configuration is defined via environment variables defined in [.env.api.template](.env.api.template), [.env.ui.template](.env.ui.template) and [.env.sui.template](.env.sui.template). These are copied into .env.api, .env.sui and .env.ui files on startup, with any variables in them resolved from k8s secrets

To resolve a secret from k8s use the ${SECRET_KEY} notation. You may also need to update the [start-server script](bin/start-server) to add the secret name if it's not already being resolved

## Using a locally built docker image

Build image locally:

- `docker build -t hmpps-community-payback-api:local .`

Then update compose.yml to refer to this image instead

## Wiremock

All upstream requests (excluding auth) are proxied via wiremock.

We can selectively mock a request by adding a mapping to the `mappings/overrides` folder with a priority < 100

### Viewing Requests & Responses

Wiremock provides an API to view requests:

```curl http://localhost:9004/__admin/requests```

Combine with jq to make it more usable:

```curl http://localhost:9004/__admin/requests | jq '.requests[].request.url'```

Note that newest requests are shown at the top of the list

## Localstack

We start localstack to provide us with an SNS topic to send domain events to

### Publishing Community Campus Messages

```
brew install awscli-local
AWS_DEFAULT_REGION=eu-west-2
# list queues (if running integration tests there may be many)
awslocal sqs list-queues 
awslocal sqs send-message --queue-url http://sqs.eu-west-2.localhost.localstack.cloud:4566/000000000000/cp_stack_cc_course_completion --message-body '{"communityCampusId":"123","person":{"crn":"CRN01","firstName":"first","lastName":"last","dateOfBirth":"2018-04-21","region":"midlands","email":"someone@test.com"},"course":{"courseName":"the course","source":"F6I0f","enrollmentDateTime":"2025-11-10T14:19:37.401581","completionDateTime":"2025-11-15T14:19:37.401604","status":"Completed","totalTime":"07:06","attempts":1,"expectedMinutes":0,"expectedMinutesAdditional":3}}'
```

### Listening for Domain Events

We have configured an SQS queue that listens to the domain event topic so we can debug messages being sent

This can be monitored as follows:

```bash
brew install awscli-local
AWS_DEFAULT_REGION=eu-west-2
# list topics (if running integration tests there may be many)
awslocal sns list-topics
# list topic subscriber (if running integration tests there may be many)
awslocal sqs list-queues 
# show domain events sent to the cp-stack API instance. Note! this will typically be empty because the API domain event listener
# will have consumed the messages. Can disable that by setting `community-payback.scheduling.enabled` to false
awslocal sqs receive-message --max-number-of-messages 10 --visibility-timeout 0 --queue-url http://sqs.eu-west-2.localhost.localstack.cloud:4566/000000000000/cp_stack_domain_event_subscriber
```

## Debugging the API

Note : this only works when using the `--local-api` option

To debug the locally running API, do the following in Intellij:

1. Click 'Run' -> 'Edit Configurations...'
2. Click on the '+' symbol at the top left
3. Choose 'Remote JVM Debug'
4. In the Run Configuration set the following:
    1. Name can be whatever you like e.g. 'Remote API'
    2. Host and port should be localhost:32323
    3. Module classpath should be 'hmpps-community-payback-api'
5. After starting the API locally, run the new Configuration using 'Run' -> 'Run...'

If successful, the Debug Tool Window should appear saying "Connected to the target VM, address: 'localhost:32323', transport: 'socket'".
