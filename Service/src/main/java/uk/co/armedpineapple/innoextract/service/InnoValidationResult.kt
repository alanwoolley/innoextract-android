package uk.co.armedpineapple.innoextract.service

import androidx.annotation.Keep
import java.nio.charset.Charset

@Keep
data class InnoValidationResult(
    val isValid: Boolean,
    val gogId: Long,
    private val titleUtf8: ByteArray,
    private val versionUtf8: ByteArray,
) {
    val isGogInstaller = (gogId != -1L);
    val title = String(titleUtf8, Charset.forName("UTF-8"))
    val version = String(versionUtf8, Charset.forName("UTF-8"))
}
