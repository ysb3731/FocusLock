package kr.focuslock.app

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ScrollView
import kr.focuslock.app.Ui.column
import kr.focuslock.app.Ui.primary
import kr.focuslock.app.Ui.text

/** 설치된 앱 중에서 집중 시간에 막을 앱을 고릅니다. */
class AppPickerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = FocusStore(this)
        val chosen = store.blockedPackages.toMutableSet()

        val root = column(gap = 4)
        root.addView(text("차단할 앱 고르기", 22f, bold = true))
        root.addView(text("집중 중에 이 앱들이 열리면 차단 화면이 뜹니다.", 13f, Ui.MUTED))

        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedWith(compareBy({ it.first !in chosen }, { it.second }))

        apps.forEach { (pkg, label) ->
            root.addView(CheckBox(this).apply {
                text = label
                textSize = 16f
                setTextColor(Ui.FG)
                buttonTintList = ColorStateList.valueOf(Ui.ACCENT)
                minHeight = (48 * resources.displayMetrics.density).toInt()
                isChecked = pkg in chosen
                setOnCheckedChangeListener { _, on -> if (on) chosen.add(pkg) else chosen.remove(pkg) }
            }, Ui.full())
        }

        root.addView(primary("저장").apply {
            setOnClickListener { store.blockedPackages = chosen; finish() }
        }, Ui.full())

        setContentView(ScrollView(this).apply { setBackgroundColor(Ui.BG); addView(root) })
    }
}
