package uk.co.armedpineapple.innoextract.service

import android.net.Uri

interface IExtractService {

    val isExtracting: Boolean

    fun extract(
        toExtract: Uri,
        extractDir: Uri,
        callback: ExtractCallback,
        configuration: Configuration = Configuration()
    )

    fun check(toCheck: Uri): InnoValidationResult
}

data class Configuration(
    var showOngoingNotification: Boolean = true,
    var showFinalNotification: Boolean = true,
    var showLogActionButton: Boolean = false
)


