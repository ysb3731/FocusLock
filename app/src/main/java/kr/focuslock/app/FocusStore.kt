package kr.focuslock.app

import android.content.Context
import java.util.Calendar

/** 세션·차단 앱·메모·기록을 SharedPreferences 하나에 저장합니다. */
class FocusStore(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("focus", Context.MODE_PRIVATE)

    // ── 설정 ──
    var lastMinutes: Int
        get() = p.getInt("lastMin", 25)
        set(v) = p.edit().putInt("lastMin", v).apply()

    var lastStrict: Boolean
        get() = p.getBoolean("lastStrict", true)
        set(v) = p.edit().putBoolean("lastStrict", v).apply()

    var blockedPackages: Set<String>
        get() = p.getStringSet("blocked", DEFAULT_BLOCKED)!!.toSet()
        set(v) = p.edit().putStringSet("blocked", v).apply()

    // ── 진행 중 세션 ──
    val startAt get() = p.getLong("startAt", 0L)
    val endAt get() = p.getLong("endAt", 0L)
    val minutes get() = p.getInt("minutes", 0)
    val strict get() = p.getBoolean("strict", true)
    val tempt get() = p.getInt("tempt", 0)

    fun isActive(now: Long = System.currentTimeMillis()) = endAt > now

    fun remainingMs(now: Long = System.currentTimeMillis()) = (endAt - now).coerceAtLeast(0L)

    fun start(min: Int, strictMode: Boolean) {
        val now = System.currentTimeMillis()
        p.edit()
            .putLong("startAt", now)
            .putLong("endAt", now + min * 60_000L)
            .putInt("minutes", min)
            .putBoolean("strict", strictMode)
            .putInt("tempt", 0)
            .putBoolean("recorded", false)
            .remove("allow")
            .apply()
        lastMinutes = min
        lastStrict = strictMode
    }

    fun addTempt() = p.edit().putInt("tempt", tempt + 1).apply()

    /** 끝났거나 포기한 세션을 기록에 남깁니다. 같은 세션은 한 번만 기록됩니다. */
    fun closeSession(done: Boolean) {
        if (startAt == 0L || p.getBoolean("recorded", true)) return
        val focused = if (done) minutes else ((System.currentTimeMillis() - startAt) / 60_000L).toInt()
        val line = "$startAt,$focused,${if (done) 1 else 0},$tempt"
        val list = sessionLines().toMutableList().apply { add(line) }.takeLast(500)
        p.edit()
            .putString("sessions", list.joinToString("\n"))
            .putBoolean("recorded", true)
            .putLong("endAt", 0L)
            .remove("allow")
            .apply()
    }

    /** 앱을 다시 열었을 때, 시간이 이미 끝난 세션이 있으면 완료로 기록합니다. */
    fun settleIfFinished() {
        if (startAt != 0L && !p.getBoolean("recorded", true) && endAt <= System.currentTimeMillis()) {
            // endAt이 0이면 포기로 이미 정리된 상태이므로 여기 오지 않음
            closeSession(true)
        }
    }

    // ── 유연 모드: 한 앱을 잠시 허용 ──
    fun allowFor(pkg: String, ms: Long) {
        p.edit().putString("allow", "$pkg,${System.currentTimeMillis() + ms}").apply()
    }

    fun isAllowed(pkg: String): Boolean {
        val raw = p.getString("allow", null) ?: return false
        val parts = raw.split(",")
        return parts.size == 2 && parts[0] == pkg && (parts[1].toLongOrNull() ?: 0L) > System.currentTimeMillis()
    }

    // ── 기록 ──
    data class Session(val start: Long, val focused: Int, val done: Boolean, val tempt: Int)

    private fun sessionLines() = (p.getString("sessions", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun sessions(): List<Session> = sessionLines().mapNotNull {
        val f = it.split(",")
        if (f.size < 4) null else Session(f[0].toLong(), f[1].toInt(), f[2] == "1", f[3].toInt())
    }

    fun todayDone(): Int = sessions().count { it.done && sameDay(it.start, System.currentTimeMillis()) }

    fun weekMinutes(): Int {
        val from = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        return sessions().filter { it.start >= from }.sumOf { it.focused }
    }

    fun weekTempt(): Int {
        val from = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        return sessions().filter { it.start >= from }.sumOf { it.tempt }
    }

    fun streak(): Int {
        val days = sessions().filter { it.done }.map { dayKey(it.start) }.toSet()
        val c = Calendar.getInstance()
        if (dayKey(c.timeInMillis) !in days) c.add(Calendar.DAY_OF_YEAR, -1)
        var n = 0
        while (dayKey(c.timeInMillis) in days) { n++; c.add(Calendar.DAY_OF_YEAR, -1) }
        return n
    }

    // ── 나중에 할 일 메모 ──
    fun memos(): List<String> = (p.getString("memos", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun addMemo(text: String) {
        val t = text.replace("\n", " ").trim()
        if (t.isEmpty()) return
        p.edit().putString("memos", (listOf(t) + memos()).take(100).joinToString("\n")).apply()
    }

    fun removeMemo(index: Int) {
        val m = memos().toMutableList()
        if (index in m.indices) { m.removeAt(index); p.edit().putString("memos", m.joinToString("\n")).apply() }
    }

    companion object {
        val DEFAULT_BLOCKED = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.facebook.katana",
            "com.nhn.android.search"    // 네이버
        )

        private fun dayKey(t: Long): String {
            val c = Calendar.getInstance().apply { timeInMillis = t }
            return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
        }

        fun sameDay(a: Long, b: Long) = dayKey(a) == dayKey(b)
    }
}
