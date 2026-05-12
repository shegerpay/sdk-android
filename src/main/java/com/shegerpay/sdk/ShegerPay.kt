/**
 * ShegerPay Android SDK
 * Official Android SDK for ShegerPay Payment Verification Gateway
 * 
 * Installation (Gradle):
 *   implementation("com.shegerpay:sdk:2.2.0")
 * 
 * Usage:
 *   val client = ShegerPay("sk_test_xxx")
 *   val result = client.verify("FT123456", 100.0, provider = PaymentProvider.CBE)
 */

package com.shegerpay.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// ============================================
// EXCEPTIONS
// ============================================

open class ShegerPayException(message: String) : Exception(message)
class AuthenticationException(message: String) : ShegerPayException(message)
class ValidationException(message: String) : ShegerPayException(message)
class RateLimitException(message: String) : ShegerPayException(message)
class ServerException(code: Int, message: String) : ShegerPayException("[$code] $message")

// ============================================
// DATA MODELS
// ============================================

@Serializable
data class VerificationResult(
    val verified: Boolean = false,
    val valid: Boolean,
    val status: String,
    val provider: String? = null,
    @SerialName("transaction_id") val transactionId: String? = null,
    val amount: Double? = null,
    val reason: String? = null,
    val mode: String? = null,
    val payer: String? = null,
    val timestamp: String? = null
)

@Serializable
data class PaymentLink(
    val id: String,
    @SerialName("short_code") val shortCode: String,
    @SerialName("payment_url") val paymentUrl: String,
    @SerialName("qr_code_base64") val qrCodeBase64: String? = null,
    val status: String,
    val amount: Double? = null,
    val currency: String? = null,
    val title: String? = null
)

@Serializable
data class PaymentLinksResponse(
    val links: List<PaymentLink>
)

@Serializable
data class CryptoPaymentIntent(
    @SerialName("reference_id") val referenceId: String,
    @SerialName("wallet_address") val walletAddress: String,
    @SerialName("payment_amount") val paymentAmount: String,
    val currency: String,
    val network: String,
    @SerialName("qr_code") val qrCode: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
)

// ============================================
// PROVIDER ENUM
// ============================================

enum class PaymentProvider(val value: String) {
    CBE("cbe"),
    TELEBIRR("telebirr"),
    AWASH("awash"),
    BOA("boa"),
    EBIRR("ebirr");
    
    companion object {
        fun detect(transactionId: String): PaymentProvider {
            val upperId = transactionId.uppercase()
            val lowerId = transactionId.lowercase()
            return when {
                lowerId.contains("cs.bankofabyssinia.com/slip/?trx=") -> BOA
                upperId.startsWith("AW") -> AWASH
                upperId.startsWith("BOA") -> BOA
                upperId.startsWith("EB") -> EBIRR
                else -> TELEBIRR
            }
        }
    }
}

// ============================================
// MAIN CLIENT
// ============================================

