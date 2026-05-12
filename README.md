<p align="center"><img src="logo.png" alt="ShegerPay" width="200" /></p>

# ShegerPay Android Kotlin SDK

Official Android SDK for ShegerPay — Ethiopian payment verification.

## Install (Gradle)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// build.gradle.kts
dependencies {
    implementation("com.shegerpay:sdk-android:2.2.0")
}
```

## Quick Start

```kotlin
import com.shegerpay.sdk.ShegerPay

val client = ShegerPay(apiKey = "sk_live_...")

// In a coroutine or suspend function:
val result = client.verify(
    transactionId = "FT26062K7WMY",
    amount = 1000,
    provider = "cbe"
)
```

## Requirements

- Android API 21+
- Kotlin 1.6+

## License

MIT
