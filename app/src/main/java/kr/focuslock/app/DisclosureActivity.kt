package kr.focuslock.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ScrollView
import kr.focuslock.app.Ui.applySystemBarInsets
import kr.focuslock.app.Ui.bg
import kr.focuslock.app.Ui.column
import kr.focuslock.app.Ui.dp
import kr.focuslock.app.Ui.ghost
import kr.focuslock.app.Ui.primary
import kr.focuslock.app.Ui.text

/**
 * 접근성 API 사용 전 사전 고지·동의 화면 (Google Play 접근성 API 정책의 "Prominent disclosure").
 * - 앱 안에서, 메뉴를 거치지 않고 표시됨 (첫 실행 시 자동, 이후 홈 화면 안내 카드에서 다시 열 수 있음)
 * - 접근하는 데이터, 사용 목적, 공유 여부를 설명
 * - "동의하고 설정 열기"를 눌러야만 접근성 설정으로 이동
 */
class DisclosureActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = column(padding = 24, gap = 14)

        root.addView(text("앱 차단 기능을 쓰려면\n접근성 권한이 필요해요", 24f, bold = true))
        root.addView(text(
            "숨 고르기는 Android 접근성 서비스(AccessibilityService API)를 사용해, 집중 시간 동안 내가 고른 앱이 열리지 않도록 막습니다.",
            15f
        ))

        root.addView(section(
            "무엇을 확인하나요",
            "지금 화면 맨 앞에 열린 앱이 무엇인지(앱 패키지 이름)만 확인합니다.\n" +
                "화면에 보이는 글자, 메시지, 입력한 내용, 비밀번호, 사진은 읽지 않습니다."
        ))
        root.addView(section(
            "어디에 쓰나요",
            "집중 시간이 진행 중이고, 열린 앱이 내가 고른 차단 목록에 있을 때만 홈 화면으로 돌려보내고 차단 안내 화면을 띄웁니다. " +
                "같은 순간 '참은 유혹' 횟수를 1회 기록합니다. 집중 중이 아닐 때는 아무 동작도 하지 않습니다."
        ))
        root.addView(section(
            "저장·공유하나요",
            "확인한 앱 이름은 저장하지 않습니다. 기록되는 것은 집중 시간, 유혹 횟수, 메모뿐이며 모두 이 휴대폰 안에만 저장됩니다. " +
                "이 앱은 인터넷 권한이 없어 어떤 데이터도 외부로 보내거나 다른 곳과 공유하지 않습니다."
        ))
        root.addView(section(
            "언제든 끌 수 있어요",
            "설정 › 접근성 › 설치된 앱 › '숨 고르기 앱 차단'에서 언제든 끌 수 있고, 앱을 지우면 모든 기록도 함께 지워집니다."
        ))

        root.addView(primary("동의하고 접근성 설정 열기").apply {
            setOnClickListener {
                setConsent(this@DisclosureActivity, true)
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                finish()
            }
        }, Ui.full())
        root.addView(ghost("동의하지 않음 (타이머만 사용)").apply {
            setOnClickListener {
                setConsent(this@DisclosureActivity, false)
                finish()
            }
        }, Ui.full())

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Ui.BG)
            addView(root)
            applySystemBarInsets()
        })
    }

    private fun section(title: String, body: String) = column(padding = 0, gap = 6).apply {
        background = bg(Ui.SURFACE, 14)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        addView(text(title, 15f, Ui.ACCENT, bold = true))
        addView(text(body, 14f))
    }

    companion object {
        private fun prefs(c: Context) = c.getSharedPreferences("focus", Context.MODE_PRIVATE)

        /** 사용자가 고지 화면을 한 번이라도 봤는지 (동의 여부와 무관) */
        fun seen(c: Context) = prefs(c).contains("a11yConsent")

        fun consented(c: Context) = prefs(c).getBoolean("a11yConsent", false)

        fun setConsent(c: Context, v: Boolean) = prefs(c).edit().putBoolean("a11yConsent", v).apply()
    }
}
