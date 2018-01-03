package uk.co.armedpineapple.innoextract

import java.io.*

internal interface IExtractService {

    fun extract(toExtract: File, extractDir: File,
                callback: ExtractCallback)

    fun check(toExtract: File, callback: CheckCallback)

    interface ExtractCallback {
        fun onProgress(value: Int, max: Int)

        fun onSuccess()

        fun onFailure(e: Exception)
    }

    interface CheckCallback {
        fun onResult(valid: Boolean)
    }

}
