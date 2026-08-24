package uk.co.armedpineapple.innoextract.service

import android.content.ContentResolver
import android.util.Log
import androidx.annotation.Keep
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.system.measureTimeMillis

@Keep
class TemporaryExtractedFile(
    temporaryDirectory: File,
    private val outputPath: String,
    private val contentResolver: ContentResolver,
    private val cache: DocumentFileCache
) : OutputFile, AutoCloseable {

    private val file: File = File.createTempFile("tmp", null, temporaryDirectory)

    @Keep
    override val path: String = file.absolutePath

    @Keep
    override val pathUtf8: ByteArray = path.toByteArray(Charsets.UTF_8)

    init {
        file.deleteOnExit()
    }

    override fun close() {
        try {
            copyToOutputFile()
        } catch (e: Exception) {
            // Don't allow this to throw.
            Log.e(LOG_TAG, "Error copying temporary file to target.", e)
        } finally {
            try {
                file.delete()
            } catch (e: Exception) {
                // Don't allow this to throw
                Log.e(LOG_TAG, "Unable to delete temporary file.", e)
            }
        }
    }

    private fun copyToOutputFile() {
        Log.d(LOG_TAG, "Writing to target: $path")
        var outputFile: DocumentFileCompat
        val creatingTime = measureTimeMillis { outputFile = createDocumentFile() }
        Log.d(LOG_TAG, "Creating document took $creatingTime ms")

        var outputStream: OutputStream
        val openTime = measureTimeMillis {
            outputStream = contentResolver.openOutputStream(outputFile.uri)
                ?: throw IOException("Couldn't open output file for writing.")
        }
        Log.d(LOG_TAG, "Opening document output stream took $openTime ms")

        val writeTime = measureTimeMillis {
            outputStream.use { output: OutputStream ->
                val inputStream = FileInputStream(file)
                inputStream.use { input: FileInputStream ->
                    input.copyTo(output, 64 * 1024)
                }
            }
        }

        Log.d(LOG_TAG, "Writing document output stream took $writeTime ms")
    }

    private fun createDocumentFile(): DocumentFileCompat {
        val pathComponents = outputPath.split("/").filterNot { it.isEmpty() }
        val directoryPath = pathComponents.dropLast(1).joinToString("/")
        val documentRoot = cache.getDirectory(directoryPath)

        return documentRoot.createFile("*/*", pathComponents.last())
            ?: throw IOException("Could not create file.")
    }

    companion object {
        private const val LOG_TAG = "TemporaryExtractedFile"
    }
}