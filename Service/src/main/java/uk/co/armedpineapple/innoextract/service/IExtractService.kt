package uk.co.armedpineapple.innoextract.service

import java.io.*

interface IExtractService {

    fun isExtractInProgress() : Boolean

    fun extract(toExtract: File, extractDir: File,
                callback: ExtractCallback)

    fun check(toExtract: File, callback: (Boolean) -> Unit)

    interface ExtractCallback {
        fun onProgress(value: Int, max: Int, speedBps: Int, remainingSeconds: Int)

        fun onSuccess()

        fun onFailure(e: Exception)
    }


}
