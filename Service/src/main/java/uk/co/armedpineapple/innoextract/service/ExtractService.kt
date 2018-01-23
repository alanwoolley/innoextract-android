/* Copyright (c) 2013 Alan Woolley
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package uk.co.armedpineapple.innoextract.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.support.annotation.Keep
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import org.jetbrains.anko.*
import org.joda.time.Period
import org.joda.time.format.PeriodFormat
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ExtractService : Service(), IExtractService, AnkoLogger {

    private var isBusy = false
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mNotificationBuilder: NotificationCompat.Builder
    private lateinit var mFinalNotificationBuilder: NotificationCompat.Builder

    private var mLoggingThread: LoggingThread? = null
    private val serviceBinder = ServiceBinder()
    private val logBuilder = StringBuilder()

    // Native methods

    private external fun nativeInit()
    private external fun nativeDoExtract(sourceFile: String, extractDir: String): Int
    private external fun nativeCheckInno(sourceFile: String): Int

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int =
            Service.START_NOT_STICKY

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val progressChannel = NotificationChannel(
                NOTIFICATION_CHANNEL, getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT)
        progressChannel.description = getString(R.string.notification_channel_description)
        progressChannel.enableLights(false)
        progressChannel.enableVibration(false)
        progressChannel.setSound(null, null)
        mNotificationManager.createNotificationChannel(progressChannel)
    }

    override fun onCreate() {
        super.onCreate()

        mNotificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
        mNotificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    override fun check(toCheck: File,
                       callback: Function1<Boolean, Unit>) {

        if (isBusy)
            throw ServiceBusyException()

        debug("Checking " + toCheck.absolutePath)

        val result = nativeCheckInno(toCheck.absolutePath)

        callback.invoke(result == 0)
    }

    override fun extract(toExtract: File,
                         extractDir: File,
                         callback: IExtractService.ExtractCallback) {

        if (isBusy)
            throw ServiceBusyException()

        info(
                "Performing extract on: " + toExtract.absolutePath + ", "
                        + extractDir.absolutePath)

        val logIntent = Intent(this, LogActivity::class.java)

        val cb = object : IExtractService.ExtractCallback {

            init {
                mFinalNotificationBuilder = NotificationCompat.Builder(this@ExtractService, NOTIFICATION_CHANNEL)
            }

            private var speedCalculator: SpeedCalculator? = SpeedCalculator()
            private val done = AtomicBoolean(false)

            @Throws(IOException::class)
            private fun writeLogToFile(): String {
                debug("Writing log to file")
                val path = (cacheDir.absolutePath + File.separator
                        + toExtract.name + ".log.html")
                val bw = BufferedWriter(
                        FileWriter(File(path)))
                bw.write(logBuilder.toString())
                bw.close()
                debug("Done writing to file")
                return path
            }

            override fun onProgress(value: Int, max: Int, speedBps: Int, remainingSeconds: Int) {
                if (done.get()) return
                mNotificationBuilder.setProgress(max, value, false)

                val bps = Math.max(speedCalculator!!.update(value.toLong()), 1).toInt()
                val kbps = bps / 1024
                val secondsLeft = (max - value) / bps

                val remainingText = PeriodFormat
                        .getDefault().print(Period((secondsLeft * 1000).toLong()))


                val message = String.format(Locale.US, "Extracting: %s\nSpeed: %dKB/s\nTime Remaining:%s", toExtract.name, kbps, remainingText)


                mNotificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
                startForeground(ONGOING_NOTIFICATION,
                        mNotificationBuilder.build())
                callback.onProgress(value, max, bps, secondsLeft)
            }

            override fun onSuccess() {
                info("Successfully extracted")
                done.set(true)
                speedCalculator = null

                mFinalNotificationBuilder.setTicker("Extract Successful")
                        .setSmallIcon(R.drawable.ic_extracting)
                        .setContentTitle("Extracted")
                        .setChannelId(NOTIFICATION_CHANNEL)
                        .setContentText("Extraction Successful")

                try {
                    logIntent.putExtra("log", writeLogToFile())

                    val logPendingIntent = PendingIntent
                            .getActivity(this@ExtractService, 0, logIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT)

                    mFinalNotificationBuilder
                            .addAction(R.drawable.ic_view_log, "View Log",
                                    logPendingIntent)
                } catch (e: IOException) {
                    error("couldn't write log file!")
                }

                mNotificationManager.notify(FINAL_NOTIFICATION,
                        mFinalNotificationBuilder.build())
                stopForeground(true)
                callback.onSuccess()
                stopSelf()
            }

            override fun onFailure(e: Exception) {
                warn("Failed to extract")
                done.set(true)
                mFinalNotificationBuilder.setTicker("Extract Failed")
                        .setSmallIcon(R.drawable.ic_extracting)
                        .setChannelId(NOTIFICATION_CHANNEL)
                        .setContentTitle("Extract Failed")

                try {
                    logIntent.putExtra("log", writeLogToFile())

                    val logPendingIntent = PendingIntent
                            .getActivity(this@ExtractService, 0, logIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT)

                    mFinalNotificationBuilder
                            .addAction(R.drawable.ic_view_log, "View Log",
                                    logPendingIntent)
                } catch (eb: IOException) {
                    error("couldn't write log file", eb)
                }

                mNotificationManager.notify(FINAL_NOTIFICATION,
                        mFinalNotificationBuilder.build())
                callback.onFailure(e)
                stopSelf()
            }

        }

        mNotificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager

        mNotificationBuilder.setContentTitle("Extracting...")
                .setSmallIcon(R.drawable.ic_extracting)
                .setTicker("Extracting inno setup file")
                .setChannelId(NOTIFICATION_CHANNEL)
                .setContentText("Extracting inno setup file")

        startForeground(ONGOING_NOTIFICATION, mNotificationBuilder.build())

        if (mLoggingThread != null && mLoggingThread!!.isAlive) {
            mLoggingThread!!.interrupt()
        }

        mLoggingThread = LoggingThread("Logging", cb)

        val performThread = object : Thread() {

            override fun run() {
                isBusy = true
                // Initialise
                nativeInit()

                // Finish

                if (nativeDoExtract(toExtract.absolutePath,
                                extractDir.absolutePath) == 0) {
                    isBusy = false
                    cb.onSuccess()
                } else {
                    isBusy = false
                    cb.onFailure(RuntimeException())
                }
            }
        }

        mLoggingThread!!.start()
        performThread.start()
    }

    override fun onBind(intent: Intent): IBinder? {
        debug("Service Bound")
        return serviceBinder
    }

    override fun isExtractInProgress(): Boolean = isBusy

    inner class ServiceBinder : Binder() {
        val service: ExtractService
            get() = this@ExtractService
    }

    private inner class ServiceBusyException : RuntimeException()

    @Keep
    fun gotString(inString: String, streamno: Int) {
        verbose("Received: $inString")
        val msg = mLoggingThread!!.lineHandler.obtainMessage()
        msg.what = streamno
        msg.obj = inString
        mLoggingThread!!.lineHandler.sendMessage(msg)
    }

    inner class LoggingThread internal constructor(name: String, internal var callback: IExtractService.ExtractCallback) : HandlerThread(name) {
        internal lateinit var lineHandler: Handler

        override fun onLooperPrepared() {
            super.onLooperPrepared()
            lineHandler = LoggerHandler(looper)
        }

        internal inner class LoggerHandler(looper: Looper) : Handler(looper) {

            override fun handleMessage(msg: Message) {

                when (msg.what) {
                    STDOUT -> parseOut(msg.obj as String)
                    STDERR -> parseErr(msg.obj as String)
                    else -> run {
                        warn("Unknown message")
                        parseErr(msg.obj as String)
                    }
                }
            }

            private fun parseOut(line: String) {
                if (line.isNotEmpty()) {
                    if (line.startsWith("T$")) {
                        val parts = line.split("\\$".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        var current = 0
                        var max = 1

                        if (parts.size > 3) {
                            try {
                                current = Integer.valueOf(parts[1])!!
                            } catch (e: NumberFormatException) {
                                warn("Couldn't parse current progress", e)
                            }

                            try {
                                max = Integer.valueOf(parts[2])!!
                            } catch (e: NumberFormatException) {
                                warn("Couldn't parse max progress", e)
                            }

                        }

                        callback.onProgress(current, max, 0, 0)
                        return
                    }
                    logBuilder.append(line).append("<br/>")
                }
            }

            private fun parseErr(line: String) {
                if (line.isNotEmpty()) {
                    val newLine = SpannableString(line)
                    newLine.setSpan(
                            ForegroundColorSpan(Color.rgb(255, 0, 0)), 0,
                            newLine.length,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        logBuilder.append(Html.toHtml(newLine, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE))
                    } else {
                        logBuilder.append(Html.toHtml(newLine))
                    }
                }

            }
        }

    }

    companion object {
        private const val STDERR = 2
        private const val STDOUT = 1

        private const val ONGOING_NOTIFICATION = 1
        private const val FINAL_NOTIFICATION = 2
        private const val NOTIFICATION_CHANNEL = "Extract Progress"

        init {
            System.loadLibrary("innoextract")
        }
    }
}