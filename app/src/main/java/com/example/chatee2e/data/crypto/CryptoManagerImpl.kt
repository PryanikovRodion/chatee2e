package com.example.chatee2e.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManagerImpl @Inject constructor() : CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    companion object {
        private const val ALIAS_IDENTITY = "chatee2e_identity_rsa"
        private const val ALIAS_MASTER = "chatee2e_db_master_key"
        private const val AUTH_VALIDITY_DURATION_SECONDS = 300
    }

    override fun ensureKeyPairExists() {
        if (!keyStore.containsAlias(ALIAS_IDENTITY)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                ALIAS_IDENTITY,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build()

            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
        }
    }

    override fun getMyPublicKeyBase64(): String {
        ensureKeyPairExists()

        val entry = keyStore.getEntry(ALIAS_IDENTITY, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Keystore keys not found even after generation")

        return Base64.encodeToString(entry.certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    override fun getPublicKeyFromBase64(base64: String): PublicKey {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(bytes)
        val factory = KeyFactory.getInstance("RSA")
        return factory.generatePublic(spec)
    }

    override fun encryptAesKeyWithRsa(aesKey: SecretKey, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(aesKey.encoded)
    }

    override fun decryptAesKeyWithRsa(encryptedKey: ByteArray): SecretKey {
        val privateKey = (keyStore.getEntry(ALIAS_IDENTITY, null) as KeyStore.PrivateKeyEntry).privateKey
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decryptedBytes = cipher.doFinal(encryptedKey)
        return SecretKeySpec(decryptedBytes, "AES")
    }

    override fun generateRandomAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    override fun encryptMsg(text: String, aesKey: SecretKey): CryptoResult {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
        return CryptoResult(encryptedBytes, iv)
    }

    override fun decryptMsg(encryptedData: ByteArray, iv: ByteArray, aesKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec)
        val decodedBytes = cipher.doFinal(encryptedData)
        return String(decodedBytes, StandardCharsets.UTF_8)
    }

    override fun createMasterKey() {
        if (!keyStore.containsAlias(ALIAS_MASTER)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                ALIAS_MASTER,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(AUTH_VALIDITY_DURATION_SECONDS)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    override fun generateAndEncryptDatabasePassphrase(): Pair<ByteArray, CryptoResult> {
        createMasterKey()
        val rawPassphrase = ByteArray(32)
        java.security.SecureRandom().nextBytes(rawPassphrase)
        val masterKey = keyStore.getKey(ALIAS_MASTER, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val encryptedBytes = cipher.doFinal(rawPassphrase)
        val iv = cipher.iv
        return Pair(rawPassphrase, CryptoResult(encryptedBytes, iv))
    }
    override fun decryptDatabasePassphrase(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val masterKey = keyStore.getKey(ALIAS_MASTER, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
        return cipher.doFinal(encryptedData)
    }
}