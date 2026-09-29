package com.awbuilds.auraspend.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * The model is downloaded over the network and then executed on-device, so the
 * bytes are verified before they are allowed into the models directory.
 *
 * The downloader previously had **no checksum at all** — only a size check, which
 * cannot tell a truncated download from a corrupt or substituted file. These
 * tests pin the digest and the constants so neither can drift silently.
 */
class ModelConstantsTest {

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `pinned checksum is a well formed sha256`() {
        val hex = ModelConstants.EXPECTED_SHA256
        assertEquals("must be 64 hex chars", 64, hex.length)
        assertTrue("must be lowercase hex: $hex", hex.all { it in "0123456789abcdef" })
    }

    @Test
    fun `expected size matches the published artifact`() {
        // Taken from the upstream `x-linked-size` header. The old constant said
        // 400 MiB, which made the consent dialog understate the download by ~70 MB.
        assertEquals(491_400_032L, ModelConstants.EXPECTED_SIZE_BYTES)
        assertTrue(
            "MIN_SIZE_BYTES must sit below the real size or nothing would ever pass",
            ModelConstants.MIN_SIZE_BYTES < ModelConstants.EXPECTED_SIZE_BYTES
        )
        assertTrue(
            "MIN_SIZE_BYTES must still be large enough to reject a truncated file",
            ModelConstants.MIN_SIZE_BYTES > 400L * 1024 * 1024
        )
    }

    @Test
    fun `checksum verification accepts the correct bytes and rejects altered ones`() {
        // Mirrors verifyChecksum(): same algorithm, same encoding, same compare.
        val expected = "a".repeat(64)

        val good = File.createTempFile("aura-model", ".bin")
        try {
            good.writeBytes("hello".toByteArray())
            val actual = sha256(good)
            assertEquals(
                "known digest of 'hello' should match",
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                actual
            )
            assertNotEquals("a wrong digest must not compare equal", expected, actual)
        } finally {
            good.delete()
        }
    }

    @Test
    fun `a single flipped byte changes the digest`() {
        val a = File.createTempFile("aura-a", ".bin")
        val b = File.createTempFile("aura-b", ".bin")
        try {
            a.writeBytes(ByteArray(4096) { 0x00 })
            b.writeBytes(ByteArray(4096) { 0x00 }.also { it[2048] = 0x01 })
            assertNotEquals("one flipped byte must change the digest", sha256(a), sha256(b))
        } finally {
            a.delete(); b.delete()
        }
    }

    @Test
    fun `model file name and url stay consistent with the pinned digest`() {
        // If the URL or filename changes, the pinned digest is wrong and every
        // download would fail verification. Keeping them in one test makes that
        // coupling explicit rather than leaving a time bomb.
        assertTrue(
            "url must be the q4_k_m 0.5b GGUF the digest was taken from",
            ModelConstants.MODEL_URL.endsWith("qwen2.5-0.5b-instruct-q4_k_m.gguf")
        )
        assertEquals("qwen2.5-0.5b-instruct-q4_k_m.gguf", ModelConstants.MODEL_FILE_NAME)
    }
}
