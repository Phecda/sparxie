# Apple platform iperf3 integration

Sparxie vendors iperf 3.21 under `native/iperf3` and builds it as a static
XCFramework for iOS and macOS. Run `native/ios/build-ios.sh` from any directory
to rebuild the iOS device, iOS simulator, and macOS slices and copy the result to
`ios/Frameworks/Iperf3.xcframework`.

The XCFramework exposes the `Iperf3` C module through a small umbrella header.
Application code should import that module rather than adding the vendored
source directory to Xcode header search paths.

## Runtime contract for I3

The Client/Server integration added in I3 must follow these rules:

- `iperf_run_client` and `iperf_run_server` are blocking calls. Run them on one
  dedicated serial background executor and never on the main actor.
- Only one iperf Client or Server may run in the process at a time. iperf uses
  process-global error state such as `i_errno`, so concurrent sessions are not
  supported.
- A running test owns its `iperf_test` pointer. Cancellation may call
  `iperf_interrupt` while that pointer remains valid, but only the worker that
  runs iperf may call `iperf_free_test`.
- The lifecycle is `iperf_new_test` -> `iperf_defaults` -> configure/run ->
  `iperf_free_test`. Every test must be freed exactly once.
- On failure, copy `i_errno` and the text returned by `iperf_strerror` before
  freeing the test or starting another session.

This phase intentionally does not define Swift Client/Server configuration,
session state, callbacks, or UI behavior.
