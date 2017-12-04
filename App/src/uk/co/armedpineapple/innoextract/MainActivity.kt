package uk.co.armedpineapple.innoextract

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.karumi.dexter.Dexter
import uk.co.armedpineapple.innoextract.permissions.PermissionsDialog
import java.io.File

class MainActivity : SelectorFragment.OnFragmentInteractionListener, AppCompatActivity() {


    override fun onExtractButtonPressed(extractFile: File, extractTo: File) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(PermissionsDialog(this, {onResult(it)}))
                .check()

        setContentView(R.layout.activity_main)
    }

    private fun onResult(success: Boolean) {
        if (success) {
            Log.d("TEST", "SUCCESS!!");
        } else {
            finish();
        }
    }


}
