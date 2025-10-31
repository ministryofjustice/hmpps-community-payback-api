#!/usr/bin/env bash

set -e
# shellcheck disable=SC3040
set -o pipefail

cd "$(dirname "$0")"
this_dir="$(pwd)"

. "$this_dir"/../scripts/resolve_secrets.sh

ui_secret_names=("hmpps-community-payback-ui-client-creds")
resolve_secrets "$this_dir/http-client.private.env.json.template" \
                "$this_dir/http-client.private.env.json" \
                "hmpps-community-payback-dev" \
                "${ui_secret_names[@]}"
