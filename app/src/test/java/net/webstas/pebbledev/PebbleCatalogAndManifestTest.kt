package net.webstas.pebbledev

import net.webstas.pebbledev.data.ManifestVersionHelper
import net.webstas.pebbledev.data.PebbleProjectCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PebbleCatalogAndManifestTest {

    @Test
    fun defaultCatalogContainsAllFourteenPebbleProjects() {
        val projects = PebbleProjectCatalog.defaultProjects
        assertEquals(14, projects.size)
        assertTrue(projects.any { it.repoName == "Pebble-SleepSense" })
        assertTrue(projects.any { it.repoName == "PebbleSuperProductivity" })
        assertTrue(projects.any { it.repoName == "PebbleUTA" })
        assertTrue(projects.any { it.repoName == "xDrip-Pebble-E" })
    }

    @Test
    fun extractAndBumpPackageJsonVersion() {
        val samplePackageJson = """
            {
              "name": "pebble-super-productivity",
              "author": "BigWebstas",
              "version": "1.4.2",
              "pebble": {
                "sdkVersion": "3",
                "targetPlatforms": ["aplite", "basalt", "chalk", "diorite"]
              }
            }
        """.trimIndent()

        val extracted = ManifestVersionHelper.extractVersion(samplePackageJson)
        assertEquals("1.4.2", extracted)

        val nextPatch = ManifestVersionHelper.bumpPatchVersion(extracted!!)
        assertEquals("1.4.3", nextPatch)

        val nextMinor = ManifestVersionHelper.bumpMinorVersion(extracted)
        assertEquals("1.5.0", nextMinor)

        val updatedJson = ManifestVersionHelper.updateVersionInManifest(samplePackageJson, nextPatch)
        assertEquals("1.4.3", ManifestVersionHelper.extractVersion(updatedJson))
    }

    @Test
    fun extractAndBumpLegacyAppInfoJson() {
        val sampleAppInfo = """
            {
              "uuid": "12345678-1234-1234-1234-1234567890ab",
              "shortName": "Cardia",
              "versionLabel": "2.1",
              "watchapp": { "watchface": false }
            }
        """.trimIndent()

        val extracted = ManifestVersionHelper.extractVersion(sampleAppInfo)
        assertNotNull(extracted)
        assertEquals("2.1", extracted)

        val nextPatch = ManifestVersionHelper.bumpPatchVersion(extracted!!)
        assertEquals("2.2", nextPatch)

        val updated = ManifestVersionHelper.updateVersionInManifest(sampleAppInfo, "2.2")
        assertEquals("2.2", ManifestVersionHelper.extractVersion(updated))
    }
}
