package uk.co.armedpineapple.innoextract.service

import java.io.*

interface IExtractService {

    fun isExtractInProgress() : Boolean

    fun extract(toExtract: File, extractDir: File,
                callback: ExtractCallback, configuration : Configuration = Configuration())

    fun check(toCheck: File, callback: (Boolean) -> Unit)

    interface ExtractCallback {
        fun onProgress(value: Int, max: Int, speedBps: Int, remainingSeconds: Int)

        fun onSuccess()

        fun onFailure(e: Exception)
    }

    data class Configuration(
            val showOngoingNotification : Boolean = true,
            val showFinalNotification: Boolean = true,
            val showLogActionButton : Boolean = false
    )
}
