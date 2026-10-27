package app.trackar.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import app.trackar.BuildConfig
import app.trackar.core.common.di.Dispatcher
import app.trackar.core.common.di.TrackarDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Self-update for the test APK distributed through the rolling GitHub release.
 * Disabled in release builds: the install permission only exists in the debug manifest
 * and store builds are updated by Google Play.
 */
@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(TrackarDispatcher.IO) private val ioDispatcher: CoroutineDispatcher
) {
    val isEnabled: Boolean get() = BuildConfig.DEBUG

    /** Newer build, or null when up to date, offline or disabled. */
    suspend fun check(): UpdateManifest? = withContext(ioDispatcher) {
        if (!isEnabled) return@withContext null
        val body = runCatching { fetch(MANIFEST_URL) }.getOrNull() ?: return@withContext null
        parseNewerManifest(body, BuildConfig.VERSION_CODE)
    }

    /** Android 8+ requires a per-app grant to install packages. */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /**
     * Downloads the APK (progress 0..1, or -1 when the size is unknown) and opens the system installer.
     * Returns false if the download or the hand-off failed.
     */
    suspend fun downloadAndInstall(manifest: UpdateManifest, onProgress: (Float) -> Unit): Boolean {
        val apk = withContext(ioDispatcher) {
            runCatching { download(manifest.apkUrl, onProgress) }.getOrNull()
        } ?: return false

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    private fun fetch(url: String): String = open(url, READ_TIMEOUT_MS).let { conn ->
        try {
            conn.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            conn.disconnect()
        }
    }

    private fun download(url: String, onProgress: (Float) -> Unit): File {
        // Fixed name: a previous partial download is overwritten instead of piling up.
        val out = File(File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }, "trackar-update.apk")
        val conn = open(url, DOWNLOAD_TIMEOUT_MS)
        try {
            val total = conn.contentLengthLong
            var read = 0L
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        onProgress(if (total > 0) read.toFloat() / total else -1f)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return out
    }

    private fun open(url: String, readTimeoutMs: Int) = (URL(url).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = readTimeoutMs
        useCaches = false
    }

    private companion object {
        const val MANIFEST_URL = "https://github.com/tenoriodouglas/trackar/releases/download/apk-latest/latest.json"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val UPDATES_DIR = "updates"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 15_000
        const val DOWNLOAD_TIMEOUT_MS = 60_000
        const val BUFFER_SIZE = 64 * 1024
    }
}
