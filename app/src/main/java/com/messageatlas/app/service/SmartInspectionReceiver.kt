package com.messageatlas.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.messageatlas.app.MessageAtlasApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SmartInspectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            SmartInspector.ACTION_INSPECT -> {
                // 先续上下一轮闹钟，AI 研判再慢也不会中断定时循环
                SmartInspector.scheduleNextBlocking(context)
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        // 后台广播必须在系统超时（约 60 秒）前结束，否则进程会被判定超时；
                        // 宁可本轮放弃并如实记录，也不让整个应用被杀。
                        withTimeout(INSPECTION_BUDGET_MS) { SmartInspector.inspectNow(context) }
                    } catch (_: TimeoutCancellationException) {
                        runCatching {
                            (context.applicationContext as? MessageAtlasApp)?.settings
                                ?.recordInspectionFailure("本轮巡检超过 50 秒已安全中止，下一周期自动重试")
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                SmartInspector.scheduleNextBlocking(context)
            }
        }
    }

    private companion object {
        /** 低于 Android 后台广播的 60 秒上限，避免 AI 网络调用拖垮广播接收器。 */
        const val INSPECTION_BUDGET_MS = 50_000L
    }
}
