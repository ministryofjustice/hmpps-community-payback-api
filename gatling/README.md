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

## Runtime configuration

At run time we can pass a number of parameters to control the load test. These are passed as Gradle properties.

e.g. 

```shell
./gradlew gatlingRunWithK8sCreds -PnothingFor=5 -PatOnceUsers=20 -PrampUsers=50 -PrampUsersDuring=60 -PconstantUsersPerSec=20 -PconstantUsersPerSecDuring=30
```

 * `envName` - Which API should this run against. Defaults to dev. **TODO** when we have function test, switch this to default to test
 * `simulationFqn` - Optional fully-qualified simulation class to run. If omitted, all simulations will be run.
 * `nothingFor` - How long to wait before starting the onslaught
 * `atOnceUsers` - Number of initial concurrent users
 * `rampUsers` - How many users to increase to over time
 * `rampUsersDuring` - Number of seconds over which to increase the user count
 * `constantUsersPerSec` - The number of concurrent users to keep active per second
 * `constantUsersPerSecDuring` - The time period in seconds over which peak load should be applied

### CLI parameters NOT TO BE USED

So the system can run in CI, we need to be able to pass in the following parameters: CLIENT_ID and CLIENT_SECRET. These should never be used on your CLI, if you have the urge to use them, then please go and review your sec-ops training.

## CI (Github action)

There is a workflow dispatch that will run the gatlingRunCi task. It has a number of option that match those outlined above.

## Recreate an E2E test

* start cp-stack
* clear existing logs
* run e2e tests in th ui package
* grab the logs in to a file (e2e.log)
* `grep "CommunityPaybackRequestLoggingFilter : Request data" e2e.log > e2e-request-data.log` > e2e-requests.log
* Ask a