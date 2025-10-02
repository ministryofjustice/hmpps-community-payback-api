# Load testing tool for the API

This is a subproject inside the Community Payback API. Attempts to fold this into the main repo are non-trivial due to dependency concerns.

## How to use this tool

You can either let Gatling automatically fetch a JWT using client credentials, let Gradle fetch client creds from Kubernetes for you, or provide a token yourself.

### Option A: One-step Gradle task (kubectl + Gatling)

If you have kubectl access to the cluster and jq installed, you can let Gradle fetch CLIENT_ID/CLIENT_SECRET from the hmpps-community-payback-<env> namespace and then run Gatling:

```bash
# From the gatling subproject directory
./gradlew gatlingRunWithK8sCreds -Penv=dev -PsimulationFqn=uk.gov.justice.digital.hmpps.communitypayback.simulations.ProviderSimulation
```

Notes:
- Requires kubectl and jq on your PATH.
- Uses namespace hmpps-community-payback-<env> and secret hmpps-community-payback-ui-client-creds.
- simulationFqn is optional; if omitted, you can be prompted by the plugin or set defaults.

### Option B: Automatic token fetch (client credentials)

Set the following environment variables or system properties and Gatling will obtain a token at startup if `jwt` is not provided:

- AUTH_BASE_URL or authBaseUrl (default: https://sign-in-dev.hmpps.service.justice.gov.uk/auth)
- AUTH_TOKEN_PATH or authTokenPath (default: /oauth/token)
- CLIENT_ID or clientId
- CLIENT_SECRET or clientSecret
- Optional: SCOPE or scope, GRANT_TYPE or grantType (default: client_credentials)

Example (dev):

```bash
export CLIENT_ID=hmpps-community-payback-ui-client-1
export CLIENT_SECRET=... # obtain from kubectl as below or your secrets manager
# now just run Gatling without providing JWT
./gradlew gatlingRun --simulation uk.gov.justice.digital.hmpps.communitypayback.simulations.ProviderSimulation
```

If automation fails (e.g., missing credentials), Gatling will continue without an Authorization header unless you provide a JWT manually.

### Option C: Manual token (curl/jq)

You can obtain a client-credentials access token from HMPPS Auth in the dev environment. You will need kubectl access to the hmpps-community-payback-dev namespace and jq installed.

```bash
export .env=dev
# Fetch the client credentials from Kubernetes and decode
kubectl -n hmpps-community-payback-$env get secrets hmpps-community-payback-ui-client-creds -o json \
  | jq ".data | map_values(@base64d)"

# Paste the value for CLIENT_SECRET into your shell (no echo)
read -rs CLIENT_SECRET

# Exchange for an access token
TOKEN=$(curl -s -X POST \
  "https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token?grant_type=client_credentials" \
  -H 'Content-Type: application/json' \
  -u "hmpps-community-payback-ui-client-1":$CLIENT_SECRET \
  | jq -r .access_token)

# You can export it for convenience
export JWT="$TOKEN"
```

### Running simulations

You can provide the token to Gatling in two ways:

- System property: `-Djwt=$TOKEN`
- Environment variable: `JWT=$TOKEN`

Examples:

```bash
# Using system property
./gradlew gatlingRun \
  --simulation uk.gov.justice.digital.hmpps.communitypayback.simulations.ProviderSimulation \
  -Djwt="$TOKEN"

# Or using environment variable
JWT="$TOKEN" ./gradlew gatlingRun \
  --simulation uk.gov.justice.digital.hmpps.communitypayback.simulations.ProviderSimulation
```

Notes:
- If no JWT can be obtained (automatically or manually), the simulations will run without an Authorization header.
- You can also set HMPPS_AUTH_TOKEN as an alternative environment variable name for the token.
