plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}

// Keep D8/R8 compatible with Kotlin 2.4 metadata. This matches the pinned FCL toolchain.
buildscript {
    dependencies {
        classpath("com.android.tools:r8:9.4.17")
    }
}
