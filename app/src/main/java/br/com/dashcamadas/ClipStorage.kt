package br.com.dashcamadas

import java.io.File
import java.security.MessageDigest

/** Circular cleanup is restricted to closed common clips; protected files live elsewhere. */
object ClipStorage {
    fun prune(common: File, active: File?, maxBytes: Long): Long {
        require(maxBytes >= 0)
        val files = common.listFiles()?.filter { it.isFile && it.extension == "mp4" && !File(it.parentFile,it.name+".lock").exists() }?.sortedBy { it.lastModified() } ?: emptyList()
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= maxBytes) break
            if (file.canonicalFile == active?.canonicalFile) continue
            val size = file.length()
            if (file.delete()) total -= size
        }
        return total
    }
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(65536)
            while (true) { val count=input.read(buffer); if(count<0) break; digest.update(buffer,0,count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
