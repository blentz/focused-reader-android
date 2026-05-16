#!/usr/bin/env bash
# Usage: ./bump-version.sh [major|minor|patch]
# Bumps versionName (semver) + increments versionCode, commits + tags + pushes.
set -euo pipefail
KIND=${1:-patch}
PROPS=version.properties
NAME=$(grep '^versionName=' $PROPS | cut -d= -f2)
CODE=$(grep '^versionCode=' $PROPS | cut -d= -f2)
IFS='.' read -r MA MI PA <<< "$NAME"
case "$KIND" in
  major) MA=$((MA+1)); MI=0; PA=0 ;;
  minor) MI=$((MI+1)); PA=0 ;;
  patch) PA=$((PA+1)) ;;
  *) echo "usage: $0 [major|minor|patch]"; exit 1 ;;
esac
NEW_NAME="$MA.$MI.$PA"
NEW_CODE=$((CODE+1))
sed -i "s/^versionName=.*/versionName=$NEW_NAME/" $PROPS
sed -i "s/^versionCode=.*/versionCode=$NEW_CODE/" $PROPS
git add $PROPS
git commit -m "release: v$NEW_NAME (versionCode $NEW_CODE)"
git tag "v$NEW_NAME"
echo "Bumped to v$NEW_NAME (code $NEW_CODE). Run: git push --follow-tags"
