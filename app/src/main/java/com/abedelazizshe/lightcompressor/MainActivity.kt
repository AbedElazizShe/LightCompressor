package com.abedelazizshe.lightcompressor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abedelazizshe.lightcompressor.databinding.ActivityMainBinding
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import kotlinx.coroutines.launch

/**
 * Created by AbedElaziz Shehadeh on 26 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val REQUEST_SELECT_VIDEO = 0
        const val REQUEST_CAPTURE_VIDEO = 1
    }

    private lateinit var adapter: RecyclerViewAdapter

    private val compressors: MutableList<VideoCompressor> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setReadStoragePermission()

        binding.pickVideo.setOnClickListener {
            pickVideo()
        }

        binding.recordVideo.setOnClickListener {
            dispatchTakeVideoIntent()
        }

        binding.cancel.setOnClickListener {
            compressors.forEach {
                it.cancel()
            }
        }

        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = RecyclerViewAdapter(applicationContext, mutableListOf())
        recyclerview.adapter = adapter
    }

    //Pick a video file from device
    private fun pickVideo() {
        val intent = Intent()
        intent.apply {
            type = "video/*"
            action = Intent.ACTION_PICK
        }
        intent.putExtra(
            Intent.EXTRA_ALLOW_MULTIPLE,
            true
        )
        startActivityForResult(Intent.createChooser(intent, "Select video"), REQUEST_SELECT_VIDEO)
    }

    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_CAPTURE_VIDEO)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {

        reset()

        if (resultCode == Activity.RESULT_OK)
            if (requestCode == REQUEST_SELECT_VIDEO || requestCode == REQUEST_CAPTURE_VIDEO) {
                handleResult(intent)
            }

        super.onActivityResult(requestCode, resultCode, intent)
    }

    private fun handleResult(data: Intent?) {
        val clipData: ClipData? = data?.clipData
        val uris = mutableListOf<Uri>()
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val videoItem = clipData.getItemAt(i)
                uris.add(videoItem.uri)
            }
            processVideo(uris)
        } else if (data != null && data.data != null) {
            val uri = data.data
            uris.add(uri!!)
            processVideo(uris)
        }
    }

    private fun reset() {
        binding.mainContents.visibility = View.GONE
    }

    private fun setReadStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                        1
                    )
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1
                    )
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun processVideo(uris: List<Uri>) {
        binding.mainContents.visibility = View.VISIBLE

        uris.forEach { uri ->
            lifecycleScope.launch {
                val compressor = VideoCompressor.createInstance(uri = uri)
                compressors.add(compressor)
                compressor.start(
                    context = applicationContext,
                    isStreamable = false,
                    sharedStorageConfiguration = SharedStorageConfiguration(
                        saveAt = SaveLocation.movies,
                        subFolderName = "my-demo-videos"
                    ),
                    /*appSpecificStorageConfiguration = AppSpecificStorageConfiguration(
                        subFolderName = "temp-videos"
                    ),*/
                    configureWith = Configuration(
                        quality = VideoQuality.LOW,
                        videoName = uri.pathSegments.last(),
                        isMinBitrateCheckEnabled = true,
                    ),
                    listener = object : CompressionListener {
                        override fun onProgress(percent: Float) {
                            //Update UI
                            if (percent <= 100)
                                runOnUiThread {
                                    adapter.updateProgressForUri(uri, percent)
                                }
                        }

                        override fun onStart() {
                            adapter.addData(VideoDetailsModel("", uri, ""))
                        }

                        override fun onSuccess(size: Long, path: String?) {
                            adapter.refreshItem(VideoDetailsModel(
                                path,
                                uri,
                                getFileSize(size),
                                100F
                            ))
                        }

                        override fun onFailure(failureMessage: String) {
                            Log.wtf("failureMessage", failureMessage)
                        }

                        override fun onCancelled() {
                            Log.wtf("TAG", "compression has been cancelled")
                            // make UI changes, cleanup, etc
                        }
                    },
                )
            }
        }
    }
}
