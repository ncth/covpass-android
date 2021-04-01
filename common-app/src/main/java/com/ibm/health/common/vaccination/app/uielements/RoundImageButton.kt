package com.ibm.health.common.vaccination.app.uielements

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.ibm.health.common.vaccination.app.R
import com.ibm.health.common.vaccination.app.databinding.ButtonRoundImageBinding

/**
 * A round button with an image inside.
 */
public class RoundImageButton
@JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttributeSet: Int = 0
) : RelativeLayout(
    context,
    attributeSet,
    defStyleAttributeSet
) {

    private var binding: ButtonRoundImageBinding

    init {
        binding = ButtonRoundImageBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)
        val typedArray =
            context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.RoundImageButton,
                0,
                0
            )
        typedArray.getResourceId(
            R.styleable.RoundImageButton_round_button_icon,
            R.drawable.plus
        ).let { icon ->
            setIcon(icon)
        }
        typedArray.getResourceId(
            R.styleable.RoundImageButton_round_button_background,
            R.drawable.button_round_selector
        ).let { background ->
            setBackground(background)
        }
        typedArray.recycle()
    }

    public fun setIcon(iconId: Int) {
        setIcon(ContextCompat.getDrawable(context, iconId))
    }

    public fun setContentDescription(@StringRes description: Int) {
        binding.roundButtonContainer.contentDescription = resources.getString(description)
    }

    private fun setIcon(drawable: Drawable?) {
        binding.roundButtonIcon.setImageDrawable(drawable)
    }

    private fun setBackground(drawableId: Int) {
        background = ContextCompat.getDrawable(context, drawableId)
    }
}