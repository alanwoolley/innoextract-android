package uk.co.armedpineapple.innoextract.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import uk.co.armedpineapple.innoextract.databinding.FragmentProgressBinding
import uk.co.armedpineapple.innoextract.viewmodels.ExtractionViewModel

/**
 * A fragment showing the process of an ongoing extraction.
 */
class ProgressFragment : androidx.fragment.app.Fragment() {
    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private var fragmentInteractionListener: OnFragmentInteractionListener? = null
    private lateinit var extractionViewModel: ExtractionViewModel

    private fun updateProgress(pct: Int) {
        if (!isAdded) {
            return
        }

        val percentView = binding.progressIndicator
        percentView.progress = pct
    }

    private fun updateStatus(status: String) {
        if (!isAdded) {
            return
        }

        val progressText = binding.progressText
        progressText.text = status
    }

    private fun onExtractFinished() {
        if (!isAdded) {
            return
        }
        val percentView = binding.progressText
        val returnButton = binding.returnButton
        activity?.runOnUiThread {
            percentView.text = "Complete"
            returnButton.visibility = View.VISIBLE
        }
    }

    private fun onExtractFailed() {
        if (!isAdded) {
            return
        }
        val percentView = binding.progressText
        val returnButton = binding.returnButton
        activity?.runOnUiThread {
            percentView.text = "Failed"
            returnButton.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun onReturnPressed() {
        fragmentInteractionListener?.onReturnButtonPressed()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            fragmentInteractionListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.returnButton.setOnClickListener { onReturnPressed() }
    }

    override fun onDetach() {
        super.onDetach()
        fragmentInteractionListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractionViewModel = ViewModelProvider(requireActivity())[ExtractionViewModel::class.java]
        extractionViewModel.progress.observe(this) { v -> updateProgress(v) }
        extractionViewModel.status.observe(this) { status -> updateStatus(status) }
        extractionViewModel.isComplete.observe(this) { complete -> onExtractFinished() }
        extractionViewModel.isError.observe(this) { error -> onExtractFailed() }
    }
}
