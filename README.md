# ShegerPay Android SDK

Official Android SDK for ShegerPay Payment Verification Gateway.

## 📦 Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.shegerpay:sdk:2.2.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.shegerpay:sdk:2.2.0'
}
```

### JitPack

Add to your root `build.gradle`:

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Then add:

```groovy
dependencies {
    implementation 'com.github.shegerpay:android-sdk:2.2.0'
}
```

---

## 🚀 Quick Start

### Kotlin (Recommended)

```kotlin
import com.shegerpay.sdk.ShegerPay

// Initialize client
val client = ShegerPay("sk_test_xxx")

// Verify Ethiopian payment (in a coroutine)
lifecycleScope.launch {
    val result = client.verify(
        transactionId = "FT24352648751234",
        amount = 100.0,
        provider = PaymentProvider.CBE
    )

    if (result.valid) {
        Log.d("ShegerPay", "✅ Payment verified!")
        Log.d("ShegerPay", "Payer: ${result.payer}")
    }
}
```

### Java

```java
import com.shegerpay.sdk.ShegerPayJava;
import com.shegerpay.sdk.ShegerPayCallback;
import com.shegerpay.sdk.VerificationResult;

// Initialize client
ShegerPayJava client = new ShegerPayJava("sk_test_xxx");

// Verify payment
client.verify("FT24352648751234", 100.0, new ShegerPayCallback<VerificationResult>() {
    @Override
    public void onSuccess(VerificationResult result) {
        if (result.getValid()) {
            Log.d("ShegerPay", "✅ Payment verified!");
        }
    }

    @Override
    public void onError(ShegerPayException error) {
        Log.e("ShegerPay", "Error: " + error.getMessage());
    }
});
```

---

## 💳 Ethiopian Payment Verification

### Auto-Detect Provider

```kotlin
// Use quickVerify() when the provider is ambiguous.
val result = client.quickVerify(
    transactionId = "FT24352648751234",
    amount = 100.0
)
```

### BOA Verification

```kotlin
val result = client.verify(
    transactionId = "https://cs.bankofabyssinia.com/slip/?trx=FT26091B1X5152078",
    amount = 100.0,
    provider = PaymentProvider.BOA,
    merchantName = "My Shop",
    senderAccount = "52078"
)
```

### Specify Provider

```kotlin
import com.shegerpay.sdk.PaymentProvider

val result = client.verify(
    transactionId = "FT24352648751234",
    amount = 100.0,
    provider = PaymentProvider.CBE,
    merchantName = "My Shop"
)
```

### Supported Providers

| Provider | Enum                       | ID Format        |
| -------- | -------------------------- | ---------------- |
| CBE      | `PaymentProvider.CBE`      | `FT` prefix      |
| Telebirr | `PaymentProvider.TELEBIRR` | Reference number |
| Awash    | `PaymentProvider.AWASH`    | `AW` prefix      |
| BoA      | `PaymentProvider.BOA`      | Receipt URL / full `trx` |
| E-Birr   | `PaymentProvider.EBIRR`    | Reference code   |

---

## 🖼 Receipt Image Verification

```kotlin
// Verify from base64-encoded screenshot
val result = client.verifyImage(
    image = "iVBORw0KGgoAAAANSUhEUgAA...",  // base64 string
    provider = "cbe",
    amount = 150.0,
    merchantName = "My Shop"
)

if (result.valid) {
    Log.d("ShegerPay", "Payment verified from receipt image!")
}

// Or verify from a public URL
val result2 = client.verifyImage(
    image = "https://example.com/receipt.png",
    merchantName = "My Shop"
)
```

---

## 📋 Get Supported Providers

```kotlin
val providers = client.getProviders()
Log.d("ShegerPay", providers.toString())
```

---

## 🔗 Payment Links

### Create Payment Link

```kotlin
val link = client.createPaymentLink(
    title = "Product Purchase",
    amount = 500.0,
    currency = "ETB",
    description = "Premium subscription",
    enableCbe = true,
    enableTelebirr = true,
    enableCrypto = false
)

Log.d("ShegerPay", "Payment URL: ${link.paymentUrl}")
Log.d("ShegerPay", "QR Code: ${link.qrCodeBase64}")
```

### List Payment Links

```kotlin
val links = client.listPaymentLinks(limit = 50)
links.forEach { link ->
    Log.d("ShegerPay", "${link.title}: ${link.status}")
}
```

---

## 🪙 Crypto Payments

### Generate Payment Intent

```kotlin
val intent = client.generateCryptoIntent(
    amountUsd = 50.0,
    walletAddress = "TJCnKsPa7y5okkXvQAidZBzqx3QyQ6sxMW",
    currency = "USDT",
    chain = "TRON"
)

Log.d("ShegerPay", "Send ${intent.paymentAmount} to ${intent.walletAddress}")
Log.d("ShegerPay", "Reference: ${intent.referenceId}")
```

### Verify Crypto Payment

```kotlin
val result = client.verifyCrypto(referenceId = "SHGR-TRO-ABC123")
if (result.valid) {
    Log.d("ShegerPay", "Crypto payment confirmed!")
}
```

---

## 🔔 Webhook Verification

```kotlin
fun handleWebhook(payload: String, signature: String): Boolean {
    return ShegerPay.verifyWebhookSignature(
        payload = payload,
        signature = signature,
        secret = "whsec_your_webhook_secret"
    )
}
```

---

## ⚙️ Configuration

### Custom Base URL

```kotlin
val client = ShegerPay(
    apiKey = "sk_test_xxx",
    baseUrl = "https://custom-api.example.com"
)
```

### Check Test Mode

```kotlin
if (client.isTestMode) {
    Log.d("ShegerPay", "Running in test mode")
}
```

---

## 🧪 Test Mode

Use `sk_test_*` API keys for testing:

| Transaction ID | Result     |
| -------------- | ---------- |
| `FT123456`     | ✅ Success |
| `FAIL_TEST`    | ❌ Failed  |
| `PENDING_123`  | ⏳ Pending |

---

## 📋 ProGuard Rules

Add to your `proguard-rules.pro`:

```proguard
-keep class com.shegerpay.sdk.** { *; }
-keepclassmembers class com.shegerpay.sdk.** { *; }
```

---

## 📄 License

MIT © 2026 ShegerPay
