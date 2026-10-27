package app.trackar.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateManifestTest {

    private val url = "https://github.com/tenoriodouglas/trackar/releases/download/apk-latest/trackar-latest-debug.apk"

    @Test
    fun `newer build is returned`() {
        val manifest =
            parseNewerManifest("""{"versionCode":12,"versionName":"0.1.0","apkUrl":"$url","notes":"Fix"}""", 10)
        assertEquals(12, manifest?.versionCode)
        assertEquals("Fix", manifest?.notes)
    }

    @Test
    fun `same or older build is ignored`() {
        assertNull(parseNewerManifest("""{"versionCode":10,"apkUrl":"$url"}""", 10))
        assertNull(parseNewerManifest("""{"versionCode":9,"apkUrl":"$url"}""", 10))
    }

    @Test
    fun `invalid or insecure manifests are ignored`() {
        assertNull(parseNewerManifest("not json", 1))
        assertNull(parseNewerManifest("""{"versionCode":12,"apkUrl":"http://insecure/app.apk"}""", 1))
        assertNull(parseNewerManifest("""{"versionCode":12}""", 1))
    }
}
