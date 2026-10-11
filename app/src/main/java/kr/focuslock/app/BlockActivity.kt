package kr.focuslock.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import kr.focuslock.app.Ui.column
import kr.focuslock.app.Ui.ghost
import kr.focuslock.app.Ui.input
import kr.focuslock.app.Ui.primary
import kr.focuslock.app.Ui.text

/** 차단한 앱을 열려고 할 때 뜨는 화면. */
class BlockActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        build(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        build(intent)
    }

    private fun build(intent: Intent) {
        handler.removeCallbacksAndMessages(null)
        val store = FocusStore(this)
        if (!store.isActive()) { finish(); return }

        val pkg = intent.getStringExtra(EXTRA_PKG)
        val appName = pkg?.let {
            try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString() }
            catch (_: PackageManager.NameNotFoundException) { null }
        }
        val leftMin = ((store.remainingMs() + 59_999) / 60_000).toInt()

        val root = column(padding = 28, gap = 18).apply { gravity = Gravity.CENTER }
        root.addView(text(
            if (appName != null) "${appName}은(는)\n${leftMin}분 뒤에 열려요" else "아직 ${leftMin}분 남았어요",
            26f, bold = true
        ).apply { gravity = Gravity.CENTER }, Ui.full())
        root.addView(text("방금 무엇을 하려고 했나요? 적어 두고 나중에 하세요.", 15f, Ui.MUTED).apply {
            gravity = Gravity.CENTER
        }, Ui.full())

        val memo = input("예: 쇼핑몰 배송 확인")
        root.addView(memo, Ui.full())
        root.addView(text("이번 세션 유혹 ${store.tempt}회", 14f, Ui.ACCENT).apply { gravity = Gravity.CENTER }, Ui.full())

        root.addView(primary("메모하고 집중으로 돌아가기").apply {
            setOnClickListener {
                store.addMemo(memo.text.toString())
                startActivity(Intent(this@BlockActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
        }, Ui.full())

        // 유연 모드: 10초 기다리면 이 앱만 5분 동안 허용
        if (!store.strict && pkg != null) {
            val open = ghost("10초 뒤에 열 수 있어요").apply { isEnabled = false }
            root.addView(open, Ui.full())
            var sec = 10
            val tick = object : Runnable {
                override fun run() {
                    sec--
                    if (sec <= 0) {
                        open.isEnabled = true
                        open.text = "그래도 5분만 열기"
                        open.setOnClickListener {
                            store.allowFor(pkg, 5 * 60_000L)
                            packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                            finish()
                        }
                    } else {
                        open.text = "${sec}초 뒤에 열 수 있어요"
                        handler.postDelayed(this, 1000)
                    }
                }
            }
            handler.postDelayed(tick, 1000)
        }

        setContentView(root)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /** 뒤로 가기로 차단 앱에 돌아가지 못하게 홈으로 보냅니다. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_PKG = "pkg"
    }
}
