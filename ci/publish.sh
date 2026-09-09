#!/usr/bin/env bash
# Jenkins build step for uploading kwiklink-sdk to Maven Central.
#
# Upload only (publishToMavenCentral, not publishAndReleaseToMavenCentral)
# — the deployment sits in Central Portal for a human to review and click
# Publish manually. Same split as the local ~/creds/kwiklink-creds/publish.sh
# workflow on the dev machine; see docs/android-sdk-plan.md (parent repo)
# for the full publishing history/setup.
#
# Expects these already bound as env vars by the Jenkins job's credential
# bindings (Manage Jenkins -> Credentials) — never committed or hardcoded
# here, never passed on the command line where they'd land in build logs:
#   KWIKLINK_SIGNING_KEY_FILE       - path from a "Secret file" credential
#                                     (the armored GPG private key)
#   KWIKLINK_SIGNING_PASSWORD_FILE  - path from a "Secret file" credential
#                                     (the GPG passphrase, as a one-line file)
#   MAVEN_CENTRAL_USERNAME          - Central Portal user token username
#                                     ("Secret text" credential)
#   MAVEN_CENTRAL_PASSWORD          - Central Portal user token password
#                                     ("Secret text" credential)
#
# ASSUMPTIONS (adjust this script if any of these don't hold):
#   1. The Jenkins agent running this already has JDK 17 and the Android SDK
#      cmdline-tools on PATH (JAVA_HOME/ANDROID_HOME set), the same
#      toolchain this repo's own Gradle build needs. Neither is installed
#      by this script — see the Jenkins job setup notes for how to provide
#      that (a pre-provisioned agent, or a Docker build step).
#   2. This script lives in the kwiklink-sdk-android repo itself (checked
#      out at the repo root by the Jenkins job) — REPO_ROOT below assumes
#      that, not the parent kwiklink-platform monorepo.

set -euo pipefail

for var in KWIKLINK_SIGNING_KEY_FILE KWIKLINK_SIGNING_PASSWORD_FILE MAVEN_CENTRAL_USERNAME MAVEN_CENTRAL_PASSWORD; do
  if [[ -z "${!var:-}" ]]; then
    echo "ERROR: $var is not set — check the job's credential bindings." >&2
    exit 1
  fi
done

export ORG_GRADLE_PROJECT_kwiklinkSigningKeyFile="$KWIKLINK_SIGNING_KEY_FILE"
export ORG_GRADLE_PROJECT_kwiklinkSigningPasswordFile="$KWIKLINK_SIGNING_PASSWORD_FILE"
export ORG_GRADLE_PROJECT_mavenCentralUsername="$MAVEN_CENTRAL_USERNAME"
export ORG_GRADLE_PROJECT_mavenCentralPassword="$MAVEN_CENTRAL_PASSWORD"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "==> Uploading kwiklink-sdk to Maven Central (upload only, not released) from $REPO_ROOT"
./gradlew :kwiklink-sdk:publishToMavenCentral

echo "==> Uploaded. Review and click Publish manually at https://central.sonatype.com"
