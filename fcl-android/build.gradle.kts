plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tungsten.fcl"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")

        // FCL reads these values at runtime. API keys remain unset until a supported
        // authentication/download configuration is added by the Mikael Launcher.
        resValue("string", "app_version", "0.1.0")
        resValue("string", "curse_api_key", "")
        resValue("string", "oauth_api_key", "")
        resValue("string", "file_browser_provider", "com.mikael.launcher.provider")

        ndk {
            abiFilters += setOf("arm64-v8a")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
        prefab = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets.getByName("main") {
        val upstream = file("../third_party/FoldCraftLauncher/FCL/src/main")
        java.srcDir(upstream.resolve("java"))
        res.srcDir(upstream.resolve("res"))
        assets.srcDir(upstream.resolve("assets"))
        jniLibs.srcDir(upstream.resolve("jniLibs"))
        manifest.srcFile(upstream.resolve("AndroidManifest.xml"))
        // jreAssets intentionally are not bundled here. The four upstream runtimes
        // are large; runtime acquisition must be implemented as an on-demand step.
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf("**/libbytehook.so")
        }
        resources {
            excludes += setOf("/META-INF/AL2.0", "/META-INF/LGPL2.1")
        }
    }

    lint {
        targetSdk = 34
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(fileTree(mapOf(
        "dir" to "../third_party/FoldCraftLauncher/FCL/libs",
        "include" to listOf("*.jar", "*.aar")
    )))

    implementation(project(":ZipFileSystem"))
    implementation(project(":Terracotta"))
    implementation(project(":fcl-native"))

    implementation(libs.commons.io)
    implementation(libs.jelf)
    implementation(libs.nanohttpd)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.opennbt)
    implementation(libs.lz4)
    implementation(libs.gson)
    implementation(libs.tomlj)
    implementation(libs.constant.pool.scanner)
    implementation(libs.jsoup)
    implementation(libs.chardet)
    implementation(libs.junrar)
    implementation(libs.bytehook)
    implementation(libs.appcompat)
    implementation(libs.androidx.viewpager2)
    implementation(libs.core.splashscreen)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.recyclerview)
    implementation(libs.coroutines.android)
    implementation(libs.glide)
    implementation(libs.touchcontroller)
    implementation(libs.palette.ktx)
    implementation(libs.gamepad.remapper)
    implementation(libs.datastore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation("junit:junit:4.13.2")
}
