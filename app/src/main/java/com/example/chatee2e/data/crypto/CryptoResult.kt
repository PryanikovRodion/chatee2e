package com.example.chatee2e.data.crypto

data class CryptoResult(
    val encryptedData: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CryptoResult
        if (!encryptedData.contentEquals(other.encryptedData)) return false
        if (!iv.contentEquals(other.iv)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = encryptedData.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}