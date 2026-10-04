#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ugs_dir="$project_dir/upstream/ugs"
ugs_tag="v2.1.26"

if [[ ! -d "$ugs_dir/.git" ]]; then
  mkdir -p "$project_dir/upstream"
  git clone --depth 1 --branch "$ugs_tag" https://github.com/winder/Universal-G-Code-Sender.git "$ugs_dir"
fi
if [[ "$(git -C "$ugs_dir" describe --tags --exact-match 2>/dev/null || true)" != "$ugs_tag" ]]; then
  echo "UGS checkout in $ugs_dir must be at tag $ugs_tag" >&2
  exit 1
fi

if [[ -z "${JAVA_HOME:-}" && -x /usr/lib/jvm/java-17-openjdk-amd64/bin/javac ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
  export PATH="$JAVA_HOME/bin:$PATH"
fi

"$ugs_dir/mvnw" -f "$ugs_dir/pom.xml" install \
  -pl ugs-platform/ugs-platform-surfacescanner -am -DskipTests -ntp
"$ugs_dir/mvnw" -f "$project_dir/pom.xml" clean package -ntp
echo "Module: $project_dir/target/nbm/ugs-manual-heightmap-0.1.0-SNAPSHOT.nbm"
