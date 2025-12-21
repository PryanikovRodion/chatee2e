package com.example.chatee2e.data.crypto

import java.security.PublicKey
import javax.crypto.SecretKey

interface CryptoManager {
    fun ensureKeyPairExists()
    fun getMyPublicKeyBase64(): String
    fun getPublicKeyFromBase64(base64: String): PublicKey
    fun encryptAesKeyWithRsa(aesKey: SecretKey, publicKey: PublicKey): ByteArray
    fun decryptAesKeyWithRsa(encryptedKey: ByteArray): SecretKey
    fun generateRandomAesKey(): SecretKey
    fun encryptMsg(text: String, aesKey: SecretKey): CryptoResult
    fun decryptMsg(encryptedData: ByteArray, iv: ByteArray, aesKey: SecretKey): String
    fun createMasterKey()
    fun generateAndEncryptDatabasePassphrase(): Pair<ByteArray, CryptoResult>
    fun decryptDatabasePassphrase(encryptedData: ByteArray, iv: ByteArray): ByteArray
    fun deleteKeys()
}