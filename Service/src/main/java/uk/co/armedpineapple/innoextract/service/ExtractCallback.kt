package uk.co.armedpineapple.innoextract.service

/**
 * A callback that receives changes in the extract process.
 */
interface ExtractCallback {

    /**
     * Invoked when there's a progress update.
     *
     * @param value The progress through the extraction.
     * @param max The value at which [value] would represent completion.
     * @param file The currently extracting file.
     */
    fun onProgress(value: Long, max: Long, file: String)

    /**
     * Invoked upon successful extraction.
     */
    fun onSuccess()

    /**
     * Invoked upon failed extraction.
     *
     * @param e The exception describing the failure.
     */
    fun onFailure(e: Exception)
}