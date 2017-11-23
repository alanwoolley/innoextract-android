package uk.co.armedpineapple.innoextract

import android.Manifest
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import uk.co.armedpineapple.innoextract.permissions.PermissionsDialog
import java.io.File

class MainActivity : ProgressFragment.OnFragmentInteractionListener, SelectorFragment.OnFragmentInteractionListener, AppCompatActivity() {


    override fun onExtractButtonPressed(extractFile: File, extractTo: File) {

    }

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
