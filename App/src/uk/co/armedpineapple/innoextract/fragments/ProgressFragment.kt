package uk.co.armedpineapple.innoextract.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import org.joda.time.Period
import org.joda.time.format.PeriodFormat
import uk.co.armedpineapple.innoextract.R

class ProgressFragment : Fragment() {

    private var mListener: OnFragmentInteractionListener? = null

    fun update(pct: Int, remainingSeconds: Int) {
        if (!isAdded) {
            return
        }

        val remainingText = "~ " + PeriodFormat
                .getDefault().print(Period((remainingSeconds * 1000).toLong())) + " remaining"

        val percentView = view?.findViewById<TextView>(R.id.percentTextView)
        val remainingView = view?.findViewById<TextView>(R.id.remainingTextView)

        activity?.runOnUiThread {
            percentView?.visibility = VISIBLE
            remainingView?.visibility = VISIBLE

            percentView?.text = String.format("%d%%", pct)
            remainingView?.text = remainingText
        }
    }

    fun onExtractFinished() {
        val percentView = view?.findViewById<TextView>(R.id.percentTextView)
        val remainingView = view?.findViewById<TextView>(R.id.remainingTextView)
        activity?.runOnUiThread {
            percentView?.visibility = GONE
            remainingView?.visibility = GONE
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_progress, container, false)

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


    interface OnFragmentInteractionListener
}
