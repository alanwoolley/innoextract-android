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
package uk.co.armedpineapple.innoextract.service;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.support.annotation.*;
import android.support.v4.app.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import kotlin.*;
import kotlin.jvm.functions.*;
import org.jetbrains.annotations.*;
import org.joda.time.*;
import org.joda.time.format.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class ExtractService extends Service implements IExtractService {

    private static final int STDERR = 2;
    private static final int STDOUT = 1;

    private static final String LOG_TAG = "ExtractService";
    private static final int ONGOING_NOTIFICATION = 1;
    private static final int FINAL_NOTIFICATION = 2;
    private static final String NOTIFICATION_CHANNEL = "Extract Progress";

    private boolean isBusy = false;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationCompat.Builder mFinalNotificationBuilder;

    private LoggingThread mLoggingThread;
    private final IBinder serviceBinder = new ServiceBinder();

    private StringBuilder logBuilder = new StringBuilder();

    // Native methods
    public native void nativeInit();

    public native int nativeDoExtract(String sourceFile, String extractDir);

    public native int nativeCheckInno(String sourceFile);

    static {
        System.loadLibrary("innoextract");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }

    @Override public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel progressChannel = new NotificationChannel(
                        NOTIFICATION_CHANNEL, getString(R.string.notification_channel),
                        NotificationManager.IMPORTANCE_DEFAULT);
                progressChannel.setDescription(getString(R.string.notification_channel_description));
                progressChannel.enableLights(false);
                progressChannel.enableVibration(false);
                progressChannel.setSound(null, null);
                notificationManager.createNotificationChannel(progressChannel);
            }
        }

        mNotificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL);

    }

    @Override public void onDestroy() {
        super.onDestroy();
    }

    @Override public void check(@NotNull File toCheck,
            @NotNull Function1<? super Boolean, Unit> callback) {

        if (isBusy)
            throw new ServiceBusyException();

        Log.d(LOG_TAG, "Checking " + toCheck.getAbsolutePath());

        int result = nativeCheckInno(toCheck.getAbsolutePath());

        callback.invoke(result == 0);
    }

    @Override public void extract(@NotNull final File toExtract,
            @NotNull final File extractDir,
            @NotNull final ExtractCallback callback) {

        if (isBusy)
            throw new ServiceBusyException();

        Log.d(LOG_TAG,
                "Performing extract on: " + toExtract.getAbsolutePath() + ", "
                        + extractDir.getAbsolutePath());

        final Intent logIntent = new Intent(this, LogActivity.class);

        final ExtractCallback cb = new ExtractCallback() {

            private SpeedCalculator speedCalculator = new SpeedCalculator();
            private AtomicBoolean done = new AtomicBoolean(false);

            private String writeLogToFile() throws IOException {
                Log.d(LOG_TAG, "Writing log to file");
                String path = getCacheDir().getAbsolutePath() + File.separator
                        + toExtract.getName() + ".log.html";
                BufferedWriter bw = new BufferedWriter(
                        new FileWriter(new File(path)));
                bw.write(logBuilder.toString());
                bw.close();
                Log.d(LOG_TAG, "Done writing to file");
                return path;
            }

            @Override public void onProgress(int value, int max, int speedBps, int remainingSeconds ) {
                if (done.get()) return;
                mNotificationBuilder.setProgress(max, value, false);

                int bps = (int) Math.max(speedCalculator.update(value),1);
                int kbps = bps / 1024;
                int secondsLeft = (max-value)/bps;

                String remainingText = PeriodFormat
                        .getDefault().print(new Period(secondsLeft * 1000));


                String message = String.format(Locale.US, "Extracting: %s\nSpeed: %dKB/s\nTime Remaining:%s", toExtract.getName(), kbps, remainingText);


                mNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
                startForeground(ONGOING_NOTIFICATION,
                        mNotificationBuilder.build());
                callback.onProgress(value,max, bps, secondsLeft);
            }

            @Override public void onSuccess() {
                Log.i(LOG_TAG, "SUCCESS! :)");
                done.set(true);
                speedCalculator = null;

                mFinalNotificationBuilder.setTicker("Extract Successful")
                        .setSmallIcon(R.drawable.ic_extracting)
                        .setContentTitle("Extracted")
                        .setChannelId(NOTIFICATION_CHANNEL)
                        .setContentText("Extraction Successful");

                try {
                    logIntent.putExtra("log", writeLogToFile());

                    PendingIntent logPendingIntent = PendingIntent
                            .getActivity(ExtractService.this, 0, logIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                    mFinalNotificationBuilder
                            .addAction(R.drawable.ic_view_log, "View Log",
                                    logPendingIntent);
                } catch (IOException e) {
                    Log.d(LOG_TAG, "couldn't write log");
                }

                mNotificationManager.notify(FINAL_NOTIFICATION,
                        mFinalNotificationBuilder.build());
                stopForeground(true);
                callback.onSuccess();
                stopSelf();
            }

            @Override public void onFailure(Exception e) {
                Log.e(LOG_TAG, "FAIL! :(");
                done.set(true);
                mFinalNotificationBuilder.setTicker("Extract Failed")
                        .setSmallIcon(R.drawable.ic_extracting)
                        .setChannelId(NOTIFICATION_CHANNEL)
                        .setContentTitle("Extract Failed");

                try {
                    logIntent.putExtra("log", writeLogToFile());

                    PendingIntent logPendingIntent = PendingIntent
                            .getActivity(ExtractService.this, 0, logIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);

                    mFinalNotificationBuilder
                            .addAction(R.drawable.ic_view_log, "View Log",
                                    logPendingIntent);
                } catch (IOException eb) {
                    Log.d(LOG_TAG, "couldn't write log");
                }

                mNotificationManager.notify(FINAL_NOTIFICATION,
                        mFinalNotificationBuilder.build());
                callback.onFailure(e);
                stopSelf();
            }

        };
        mFinalNotificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL);
        mNotificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);

        mNotificationBuilder.setContentTitle("Extracting...")
                .setSmallIcon(R.drawable.ic_extracting)
                .setTicker("Extracting inno setup file")
                .setChannelId(NOTIFICATION_CHANNEL)
                .setContentText("Extracting inno setup file");

        startForeground(ONGOING_NOTIFICATION, mNotificationBuilder.build());

        if (mLoggingThread != null && mLoggingThread.isAlive()) {
            mLoggingThread.interrupt();
        }

        mLoggingThread = new LoggingThread("Logging", cb);

        Thread performThread = new Thread() {

            @Override public void run() {
                isBusy = true;
                // Initialise
                nativeInit();

                // Finish

                if (nativeDoExtract(toExtract.getAbsolutePath(),
                        extractDir.getAbsolutePath()) == 0) {
                    isBusy = false;
                    cb.onSuccess();
                } else {
                    isBusy = false;
                    cb.onFailure(new RuntimeException());
                }
            }
        };

        mLoggingThread.start();
        performThread.start();
    }

    @Override public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "Service Bound");
        return serviceBinder;
    }

    @Override public boolean isExtractInProgress() {
        return isBusy;
    }

    public class ServiceBinder extends Binder {
        public ExtractService getService() {
            return ExtractService.this;
        }
    }

    private class ServiceBusyException extends RuntimeException {
    }

    @Keep
    public void gotString(String inString, int streamno) {
        Message msg = mLoggingThread.lineHandler.obtainMessage();
        msg.what = streamno;
        msg.obj = inString;
        mLoggingThread.lineHandler.sendMessage(msg);
        Log.d(LOG_TAG, inString);
    }

    public class LoggingThread extends HandlerThread {
        Handler lineHandler;
        ExtractCallback callback;

        LoggingThread(String name, ExtractCallback callback) {
            super(name);
            this.callback = callback;
        }

        @Override protected void onLooperPrepared() {
            super.onLooperPrepared();
            lineHandler = new LoggerHandler(getLooper());
        }

        class LoggerHandler extends Handler {

            LoggerHandler(Looper looper) {
                super(looper);

            }

            @Override public void handleMessage(Message msg) {

                if (msg.what == STDOUT) {
                    parseOut((String) msg.obj);
                } else {
                    parseErr((String) msg.obj);

                }
            }

            void parseOut(String line) {
                if (line.length() > 0) {
                    if (line.startsWith("T$")) {
                        String[] parts = line.split("\\$");
                        int current = 0;
                        int max = 1;

                        if (parts.length > 3) {
                            try {
                                current = Integer.valueOf(parts[1]);
                            } catch (NumberFormatException e) {
                                Log.e(LOG_TAG, "Couldn't parse current progress");
                            }
                            try {
                                max = Integer.valueOf(parts[2]);
                            } catch (NumberFormatException e) {
                                Log.e(LOG_TAG, "Couldn't parse max progress");
                            }
                        }

                        callback.onProgress(current, max, 0, 0);
                        return;
                    }
                    logBuilder.append(line).append("<br/>");
                }
            }

            void parseErr(String line) {
                if (line.length() > 0) {
                    SpannableString newLine = new SpannableString(line);
                    newLine.setSpan(
                            new ForegroundColorSpan(Color.rgb(255, 0, 0)), 0,
                            newLine.length(),
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                    logBuilder.append(Html.toHtml(newLine));
                }

            }
        }

    }

}
