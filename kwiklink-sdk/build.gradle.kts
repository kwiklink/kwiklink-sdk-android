import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("signing")
    id("com.vanniktech.maven.publish")
}

// Single source of truth for the published version — also exposed to the
// SDK's own runtime via BuildConfig.SDK_VERSION (see AttributionInterceptor)
// so what it reports about itself can't drift from what's actually on
// Maven Central.
val sdkVersion = "0.1.1"

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
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
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
            // orbit-in1.kwiklink.io is the only SDK API host that actually
            // exists — it already serves against the real production
            // database (a deliberate choice, see docs/android-sdk-plan.md),
            // so there's no separate "real prod host" to point at instead of
            // what dev already uses. No more placeholder.
            buildConfigField("String", "API_HOST", "\"orbit-in1.kwiklink.io\"")
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

// Maven Central coordinates + POM. Upload credentials (mavenCentralUsername/
// mavenCentralPassword) come from the same ~/.gradle/gradle.properties as the
// signing config above — publishToMavenCentral() only needs them at publish
// time, so their absence doesn't block a normal build.
mavenPublishing {
    // Maven Central gets exactly one variant — the one that talks to the
    // real production API host, never the dev-flavored build. This also
    // registers the android.publishing.singleVariant() config for us.
    configure(AndroidSingleVariantLibrary(variant = "prodRelease", sourcesJar = true, publishJavadocJar = true))

    // Explicit, not relying on the plugin's default host — this account only
    // exists on the modern Central Portal (verified via DNS TXT on
    // kwiklink.io), it has no legacy OSSRH/Nexus staging profile at all.
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("io.kwiklink.sdk.android", "kwiklink-sdk", sdkVersion)

    pom {
        name.set("Kwiklink Android SDK")
        description.set("Deep link resolution and deferred-install attribution for Android apps using Kwiklink.")
        url.set("https://github.com/kwiklink/kwiklink-sdk-android")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("kwiklink")
                name.set("Kwiklink")
                email.set("info@kwiklink.io")
                url.set("https://kwiklink.io")
            }
        }

        scm {
            url.set("https://github.com/kwiklink/kwiklink-sdk-android")
            connection.set("scm:git:git://github.com/kwiklink/kwiklink-sdk-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/kwiklink/kwiklink-sdk-android.git")
        }
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
