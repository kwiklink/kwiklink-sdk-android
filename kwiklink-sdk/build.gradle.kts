import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("signing")
}

android {
    namespace = "io.kwiklink.android.sdk"
    compileSdk = 34

    defaultConfig {
        // 23 (Android 6.0) is the floor Android App Links verification
        // itself supports — no point supporting a minSdk the SDK's own
        // warm-open path can't run on.
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Off by default since AGP 8 — turned on so ApiHost.kt can read
    // BuildConfig.API_HOST instead of hand-editing a hardcoded constant.
    buildFeatures {
        buildConfig = true
    }

    // Which backend this build talks to (ApiHost.kt) — not a customer-facing
    // config, purely so this repo's own dev/prod builds differ without
    // touching source. sample-app declares the same dimension/flavors so
    // AGP can match variants across the project(":kwiklink-sdk") dependency.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "API_HOST", "\"orbit-in1.kwiklink.io\"")
        }
        create("prod") {
            dimension = "environment"
            // TODO: replace with the real production SDK API host once it exists.
            buildConfigField("String", "API_HOST", "\"REPLACE_ME_PROD_API_HOST.kwiklink.io\"")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Maven Central release signing. Key file + passphrase come from
// ~/.gradle/gradle.properties (outside the repo, never committed) — see
// docs/android-sdk-plan.md for how those were provisioned. Guarded so a
// build without them configured (a fresh machine, CI before secrets are
// wired up) doesn't fail — it just produces unsigned artifacts.
val kwiklinkSigningKeyFile = providers.gradleProperty("kwiklinkSigningKeyFile")
val kwiklinkSigningPasswordFile = providers.gradleProperty("kwiklinkSigningPasswordFile")
if (kwiklinkSigningKeyFile.isPresent && kwiklinkSigningPasswordFile.isPresent) {
    signing {
        useInMemoryPgpKeys(
            file(kwiklinkSigningKeyFile.get()).readText(),
            file(kwiklinkSigningPasswordFile.get()).readText().trim(),
        )
    }
}

dependencies {
    // OkHttp + kotlinx.serialization, not Retrofit/Ktor — this is a library
    // embedded in someone else's app, so minimizing transitive-dependency
    // conflicts with whatever networking stack the host app already ships
    // matters more here than in a standalone app (see docs/android-sdk-plan.md).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Mandatory for deferred/cold-open attribution — parses the Play
    // Install Referrer string on first launch (see internal/referrer/).
    implementation("com.android.installreferrer:installreferrer:2.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // Instrumented tests exercise the one thing JVM unit tests can't: real
    // android.net.Uri/Intent behavior (see IntentLinkParserInstrumentedTest).
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
