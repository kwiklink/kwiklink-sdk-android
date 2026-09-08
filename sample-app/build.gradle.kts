import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Real per-developer API keys don't belong in committed Kotlin source —
// read from local.properties (already gitignored, already this project's
// place for per-machine config like sdk.dir) instead. See
// local.properties.example for the key this expects.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val kwiklinkApiKey: String =
    localProperties.getProperty("KWIKLINK_API_KEY") ?: "REPLACE_ME_WITH_A_REAL_API_KEY"

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.kwiklink.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.kwiklink.sample"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // Off by default since AGP 8 — turned on so KwiklinkSampleApp.kt can
    // read BuildConfig.API_KEY.
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // Mirrors kwiklink-sdk's own "environment" dimension/flavors — required
    // so AGP can match a variant of project(":kwiklink-sdk") to build
    // against; this app has no dev/prod behavior of its own, it just needs
    // to pick which SDK backend it links to via the same Build Variant.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "API_KEY", "\"$kwiklinkApiKey\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "API_KEY", "\"$kwiklinkApiKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":kwiklink-sdk"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("com.google.android.material:material:1.12.0")
}
