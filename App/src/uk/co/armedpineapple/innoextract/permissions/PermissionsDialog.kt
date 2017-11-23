package uk.co.armedpineapple.innoextract.permissions

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.support.v7.app.AlertDialog

import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener


class PermissionsDialog(private val context: Context, private val onResult: (success: Boolean) -> Unit) : BaseMultiplePermissionsListener() {
    private val title: String = "Title"
    private val message: String = "Message"
    private val positiveButtonText: String = "Button"
    private val icon: Drawable? = null

    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
        super.onPermissionsChecked(report)

        if (!report.areAllPermissionsGranted()) {
            showDialog()
        } else {
            onResult(true);
        }
    }

    private fun showDialog() {
        AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { dialog, which -> dialog.dismiss() }
                .setIcon(icon)
                .setOnDismissListener { onResult(false); }
                .show()
    }

    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
        super.onPermissionRationaleShouldBeShown(permissions, token)
    }
}
