#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_ROOT="${IPERF3_IOS_BUILD_DIR:-$SCRIPT_DIR/build}"
DIST_ROOT="${IPERF3_IOS_DIST_DIR:-$SCRIPT_DIR/dist}"
DEVICE_BUILD="$BUILD_ROOT/device"
SIMULATOR_BUILD="$BUILD_ROOT/simulator"
OUTPUT="$DIST_ROOT/Iperf3.xcframework"
DEPLOYMENT_TARGET="${IPHONEOS_DEPLOYMENT_TARGET:-15.0}"
DEVICE_ARCHS="${IOS_DEVICE_ARCHS:-arm64}"
SIMULATOR_ARCHS="${IOS_SIMULATOR_ARCHS:-arm64}"

rm -rf "$DEVICE_BUILD" "$SIMULATOR_BUILD" "$OUTPUT"
mkdir -p "$DIST_ROOT"

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

xcodebuild -create-xcframework \
  -library "$DEVICE_BUILD/Release-iphoneos/libiperf3.a" \
  -headers "$SCRIPT_DIR/../iperf3/src" \
  -library "$SIMULATOR_BUILD/Release-iphonesimulator/libiperf3.a" \
  -headers "$SCRIPT_DIR/../iperf3/src" \
  -output "$OUTPUT"

echo "Built: $OUTPUT"
