package kr.focuslock.app

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kr.focuslock.app.Ui.applySystemBarInsets
import kr.focuslock.app.Ui.bigTime
import kr.focuslock.app.Ui.bg
import kr.focuslock.app.Ui.chip
import kr.focuslock.app.Ui.column
import kr.focuslock.app.Ui.dp
import kr.focuslock.app.Ui.ghost
import kr.focuslock.app.Ui.input
import kr.focuslock.app.Ui.primary
import kr.focuslock.app.Ui.row
import kr.focuslock.app.Ui.text

class MainActivity : Activity() {

    private lateinit var store: FocusStore
    private val handler = Handler(Looper.getMainLooper())
    private var selMin = 25
    private var selStrict = true
    private lateinit var root: LinearLayout

    private val durations = listOf(15, 25, 50, 90)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = FocusStore(this)
        selMin = store.lastMinutes
        selStrict = store.lastStrict
        root = column()
        setContentView(ScrollView(this).apply { setBackgroundColor(Ui.BG); addView(root); applySystemBarInsets() })

        // 첫 실행: 접근성 사용 전 사전 고지·동의 화면을 메뉴 없이 바로 보여줌
        if (savedInstanceState == null && !serviceEnabled() && !DisclosureActivity.seen(this)) {
            startActivity(Intent(this, DisclosureActivity::class.java))
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() {
        super.onResume()
        store.settleIfFinished()
        render()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    private fun render() {
        handler.removeCallbacksAndMessages(null)
        root.removeAllViews()
        if (store.isActive()) renderRunning() else renderSetup()
    }

    // ── 설정 화면 ──
    private fun renderSetup() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root.addView(text("폰 내려놓을 시간", 24f, bold = true))

        if (!serviceEnabled()) {
            root.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = bg(Ui.SURFACE, 14, Ui.ACCENT)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                addView(text("앱 차단이 꺼져 있어요", 15f, bold = true))
                addView(text("타이머는 지금도 쓸 수 있어요. 고른 앱까지 막으려면 접근성 권한이 필요합니다.", 13f, Ui.MUTED))
                addView(ghost("앱 차단 켜기 (권한 안내 보기)").apply {
                    setOnClickListener { startActivity(Intent(this@MainActivity, DisclosureActivity::class.java)) }
                }, Ui.full().apply { topMargin = dp(10) })
            })
        }

        root.addView(bigTime().apply { text = String.format("%02d:00", selMin) })
        root.addView(text(if (selStrict) "엄격 모드" else "유연 모드", 14f, Ui.MUTED).apply {
            gravity = android.view.Gravity.CENTER
        }, Ui.full())

        root.addView(text("집중 시간", 14f, Ui.MUTED, true))
        root.addView(row().apply {
            durations.forEach { m ->
                addView(chip("${m}분", m == selMin).apply {
                    setOnClickListener { selMin = m; render() }
                }, Ui.weight())
            }
        }, Ui.full())

        root.addView(text("모드", 14f, Ui.MUTED, true))
        root.addView(row().apply {
            addView(chip("엄격: 3초 눌러야 포기", selStrict).apply {
                textSize = 13f; setOnClickListener { selStrict = true; render() }
            }, Ui.weight())
            addView(chip("유연: 10초 뒤 열기", !selStrict).apply {
                textSize = 13f; setOnClickListener { selStrict = false; render() }
            }, Ui.weight())
        }, Ui.full())

        root.addView(ghost("차단할 앱 ${store.blockedPackages.size}개 · 편집").apply {
            setOnClickListener { startActivity(Intent(this@MainActivity, AppPickerActivity::class.java)) }
        }, Ui.full())

        root.addView(primary("집중 시작하기").apply {
            setOnClickListener {
                store.start(selMin, selStrict)
                FinishReceiver.schedule(this@MainActivity, store.endAt)
                render()
            }
        }, Ui.full())

