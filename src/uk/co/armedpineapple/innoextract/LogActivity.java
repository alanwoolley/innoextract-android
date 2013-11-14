package uk.co.armedpineapple.innoextract;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;

public class LogActivity extends Activity {

	private static final String LOG_TAG = "LENGTH";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);
		Intent intent = getIntent();
		CharSequence log = intent.getCharSequenceExtra("log");

		WebView logView = (WebView) findViewById(R.id.logWebView);

		logView.loadUrl("file://" + log);
	}

	@Override
	protected void onNewIntent(Intent intent) {

		super.onNewIntent(intent);
		Log.d(LOG_TAG, "NEW INTENT!");
		Log.d(LOG_TAG, "Extras: " + intent.getExtras().size());
	}

}
