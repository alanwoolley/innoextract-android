package uk.co.armedpineapple.innoextract.service

import android.content.ContentResolver
import androidx.annotation.Keep
import com.lazygeniouz.dfc.file.DocumentFileCompat
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.system.measureTimeMillis

@Keep
class TemporaryExtractedFile constructor(
    private val documentRoot: DocumentFileCompat,
    temporaryDirectory: File,
    private val outputPath: String,
    private val contentResolver: ContentResolver,
    private val cache : DocumentFileCache
) : AutoCloseable, AnkoLogger {

    private val file: File = File.createTempFile("tmp", null, temporaryDirectory);

    @Keep
    val path: String = file.absolutePath;

    @Keep
    val pathUtf8: ByteArray = path.toByteArray(Charsets.UTF_8);

    init {
        file.deleteOnExit()
    }

    override fun close() {
        try {
            copyToOutputFile()
        } catch (e: Exception) {
            // Don't allow this to throw.
            error("Error copying temporary file to target.", e)
        } finally {
            try {
                file.delete()
            } catch (e: Exception) {
                // Don't allow this to throw
                error("Unable to delete temporary file.", e)
            }
        }
    }

    private fun copyToOutputFile() {
        val cache =
        info("Writing to target: $path")
        var outputFile: DocumentFileCompat
        val creatingTime = measureTimeMillis { outputFile = createDocumentFile() }
        info("Creating document took $creatingTime ms")

        var outputStream: OutputStream
        val openTime = measureTimeMillis {
            outputStream = contentResolver.openOutputStream(outputFile.uri)
                ?: throw IOException("Couldn't open output file for writing.")
        }
        info("Opening document output stream took $openTime ms")

        val writeTime = measureTimeMillis {
            outputStream.use { output: OutputStream ->
                val inputStream = FileInputStream(file)
                inputStream.use { input: FileInputStream ->
                    input.copyTo(output, 64 * 1024)
                }
            }
        }

        info("Writing document output stream took $writeTime ms")

    }

    private fun createDocumentFile(): DocumentFileCompat {
        val pathComponents = outputPath.split("/").filterNot { it.isEmpty() }
        val directoryPath = pathComponents.dropLast(1).joinToString("/")
        val documentRoot = cache.getDirectory(directoryPath)
//
//        pathComponents.take(pathComponents.size - 1).forEach { dirComponent ->
//            val newRoot = documentRoot.listFiles()
//               .firstOrNull { it.isDirectory() && it.name.equals(dirComponent) }
//            if (newRoot == null) {
//                val newDir = documentRoot.createDirectory(dirComponent)
//                    ?: throw IOException("Could not create tree.")
//                documentRoot = newDir;
//            }
//        }

        return documentRoot.createFile("*/*", pathComponents.last())
            ?: throw IOException("Could not create file.")
    }
}