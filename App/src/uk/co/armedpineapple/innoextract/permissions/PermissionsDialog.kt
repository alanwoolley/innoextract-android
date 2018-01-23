package uk.co.armedpineapple.innoextract.permissions

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.app.AlertDialog
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
import uk.co.armedpineapple.innoextract.R


class PermissionsDialog(private val context: Context, private val onResult: (success: Boolean) -> Unit) : BaseMultiplePermissionsListener() {
    private val title: Int = R.string.permission_title
    private val message: Int = R.string.permission_message
    private val positiveButtonText: Int = R.string.permission_button
    private val icon: Drawable? = null

    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
        super.onPermissionsChecked(report)

        if (!report.areAllPermissionsGranted()) {
            showDialog()
        } else {
            onResult(true)
        }
    }

    private fun showDialog() {
        AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { dialog, _ -> dialog.dismiss() }
                .setIcon(icon)
                .setOnDismissListener { onResult(false); }
                .show()
    }

}
