# ShegerPay Android SDK consumer ProGuard rules.
# These rules are packaged into the AAR and applied automatically to apps
# that depend on the SDK, so consumers do not need their own keep rules.

# Keep the public SDK surface.
-keep class com.shegerpay.sdk.** { *; }

# kotlinx.serialization: keep serializers generated for SDK models.
-keepclassmembers class com.shegerpay.sdk.** {
    *** Companion;
}
-keepclasseswithmembers class com.shegerpay.sdk.** {
    kotlinx.serialization.KSerializer serializer(...);
}
