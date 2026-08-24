package uk.co.armedpineapple.innoextract.service

import android.os.HandlerThread

/**
 * A thread that is responsible for processing the log messages that come back from the native
 * code.
 *
 * @property callback The callback to invoke with progress updates parsed from the log.
 * @constructor
 * TODO
 *
 */
internal class LoggingThread internal constructor(internal var callback: ExtractCallback) :
    HandlerThread("LoggingThread") {

    private lateinit var lineHandler: LoggerHandler

    fun PostLogMessage(streamNo: Int, text: String) {
        val msg = lineHandler.obtainMessage()
        msg.what = streamNo
        msg.obj = text
        lineHandler.sendMessage(msg)
    }

    /**
     * @see HandlerThread.onLooperPrepared
     */
    override fun onLooperPrepared() {
        super.onLooperPrepared()
        lineHandler = LoggerHandler(looper, callback)
    }
}