# Load testing tool for the API

This is a subproject inside the Community Payback API. Attempts to fold this into the main repo are non-trivial due to dependency concerns.

## How to use this tool

This tool is designed to run in github CI via an [action](https://github.com/ministryofjustice/hmpps-community-payback-api/actions/workflows/gatling_run.yml). The action can be manually triggered via workflow dispatch. The following settings are available for the workflow:

 * Which API should this run against
 * Optional fully-qualified simulation class to run. Use a class name here, e.g. uk.gov.justice.digital.hmpps.communitypayback.simulations.api.HealthSimulation. If omitted, all simulations will be run.
 * How long to wait before starting the onslaught
 * Number of initial concurrent users
 * How many users to increase to over time
 * Number of seconds over which to increase the user count
 * The number of concurrent users to keep active per second
 * The time period in seconds over which peak load should be applied

## Running locally

There is a help script in the tools folder. [run_loadtest.sh](../tools/run_loadtest.sh). It needs kubectl and jq to be unstalled and configured to point at the target cluster. Run this from the root of the project.

Run it with `-h` to see the options:

```shell
$ ./tools/scripts/run_loadtest.sh -h
```

An example run command is:

```shell
./tools/scripts/run_loadtest.sh \
    --env dev \
    --nothing-for 5 \
    --at-once-users 1 \
    --ramp-users 0 \
    --ramp-users-during 0 \
    --constant-users-per-sec 0 \
    --constant-users-per-sec-during 0
```

Run againast the dev environment, starting with 5 users, increasing by 20 users over 50 seconds, then maintaining 60 users for 20 seconds, with a 30 second ramp down.

## Recreate an E2E test

* start cp-stack
* clear existing logs
* run e2e tests in th ui package
* grab the logs in to a file (e2e.log)
* `grep "CommunityPaybackRequestLoggingFilter : Request data" e2e.log > e2e-request-data.log` > e2e-requests.log
* Use that new log to create a gatling feeder or modify an existing one.