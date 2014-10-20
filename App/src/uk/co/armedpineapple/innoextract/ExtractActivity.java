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

import java.io.File;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ExtractActivity extends Activity {

	private final String LOG_TAG = "ExtractActivity";

	EditText extractToEditText;
	Button extractButton;
	Button cancelButton;
	Button browseButton;

	String fileToExtract;

    public native int nativeCheckInno(String sourceFile);

    static {
        System.loadLibrary("innoextract");
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri uri = intent.getData();

        fileToExtract = uri.getPath();
        int fileState = nativeCheckInno(uri.getPath());

        if (fileState > 0) {
            // Error
            setContentView(R.layout.dialog_error);
            Button closeButton = (Button) findViewById(R.id.closeBtn);
            closeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                  finish();
                }
            });

            return;
        }

        setContentView(R.layout.dialog_layout);

		extractToEditText = (EditText) findViewById(R.id.extract_to);
		extractButton = (Button) findViewById(R.id.extract);
		cancelButton = (Button) findViewById(R.id.cancel);
		browseButton = (Button) findViewById(R.id.browse);

		String extDir;

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			extDir = Environment.getExternalStorageDirectory()
					.getAbsolutePath();
		} else {
			extDir = getFilesDir().getAbsolutePath();
		}

		extractToEditText.setText(extDir + "/extracted");

		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ExtractActivity.this.finish();
			}
		});

		extractButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				doExtract();
				ExtractActivity.this.finish();
			}
		});

		browseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showBrowseActivity();
			}

		});
	}

	private void doExtract() {
		Intent serviceIntent = new Intent(this, ExtractService.class);
		serviceIntent.putExtra(ExtractService.EXTRACT_FILE_PATH, fileToExtract);
		serviceIntent.putExtra(ExtractService.EXTRACT_FILE_NAME, new File(
				fileToExtract).getName());
		serviceIntent.putExtra(ExtractService.EXTRACT_DIR, extractToEditText
				.getText().toString());
		startService(serviceIntent);

	}

	private void showBrowseActivity() {
		final Intent chooserIntent = new Intent(this,
				DirectoryChooserActivity.class);

		// Optional: Allow users to create a new directory with a fixed
		// name.
		chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_NEW_DIR_NAME,
				"extracted");

		// REQUEST_DIRECTORY is a constant integer to identify the
		// request, e.g. 0
		startActivityForResult(chooserIntent, 111);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 111) {
			if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
				extractToEditText
						.setText(data
								.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR));
			}
		}

	}

}
