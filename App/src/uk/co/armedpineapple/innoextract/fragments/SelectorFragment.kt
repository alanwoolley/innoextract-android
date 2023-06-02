package uk.co.armedpineapple.innoextract.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import uk.co.armedpineapple.innoextract.databinding.FragmentSelectorBinding
import uk.co.armedpineapple.innoextract.layouts.SelectorFrameLayout
import uk.co.armedpineapple.innoextract.viewmodels.ExtractionViewModel

/**
 * A fragment allowing the selection of an extraction source and target.
 */
class SelectorFragment : androidx.fragment.app.Fragment() {

    private var _binding: FragmentSelectorBinding? = null
    private val binding get() = _binding!!
    private var fragmentInteractionListener: OnFragmentInteractionListener? = null
    private lateinit var extractionViewModel: ExtractionViewModel

    private fun refreshStages() {
        if (extractionViewModel.validationResult.value?.isValid == true) {
            binding.fileSelectorStage.state = SelectorFrameLayout.State.Complete
            if (extractionViewModel.target.value != null) {
                binding.directorySelectorStage.state = SelectorFrameLayout.State.Complete
                binding.extractStage.state = SelectorFrameLayout.State.Active
            } else {
                binding.directorySelectorStage.state = SelectorFrameLayout.State.Active
                binding.extractStage.state = SelectorFrameLayout.State.Inactive
            }
        } else if (extractionViewModel.validationResult.value == null) {
            binding.fileSelectorStage.state = SelectorFrameLayout.State.Active
            binding.directorySelectorStage.state = SelectorFrameLayout.State.Inactive
            binding.extractStage.state = SelectorFrameLayout.State.Inactive
        } else {
            binding.fileSelectorStage.state = SelectorFrameLayout.State.Warning
            binding.directorySelectorStage.state = SelectorFrameLayout.State.Inactive
            binding.extractStage.state = SelectorFrameLayout.State.Inactive
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractionViewModel = ViewModelProvider(requireActivity())[ExtractionViewModel::class.java]
        extractionViewModel.validationResult.observe(this) { refreshStages() }
        extractionViewModel.target.observe(this) { refreshStages() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectorBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onClickExecute() {
        fragmentInteractionListener?.onExtractButtonPressed()
    }

    private fun onClickDirBrowser() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        i.addCategory(Intent.CATEGORY_DEFAULT)
        startActivityForResult(
            Intent.createChooser(i, "Choose extract directory"), REQUEST_DIRECTORY
        )
    }

    private fun onClickFileSelect() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, REQUEST_PICK_FILE)
    }


    fun onNewFile(file: Uri) {
        extractionViewModel.onFileSelected(file)
        fragmentInteractionListener?.onFileSelected()
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
                if (resultCode == RESULT_OK) {
                    val directory: Uri? = data?.data
                    directory?.let {
                        extractionViewModel.updateTarget(it)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chooseFileButton.setOnClickListener { onClickFileSelect() }
        binding.chooseTargetButton.setOnClickListener { onClickDirBrowser() }
        binding.extractButton.setOnClickListener { onClickExecute() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            fragmentInteractionListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        fragmentInteractionListener = null
    }

    companion object {
        private const val REQUEST_PICK_FILE: Int = 1
        private const val REQUEST_DIRECTORY: Int = 2
    }
}
