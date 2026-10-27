package app.trackar.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `latest.json` published by CI next to the APK in the rolling `apk-latest` release. */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String = "",
    val apkUrl: String,
    val notes: String = ""
)

private val json = Json { ignoreUnknownKeys = true }

/** Returns the manifest only when it describes a newer, downloadable build. */
internal fun parseNewerManifest(body: String, currentVersionCode: Int): UpdateManifest? {
    val manifest = runCatching { json.decodeFromString<UpdateManifest>(body) }.getOrNull() ?: return null
    return manifest.takeIf { it.versionCode > currentVersionCode && it.apkUrl.startsWith("https://") }
}
