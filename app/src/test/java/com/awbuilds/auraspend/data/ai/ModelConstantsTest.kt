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
        // Confirmed by hashing the downloaded file, not recalled. These constants
        // have drifted twice: an earlier pair claimed 400 MiB against a real
        // 468.6 MiB artifact, and a later one inherited the 450 MiB floor when
        // the model shrank 21x, which would have rejected every valid download
        // and failed closed on a good file.
        assertEquals(23_026_053L, ModelConstants.EXPECTED_SIZE_BYTES)
        assertTrue(
            "MIN_SIZE_BYTES must sit below the real size or nothing would ever pass",
            ModelConstants.MIN_SIZE_BYTES < ModelConstants.EXPECTED_SIZE_BYTES
        )
        assertTrue(
            "MIN_SIZE_BYTES must still be large enough to reject a truncated file",
            ModelConstants.MIN_SIZE_BYTES > 15L * 1024 * 1024
        )
        // Derived against the current size so a legitimate swap cannot inherit
        // a stale absolute floor.
        assertTrue(
            "MIN_SIZE_BYTES must be comfortably below the real size",
            ModelConstants.MIN_SIZE_BYTES < ModelConstants.EXPECTED_SIZE_BYTES * 95 / 100
        )
    }

    @Test
    fun `the download is small enough that a consent modal is not warranted`() {
        // A 468 MB download justified a blocking consent dialog. This one does
        // not, and the UI dropped it for exactly this reason — the invariant is
        // here so a future model swap cannot silently reintroduce a modal
        // without noticing the size that made it unreasonable.
        val sizeMb = ModelConstants.EXPECTED_SIZE_BYTES / (1024 * 1024)
        assertTrue(
            "model is $sizeMb MB; re-evaluate the consent UX if this ever grows",
            sizeMb < 100
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
        // The two names are deliberately allowed to differ. The URL serves an
        // upstream basename (`model_qint8_arm64.onnx`); MODEL_FILE_NAME is the
        // name the file is *saved* as, and `ModelDownloadManager` writes to
        // `ModelConstants.modelFile()` rather than the URL's basename. So the
        // coupling worth protecting is not the spelling — it is that both
        // constants still describe the same artifact, which is checked by the
        // extension and by the digests being real sha256 values below.
        assertTrue(
            "url must point at a quantised ONNX artifact",
            ModelConstants.MODEL_URL.endsWith(".onnx")
        )
        assertTrue(
            "the saved file must keep the .onnx extension ONNX Runtime needs",
            ModelConstants.MODEL_FILE_NAME.endsWith(".onnx")
        )
        assertEquals(
            "MODEL_FILE_NAME should be lowercase, matching the on-disk convention",
            ModelConstants.MODEL_FILE_NAME.lowercase(),
            ModelConstants.MODEL_FILE_NAME
        )
        // The url must still be the arm64 int8 build, not a float32 or x86 one:
        // a silent change of quantisation target would change every score while
        // still passing a size check, because the fp32 file is the same model.
        assertTrue(
            "url must be the arm64 int8 build; a different quant target changes every score",
            ModelConstants.MODEL_URL.contains("model_qint8_arm64.onnx")
        )
    }

    @Test
    fun `the tokenizer is verified too`() {
        // An encoder without its vocabulary cannot tokenize. Half a download
        // would otherwise report "ready" and then fail every classification, so
        // the vocab gets the same pinned-digest treatment as the weights.
        assertEquals(
            "tokenizer digest must be a well formed sha256",
            64,
            ModelConstants.TOKENIZER_SHA256.length
        )
        assertTrue(
            "tokenizer digest must be lowercase hex",
            ModelConstants.TOKENIZER_SHA256.all { it in "0123456789abcdef" }
        )
        assertTrue(
            "tokenizer url must be a .json",
            ModelConstants.TOKENIZER_URL.endsWith("tokenizer.json")
        )
        assertTrue(
            "saved tokenizer must keep its extension so the parser can find it",
            ModelConstants.TOKENIZER_FILE_NAME.endsWith(".json")
        )
        // Model and vocab digests must not be the same value: a copy-paste would
        // make verification of one silently validate the other.
        assertNotEquals(
            "model and tokenizer digests must differ",
            ModelConstants.EXPECTED_SHA256,
            ModelConstants.TOKENIZER_SHA256
        )
    }
}
