package kr.focuslock.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 집중 시간이 끝나면 알림을 보냅니다. */
class FinishReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = FocusStore(context)
        if (store.isActive()) return          // 시간이 아직 남았으면(재설정 등) 무시
        val min = store.minutes
        val tempt = store.tempt
        store.settleIfFinished()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "집중 완료", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = if (tempt > 0) "유혹을 ${tempt}번 이겨냈어요." else "한 번도 흔들리지 않았어요."
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${min}분 집중 완료")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try { nm.notify(1, n) } catch (_: SecurityException) { /* 알림 권한 거부 시 */ }
    }

    companion object {
        private const val CHANNEL = "finish"

        private fun pending(context: Context) = PendingIntent.getBroadcast(
            context, 0, Intent(context, FinishReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        /** 정확한 알람 권한 없이도 쓰는 방식(수십 초 오차 가능). */
        fun schedule(context: Context, at: Long) {
            context.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
        }

        fun cancel(context: Context) {
            context.getSystemService(AlarmManager::class.java).cancel(pending(context))
        }
    }
}