class ShegerPay(
    private val apiKey: String,
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    val mode: String
    val isTestMode: Boolean
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    init {
        require(apiKey.isNotEmpty()) { "API key is required" }
        require(apiKey.startsWith("sk_test_") || apiKey.startsWith("sk_live_")) {
            "Invalid API key format. Use sk_test_* or sk_live_*"
        }
        mode = if (apiKey.startsWith("sk_test_")) "test" else "live"
        isTestMode = apiKey.startsWith("sk_test_")
    }
    
    // ============================================
    // ETHIOPIAN PAYMENT VERIFICATION
    // ============================================
    
    /**
     * Verify an Ethiopian bank payment
     * @param transactionId The transaction ID from the bank
     * @param amount Expected amount in ETB
     * @param provider Payment provider (auto-detected if null)
     * @param merchantName Merchant name for matching
     */
    suspend fun verify(
        transactionId: String,
        amount: Double,
        provider: PaymentProvider? = null,
        merchantName: String? = null,
        senderAccount: String? = null
    ): VerificationResult = withContext(Dispatchers.IO) {
        val detectedProvider = provider ?: if (
            transactionId.lowercase().contains("cs.bankofabyssinia.com/slip/?trx=")
        ) {
            PaymentProvider.BOA
        } else {
            throw ValidationException("provider is required for ambiguous transaction references. Pass provider explicitly or use quickVerify().")
        }
        
        val params = buildMap {
            put("provider", detectedProvider.value)
            put("transaction_id", transactionId)
            put("amount", amount)
            put("merchant_name", merchantName ?: "ShegerPay Verification")
            senderAccount?.takeIf { it.isNotBlank() }?.let { put("sender_account", it) }
        }
        
        post("/api/v1/verify", params)
    }
    
    /**
     * Quick verification with auto-detected provider
     */
    suspend fun quickVerify(
        transactionId: String,
        amount: Double? = null,
        expectedProvider: PaymentProvider? = null,
        senderAccount: String? = null
    ): VerificationResult = 
        withContext(Dispatchers.IO) {
            val params = buildMap {
                put("transaction_id", transactionId)
                amount?.let { put("amount", it) }
                expectedProvider?.let { put("expected_provider", it.value) }
                senderAccount?.takeIf { it.isNotBlank() }?.let { put("sender_account", it) }
            }
            post("/api/v1/quick-verify", params)
        }
    
    // ============================================
    // IMAGE VERIFICATION
    // ============================================

    /**
     * Verify payment from a receipt screenshot (base64 or URL)
     */
    suspend fun verifyImage(
        image: String,
        provider: String? = null,
        amount: Double? = null,
        merchantName: String = "ShegerPay Verification"
    ): VerificationResult {
        val params = mutableMapOf<String, Any>("image" to image, "merchant_name" to merchantName)
        provider?.let { params["provider"] = it }
        amount?.let { params["amount"] = it }
        return post("/api/v1/verify/image", params)
    }

    // ============================================
    // PROVIDERS
    // ============================================

    /**
     * Get list of supported payment providers and their status
     */
    suspend fun getProviders(): Map<String, Any> = withContext(Dispatchers.IO) {
        get("/api/v1/providers")
    }

    // ============================================
    // PAYMENT LINKS
    // ============================================
    
    /**
     * Create a shareable payment link
     */
    suspend fun createPaymentLink(
        title: String,
        amount: Double,
        currency: String = "ETB",
        description: String? = null,
        enableCbe: Boolean = true,
        enableTelebirr: Boolean = true,
        enableCrypto: Boolean = false
    ): PaymentLink = withContext(Dispatchers.IO) {
        val params = buildMap {
            put("title", title)
            put("amount", amount)
            put("currency", currency)
            put("enable_cbe", enableCbe)
            put("enable_telebirr", enableTelebirr)
            put("enable_crypto", enableCrypto)
            description?.let { put("description", it) }
        }
        post("/api/v1/payment-links", params)
    }
    
    /**
     * List all payment links
     */
    suspend fun listPaymentLinks(limit: Int = 50, offset: Int = 0): List<PaymentLink> = 
        withContext(Dispatchers.IO) {
            val response: PaymentLinksResponse = get("/api/v1/payment-links?limit=$limit&offset=$offset")
            response.links
        }
    
    // ============================================
    // CRYPTO PAYMENTS
    // ============================================
    
    /**
     * Generate a crypto payment intent
     */
    suspend fun generateCryptoIntent(
        amountUsd: Double,
        walletAddress: String,
        currency: String = "USDT",
        chain: String = "TRON"
    ): CryptoPaymentIntent = withContext(Dispatchers.IO) {
        val params = mapOf(
            "amount_usd" to amountUsd,
            "currency" to currency,
            "wallet_address" to walletAddress,
            "chain" to chain
        )
        post("/api/v1/crypto/generate-intent", params)
    }
    
    /**
     * Verify a crypto payment by reference ID
     */
    suspend fun verifyCrypto(referenceId: String): VerificationResult = 
        withContext(Dispatchers.IO) {
            val params = mapOf("reference_id" to referenceId)
            post("/api/v1/crypto/verify-reference", params)
        }
    
    // ============================================
    // HTTP METHODS
    // ============================================
    
    private inline fun <reified T> get(path: String): T {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .get()
            .addHeader("X-API-Key", apiKey)
            .addHeader("User-Agent", "ShegerPay-Android-SDK/$SDK_VERSION")
            .build()
        
        return execute(request)
    }
    
    private inline fun <reified T> post(path: String, params: Map<String, Any>): T {
        val jsonBody = json.encodeToString(
            kotlinx.serialization.serializer<Map<String, @kotlinx.serialization.Contextual Any>>(),
            params
        )
        
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("X-API-Key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "ShegerPay-Android-SDK/$SDK_VERSION")
            .build()
        
        return execute(request)
    }
    
    private inline fun <reified T> execute(request: Request): T {
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        
        return when (response.code) {
            in 200..299 -> json.decodeFromString(body)
            401 -> throw AuthenticationException("Invalid API key")
            429 -> throw RateLimitException("Rate limit exceeded. Please slow down.")
            else -> throw ServerException(response.code, body)
        }
    }
    
    companion object {
        private const val DEFAULT_BASE_URL = "https://api.shegerpay.com"
        private const val SDK_VERSION = "2.2.0"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        
        /**
         * Verify webhook signature (HMAC-SHA256)
         */
        fun verifyWebhookSignature(payload: String, signature: String, secret: String): Boolean {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
            val hash = mac.doFinal(payload.toByteArray())
            val expected = "sha256=" + hash.joinToString("") { "%02x".format(it) }
            return expected == signature
        }
    }
}

// ============================================
// JAVA INTEROP (Callback-based API)
// ============================================

interface ShegerPayCallback<T> {
    fun onSuccess(result: T)
    fun onError(error: ShegerPayException)
}

/**
 * Java-friendly wrapper for callback-based usage
 */
class ShegerPayJava(apiKey: String, baseUrl: String = "https://api.shegerpay.com") {
    private val client = ShegerPay(apiKey, baseUrl)
    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    
    fun verify(
        transactionId: String,
        amount: Double,
        callback: ShegerPayCallback<VerificationResult>
    ) {
        scope.launch {
            try {
                val result = client.quickVerify(transactionId, amount)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: ShegerPayException) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }
    
    private fun kotlinx.coroutines.CoroutineScope.launch(block: suspend () -> Unit) {
        kotlinx.coroutines.launch { block() }
    }
}
