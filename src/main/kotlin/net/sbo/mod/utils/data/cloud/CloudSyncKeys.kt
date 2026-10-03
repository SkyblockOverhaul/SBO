package net.sbo.mod.utils.data.cloud

import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.LocalStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CloudSyncKeys {
    const val MIN_SIGN_KEY_LENGTH = 3
    private const val ITERATIONS = 600_000

    // No 0/o, 1/l/i, so a key can be copied by hand
    private const val ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"
    private const val GROUPS = 4
    private const val GROUP_LENGTH = 4

    // uuid -> base64 key
    private val store = LocalStore("cloud-auth.json")
    // uuid -> sign key as typed, so the window can show it
    private val texts = LocalStore("cloud-sign-key.json")

    fun key(uuid: String = Player.accountUuid()): ByteArray? =
        store[uuid]?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() }

    fun hasKey(): Boolean = key() != null

    // null if no key is set or it was set before the text was kept
    fun text(uuid: String = Player.accountUuid()): String? = texts[uuid]

    // Slow (key derivation), call off the render thread
    fun setSignKey(signKey: String, uuid: String = Player.accountUuid()) {
        store[uuid] = Base64.getEncoder().encodeToString(derive(signKey.toCharArray(), uuid))
        texts[uuid] = signKey
    }

    fun removeKey(uuid: String = Player.accountUuid()) {
        store[uuid] = null
        texts[uuid] = null
    }

    fun generate(): String {
        val random = SecureRandom()
        return (1..GROUPS).joinToString("-") {
            (1..GROUP_LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        }
    }

    private fun derive(signKey: CharArray, uuid: String): ByteArray {
        val spec = PBEKeySpec(signKey, "sbo-cloud-sync:$uuid".toByteArray(), ITERATIONS, 256)
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
