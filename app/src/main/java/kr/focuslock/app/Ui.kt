package kr.focuslock.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** XML 레이아웃 없이 화면을 짜기 위한 작은 도우미 모음. 색은 웹앱과 같습니다. */
object Ui {
    val BG = Color.parseColor("#0F1A17")
    val SURFACE = Color.parseColor("#17241F")
    val LINE = Color.parseColor("#2A3B35")
    val FG = Color.parseColor("#E8EFEA")
    val MUTED = Color.parseColor("#9FB3AA")
    val ACCENT = Color.parseColor("#E8A54B")
    val ON_ACCENT = Color.parseColor("#1A1206")

    fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    fun Context.column(padding: Int = 24, gap: Int = 16) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        dividerDrawable = GradientDrawable().apply { setSize(1, dp(gap)) }
        setBackgroundColor(BG)
    }

    fun Context.row(gap: Int = 8) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        dividerDrawable = GradientDrawable().apply { setSize(dp(gap), 1) }
    }

    fun Context.text(s: String, size: Float = 15f, color: Int = FG, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(0f, 1.3f)
        }

    fun Context.bigTime() = TextView(this).apply {
        textSize = 60f
        setTextColor(FG)
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
    }

    fun Context.bg(color: Int, radius: Int = 14, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    fun Context.primary(label: String) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 17f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(ON_ACCENT)
        background = bg(ACCENT, 16)
        stateListAnimator = null
        minHeight = dp(56)
    }

    fun Context.ghost(label: String) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(MUTED)
        background = bg(BG, 16, LINE)
        stateListAnimator = null
        minHeight = dp(50)
    }

    fun Context.chip(label: String, on: Boolean) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (on) ON_ACCENT else FG)
        background = if (on) bg(ACCENT, 12) else bg(SURFACE, 12, LINE)
        stateListAnimator = null
        minHeight = dp(48)
    }

    fun Context.input(hint: String) = EditText(this).apply {
        this.hint = hint
        setHintTextColor(MUTED)
        setTextColor(FG)
        textSize = 15f
        isSingleLine = true
        background = bg(SURFACE, 12, LINE)
        setPadding(dp(14), dp(12), dp(14), dp(12))
    }

    fun full() = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    fun weight() = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
}
