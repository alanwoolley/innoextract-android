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
package uk.co.armedpineapple.innoextract;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

public class ExtractService extends Service {

	public static final String EXTRACT_FILE_PATH = "extract_file";
	public static final String EXTRACT_DIR = "extract_dir";
	public static final String EXTRACT_FILE_NAME = "extract_file_name";
	private static final int STDERR = 2;
	private static final int STDOUT = 1;

	private static final String LOG_TAG = "ExtractService";
	private static final int ONGOING_NOTIFICATION = 1;
	private static final int FINAL_NOTIFICATION = 2;

	private boolean isBusy = false;
	private Thread mPerformThread;
	private ExtractCallback mExtractCallback;
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mNotificationBuilder;
	private NotificationCompat.Builder mFinalNotificationBuilder;

	private Notification mProgressNotification;

	private LoggingThread mLoggingThread;

	private StringBuilder logBuilder = new StringBuilder();
	private String mExtractDir, mExtractFile, mExtractFileName;

	// Native methods
	public native void nativeInit();

	public native int nativeDoTest(String sourceFile, String extractDir);

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		mLoggingThread = new LoggingThread("Logging");

		Log.d(LOG_TAG, "Loading Library");
		System.loadLibrary("innoextract");

		mExtractDir = intent.getStringExtra(EXTRACT_DIR);
		mExtractFile = intent.getStringExtra(EXTRACT_FILE_PATH);
		mExtractFileName = intent.getStringExtra(EXTRACT_FILE_NAME);

		final Intent logIntent = new Intent(this, LogActivity.class);

		ExtractCallback cb = new ExtractCallback() {

			private String writeLogToFile() throws IOException {
				Log.d(LOG_TAG, "Writing log to file");
				String path = getCacheDir().getAbsolutePath() + File.separator
						+ mExtractFileName + ".log.html";
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
						path)));
				bw.write(logBuilder.toString());
				bw.close();
				Log.d(LOG_TAG, "Done writing to file");
				return path;
			}

			@Override
			public void onProgress(int value, int max) {
				mNotificationBuilder.setProgress(max, value, false);
				startForeground(ONGOING_NOTIFICATION,
						mNotificationBuilder.build());
			}

			@Override
			public void onSuccess() {
				Log.i(LOG_TAG, "SUCCESS! :)");

				mFinalNotificationBuilder.setTicker("Extract Successful")
						.setSmallIcon(R.drawable.ic_extracting)
						.setContentTitle("Extracted").setContentText("Extraction Successful");

				try {
					logIntent.putExtra("log", writeLogToFile());

					PendingIntent logPendingIntent = PendingIntent.getActivity(
							ExtractService.this, 0, logIntent,
							PendingIntent.FLAG_UPDATE_CURRENT);

					mFinalNotificationBuilder.addAction(R.drawable.ic_view_log,
							"View Log", logPendingIntent);
				} catch (IOException e) {
					Log.d(LOG_TAG, "couldn't write log");
				}

				mNotificationManager.notify(FINAL_NOTIFICATION,
						mFinalNotificationBuilder.build());
				stopSelf();

			}

			@Override
			public void onFailure(Exception e) {
				Log.i(LOG_TAG, "FAIL! :(");
				mFinalNotificationBuilder.setTicker("Extract Failed")
						.setSmallIcon(R.drawable.ic_extracting)
						.setContentTitle("Extract Failed");

				try {
					logIntent.putExtra("log", writeLogToFile());

					PendingIntent logPendingIntent = PendingIntent.getActivity(
							ExtractService.this, 0, logIntent,
							PendingIntent.FLAG_UPDATE_CURRENT);

					mFinalNotificationBuilder.addAction(R.drawable.ic_view_log,
							"View Log", logPendingIntent);
				} catch (IOException eb) {
					Log.d(LOG_TAG, "couldn't write log");
				}

				mNotificationManager.notify(FINAL_NOTIFICATION,
						mFinalNotificationBuilder.build());
				stopSelf();

			}

		};
		mFinalNotificationBuilder = new NotificationCompat.Builder(this);
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationBuilder.setContentTitle("Extracting...")
				.setSmallIcon(R.drawable.ic_extracting)
				.setTicker("Extracting inno setup file")
				.setContentText("Extracting inno setup file");
		startForeground(ONGOING_NOTIFICATION, mNotificationBuilder.build());

		performExtract(mExtractFile, mExtractDir, cb);

		return START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationBuilder = new NotificationCompat.Builder(this);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void performExtract(final String toExtract, final String extractDir,
			final ExtractCallback callback) throws ServiceBusyException {

		if (isBusy)
			throw new ServiceBusyException();
		Log.d(LOG_TAG, "Performing extract on: " + toExtract + ", "
				+ extractDir);
		mExtractCallback = callback;

		mPerformThread = new Thread() {

			@Override
			public void run() {
				isBusy = true;
				// Initialise
				nativeInit();

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// Finish

				if (nativeDoTest(toExtract, extractDir) == 0) {
					isBusy = false;
					callback.onSuccess();
				} else {
					isBusy = false;
					callback.onFailure(new RuntimeException());
				}

			}

		};

		mPerformThread.start();

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public class ServiceBinder extends Binder {
		ExtractService getService() {
			return ExtractService.this;
		}
	}

	public class ServiceBusyException extends RuntimeException {
	}

	public interface ExtractCallback {
		void onProgress(int value, int max);

		void onSuccess();

		void onFailure(Exception e);

	}

	public void gotString(String inString, int streamno) {
		Message msg = mLoggingThread.lineHandler.obtainMessage();
		msg.what = streamno;
		msg.obj = inString;
		mLoggingThread.lineHandler.sendMessage(msg);
		Log.d(LOG_TAG, inString);
	}

	public class LoggingThread extends HandlerThread {
		Handler lineHandler;

		public LoggingThread(String name) {
			super(name);
			start();
			lineHandler = new LoggerHandler(getLooper());
		}

		class LoggerHandler extends Handler {

			public LoggerHandler(Looper looper) {
				super(looper);

			}

			@Override
			public void handleMessage(Message msg) {

				if (msg.what == STDOUT) {
					parseOut((String) msg.obj);
				} else {
					parseErr((String) msg.obj);

				}
			}

			public void parseOut(String line) {
				if (line.length() > 0) {
					if (line.startsWith("T$")) {
						String[] parts = line.split("\\$");

						mExtractCallback.onProgress(Integer.valueOf(parts[1]),
								Integer.valueOf(parts[2]));
						return;
					}
					logBuilder.append(line + "<br/>");

				}
			}

			public void parseErr(String line) {
				if (line.length() > 0) {
					SpannableString newLine = new SpannableString(line);
					newLine.setSpan(
							new ForegroundColorSpan(Color.rgb(255, 0, 0)), 0,
							newLine.length(),
							Spannable.SPAN_INCLUSIVE_INCLUSIVE);

					logBuilder.append(Html.toHtml(newLine));
				}

			}
		};
	}

}
