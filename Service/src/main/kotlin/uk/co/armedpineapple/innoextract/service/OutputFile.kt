package uk.co.armedpineapple.innoextract.service


/**
 * An extracted file from an Inno Setup installer.
 *
 * @constructor Create empty file.
 */
interface OutputFile : AutoCloseable {
    val path: String
    val pathUtf8: ByteArray
}