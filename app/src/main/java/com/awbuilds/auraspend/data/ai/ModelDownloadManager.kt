package com.awbuilds.auraspend.data.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * App-wide state of the on-device AI model.
 */
sealed class AiModelState {
    /** Consent has not been given, or the model is not present. */
    object NotDownloaded : AiModelState()

    /** Prompt visible to the user; download has not started. */
    object Consenting : AiModelState()

    data class Downloading(val progress: Float) : AiModelState()

    object Ready : AiModelState()

    object Failed : AiModelState()
}

/**
 * Downloads the Qwen GGUF to internal storage in the background (after explicit user consent) and
 * tracks progress so the UI can reflect it. Resume/cancel are supported via HTTP Range requests.
 */
object ModelDownloadManager {

    private const val TAG = "ModelDownloadManager"
    private const val PREF_FILE = "auraspend_prefs"
    private const val PREF_CONSENT = ModelConstants.PREF_CONSENT_GIVEN

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _state = MutableStateFlow<AiModelState>(AiModelState.NotDownloaded)
    val state: StateFlow<AiModelState> = _state.asStateFlow()

    private val cancelRequested = AtomicBoolean(false)
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** True when the user has consented to download the model. */
    fun hasConsent(context: Context): Boolean =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(PREF_CONSENT, false)

