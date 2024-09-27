package com.hlt.testworkmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

class PauseDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == DownloadMp3Worker.ACTION_PAUSE_DOWNLOAD) {
            // Dừng tất cả các công việc tải về hiện tại
            WorkManager.getInstance(context!!).cancelAllWork()
        }
    }
}
