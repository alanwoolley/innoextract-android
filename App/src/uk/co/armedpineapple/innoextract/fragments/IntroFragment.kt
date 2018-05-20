package uk.co.armedpineapple.innoextract.fragments


import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_intro.view.*
import uk.co.armedpineapple.innoextract.R

class IntroFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.IntroDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater  , container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        dialog.setTitle(R.string.app_name)
        val view = inflater.inflate(R.layout.fragment_intro, container, false)
        view.agreeButton.setOnClickListener {onButtonClick() }
        return view
    }

    private fun onButtonClick() {
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        val prefs = activity?.getPreferences(Context.MODE_PRIVATE)

        if (prefs != null) {
            with(prefs.edit()) {
                putBoolean(getString(R.string.pref_intro), true)
                commit()
            }
        }
    }
}
