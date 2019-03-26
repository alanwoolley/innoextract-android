package uk.co.armedpineapple.innoextract.service

import java.io.File

interface IExtractService {

    fun isExtractInProgress(): Boolean

    fun extract(toExtract: File, extractDir: File,
                callback: ExtractCallback, configuration: Configuration = Configuration())

    fun check(toCheck: File, callback: (Boolean) -> Unit)
    fun check(toCheck: File, callback: CheckCallback) = check(toCheck, callback::onResult)

}

interface ExtractCallback {
    fun onProgress(value: Long, max: Long, speedBps: Long, remainingSeconds: Long)

    fun onSuccess()

    fun onFailure(e: Exception)
}

interface CheckCallback {
    fun onResult(success: Boolean)
}

data class Configuration(
        var showOngoingNotification: Boolean = true,
        var showFinalNotification: Boolean = true,
        var showLogActionButton: Boolean = false
)


