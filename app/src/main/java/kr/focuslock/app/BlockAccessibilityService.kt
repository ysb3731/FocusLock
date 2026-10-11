package kr.focuslock.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * 화면 맨 앞에 뜬 앱의 패키지명만 확인합니다.
 * 집중 중이고 차단 목록에 있는 앱이면 홈으로 보내고 차단 화면을 띄웁니다.
 * (화면 내용·입력값은 읽지 않습니다: canRetrieveWindowContent=false)
 */
class BlockAccessibilityService : AccessibilityService() {

    private lateinit var store: FocusStore
    private var lastPkg = ""
    private var lastAt = 0L

    override fun onServiceConnected() {
        store = FocusStore(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (!store.isActive()) return
        if (pkg !in store.blockedPackages) return
        if (store.isAllowed(pkg)) return

        // 같은 앱의 연속 이벤트는 유혹 1회로만 셉니다.
        val now = System.currentTimeMillis()
        if (pkg != lastPkg || now - lastAt > 5_000) store.addTempt()
        lastPkg = pkg
        lastAt = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BlockActivity.EXTRA_PKG, pkg)
        )
    }

    override fun onInterrupt() {}
}
