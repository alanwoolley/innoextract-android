package uk.co.armedpineapple.innoextract

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.karumi.dexter.Dexter
import uk.co.armedpineapple.innoextract.permissions.PermissionsDialog
import java.io.File

class MainActivity : SelectorFragment.OnFragmentInteractionListener, ProgressFragment.OnFragmentInteractionListener, AppCompatActivity() {
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val LOG_TAG = "MainActivity"
    var isServiceBound = false
    lateinit private var extractService: IExtractService

    inner class Connection : ServiceConnection{
        override fun onServiceDisconnected(className: ComponentName?) {
            Log.i(LOG_TAG, "Service disconnected")
            isServiceBound = false
        }

        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            Log.i(LOG_TAG, "Service connected")
            extractService = (binder as ExtractService.ServiceBinder).service
            isServiceBound = true
        }

    }

    override fun onExtractButtonPressed(extractFile: File, extractTo: File) {
        extractService.check(extractFile.absolutePath, { v -> Log.d(LOG_TAG, "Result: " + v)})
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(PermissionsDialog(this, {onResult(it)}))
                .check()


        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        Log.d(LOG_TAG, "Binding service")
        val i = Intent(this, ExtractService::class.java)
        var serviceConnected = bindService(i, Connection(), Context.BIND_ABOVE_CLIENT or Context.BIND_AUTO_CREATE)
        Log.i(LOG_TAG, "Service connected? : " + serviceConnected)

    }

    override fun onStop() {
        super.onStop()
    }

    private fun onResult(success: Boolean) {
        if (success) {
            Log.d("TEST", "SUCCESS!!")
        } else {
            finish()
        }
    }


}
