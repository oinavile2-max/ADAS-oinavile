package br.com.dashcamadas

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class ClipStorageTest {
    @Test fun hashMatchesKnownVector() {
        val dir=Files.createTempDirectory("hash").toFile()
        try { val f=File(dir,"evidence.mp4").apply {writeText("abc")}
            assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",ClipStorage.sha256(f))
        } finally {dir.deleteRecursively()}
    }
    @Test fun pruningKeepsActiveAndProtectedFiles() {
        val dir=Files.createTempDirectory("clips").toFile()
        try {
            val common=File(dir,"clips").apply {mkdirs()}
            val protected=File(dir,"protected").apply {mkdirs()}
            val old=File(common,"old.mp4").apply {writeBytes(ByteArray(10));setLastModified(1000)}
            val active=File(common,"active.mp4").apply {writeBytes(ByteArray(10));setLastModified(2000)}
            val evidence=File(protected,"evidence.mp4").apply {writeBytes(ByteArray(10))}
            val metadata=File(common,"metadata.json").apply {writeText("keep")}
            assertEquals(10L,ClipStorage.prune(common,active,0))
            assertFalse(old.exists());assertTrue(active.exists());assertTrue(evidence.exists());assertTrue(metadata.exists())
        } finally {dir.deleteRecursively()}
    }
    @Test fun lockMarkerProtectsClipWhenMoveFails() {
        val dir=Files.createTempDirectory("locked").toFile()
        try {
            val clip=File(dir,"evidence.mp4").apply{writeBytes(ByteArray(10))}
            File(dir,"evidence.mp4.lock").createNewFile()
            ClipStorage.prune(dir,null,0)
            assertTrue(clip.exists())
        } finally {dir.deleteRecursively()}
    }
    @Test fun oldestClosedClipIsRemovedFirst() {
        val dir=Files.createTempDirectory("retention").toFile()
        try {
            val old=File(dir,"old.mp4").apply {writeBytes(ByteArray(10));setLastModified(1000)}
            val recent=File(dir,"recent.mp4").apply {writeBytes(ByteArray(10));setLastModified(2000)}
            assertEquals(10L,ClipStorage.prune(dir,null,10));assertFalse(old.exists());assertTrue(recent.exists())
        } finally {dir.deleteRecursively()}
    }
}
