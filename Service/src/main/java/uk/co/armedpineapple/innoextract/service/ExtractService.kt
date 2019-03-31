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
                       callback: (Boolean) -> Unit) {

        if (isBusy)
            throw ServiceBusyException()

        debug("Checking " + toCheck.absolutePath)

        val result = nativeCheckInno(toCheck.absolutePath)

        callback.invoke(result == 0)
    }

    override fun extract(toExtract: File, extractDir: File, callback: ExtractCallback, configuration: Configuration) {

        if (isBusy)
            throw ServiceBusyException()

        info(
                "Performing extract on: " + toExtract.absolutePath + ", "
                        + extractDir.absolutePath)

        val logIntent = Intent(this, LogActivity::class.java)

        val cb = object : ExtractCallback {

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

            override fun onProgress(value: Long, max: Long, speedBps: Long, remainingSeconds: Long) {
                if (done.get()) return

                val calculated = speedCalculator!!.update(value)

                val secondsLeft = if (calculated >=0) {
                    (max - value) / calculated
                } else {
                    Long.MAX_VALUE
                }

                val message = if (calculated >= 0) {
                    val mbps = calculated / 1048576

                    val remainingText = PeriodFormat
                            .getDefault().print(Period((secondsLeft * 1000)))

                    String.format(Locale.US, "Extracting: %s\nSpeed: %dMB/s\nTime Remaining:%s", toExtract.name, mbps, remainingText)

                } else {
                    String.format(Locale.US, "Extracting: %s", toExtract.name)
                }

                if (configuration.showOngoingNotification) {
                    val progress = mapProgress(value, max)
                    mNotificationBuilder.setProgress(progress.second, progress.first, false)
                    mNotificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    startForeground(ONGOING_NOTIFICATION,
                            mNotificationBuilder.build())
                }
                callback.onProgress(value, max, calculated, secondsLeft)
            }

            override fun onSuccess() {
                info("Successfully extracted")
                done.set(true)
                speedCalculator = null

                mFinalNotificationBuilder.setTicker(getString(R.string.final_notification_success_ticker))
                        .setSmallIcon(R.drawable.ic_extracting)
                        .setContentTitle(getString(R.string.final_notification_success_title))
                        .setChannelId(NOTIFICATION_CHANNEL)
                        .setContentText(getString(R.string.final_notification_success_text))

                try {
                    if (configuration.showLogActionButton) {
                    logIntent.putExtra("log", writeLogToFile())

                        val logPendingIntent = PendingIntent
                                .getActivity(this@ExtractService, 0, logIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT)


                        mFinalNotificationBuilder
                                .addAction(R.drawable.ic_view_log, "View Log",
                                        logPendingIntent)
                    }
                } catch (e: IOException) {
                    error("couldn't write log file!")
                }

                if (configuration.showOngoingNotification) {
                    mNotificationManager.notify(FINAL_NOTIFICATION,
                            mFinalNotificationBuilder.build())
                }
                stopForeground(true)
                callback.onSuccess()
                stopSelf()
            }

            override fun onFailure(e: Exception) {
                warn("Failed to extract")
                done.set(true)
                mFinalNotificationBuilder.setTicker(getString(R.string.extract_failed))
                        .setSmallIcon(R.drawable.ic_extracting)
                        .setChannelId(NOTIFICATION_CHANNEL)
                        .setContentTitle(getString(R.string.extract_failed))

                try {
                    logIntent.putExtra("log", writeLogToFile())

                    val logPendingIntent = PendingIntent
                            .getActivity(this@ExtractService, 0, logIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT)

                    mFinalNotificationBuilder
                            .addAction(R.drawable.ic_view_log, getString(R.string.final_notification_action_text),
                                    logPendingIntent)
                } catch (eb: IOException) {
                    error("couldn't write log file", eb)
                }

                if (configuration.showFinalNotification) {
                    mNotificationManager.notify(FINAL_NOTIFICATION,
                            mFinalNotificationBuilder.build())
                }
                callback.onFailure(e)
                stopSelf()
            }

        }

        mNotificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager

        mNotificationBuilder.setContentTitle(getString(R.string.progress_notification_title))
                .setSmallIcon(R.drawable.ic_extracting)
                .setTicker(getString(R.string.progress_notification_ticker))
                .setChannelId(NOTIFICATION_CHANNEL)
                .setContentText(getString(R.string.progress_notification_text))

        if (configuration.showOngoingNotification) {
            startForeground(ONGOING_NOTIFICATION, mNotificationBuilder.build())
        }


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
        // Try to get the looper so we know its been created properly
        mLoggingThread!!.looper
        performThread.start()
    }

    override fun onBind(intent: Intent): IBinder? {
        debug("Service Bound")
        return serviceBinder
    }

    override fun isExtractInProgress(): Boolean = isBusy

    private fun mapProgress(progress: Long, max: Long) : Pair<Int, Int> {
        if (progress <= Integer.MAX_VALUE) {
            return Pair(progress.toInt(), max.toInt())
        }

        val newMax = Integer.MAX_VALUE
        val newProgress = Integer.MAX_VALUE * (max/progress)
        return Pair(newProgress.toInt(), newMax)
    }

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

    inner class LoggingThread internal constructor(name: String, internal var callback: ExtractCallback) : HandlerThread(name) {
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
                    info { line }
                    if (line.startsWith("T$")) {

                        val parts = line.split("\\$".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        var current = 0L
                        var max = 1L

                        if (parts.size > 3) {
                            try {
                                current = parts[1].toLong()
                            } catch (e: NumberFormatException) {
                                warn("Couldn't parse current progress", e)
                            }

                            try {
                                max = parts[2].toLong()
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
                    info { line }
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