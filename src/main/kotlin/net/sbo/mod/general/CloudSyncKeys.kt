package net.sbo.mod.general

import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.HomeStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CloudSyncKeys {
    const val MIN_PASSWORD_LENGTH = 8
    private const val ITERATIONS = 600_000

    // uuid -> base64 key
    private val store = HomeStore("cloud-keys.json")

    fun key(uuid: String = Player.accountUuid()): ByteArray? =
        store[uuid]?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() }

    fun hasKey(): Boolean = key() != null

    fun setPassword(password: CharArray, uuid: String = Player.accountUuid()) {
        store[uuid] = Base64.getEncoder().encodeToString(derive(password, uuid))
    }

    fun removeKey(uuid: String = Player.accountUuid()) {
        store[uuid] = null
    }

    private fun derive(password: CharArray, uuid: String): ByteArray {
        val spec = PBEKeySpec(password, "sbo-cloud-sync:$uuid".toByteArray(), ITERATIONS, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun sign(key: ByteArray, message: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(message))
    }

    fun verify(key: ByteArray, message: ByteArray, signature: String): Boolean {
        val expected = Base64.getDecoder().decode(sign(key, message))
        val actual = runCatching { Base64.getDecoder().decode(signature) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
    }
}