        root.addView(text(
            "오늘 완료 ${store.todayDone()}회 · 연속 ${store.streak()}일 · 최근 7일 ${store.weekMinutes()}분 · 참은 유혹 ${store.weekTempt()}회",
            13f, Ui.MUTED
        ))

        renderMemos()
    }

    // ── 집중 중 화면 ──
    private fun renderRunning() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root.addView(text(if (store.strict) "엄격 모드로 집중 중" else "유연 모드로 집중 중", 14f, Ui.MUTED).apply {
            gravity = android.view.Gravity.CENTER
        }, Ui.full())

        val time = bigTime()
        root.addView(time, Ui.full())
        root.addView(text("남은 시간 / ${store.minutes}분", 14f, Ui.MUTED).apply {
            gravity = android.view.Gravity.CENTER
        }, Ui.full())

        root.addView(text("지금은 폰이 쉬는 시간", 19f, bold = true).apply {
            gravity = android.view.Gravity.CENTER
        }, Ui.full())

        val stats = text("", 14f, Ui.MUTED).apply { gravity = android.view.Gravity.CENTER }
        root.addView(stats, Ui.full())

        root.addView(ghost("폰 보고 싶어졌어요 (유혹 기록)").apply {
            setOnClickListener {
                store.addTempt()
                startActivity(Intent(this@MainActivity, BlockActivity::class.java))
            }
        }, Ui.full())

        val give = ghost(if (store.strict) "3초 길게 눌러 포기" else "포기하기")
        root.addView(give, Ui.full())
        if (store.strict) holdToGiveUp(give) else give.setOnClickListener { giveUp() }

        val tick = object : Runnable {
            override fun run() {
                val left = store.remainingMs()
                val s = (left + 999) / 1000
                time.text = String.format("%02d:%02d", s / 60, s % 60)
                stats.text = "이번 유혹 ${store.tempt}회 · 오늘 완료 ${store.todayDone()}회 · 연속 ${store.streak()}일"
                if (left <= 0) { store.closeSession(true); render() } else handler.postDelayed(this, 500)
            }
        }
        tick.run()
    }

    private fun holdToGiveUp(btn: View) {
        var left = 3
        val step = object : Runnable {
            override fun run() {
                left--
                if (left <= 0) giveUp() else {
                    (btn as TextView).text = "놓지 마세요… $left"
                    handler.postDelayed(this, 1000)
                }
            }
        }
        btn.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { left = 3; (v as TextView).text = "놓지 마세요… 3"; handler.postDelayed(step, 1000) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(step)
                    if (store.isActive()) (v as TextView).text = "3초 길게 눌러 포기"
                    if (e.action == MotionEvent.ACTION_UP) v.performClick()
                }
            }
            true
        }
    }

    private fun giveUp() {
        FinishReceiver.cancel(this)
        store.closeSession(false)
        render()
    }

    // ── 나중에 할 일 메모 ──
    private fun renderMemos() {
        root.addView(text("나중에 할 일", 14f, Ui.MUTED, true))
        val field = input("예: 친구 DM 답장")
        root.addView(row().apply {
            addView(field, Ui.weight())
            addView(chip("추가", true).apply {
                setOnClickListener { store.addMemo(field.text.toString()); render() }
            })
        }, Ui.full())
        val memos = store.memos()
        if (memos.isEmpty()) {
            root.addView(text("집중 중 떠오른 일을 적어 두면, 폰을 열 이유가 하나 줄어듭니다.", 13f, Ui.MUTED))
        }
        memos.forEachIndexed { i, m ->
            root.addView(row().apply {
                background = bg(Ui.SURFACE, 12)
                setPadding(dp(14), dp(6), dp(6), dp(6))
                gravity = android.view.Gravity.CENTER_VERTICAL
                addView(text(m, 15f), Ui.weight())
                addView(ghost("완료").apply {
                    minHeight = dp(40); background = null
                    setOnClickListener { store.removeMemo(i); render() }
                })
            }, Ui.full())
        }
    }

    private fun serviceEnabled(): Boolean {
        val want = ComponentName(this, BlockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(want, ignoreCase = true) }
    }
}
