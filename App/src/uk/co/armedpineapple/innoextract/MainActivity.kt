package uk.co.armedpineapple.innoextract

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.karumi.dexter.Dexter
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import uk.co.armedpineapple.innoextract.permissions.PermissionsDialog
import uk.co.armedpineapple.innoextract.service.ExtractService
import uk.co.armedpineapple.innoextract.service.IExtractService
import java.io.File

class MainActivity : SelectorFragment.OnFragmentInteractionListener, ProgressFragment.OnFragmentInteractionListener, IExtractService.ExtractCallback, AnkoLogger, AppCompatActivity() {

    override fun onProgress(value: Int, max: Int, speedBps: Int, remainingSeconds: Int) {
        val progressFragment = supportFragmentManager.findFragmentById(R.id.progressFragment) as? ProgressFragment
        if (progressFragment != null) {
            val pct = (1.0f * value/max) * 100
            progressFragment.update(pct.toInt(), remainingSeconds)
        }
    }

    override fun onSuccess() {
        val progressFragment = supportFragmentManager.findFragmentById(R.id.progressFragment) as? ProgressFragment
        progressFragment?.onExtractFinished()
        showSelectorFragment()
    }

    override fun onFailure(e: Exception) {
        val progressFragment = supportFragmentManager.findFragmentById(R.id.progressFragment) as? ProgressFragment
        progressFragment?.onExtractFinished()
        showSelectorFragment()

    }


    var isServiceBound = false
    var connection = Connection()
    var launchIntent : Intent? = null

    private lateinit var extractService: IExtractService

    private fun hideSelectorFragment() {

        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom).hide(supportFragmentManager.findFragmentById(R.id.selectorFragment)).commitAllowingStateLoss()
    }

    private fun showSelectorFragment() {
        supportFragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom).show(supportFragmentManager.findFragmentById(R.id.selectorFragment)).commitAllowingStateLoss()
    }

    inner class Connection : ServiceConnection {
        override fun onServiceDisconnected(className: ComponentName?) {
            debug("Service disconnected")
            isServiceBound = false
        }

        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            debug("Service connected")
            extractService = (binder as ExtractService.ServiceBinder).service
            isServiceBound = true

            if (launchIntent != null) {
                val uri = launchIntent?.data
                if (uri != null) {
                    (supportFragmentManager.findFragmentById(R.id.selectorFragment) as? SelectorFragment)?.onNewFile(uri)
                    onFileSelected(File(uri.path))
                }
                launchIntent = null
            }
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
        hideSelectorFragment()
        toast("Extracting", Toast.LENGTH_SHORT)
        extractService.extract(extractFile, extractTo, this)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(PermissionsDialog(this, { onResult(it) }))
                .check()

        debug("Binding service")
        val i = Intent(this, ExtractService::class.java)
        val serviceConnected = bindService(i, connection, Context.BIND_ABOVE_CLIENT or Context.BIND_AUTO_CREATE)
        debug("Service connected? : " + serviceConnected)


        setContentView(R.layout.activity_main)

        launchIntent = intent;
    }

    override fun onStart() {
        super.onStart()
        if (isServiceBound && extractService.isExtractInProgress()) {
            hideSelectorFragment()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        isServiceBound = false
    }

    private fun onResult(success: Boolean) {
        if (success) {
            debug("Permissions granted")
        } else {
            finish()
        }
    }
}


