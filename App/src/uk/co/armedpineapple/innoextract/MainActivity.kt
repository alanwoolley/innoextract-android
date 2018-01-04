package uk.co.armedpineapple.innoextract

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.karumi.dexter.Dexter
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import uk.co.armedpineapple.innoextract.permissions.PermissionsDialog
import java.io.File

class MainActivity : SelectorFragment.OnFragmentInteractionListener, ProgressFragment.OnFragmentInteractionListener, IExtractService.ExtractCallback, AnkoLogger, AppCompatActivity() {

    override fun onProgress(value: Int, max: Int, speedBps: Int, remainingSeconds: Int) {
        //info("Success")
    }

    override fun onSuccess() {
      info("Success")
    }

    override fun onFailure(e: Exception) {
        info("Failure")
    }

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val LOG_TAG = "MainActivity"
    var isServiceBound = false
    var connection = Connection()
    lateinit private var extractService: IExtractService

    inner class Connection : ServiceConnection {
        override fun onServiceDisconnected(className: ComponentName?) {
            Log.d(LOG_TAG, "Service disconnected")
            isServiceBound = false
        }

        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            Log.d(LOG_TAG, "Service connected")
            extractService = (binder as ExtractService.ServiceBinder).service
            isServiceBound = true
        }

    }

    override fun onFileSelected(extractFile: File) {
        if (!isServiceBound) {
            return
        }

        fun reportStatus(valid: Boolean) {
            val selectorFragment = supportFragmentManager.findFragmentById(R.id.selectorFragment) as? SelectorFragment
            selectorFragment?.isFileValid = valid

            if (!valid) {
                toast("Selected file is not a valid Inno Setup file", Toast.LENGTH_LONG)
            }
        }

        if (extractFile.exists() && extractFile.canRead() && extractFile.isFile) {
            extractService.check(extractFile, ::reportStatus)
        } else {
            reportStatus(false)
        }
    }

    override fun onTargetSelected(target: File) {
        val selectorFragment = supportFragmentManager.findFragmentById(R.id.selectorFragment) as? SelectorFragment
        val valid = target.exists() && target.isDirectory && target.canWrite()
        selectorFragment?.isTargetValid = valid
    }

    override fun onExtractButtonPressed(extractFile: File, extractTo: File) {
        if (!isServiceBound) {
            return
        }
        toast("Extracting", Toast.LENGTH_SHORT)
        extractService.extract(extractFile, extractTo, this)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(PermissionsDialog(this, { onResult(it) }))
                .check()

        Log.d(LOG_TAG, "Binding service")
        val i = Intent(this, ExtractService::class.java)
        var serviceConnected = bindService(i, connection, Context.BIND_ABOVE_CLIENT or Context.BIND_AUTO_CREATE)
        Log.i(LOG_TAG, "Service connected? : " + serviceConnected)


        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        isServiceBound = false
    }

    private fun onResult(success: Boolean) {
        if (success) {
            Log.d("TEST", "SUCCESS!!")
        } else {
            finish()
        }
    }



}
