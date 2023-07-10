package uk.co.armedpineapple.innoextract.service

import androidx.annotation.Keep
import java.io.File


@Keep
class DirectAccessFile(outputPath: String) : OutputFile {
    @Keep
    override val path: String = File(outputPath).absolutePath

    @Keep
    override val pathUtf8: ByteArray = path.toByteArray(Charsets.UTF_8)

    init {
        val pathComponents = outputPath.split("/").filterNot { it.isEmpty() }
        val directoryPath = pathComponents.dropLast(1).joinToString("/")
        File(directoryPath).mkdirs()
        File(path).createNewFile()
    }

    override fun close() {
    }
}