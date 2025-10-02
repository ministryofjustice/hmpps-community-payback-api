# Load testing tool for the API

This is a subproject inside the Community Payback API. Attempts to fold this into the main repo are non-trivial due to dependency concerns.

## How to use this tool

You can either let Gatling automatically fetch a JWT using client credentials, let Gradle fetch client creds from Kubernetes for you, or provide a token yourself.

### One-step Gradle task (kubectl + Gatling)

If you have kubectl access to the cluster and jq installed, you can let Gradle fetch CLIENT_ID/CLIENT_SECRET from the hmpps-community-payback-<env> namespace and then run Gatling:

```bash
./gradlew gatlingRunWithK8sCreds -Penv=dev -PsimulationFqn=uk.gov.justice.digital.hmpps.communitypayback.simulations.ProviderSimulation
```

Notes:
- Requires kubectl and jq on your PATH.
- Uses namespace hmpps-community-payback-<env> and secret hmpps-community-payback-ui-client-creds.
