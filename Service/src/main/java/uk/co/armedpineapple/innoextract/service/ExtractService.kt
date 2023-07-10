package uk.co.armedpineapple.innoextract.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.io.File
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ExtractService : Service(), IExtractService {

    private lateinit var temporaryRoot: File
    private var extractDocumentRoot: DocumentFileCompat? = null
    private var extractRoot: File? = null
    private var documentCache: DocumentFileCache? = null
    private var isBusy = false
    private var callback: ExtractCallback? = null
    private var configuration: Configuration? = null
    private var currentFile: String? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var finalNotificationBuilder: NotificationCompat.Builder

    private var loggingThread: LoggingThread? = null
    private val serviceBinder = ServiceBinder()

    override val isExtracting get() = isBusy

    // Native methods
    private external fun nativePrepare()
    private external fun nativeExtract(sourceFd: Int, extractDir: String): Int
    private external fun nativeCheck(sourceFd: Int): InnoValidationResult

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int = START_NOT_STICKY

    private fun createNotificationChannel() {
        val progressChannel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        progressChannel.description = getString(R.string.notification_channel_description)
        progressChannel.enableLights(false)
        progressChannel.enableVibration(false)
        progressChannel.setSound(null, null)
        notificationManager.createNotificationChannel(progressChannel)
    }

    override fun onCreate() {
        super.onCreate()

        temporaryRoot = File(noBackupFilesDir, "extract")
        temporaryRoot.mkdirs()
        temporaryRoot.deleteOnExit()

        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
    }

    override fun check(toCheck: Uri): InnoValidationResult {
        if (isBusy) throw ServiceBusyException()

        Log.d(LOG_TAG, "Checking $toCheck")
        var result: InnoValidationResult
        val fd = this.applicationContext.contentResolver.openFileDescriptor(toCheck, "r")
        fd.use {
            val nativeFd = it!!.fd
            result = nativeCheck(nativeFd)
        }

        return result
    }

    override fun extract(
        toExtract: Uri, extractDir: File, callback: ExtractCallback, configuration: Configuration
    ) {
        if (isBusy) throw ServiceBusyException()

        extractDocumentRoot = null
        documentCache = null

        extractDir.mkdirs()

        doExtract(toExtract, extractDir.absolutePath, callback, configuration)
    }

    override fun extract(
        toExtract: Uri, extractDir: Uri, callback: ExtractCallback, configuration: Configuration
    ) {
        if (isBusy) throw ServiceBusyException()

        extractDocumentRoot = DocumentFileCompat.fromTreeUri(applicationContext, extractDir)
        documentCache = extractDocumentRoot?.let { DocumentFileCache(it) }

        doExtract(toExtract, "/", callback, configuration)
    }

    private fun doExtract(
        toExtract: Uri, extractDir: String, callback: ExtractCallback, configuration: Configuration
    ) {
        val toExtractFd = applicationContext.contentResolver.openFileDescriptor(toExtract, "r")

        this.callback = callback
        this.configuration = configuration

        Log.i(LOG_TAG, "Performing extract on: $toExtract, $extractDir")

        val cb = object : ExtractCallback {
            init {
                finalNotificationBuilder =
                    NotificationCompat.Builder(this@ExtractService, NOTIFICATION_CHANNEL)
            }

            private val done = AtomicBoolean(false)

            override fun onProgress(
                value: Long, max: Long, file: String
            ) {
            }

            override fun onSuccess() {
                Log.i(LOG_TAG, "Successfully extracted")
                done.set(true)
                documentCache?.clearCache()
                finalNotificationBuilder.setTicker(getString(R.string.final_notification_success_ticker))
                    .setSmallIcon(R.drawable.ic_extracting)
                    .setContentTitle(getString(R.string.final_notification_success_title))
                    .setChannelId(NOTIFICATION_CHANNEL)
                    .setContentText(getString(R.string.final_notification_success_text))


                if (configuration.showOngoingNotification) {
                    notificationManager.notify(
                        FINAL_NOTIFICATION, finalNotificationBuilder.build()
                    )
                }
                stopForeground(true)
                callback.onSuccess()
                stopSelf()
            }

            override fun onFailure(e: Exception) {
                Log.w(LOG_TAG, "Failed to extract")
                done.set(true)
                documentCache?.clearCache()
                finalNotificationBuilder.setTicker(getString(R.string.extract_failed))
                    .setSmallIcon(R.drawable.ic_extracting).setChannelId(NOTIFICATION_CHANNEL)
                    .setContentTitle(getString(R.string.extract_failed))

                if (configuration.showFinalNotification) {
                    notificationManager.notify(
                        FINAL_NOTIFICATION, finalNotificationBuilder.build()
                    )
                }
                callback.onFailure(e)
                stopSelf()
            }
        }

        notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        notificationBuilder.setContentTitle(getString(R.string.progress_notification_title))
            .setSmallIcon(R.drawable.ic_extracting)
            .setTicker(getString(R.string.progress_notification_ticker))
            .setChannelId(NOTIFICATION_CHANNEL)
            .setContentText(getString(R.string.progress_notification_text))

        if (configuration.showOngoingNotification) {
            startForeground(ONGOING_NOTIFICATION, notificationBuilder.build())
        }


        val oldLoggingThread = loggingThread
        oldLoggingThread?.isAlive?.let {
            oldLoggingThread.interrupt()
        }

        this.loggingThread = LoggingThread(cb)

        val performThread = object : Thread() {

            override fun run() {
                isBusy = true
                // Initialise
                nativePrepare()

                // Finish
                toExtractFd.use {
                    val toExtractNativeFd = toExtractFd?.fd

                    if (nativeExtract(toExtractNativeFd!!, extractDir) == 0) {
                        isBusy = false
                        cb.onSuccess()
                    } else {
                        isBusy = false
                        cb.onFailure(RuntimeException())
                    }
                }
            }
        }

        this.loggingThread!!.start()
        // Try to get the looper so we know its been created properly
        this.loggingThread!!.looper
        performThread.start()
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(LOG_TAG, "Service Bound")
        return serviceBinder
    }

    private fun mapProgress(progress: Long, max: Long): Pair<Int, Int> {
        if (progress <= Integer.MAX_VALUE) {
            return Pair(progress.toInt(), max.toInt())
        }

        val newMax = Integer.MAX_VALUE
        val newProgress = Integer.MAX_VALUE * (max / progress)
        return Pair(newProgress.toInt(), newMax)
    }

    inner class ServiceBinder : Binder() {
        val service: ExtractService
            get() = this@ExtractService
    }

    private inner class ServiceBusyException : RuntimeException()

    @Keep
    fun gotString(inString: String, streamno: Int) {
        loggingThread?.PostLogMessage(streamno, inString)
    }

    @Keep
    fun updateProgress(progress: Long, total: Long) {

        val configuration = configuration
        val callback = callback

        if (configuration == null || callback == null) return

        if (configuration.showOngoingNotification) {
            val message = String.format(Locale.US, "Extracting %s", currentFile)
            val progress = mapProgress(progress, total)
            notificationBuilder.setProgress(progress.second, progress.first, false)
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle().bigText(message)
            )
            startForeground(
                ONGOING_NOTIFICATION, notificationBuilder.build()
            )
        }
        currentFile?.let { callback.onProgress(progress, total, it) }
    }

    @Keep
    fun updateCurrentFile(fileName: String) {
        currentFile = fileName
    }

    @Throws(RuntimeException::class)
    @Keep
    fun newFile(path: ByteArray): OutputFile {

        val pathStr = String(path, Charset.forName("UTF-8"))

        return if (extractDocumentRoot != null) {
            // We are extracting to a document storage. We need to use some indirection here.
            TemporaryExtractedFile(
                temporaryRoot, pathStr, contentResolver, documentCache!!
            )
        } else {
            // We are extracting directly to a path. We can use direct file access.
            DirectAccessFile(pathStr)
        }
    }

    companion object {
        private const val ONGOING_NOTIFICATION = 1
        private const val FINAL_NOTIFICATION = 2
        private const val NOTIFICATION_CHANNEL = "Extract Progress"

        init {
            System.loadLibrary("innoextract")
        }

        private const val LOG_TAG = "ExtractService"
    }
}