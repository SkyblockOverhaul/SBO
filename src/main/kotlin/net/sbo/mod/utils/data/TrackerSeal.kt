package net.sbo.mod.utils.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * A sealed copy of a Diana tracker, saved in its own file. Changes made outside of SBO are undone with it when the
 * file loads. A tracker whose seal was changed as well can't be brought back, it is marked as edited until it is reset.
 */
internal object TrackerSeal {
    private const val FIELD = "seal"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    // Needs to stay the same across versions, otherwise every update would mark the trackers as edited
    private const val KEY = "sbo-tracker-seal:373820cd02c880e22a50bf2758e4b559b0954fc957c214c7"

    private val key = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(KEY.toByteArray()), "AES")
    private val random = SecureRandom()

    /** The tracker as saved, with a seal of exactly the values that get written. */
    internal fun sealed(tree: JsonObject, fileName: String): JsonObject {
        tree.remove(FIELD)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val box = cipher(Cipher.ENCRYPT_MODE, iv, fileName).doFinal(pack(tree.toString()))
        tree.addProperty(FIELD, Base64.getEncoder().encodeToString(iv + box))
        return tree
    }

    internal fun isSealed(tree: JsonObject): Boolean = tree.has(FIELD)

    /** The tracker the seal was made from, null when the seal itself was changed. */
    internal fun original(tree: JsonObject, fileName: String): JsonObject? = runCatching {
        val raw = Base64.getDecoder().decode(tree.get(FIELD).asString)
        val plain = cipher(Cipher.DECRYPT_MODE, raw.copyOf(IV_BYTES), fileName).doFinal(raw, IV_BYTES, raw.size - IV_BYTES)
        JsonParser.parseString(unpack(plain)).asJsonObject
    }.getOrNull()

    /** True when the file holds other values than the ones that were sealed. */
    internal fun changed(tree: JsonObject, original: JsonObject): Boolean = tree.deepCopy().apply { remove(FIELD) } != original

    private fun cipher(mode: Int, iv: ByteArray, fileName: String): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(mode, key, GCMParameterSpec(TAG_BITS, iv))
        // The alpha copy has the same file name, so it may hold the main data
        updateAAD(fileName.substringAfterLast('/').toByteArray())
    }

    private fun pack(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }

    private fun unpack(bytes: ByteArray): String = GZIPInputStream(bytes.inputStream()).use { it.readBytes() }.decodeToString()
}
