package com.ricardo.aria

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.UUID

data class PearlDeviceIdentityInfo(
    val deviceId: String,
    val deviceName: String,
    val publicKey: String
)

object PearlDeviceIdentity {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "pearl_client_identity_rsa"
    private const val PREFS_NAME = "pearl_device_identity"
    private const val KEY_DEVICE_ID = "device_id"

    fun get(context: Context): PearlDeviceIdentityInfo {
        ensureKeyPair()
        return PearlDeviceIdentityInfo(
            deviceId = getOrCreateDeviceId(context),
            deviceName = "PEARL Android Client",
            publicKey = publicKeyBase64()
        )
    }

    fun sign(payload: String): String {
        ensureKeyPair()
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(payload.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }

    private fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        if (existing.isNotBlank()) return existing

        val created = "pearl-android-${UUID.randomUUID()}"
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    private fun publicKeyBase64(): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey.encoded
        return Base64.encodeToString(publicKey, Base64.NO_WRAP)
    }

    private fun ensureKeyPair() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) return

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setUserAuthenticationRequired(false)
            .build()
        generator.initialize(spec)
        generator.generateKeyPair()
    }
}
