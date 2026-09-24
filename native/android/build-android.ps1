$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$gradle = Join-Path $repoRoot "android\gradlew.bat"
$projectDir = Join-Path $repoRoot "native\android"
$aarSource = Join-Path $projectDir "iperf3-native\build\outputs\aar\iperf3-native-release.aar"
$appLibs = Join-Path $repoRoot "android\app\libs"
$aarDestination = Join-Path $appLibs "iperf3-native.aar"
$ndkVersion = if ($env:ANDROID_NDK_VERSION) { $env:ANDROID_NDK_VERSION } else { "27.1.12297006" }

if (-not (Test-Path $gradle)) {
    throw "Android Gradle wrapper was not found: $gradle"
}

& $gradle -p $projectDir :iperf3-native:assembleRelease "-PiperfNdkVersion=$ndkVersion"
if ($LASTEXITCODE -ne 0) {
    throw "Android AAR build failed with exit code $LASTEXITCODE."
}

if (-not (Test-Path $aarSource)) {
    throw "Expected AAR was not produced: $aarSource"
}

New-Item -ItemType Directory -Force -Path $appLibs | Out-Null
Copy-Item -Path $aarSource -Destination $aarDestination -Force
Write-Output "Built and copied: $aarDestination"
