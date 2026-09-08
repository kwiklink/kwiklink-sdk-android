# Applied automatically to any host app that depends on kwiklink-sdk — this
# is what protects the SDK's own classes from a *consumer's* R8/ProGuard
# config, which may be far more aggressive than this module's own.

# kotlinx.serialization generates a $serializer companion per @Serializable
# class and resolves it via a `serializer()` factory call at the call site
# (not runtime reflection), but a consumer's full-mode R8 can still strip or
# rename members it doesn't see referenced from its own code — this is the
# library's own documented consumer-rule shape, scoped to this SDK's
# request/response DTOs (internal/net/Dtos.kt).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclasseswithmembers class io.kwiklink.android.sdk.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.kwiklink.android.sdk.**$$serializer { *; }
-keepclassmembers class io.kwiklink.android.sdk.** {
    *** Companion;
}
