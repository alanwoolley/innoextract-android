package uk.co.armedpineapple.innoextract.service

import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

/**
 * Processes the log messages received from the native code and persists them to an appropriate log
 * file.
 *
 * This handler is also responsible for updating the progress when a progress update is detected in
 * the log.
 *
 * @constructor
 * TODO
 *
 * @param looper The looper from the thread that's receiving the messages.
 */
internal class LoggerHandler(looper: Looper, val callback: ExtractCallback) : Handler(looper),
    AnkoLogger {

    val logBuilder = StringBuilder()

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            LogFileDescriptors.STDOUT.fd -> parseOut(msg.obj as String)
            LogFileDescriptors.STDERR.fd -> parseErr(msg.obj as String)
            else -> run {
                warn("Unknown message")
                parseErr(msg.obj as String)
            }
        }
    }

    private fun parseOut(line: String) {
        if (line.isEmpty()) return;
        info { line }

        logBuilder.append(line).append("<br/>")
    }

    private fun parseErr(line: String) {
        if (line.isEmpty()) return;

        error { line }

        val newLine = SpannableString(line)
        newLine.setSpan(
            ForegroundColorSpan(Color.rgb(255, 0, 0)),
            0,
            newLine.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            logBuilder.append(
                Html.toHtml(
                    newLine, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
                )
            )
        } else {
            logBuilder.append(Html.toHtml(newLine))
        }
    }
}