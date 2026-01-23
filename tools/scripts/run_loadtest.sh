#!/usr/bin/env bash
set -euo pipefail

# ------------------------------------------------------------------------------
# Run Gatling via Gradle, loading client credentials from a Kubernetes Secret.
#
# Requirements:
#   - bash, kubectl, jq
#   - access to the cluster/namespace containing the secret
#   - ./gradlew present and executable in this project
#
# Usage examples:
#   ./run_gatling.sh --env dev \
#     --nothing-for 5 \
#     --at-once-users 20 \
#     --ramp-users 50 \
#     --ramp-users-during 60 \
#     --constant-users-per-sec 20 \
#     --constant-users-per-sec-during 30 \
#     --simulation uk.gov.justice.digital.hmpps.communitypayback.simulations.api.HealthSimulation
#
#   # If you prefer to pass clientId/clientSecret directly (skips kubectl/jq):
#   ./run_gatling.sh --env dev --client-id xxx --client-secret yyy
#
# Notes:
#   - Secret and namespace names follow the README’s example:
#       namespace: hmpps-community-payback-<env>
#       secret:    hmpps-community-payback-ui-client-creds
#   - Kubernetes Secret .data values are base64-encoded; we decode them.
# ------------------------------------------------------------------------------

# ---------- defaults (you can override via flags) ----------
ENV_NAME="dev"
NOTHING_FOR="5"
AT_ONCE_USERS="20"
RAMP_USERS="50"
RAMP_USERS_DURING="60"
CONSTANT_USERS_PER_SEC="20"
CONSTANT_USERS_PER_SEC_DURING="30"
SIMULATION_FQN=""        # empty means "run all simulations"
CLIENT_ID=""
CLIENT_SECRET=""

# ---------- helpers ----------
die() { echo "Error: $*" >&2; exit 1; }
have() { command -v "$1" >/dev/null 2>&1; }

usage() {
  cat <<'USAGE'
Usage: run_gatling.sh [options]

Options:
  --env <name>                          Environment name (e.g., dev, preprod, prod). Default: dev
  --nothing-for <seconds>               -PnothingFor (delay before starting). Default: 5
  --at-once-users <n>                   -PatOnceUsers. Default: 20
  --ramp-users <n>                      -PrampUsers. Default: 50
  --ramp-users-during <seconds>         -PrampUsersDuring. Default: 60
  --constant-users-per-sec <n>          -PconstantUsersPerSec. Default: 20
  --constant-users-per-sec-during <s>   -PconstantUsersPerSecDuring. Default: 30
  --simulation <FQN>                    Fully-qualified simulation class; if omitted, all simulations run
  --client-id <id>                      Optional: override clientId (skip fetching from Kubernetes)
  --client-secret <secret>              Optional: override clientSecret (skip fetching from Kubernetes)
  -h, --help                            Show this help and exit
USAGE
}

# ---------- parse args ----------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --env) ENV_NAME="${2:-}"; shift 2 ;;
    --nothing-for) NOTHING_FOR="${2:-}"; shift 2 ;;
    --at-once-users) AT_ONCE_USERS="${2:-}"; shift 2 ;;
    --ramp-users) RAMP_USERS="${2:-}"; shift 2 ;;
    --ramp-users-during) RAMP_USERS_DURING="${2:-}"; shift 2 ;;
    --constant-users-per-sec) CONSTANT_USERS_PER_SEC="${2:-}"; shift 2 ;;
    --constant-users-per-sec-during) CONSTANT_USERS_PER_SEC_DURING="${2:-}"; shift 2 ;;
    --simulation) SIMULATION_FQN="${2:-}"; shift 2 ;;
    --client-id) CLIENT_ID="${2:-}"; shift 2 ;;
    --client-secret) CLIENT_SECRET="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown option: $1. Use --help for usage."; ;;
  esac
done

# ---------- verify required tools ----------
[[ -x ./gradlew ]] || die "Cannot find ./gradlew (make sure you're in the repo root and it is executable)"
if [[ -z "$CLIENT_ID" || -z "$CLIENT_SECRET" ]]; then
  have kubectl || die "kubectl is required unless you pass --client-id/--client-secret"
  have jq || die "jq is required unless you pass --client-id/--client-secret"
fi

# ---------- resolve namespace and secret ----------
NAMESPACE="hmpps-community-payback-${ENV_NAME}"
SECRET_NAME="hmpps-community-payback-ui-client-creds"

# ---------- fetch credentials from Kubernetes (if not overridden) ----------
if [[ -z "$CLIENT_ID" || -z "$CLIENT_SECRET" ]]; then
  echo "Fetching client credentials from Kubernetes: ns=${NAMESPACE}, secret=${SECRET_NAME} ..."
  # Grab the secret JSON and extract/decode fields
  SECRET_JSON="$(kubectl -n "${NAMESPACE}" get secret "${SECRET_NAME}" -o json)"
  # data fields are base64-encoded; decode them
  # Some clusters may already present readable values in examples, but proper K8s semantics = base64.
  CLIENT_ID="$(echo "$SECRET_JSON" | jq -r '.data["CLIENT_CREDS_CLIENT_ID"]' | base64 --decode || true)"
  CLIENT_SECRET="$(echo "$SECRET_JSON" | jq -r '.data["CLIENT_CREDS_CLIENT_SECRET"]' | base64 --decode || true)"

  [[ -n "$CLIENT_ID" ]] || die "CLIENT_CREDS_CLIENT_ID missing or empty in secret ${SECRET_NAME}"
  [[ -n "$CLIENT_SECRET" ]] || die "CLIENT_CREDS_CLIENT_SECRET missing or empty in secret ${SECRET_NAME}"
fi

# ---------- assemble Gradle args ----------
GRADLE_ARGS=(
  "gatling:gatlingRunCi"
  "-PenvName=${ENV_NAME}"
  "-PnothingFor=${NOTHING_FOR}"
  "-PatOnceUsers=${AT_ONCE_USERS}"
  "-PrampUsers=${RAMP_USERS}"
  "-PrampUsersDuring=${RAMP_USERS_DURING}"
  "-PconstantUsersPerSec=${CONSTANT_USERS_PER_SEC}"
  "-PconstantUsersPerSecDuring=${CONSTANT_USERS_PER_SEC_DURING}"
  "-PCLIENT_ID=${CLIENT_ID}"
  "-PCLIENT_SECRET=${CLIENT_SECRET}"
)

if [[ -n "${SIMULATION_FQN}" ]]; then
  GRADLE_ARGS+=("-PsimulationFqn=${SIMULATION_FQN}")
fi

echo "Running: ./gradlew ${GRADLE_ARGS[*]}"
exec ./gradlew "${GRADLE_ARGS[@]}"
