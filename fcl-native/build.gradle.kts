plugins {
    id("com.android.library")
}

android {
    namespace = "com.mikael.launcher.fclnative"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 26

        ndk {
            abiFilters += setOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    buildFeatures {
        prefab = true
    }

    externalNativeBuild {
        cmake {
            path = file("../third_party/FoldCraftLauncher/FCL/src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets.getByName("main") {
        jniLibs.srcDir("../third_party/FoldCraftLauncher/FCL/src/main/jniLibs")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Required by FCL's native CMake project through find_package(bytehook CONFIG).
    implementation("com.bytedance:bytehook:1.0.10")
}
