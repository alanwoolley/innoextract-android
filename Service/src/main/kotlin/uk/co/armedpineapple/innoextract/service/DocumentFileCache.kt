package uk.co.armedpineapple.innoextract.service

import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.io.IOException


/**
 * A cache for Android DocumentFile directories, providing efficient access to directories within a file tree.
 *
 * @param rootDocument The root DocumentFile representing the starting point of the file tree.
 */
class DocumentFileCache(private val rootDocument: DocumentFileCompat) {
    private val cache: MutableMap<String, DocumentFileCompat> = mutableMapOf()

    /**
     * Retrieves the directory from the cache if it exists, or creates and caches it if not.
     *
     * @param path The path of the directory.
     * @return The DocumentFile representing the directory.
     */
    fun getDirectory(path: String): DocumentFileCompat {
        val cachedDir = cache[path]
        return if (cachedDir != null && cachedDir.exists() && cachedDir.isDirectory()) {
            cachedDir
        } else {
            val directory = createOrResolveDirectory(path)
            cache[path] = directory
            directory
        }
    }

    /**
     * Creates or resolves the directory at the given path, and updates the cache accordingly.
     *
     * @param path The path of the directory.
     * @return The DocumentFile representing the directory.
     */
    private fun createOrResolveDirectory(path: String): DocumentFileCompat {
        val pathComponents = path.split("/").filterNot { it.isEmpty() }
        var documentDir = rootDocument

        pathComponents.forEach { dirComponent ->
            val cachedDir = cache[dirComponent]
            if (cachedDir != null && cachedDir.exists() && cachedDir.isDirectory()) {
                documentDir = cachedDir
            } else {
                val newDir = documentDir.listFiles()
                    .firstOrNull { it.isDirectory() && it.name == dirComponent }

                documentDir = newDir ?: documentDir.createDirectory(dirComponent)
                        ?: throw IOException("Could not create directory.")

                cache[dirComponent] = documentDir
            }
        }

        return documentDir
    }

    /**
     * Clears the cache of directories.
     */
    fun clearCache() {
        cache.clear()
    }
}
