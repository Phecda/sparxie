plugins {
    id("com.android.library")
}

val defaultNdkVersion = "27.1.12297006"
val ndkVersionOverride = providers.gradleProperty("iperfNdkVersion")
    .orElse(providers.environmentVariable("ANDROID_NDK_VERSION"))
    .orElse(defaultNdkVersion)

android {
    namespace = "me.phecda.sparxie.iperf3"
    compileSdk = 37
    ndkVersion = ndkVersionOverride.get()

    defaultConfig {
        minSdk = 26

        externalNativeBuild {
            cmake {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
