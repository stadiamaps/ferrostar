package com.stadiamaps.ferrostar.core.service

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import com.stadiamaps.ferrostar.core.R

class NotificationLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

  // TODO: Improve the default colors
  private val defaultForegroundColor = Color.WHITE
  private val defaultBackgroundColor = Color.GREEN

  init {
    val a = context.obtainStyledAttributes(attrs, R.styleable.NotificationLayout, defStyleAttr, 0)
    val foregroundColor = a.getColor(R.styleable.NotificationLayout_foregroundColor, defaultForegroundColor)
    val backgroundColor = a.getColor(R.styleable.NotificationLayout_backgroundColor, defaultBackgroundColor)

    // Apply the colors to the layout
    setBackgroundColor(backgroundColor)




    a.recycle()
  }
}
