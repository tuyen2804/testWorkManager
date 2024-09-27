package com.hlt.testworkmanager

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadMp3Worker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PAUSE_DOWNLOAD = "PAUSE_DOWNLOAD"
    }

    private var isPaused = false

    override fun doWork(): Result {
        // Lấy URL từ InputData
        val fileUrl = inputData.getString("file_url") ?: return Result.failure()

        return try {
            // Mở kết nối từ URL
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val totalSize = connection.contentLength // Kích thước file

            // Lưu vào thư mục nội bộ của ứng dụng
            val fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1)
            val file = File(applicationContext.filesDir, fileName)

            val outputStream = FileOutputStream(file, true) // Hỗ trợ ghi thêm (resume)
            val inputStream = connection.inputStream

            // Thiết lập thông báo tiến trình với nút "Tạm dừng"
            createNotificationChannel()
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            val pauseIntent = Intent(applicationContext, PauseDownloadReceiver::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
            }

            // Sử dụng FLAG_IMMUTABLE để tương thích với Android 12+
            val pausePendingIntent = PendingIntent.getBroadcast(applicationContext, 0, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Downloading MP3")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent) // Nút "Pause"

            var downloadedSize = 0
            val buffer = ByteArray(1024)
            var length: Int

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { length = it } > 0) {
                        if (isPaused) {
                            // Tạm dừng khi cần thiết
                            output.flush()
                            return Result.retry()
                        }

                        output.write(buffer, 0, length)
                        downloadedSize += length

                        // Cập nhật tiến trình tải xuống
                        val progress = (downloadedSize * 100 / totalSize)
                        builder.setProgress(100, progress, false)
                            .setContentText(getProgressText(downloadedSize.toLong(), totalSize.toLong()))

                        // Kiểm tra quyền POST_NOTIFICATIONS trước khi hiển thị thông báo
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED) {
                                // Nếu chưa có quyền, không thể hiển thị thông báo, quay lại
                                return Result.failure()
                            }
                        }

                        notificationManager.notify(NOTIFICATION_ID, builder.build())
                    }
                }
            }

            // Đảm bảo rằng file đã được ghi xong
            outputStream.flush()

            // Cập nhật thông báo thành công khi tải xong
            builder.setContentTitle("Download complete")
                .setProgress(0, 0, false)  // Loại bỏ thanh tiến trình
                .setOngoing(false)  // Cho phép người dùng xóa thông báo
                .setSmallIcon(android.R.drawable.stat_sys_download_done)  // Thay đổi biểu tượng thành biểu tượng tải xong

            // Cập nhật thông báo sau khi tải xong
            notificationManager.notify(NOTIFICATION_ID, builder.build())

            // Trả về thành công
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()

            // Nếu có lỗi, hủy bỏ thông báo và trả về thất bại
            NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
            return Result.failure()
        }
    }

    // Phương thức để tạm dừng tải xuống
    fun pauseDownload() {
        isPaused = true
    }

    private fun getProgressText(downloadedSize: Long, totalSize: Long): String {
        return "${formatFileSize(downloadedSize)}/${formatFileSize(totalSize)}"
    }

    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return if (mb > 1) {
            String.format("%.2f MB", mb)
        } else {
            String.format("%.2f KB", kb)
        }
    }

    // Tạo kênh thông báo nếu đang ở Android 8.0+ (API 26+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MP3 Download"
            val descriptionText = "Notification for MP3 download"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
