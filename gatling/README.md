# Load testing tool for the API

This is a subproject inside the Community Payback API. Attempts to fold this into the main repo are non-trivial due to dependency concerns.

## How to use this tool

You can either let Gatling automatically fetch a JWT using client credentials, let Gradle fetch client creds from Kubernetes for you, or provide a token yourself.

### One-step Gradle task (kubectl + Gatling)

If you have kubectl access to the cluster and jq installed, you can let Gradle fetch CLIENT_ID/CLIENT_SECRET from the hmpps-community-payback-<env> namespace and then run Gatling:

#### Target a single 

```bash
./gradlew gatlingRunWithK8sCreds -Penv=test -PsimulationFqn=uk.gov.justice.digital.hmpps.communitypayback.simulations.api.HealthSimulation
```

#### Target a story

```shell
./gradlew gatlingRunWithK8sCreds -PsimulationFqn=uk.gov.justice.digital.hmpps.communitypayback.simulations.ui.E2E
```

#### Target all simulations

```shell
./gradlew gatlingRunWithK8sCreds
```

Notes:
- Requires kubectl and jq on your PATH.
- Uses namespace hmpps-community-payback-<env> and secret hmpps-community-payback-ui-client-creds.

## Recreate an E2E test

 * start cp-stack
 * clear existing logs
 * run e2e tests in th ui package
 * grab the logs in to a file (e2e.log)
 * `grep "CommunityPaybackRequestLoggingFilter : Request data" e2e.log > e2e-request-data.log` > e2e-requests.log
 * Ask Junie to update the script: "Using e2e-request-data.log as a source, can you update the E2E simulation to execute those requests?"