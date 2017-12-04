package uk.co.armedpineapple.innoextract

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aditya.filebrowser.Constants
import com.aditya.filebrowser.FileChooser
import kotlinx.android.synthetic.main.fragment_selector.*
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig
import java.io.File
import kotlin.properties.Delegates


class SelectorFragment : Fragment() {

    var file by Delegates.observable<File?>(null) { _, _, _ ->
        refreshButtons()
    }

    var target by Delegates.observable<File?>(null) { _, _, _ ->
        refreshButtons()
    }

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =

            inflater.inflate(R.layout.fragment_selector, container, false)

    private fun refreshButtons() {
        val isValidFile = file != null && file!!.exists() && file!!.isFile && file!!.canRead()
        val isValidTarget = target != null && target!!.exists() && target!!.isDirectory && target!!.canWrite()

        if (isValidFile && isValidTarget) {
            fabExecute.show()
        } else {
            fabExecute.hide()
        }

    }

    private fun onClickExecute() {
        if (file == null || target == null) {
            throw IllegalStateException("File or target is null")
        }

        mListener?.onExtractButtonPressed(file!!, target!!)
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
        //fileChooserIntent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "exe")


        startActivityForResult(fileChooserIntent, REQUEST_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PICK_FILE -> {
                if (resultCode == RESULT_OK) {
                    val file: Uri? = data?.data
                    if (file != null) {
                        this.file = File(file.path)
                        fileTextView.text = file.lastPathSegment
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
        fileSelectButton.setOnClickListener({ onClickFileSelect() })
        dirChooseButton.setOnClickListener({ onClickDirBrowser() })
        fabExecute.setOnClickListener({ onClickExecute() })
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


    interface OnFragmentInteractionListener {
        fun onExtractButtonPressed(extractFile: File, extractTo: File)
    }

    companion object {
        private const val REQUEST_PICK_FILE: Int = 1
        private const val REQUEST_DIRECTORY: Int = 2
    }
}
