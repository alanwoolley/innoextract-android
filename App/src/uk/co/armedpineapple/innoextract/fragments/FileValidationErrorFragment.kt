package uk.co.armedpineapple.innoextract.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import uk.co.armedpineapple.innoextract.R


/**
 * A dialog that is shown when the selected file fails validation.
 *
 * It offers the opportunity to go to an external help page.
 */
class FileValidationErrorFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setView(R.layout.fragment_error)
            builder.setPositiveButton(getString(R.string.help)) { _, _ ->
                val url =
                    "https://github.com/alanwoolley/innoextract-android/wiki/Help:-My-Inno-Setup-installer-cannot-be-extracted"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }.setNegativeButton(getString(R.string.dismiss)) { _, _ ->
                // Do nothing. Keep default behaviour.
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}