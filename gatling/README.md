# API load tests

This Gradle subproject runs the Community Payback API user journeys using Gatling. It requires JDK 25, a running API, HMPPS Auth client credentials with the appropriate API roles, and feeder records that exist in the target environment.

## Compile and check

From the repository root:

```shell
./gradlew :gatling:compileGatlingKotlin :gatling:ktlintCheck
```

## Run locally

Copy [env.example](env.example) to `gatling/.env` and set `API_URL`, `AUTH_BASE_URL`, `CLIENT_ID` and `CLIENT_SECRET`. Environment variables override the file; the file is optional. The supervisor journeys use `SUPERVISOR_CLIENT_ID` and `SUPERVISOR_CLIENT_SECRET` if supplied, otherwise the standard client credentials must also have supervisor access.

Run a single journey:

```shell
./gradlew :gatling:gatlingRun --simulation uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys.CaseAdminGroupSessionsJourneySimulation
```

Available simulations in `uk.gov.justice.digital.hmpps.communitypayback`:

* `simulations.journeys.CaseAdminGroupSessionsJourneySimulation`
* `simulations.journeys.CaseAdminIndividualPlacementJourneySimulation`
* `simulations.journeys.CaseAdminOnlineLearningJourneySimulation`
* `simulations.journeys.SupervisorGroupSessionJourneySimulation`
* `simulations.CommunityPaybackUserJourneysSimulation`

Use `:gatling:gatlingRun --all` to run every simulation sequentially. Reports are written to `gatling/build/reports/gatling`.

The individual journeys read the load settings in `env.example`. The combined simulation retains its fixed concurrent-user profile: ramp up over three minutes, then hold for five minutes, with 20 group-session admins, 5 individual-placement admins, 5 online-learning admins and 25 supervisors. The individual load settings do not change this combined profile.

Before running, update the CSV files in `src/gatling/resources/data` for the target environment. These contain recorded IDs and dates, rather than automatically provisioned test data. Course completions must support the supplied resolution. These journeys update appointments, save forms and resolve course completions.

## Run against a Kubernetes environment

The [helper script](../tools/scripts/run_loadtest.sh) retrieves credentials from Kubernetes. It needs `kubectl` and `jq` configured for the target cluster. Run it from the repository root:

```shell
./tools/scripts/run_loadtest.sh \
  --env dev \
  --simulation uk.gov.justice.digital.hmpps.communitypayback.simulations.journeys.CaseAdminGroupSessionsJourneySimulation \
  --nothing-for 0 \
  --at-once-users 1 \
  --ramp-users 0 \
  --ramp-users-during 0 \
  --constant-users-per-sec 0 \
  --constant-users-per-sec-during 0
```

Run with `--help` for all options. Passing both `--client-id` and `--client-secret` skips Kubernetes; supervisor credentials can be supplied as environment variables.

## CI

Run the [Gatling workflow](../.github/workflows/gatling_run.yml) using workflow dispatch. Choose `dev` or `test`, an optional simulation class, and the load settings. An empty simulation selection runs all simulations, including the combined profile.

The workflow calls `:gatling:gatlingRunCi`. This task also accepts Gradle properties such as `-PsimulationFqn=...`, `-PenvName=dev`, `-PatOnceUsers=1`, and the other workflow load settings. `API_URL` can override the environment URL. Credentials can be passed as environment variables or Gradle properties. Individual simulations require more than 99% successful requests; the combined simulation also requires the 95th percentile response time to be below five seconds.
