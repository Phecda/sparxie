#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
BUILD_ROOT="${IPERF3_IOS_BUILD_DIR:-$SCRIPT_DIR/build}"
DIST_ROOT="${IPERF3_IOS_DIST_DIR:-$SCRIPT_DIR/dist}"
APP_FRAMEWORKS_ROOT="${IPERF3_IOS_APP_FRAMEWORKS_DIR:-$REPO_ROOT/ios/Frameworks}"
DEVICE_BUILD="$BUILD_ROOT/device"
SIMULATOR_BUILD="$BUILD_ROOT/simulator"
MACOS_BUILD="$BUILD_ROOT/macos"
PUBLIC_HEADERS="$BUILD_ROOT/include"
OUTPUT="$DIST_ROOT/Iperf3.xcframework"
APP_OUTPUT="$APP_FRAMEWORKS_ROOT/Iperf3.xcframework"
DEPLOYMENT_TARGET="${IPHONEOS_DEPLOYMENT_TARGET:-15.0}"
MACOS_DEPLOYMENT_TARGET="${MACOSX_DEPLOYMENT_TARGET:-15.0}"
DEVICE_ARCHS="${IOS_DEVICE_ARCHS:-arm64}"
SIMULATOR_ARCHS="${IOS_SIMULATOR_ARCHS:-arm64}"
MACOS_ARCHS="${MACOS_ARCHS:-arm64}"

rm -rf "$DEVICE_BUILD" "$SIMULATOR_BUILD" "$MACOS_BUILD" "$PUBLIC_HEADERS" "$OUTPUT"
mkdir -p "$DIST_ROOT" "$PUBLIC_HEADERS"

cp "$SCRIPT_DIR/include/Iperf3.h" "$PUBLIC_HEADERS/Iperf3.h"
cp "$SCRIPT_DIR/include/module.modulemap" "$PUBLIC_HEADERS/module.modulemap"
cp "$SCRIPT_DIR/../iperf3/src/iperf_api.h" "$PUBLIC_HEADERS/iperf_api.h"
cp "$SCRIPT_DIR/../iperf3/src/iperf_config.h" "$PUBLIC_HEADERS/iperf_config.h"

cmake -S "$SCRIPT_DIR" -B "$DEVICE_BUILD" -G Xcode \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_SYSROOT=iphoneos \
  -DCMAKE_OSX_ARCHITECTURES="$DEVICE_ARCHS" \
  -DCMAKE_OSX_DEPLOYMENT_TARGET="$DEPLOYMENT_TARGET"
cmake --build "$DEVICE_BUILD" --config Release

cmake -S "$SCRIPT_DIR" -B "$SIMULATOR_BUILD" -G Xcode \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_SYSROOT=iphonesimulator \
  -DCMAKE_OSX_ARCHITECTURES="$SIMULATOR_ARCHS" \
  -DCMAKE_OSX_DEPLOYMENT_TARGET="$DEPLOYMENT_TARGET"
cmake --build "$SIMULATOR_BUILD" --config Release

cmake -S "$SCRIPT_DIR" -B "$MACOS_BUILD" -G Xcode \
  -DCMAKE_SYSTEM_NAME=Darwin \
  -DCMAKE_OSX_SYSROOT=macosx \
  -DCMAKE_OSX_ARCHITECTURES="$MACOS_ARCHS" \
  -DCMAKE_OSX_DEPLOYMENT_TARGET="$MACOS_DEPLOYMENT_TARGET"
cmake --build "$MACOS_BUILD" --config Release

xcodebuild -create-xcframework \
  -library "$DEVICE_BUILD/Release-iphoneos/libiperf3.a" \
  -headers "$PUBLIC_HEADERS" \
  -library "$SIMULATOR_BUILD/Release-iphonesimulator/libiperf3.a" \
  -headers "$PUBLIC_HEADERS" \
  -library "$MACOS_BUILD/Release/libiperf3.a" \
  -headers "$PUBLIC_HEADERS" \
  -output "$OUTPUT"

mkdir -p "$APP_FRAMEWORKS_ROOT"
rm -rf "$APP_OUTPUT"
cp -R "$OUTPUT" "$APP_OUTPUT"

echo "Built: $OUTPUT"
echo "Copied: $APP_OUTPUT"
