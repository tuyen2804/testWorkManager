package com.hlt.testworkmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hlt.testworkmanager.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: Mp3Adapter
    private lateinit var workManager: WorkManager
    private var downloadRequestId: String? = null

    private val REQUEST_CODE_POST_NOTIFICATIONS = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kiểm tra và yêu cầu quyền POST_NOTIFICATIONS trên Android 13 (API 33) trở lên
        checkAndRequestNotificationPermission()

        // Thiết lập RecyclerView để hiển thị danh sách MP3 đã tải
        adapter = Mp3Adapter(getDownloadedMp3s())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        workManager = WorkManager.getInstance(this)

        // Khi nhấn nút tải MP3
        binding.downloadButton.setOnClickListener {
            val mp3Url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            val inputData = Data.Builder().putString("file_url", mp3Url).build()

            // Tạo WorkRequest để tải tệp MP3
            val downloadRequest = OneTimeWorkRequestBuilder<DownloadMp3Worker>()
                .setInputData(inputData)
                .build()

            downloadRequestId = downloadRequest.id.toString()

            // Gửi yêu cầu tải xuống đến WorkManager
            workManager.enqueue(downloadRequest)

            // Theo dõi tiến trình tải xuống
            workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                .observe(this, Observer { workInfo ->
                    if (workInfo != null) {
                        when (workInfo.state) {
                            // Khi hoàn thành tải xuống
                            WorkInfo.State.SUCCEEDED -> {
                                binding.progressBar.visibility = View.GONE
                                updateMp3List() // Cập nhật danh sách tệp MP3 đã tải
                            }
                            // Khi tải xuống thất bại
                            WorkInfo.State.FAILED -> {
                                binding.progressBar.visibility = View.GONE
                                // Xử lý logic nếu có lỗi xảy ra
                            }
                            // Khi tải xuống đang diễn ra
                            WorkInfo.State.RUNNING -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            // Khi công việc đang chờ trong hàng đợi
                            WorkInfo.State.ENQUEUED -> {
                                binding.progressBar.visibility = View.GONE
                            }
                            // Khi công việc bị chặn
                            WorkInfo.State.BLOCKED -> {
                                binding.progressBar.visibility = View.GONE
                            }
                            // Khi công việc bị hủy
                            WorkInfo.State.CANCELLED -> {
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                    }
                })
        }
    }

    // Kiểm tra và yêu cầu quyền POST_NOTIFICATIONS trên Android 13+ (API 33)
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Yêu cầu quyền POST_NOTIFICATIONS nếu chưa được cấp
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            }
        }
    }

    // Lấy danh sách các tệp MP3 đã tải
    private fun getDownloadedMp3s(): List<File> {
        val downloadsDir = applicationContext.filesDir
        return downloadsDir?.listFiles { file -> file.extension == "mp3" }?.toList() ?: emptyList()
    }

    // Cập nhật danh sách MP3 sau khi tải xong
    private fun updateMp3List() {
        adapter.updateData(getDownloadedMp3s())
    }
}
