// Root project — no source of its own, just plugin version alignment for
// the kwiklink-sdk and sample-app modules below. Independent of the Go/Node
// build tooling at the repo root: own Gradle wrapper, own CI stage.
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.library") version "9.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}
