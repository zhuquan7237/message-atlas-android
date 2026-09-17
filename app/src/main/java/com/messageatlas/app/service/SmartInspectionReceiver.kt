package com.messageatlas.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmartInspectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            SmartInspector.ACTION_INSPECT -> {
                // 先续上下一轮闹钟，AI 研判再慢也不会中断定时循环
                SmartInspector.scheduleNextBlocking(context)
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        SmartInspector.inspectNow(context)
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
}
