package uk.co.armedpineapple.innoextract.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import uk.co.armedpineapple.innoextract.R
import uk.co.armedpineapple.innoextract.databinding.FragmentIntroBinding

/**
 * A dialog that is shown on first app launch to explain the limitations.
 *
 */
class IntroFragment : androidx.fragment.app.DialogFragment() {

    private var _binding: FragmentIntroBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.IntroDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        dialog?.setTitle(R.string.app_name)
        _binding = FragmentIntroBinding.inflate(layoutInflater)
        binding.agreeButton.setOnClickListener { onButtonClick() }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onButtonClick() {
        dismiss()
    }
}
