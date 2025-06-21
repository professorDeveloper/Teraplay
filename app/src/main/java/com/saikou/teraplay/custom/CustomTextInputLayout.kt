package com.saikou.teraplay.custom

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.saikou.teraplay.R

class CustomTextInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyle) {

    // custom error text + wrapping
    private var errorLayout: StaticLayout? = null
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.design_default_color_error)
        textSize = resources.getDimension(R.dimen.custom_error_text_size)
    }
    private val errorPadding = resources.getDimensionPixelOffset(R.dimen.custom_error_padding)

    // cache your default stroke & hint colors
    private val defaultStrokeColor = boxStrokeColor
    private val defaultHintColor = defaultHintTextColor
    private val errorColor = ContextCompat.getColor(context, R.color.design_default_color_error)

    override fun setError(error: CharSequence?) {
        // prepare wrapped error text or clear
        if (error.isNullOrEmpty()) {
            errorLayout = null
            // restore colors
            setBoxStrokeColor(defaultStrokeColor)
            this.setDefaultHintTextColor(defaultHintColor)
        } else {
            // build the StaticLayout for the error
            errorLayout = StaticLayout.Builder
                .obtain(error, 0, error.length, textPaint, maxErrorWidth())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .build()
            // switch to error colors
            setBoxStrokeColor(errorColor)
            this.setDefaultHintTextColor(ColorStateList.valueOf(errorColor))
        }

        // tell Android to re-measure & redraw
        requestLayout()
        invalidate()
    }

    private fun maxErrorWidth(): Int {
        return (editText?.width ?: (width - paddingLeft - paddingRight))
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        super.onMeasure(wSpec, hSpec)
        errorLayout?.let {
            val extra = it.height + errorPadding
            setMeasuredDimension(measuredWidth, measuredHeight + extra)
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        errorLayout?.let {
            val x = paddingLeft.toFloat()
            val y = (editText?.bottom ?: height) + errorPadding.toFloat()
            canvas.save()
            canvas.translate(x, y)
            it.draw(canvas)
            canvas.restore()
        }
    }
}
