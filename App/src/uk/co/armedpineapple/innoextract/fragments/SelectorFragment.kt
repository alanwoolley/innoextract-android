package uk.co.armedpineapple.innoextract.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.aditya.filebrowser.Constants
import com.aditya.filebrowser.FileChooser
import kotlinx.android.synthetic.main.fragment_selector.*
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import uk.co.armedpineapple.innoextract.R
import java.io.File
import kotlin.properties.Delegates


class SelectorFragment : Fragment() {

    private var file by Delegates.observable<File?>(null) { _, _, f ->

        if (f != null) {
            mListener?.onFileSelected(f)
        }

        refreshButtons()
    }

    private var target by Delegates.observable<File?>(null) { _, _, t ->

        if (t != null) {
            mListener?.onTargetSelected(t)
        }

        refreshButtons()
    }

    var isFileValid by Delegates.observable(false) { _, _, _ -> refreshButtons() }
    var isTargetValid by Delegates.observable(false) { _, _, _ -> refreshButtons() }


    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =

            inflater.inflate(R.layout.fragment_selector, container, false)


    private fun refreshButtons() {

        if (isFileValid && isTargetValid) {
            fabExecute.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_extract))
            fabExecute.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.accent))
        } else {
            fabExecute.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_not_allowed))
            fabExecute.backgroundTintList = ColorStateList.valueOf(Color.RED)
        }

        if (file != null && target != null) {
            fabExecute.show()
        } else {
            fabExecute.hide()
        }

    }

    private fun onClickExecute() {
        if (file == null || target == null) {
            throw IllegalStateException("File or target is null")
        }

        if (isFileValid && isTargetValid) {
            mListener?.onExtractButtonPressed(file!!, target!!)
        } else {
            showErrorDialog()
        }
    }

    private fun onClickDirBrowser() {
        val chooserIntent = Intent(context!!.applicationContext, DirectoryChooserActivity::class.java)

        val config = DirectoryChooserConfig.builder()
                .newDirectoryName("InnoExtract")
                .allowReadOnlyDirectory(false)
                .allowNewDirectoryNameModification(true)
                .build()

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)

        startActivityForResult(chooserIntent, REQUEST_DIRECTORY)
    }

    private fun onClickFileSelect() {
        val fileChooserIntent = Intent(context!!.applicationContext, FileChooser::class.java)
        fileChooserIntent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal)
        fileChooserIntent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "exe")

        startActivityForResult(fileChooserIntent, REQUEST_PICK_FILE)
    }

    fun onNewFile(file: Uri) {
        this.file = File(file.path)
        fileTextView.text = file.lastPathSegment
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PICK_FILE -> {
                if (resultCode == RESULT_OK) {
                    val file: Uri? = data?.data
                    if (file != null) {
                        onNewFile(file)
                    }
                }

            }
            REQUEST_DIRECTORY -> {
                if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                    val dir: String? = data?.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR)
                    if (dir != null) {
                        this.target = File(dir)
                        targetTextView.text = dir
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileSelectButton.setOnClickListener { onClickFileSelect() }
        dirChooseButton.setOnClickListener { onClickDirBrowser() }
        fabExecute.setOnClickListener { onClickExecute() }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun showErrorDialog() {
        val dialogBuilder = AlertDialog.Builder(this.context!!)
        val layout = layoutInflater.inflate(R.layout.dialog_error, null)
        val dialogText = layout.findViewById<TextView>(R.id.dialogText)

        dialogBuilder.setView(layout)
                .setPositiveButton("Close") { d: DialogInterface, _ -> d.cancel() }

        if (!isFileValid) {
            dialogBuilder.setTitle("Invalid File")
            dialogText.setText(R.string.unable_to_extract)
        } else {
            dialogBuilder.setTitle("Invalid Directory")
            dialogText.setText(R.string.cannot_write_to_directory)
        }
        dialogBuilder.create().show()

    }


    interface OnFragmentInteractionListener {
        fun onExtractButtonPressed(extractFile: File, extractTo: File)
        fun onFileSelected(extractFile: File)
        fun onTargetSelected(target: File)
    }

    companion object {
        private const val REQUEST_PICK_FILE: Int = 1
        private const val REQUEST_DIRECTORY: Int = 2
    }
}
