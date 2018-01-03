package uk.co.armedpineapple.innoextract

import java.io.*

internal interface IExtractService {

    fun extract(toExtract: File, extractDir: File,
                callback: ExtractCallback)

    fun check(toExtract: File, callback: (Boolean) -> Unit)

    interface ExtractCallback {
        fun onProgress(value: Int, max: Int)

        fun onSuccess()

        fun onFailure(e: Exception)
    }


}
