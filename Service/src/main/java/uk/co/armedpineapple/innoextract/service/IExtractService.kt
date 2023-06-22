package uk.co.armedpineapple.innoextract.service

import android.net.Uri

/**
 * A service that can extract Inno Setup installers
 *
 * @constructor Creates a service
 */
interface IExtractService {

    /**
     * Whether the service is currently extracting.
     */
    val isExtracting: Boolean

    /**
     * Extracts an Inno Setup installer.
     *
     * @param toExtract A valid URI to an installer file. This is expected to be a document URI
     * @param extractDir A valid directory to extract to. This is expected to be a document tree URI.
     * @param callback A callback for extraction status and progress
     * @param configuration A configuration describing extraction parameters.
     */
    fun extract(
        toExtract: Uri,
        extractDir: Uri,
        callback: ExtractCallback,
        configuration: Configuration = Configuration()
    )

    /**
     * Checks whether a file is a valid Inno Setup installer.
     *
     * @param toCheck A valid URI to a potential installer file. This is expected to be a document URI
     * @return true if the file is a valid installer that can be extracted, or false otherwise.
     */
    fun check(toCheck: Uri): InnoValidationResult
}

/**
 * Describes extraction behaviour.
 *
 * @property showOngoingNotification Whether to show an ongoing notification during extraction/
 * @property showFinalNotification Whether to show a notification when extraction is finished
 * @constructor Creates a default configuration
 */
data class Configuration(
    var showOngoingNotification: Boolean = true, var showFinalNotification: Boolean = true
)


