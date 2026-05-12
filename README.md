<p align="center"><img src="logo.png" alt="ShegerPay" width="200" /></p>

# ShegerPay Android SDK

[![Version](https://img.shields.io/badge/version-2.2.0-blue)](https://github.com/shegerpay/sdk-android/releases)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![Android](https://img.shields.io/badge/Android-API%2021%2B-green)](https://developer.android.com)

Official Android SDK for ShegerPay — verify Ethiopian bank payments (CBE, Telebirr, BOA, Awash).

## Install

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("com.shegerpay:sdk-android:2.2.0")
}
```

## Quick Start

```kotlin
import com.shegerpay.sdk.ShegerPay

val client = ShegerPay(apiKey = "sk_live_YOUR_API_KEY")

// In a coroutine (e.g. viewModelScope.launch):

// Verify a payment
val result = client.verify(
    transactionId = "FT26062K7WMY",
    provider = "cbe",
    amount = 1000.0
)
if (result.verified) {
    // ✅ Payment confirmed
}

// Verify without amount (lookup only)
val result2 = client.verify(transactionId = "FT26062K7WMY", provider = "telebirr")
println(result2.status)

// Verify from receipt screenshot
val bitmap = BitmapFactory.decodeFile(receiptPath)
val stream = ByteArrayOutputStream()
bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
val imageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
val imgResult = client.verifyImage(imageBase64, provider = "cbe")

// Create payment link
val link = client.createPaymentLink(
    title = "Order #1234",
    amount = 1500.0,
    currency = "ETB"
)
println(link["url"])

// Get providers list
val providers = client.getProviders()
```

**In ViewModel:**
```kotlin
viewModelScope.launch {
    val result = client.verify(txId, provider = "cbe", amount = amount)
    _uiState.value = if (result.verified) UiState.Success else UiState.Failed
}
```

## Supported Providers
`cbe` · `telebirr` · `boa` · `awash` · `ebirr_kaafi` · `ebirr_coop`

## Requirements
- Android API 21+ (Android 5.0+)
- Kotlin 1.6+


## Support
- 📚 Docs: https://shegerpay.com/docs
- 💬 Telegram: [@shegerpay_0](https://t.me/shegerpay_0)
- 📧 Email: support@shegerpay.com