    fun markConsentGiven(context: Context) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_CONSENT, true).apply()
    }

    fun hasModel(context: Context): Boolean = ModelConstants.isDownloaded(context)

    /** Refreshes [state] based on whether the model file exists. Call after app start / delete. */
    fun sync(context: Context) {
        // Don't clobber an in-flight download (e.g. onResume during a background download).
        if (_state.value is AiModelState.Downloading) return
        _state.value = when {
            hasModel(context) -> AiModelState.Ready
            else -> AiModelState.NotDownloaded
        }
    }

    fun start(context: Context) {
        if (_state.value is AiModelState.Downloading) return
        if (hasModel(context)) {
            _state.value = AiModelState.Ready
            return
        }
        _state.value = AiModelState.Downloading(0f)
        cancelRequested.set(false)
        publishDownloadProgress(context, 0f)

        val target = ModelConstants.modelFile(context)
        scope.launch {
            try {
                download(context, target)
                if (ModelConstants.isDownloaded(context)) {
                    _state.value = AiModelState.Ready
                    publishDownloadComplete(context)
                } else {
                    _state.value = AiModelState.Failed
                    publishDownloadFailed(context)
                }
            } catch (_: CancelledDownloadException) {
                // User cancelled - not a failure. Keep the partial file so a later start() resumes.
                cancelRequested.set(false)
                cancelDownloadNotification(context)
                _state.value = if (hasModel(context)) AiModelState.Ready else AiModelState.NotDownloaded
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _state.value = AiModelState.Failed
                publishDownloadFailed(context)
            }
        }
    }

    fun cancel() {
        cancelRequested.set(true)
    }

    fun deleteModel(context: Context) {
        cancelRequested.set(true)
        cancelDownloadNotification(context)
        ModelConstants.modelFile(context).delete()
        scope.launch { _state.value = AiModelState.NotDownloaded }
    }

    /**
     * Downloads [target], resuming from any partially downloaded file using a Range header so an
     * interrupted download can continue rather than restarting from zero.
     */
    private suspend fun download(context: Context, target: File) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, target.name + ".part")

        var downloadedBytes = partial.length()
        var lastPublishedProgress = 0f
        val request = Request.Builder()
            .url(ModelConstants.MODEL_URL)
            .header("Range", "bytes=$downloadedBytes-")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("Server responded ${response.code}")
            }

            val body = response.body ?: throw IOException("Empty response body")

            // If we asked for a resume but the server ignored Range (returned 200), the existing
            // partial starts at the wrong offset and would corrupt the file — restart from scratch.
            if (downloadedBytes > 0 && response.code == 200) {
                downloadedBytes = 0
                partial.delete()
                partial.createNewFile()
            }

            val totalLength = if (response.code == 206) {
                parseContentRange(response.header("Content-Range"))
            } else {
                body.contentLength().takeIf { it > 0 }
            }

            // Append mode so a resumed download continues from the existing .part file.
            val stream = java.io.FileOutputStream(partial, true)

            body.byteStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                val sourceLength = (totalLength ?: 0L)
                while (!cancelRequested.get()) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    stream.write(buffer, 0, read)
                    downloadedBytes += read

                    if (sourceLength > 0) {
                        val progress = (downloadedBytes.toDouble() / sourceLength).toFloat().coerceIn(0f, 1f)
                        _state.value = AiModelState.Downloading(progress)
                        // Throttle the notification: publish at most every ~2% of progress.
                        // Don't show "Downloading 100%" - let the final success notification handle it.
                        if (progress < 1f && progress - lastPublishedProgress >= PROGRESS_STEP) {
                            lastPublishedProgress = progress
                            publishDownloadProgress(context, progress)
                        }
                    }
                }
            }
            stream.flush()
            stream.close()

            if (cancelRequested.get()) {
                throw CancelledDownloadException()
            }
        }

        // Finalise the download: validate, then move the part file into place.
        if (partial.length() < ModelConstants.MIN_SIZE_BYTES) {
            throw IOException("Downloaded file is too small: ${partial.length()} bytes")
        }
        // Size alone cannot distinguish a truncated download from a corrupt or
        // substituted file, and this file is then executed on-device. Hash it
        // before it is allowed anywhere near the models directory.
        verifyChecksum(partial)
        if (target.exists()) target.delete()
        if (!partial.renameTo(target)) {
            // Cross-filesystem fallback: stream the file instead of loading 400 MB into RAM.
            partial.inputStream().use { input -> target.outputStream().use { out -> input.copyTo(out) } }
            partial.delete()
        }
    }

    /** Streams the file through SHA-256 and compares it with the pinned digest. */
    private fun verifyChecksum(file: File) {
        val expected = ModelConstants.EXPECTED_SHA256.lowercase()
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                if (cancelRequested.get()) throw CancelledDownloadException()
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != expected) {
            // Delete rather than keep: a file that failed verification must never
            // be retried as if it were a resumable partial.
            file.delete()
            throw IOException("Model checksum mismatch.\n  expected $expected\n  actual   $actual")
        }
    }

    private fun parseContentRange(contentRange: String?): Long? {
        if (contentRange == null) return null
        return Regex("bytes\\s+\\d+-(\\d+)/(\\d+)")
            .find(contentRange)
            ?.let { m ->
                val end = m.groupValues[1].toLongOrNull()
                val total = m.groupValues[2].toLongOrNull()
                total ?: end
            }
            ?: Regex("bytes\\s+\\d+-(\\d+)").find(contentRange)?.groupValues?.get(1)?.toLongOrNull()
    }


    private val notificationId = 2002
    private const val CHANNEL_ID = "model_download"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Local AI download",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Progress of the on-device AI model download" }
            manager.createNotificationChannel(channel)
        }
    }

    private fun canNotify(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun publishDownloadProgress(context: Context, progress: Float) {
        if (!canNotify(context)) return
        runCatching {
            ensureChannel(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading ${ModelConstants.MODEL_LABEL}")
                .setContentText("${(progress * 100).toInt()}% — works fully offline once ready")
                .setProgress(100, (progress * 100).toInt(), progress == 0f)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun publishDownloadComplete(context: Context) {
        if (!canNotify(context)) return
        runCatching {
            ensureChannel(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Local AI ready")
                .setContentText("The AI model has been downloaded. Transactions will now be automatically categorized.")
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun publishDownloadFailed(context: Context) {
        if (!canNotify(context)) return
        runCatching {
            ensureChannel(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Local AI download failed")
                .setContentText("Check your connection and try again from Settings.")
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun cancelDownloadNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    private class CancelledDownloadException : Exception()

    /** Publish a progress notification at most every 2% of the download. */
    private const val PROGRESS_STEP = 0.02f
}
