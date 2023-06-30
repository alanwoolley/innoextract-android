package uk.co.armedpineapple.innoextract.layouts

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.descendants
import uk.co.armedpineapple.innoextract.R
import uk.co.armedpineapple.innoextract.databinding.NumberedBoxBinding


class SelectorFrameLayout : FrameLayout {
    private var contentView: FrameLayout?
    private var _binding: NumberedBoxBinding? = null
    private val binding get() = _binding!!

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context, attrs, 0, 0
    )


    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        _binding = NumberedBoxBinding.inflate(LayoutInflater.from(context), this, true)
        contentView = binding.contentFrame

        context.theme.obtainStyledAttributes(
            attrs, R.styleable.SelectorFrameLayout, defStyleAttr, defStyleRes
        ).apply {
            val stateInt = getInt(R.styleable.SelectorFrameLayout_state, 0)
            state = State.values().single { it.ordinal == stateInt }

            refreshState(state, animate = false)

            val stageIndex = getInt(R.styleable.SelectorFrameLayout_stageIndex, 1)
            binding.numberView.text = stageIndex.toString()
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (contentView == null) {
            super.addView(child, index, params)
        } else {
            //Forward these calls to the content view
            contentView!!.addView(child, index, params)
            refreshState(state, animate = false)
        }
    }

    private fun refreshState(newState: State, animate: Boolean) {
        val alphaFrom = this.alpha
        val alphaTo: Float
        var colorTo: Int = ContextCompat.getColor(context, R.color.secondaryColor)
        var foregroundColor: Int = ContextCompat.getColor(context, R.color.secondaryTextColor)
        var showIcon = false
        var iconRes: Int? = null

        when (newState) {
            State.Active -> {
                alphaTo = 0.8f
            }

            State.Inactive -> {
                alphaTo = 0.05f
            }

            State.Complete -> {
                alphaTo = 0.8f
                colorTo = Color.rgb(200, 255, 135)
                foregroundColor = ContextCompat.getColor(context, R.color.secondaryColor)
                showIcon = true
                iconRes = R.drawable.check
            }

            State.Warning -> {
                alphaTo = 0.8f
                colorTo = Color.RED
                showIcon = true
                iconRes = R.drawable.ic_not_allowed
            }
        }

        if (animate) {
            val alphaAnimation = ValueAnimator.ofFloat(alphaFrom, alphaTo).apply {
                duration = 1000
                addUpdateListener { updatedAnimation ->
                    this@SelectorFrameLayout.alpha = updatedAnimation.animatedValue as Float
                }
            }
            alphaAnimation.start()
        } else {
            alpha = alphaTo
        }
        binding.outerImage.backgroundTintList = ColorStateList.valueOf(colorTo)
        binding.innerImage.imageTintList = ColorStateList.valueOf(colorTo)

        contentView?.descendants?.let {
            for (view in it) {
                if (view.tag == "recolor" && view is TextView) {
                    view.setTextColor(foregroundColor)
                }
            }
        }


        iconRes?.let {
            binding.statusImage.setImageDrawable(
                AppCompatResources.getDrawable(
                    context, iconRes
                )
            )
        }
        binding.statusImage.visibility = if (showIcon) VISIBLE else GONE
        binding.numberView.visibility = if (showIcon) GONE else VISIBLE
    }

    var state: State = State.Active
        set(value) {
            field = value
            refreshState(value, animate = true)
            invalidate()
        }

    enum class State {
        Active, Inactive, Complete, Warning
    }
}