#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
GRADLE="$REPO_ROOT/android/gradlew"
PROJECT_DIR="$REPO_ROOT/native/android"
AAR_SOURCE="$PROJECT_DIR/iperf3-native/build/outputs/aar/iperf3-native-release.aar"
APP_LIBS="$REPO_ROOT/android/app/libs"
AAR_DESTINATION="$APP_LIBS/iperf3-native.aar"
NDK_VERSION=${ANDROID_NDK_VERSION:-27.1.12297006}

if [ ! -f "$GRADLE" ]; then
  echo "Android Gradle wrapper was not found: $GRADLE" >&2
  exit 1
fi

sh "$GRADLE" -p "$PROJECT_DIR" :iperf3-native:assembleRelease "-PiperfNdkVersion=$NDK_VERSION"

if [ ! -f "$AAR_SOURCE" ]; then
  echo "Expected AAR was not produced: $AAR_SOURCE" >&2
  exit 1
fi

mkdir -p "$APP_LIBS"
cp "$AAR_SOURCE" "$AAR_DESTINATION"
echo "Built and copied: $AAR_DESTINATION"
