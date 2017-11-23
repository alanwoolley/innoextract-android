package uk.co.armedpineapple.innoextract

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.io.File
import com.aditya.filebrowser.FileBrowser
import android.content.Intent
import com.aditya.filebrowser.Constants
import com.aditya.filebrowser.FileChooser
import kotlinx.android.synthetic.main.fragment_selector.*
import net.rdrei.android.dirchooser.DirectoryChooserActivity
import net.rdrei.android.dirchooser.DirectoryChooserConfig




class SelectorFragment : Fragment() {

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_selector, container, false)
    }



    fun onClickDirBrowser() {
        val chooserIntent = Intent(context!!.applicationContext, DirectoryChooserActivity::class.java)

        val config = DirectoryChooserConfig.builder()
                .newDirectoryName("InnoExtract")
                .allowReadOnlyDirectory(false)
                .allowNewDirectoryNameModification(true)
                .build()

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config)

        startActivityForResult(chooserIntent, REQUEST_DIRECTORY)
    }

    fun onClickFileSelect() {
//        if (mListener != null) {
//            //mListener!!.onExtractButtonPressed(
//        }

        val fileChooserIntent = Intent(context!!.applicationContext, FileChooser::class.java)
        fileChooserIntent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal)
        //fileChooserIntent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "exe")


        startActivityForResult(fileChooserIntent, PICK_FILE_REQUEST)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileSelectButton.setOnClickListener({ onClickFileSelect() })
        dirChooseButton.setOnClickListener({ onClickDirBrowser() })

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
        fun onExtractButtonPressed(extractFile: File, extractTo : File)
    }

    companion object {
        private const val PICK_FILE_REQUEST: Int = 1
        private const val REQUEST_DIRECTORY: Int = 2
    }
}
