package com.abedelazizshe.lightcompressor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by AbedElaziz Shehadeh on 26 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_SELECT_VIDEO = 0
        const val REQUEST_CAPTURE_VIDEO = 1
    }

    private val uris = mutableListOf<Uri>()
    private val data = mutableListOf<VideoDetailsModel>()
    private var index = -1
    private var originalSize = ""
    private lateinit var adapter: RecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setReadStoragePermission()

        pickVideo.setOnClickListener {
            pickVideo()
        }

        recordVideo.setOnClickListener {
            dispatchTakeVideoIntent()
        }

        cancel.setOnClickListener {
            VideoCompressor.cancel()
        }

        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = RecyclerViewAdapter(applicationContext, data)
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
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val videoItem = clipData.getItemAt(i)
                uris.add(videoItem.uri)
            }
            processVideo()
        } else if (data != null && data.data != null) {
            val uri = data.data
            uris.add(uri!!)
            processVideo()
        }
    }

    private fun reset() {
        uris.clear()
        mainContents.visibility = View.GONE
        index = -1
        data.clear()
        adapter.notifyDataSetChanged()
    }

    private fun setReadStoragePermission() {
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

    @SuppressLint("SetTextI18n")
    private fun processVideo() {
        mainContents.visibility = View.VISIBLE

        GlobalScope.launch {
            var time = 0L
            VideoCompressor.start(
                context = applicationContext,
                uris,
                isStreamable = true,
                saveAt = Environment.DIRECTORY_MOVIES,
                listener = object : CompressionListener {
                    override fun onProgress(percent: Float) {
                        //Update UI
                        if (percent <= 100 && percent.toInt() % 5 == 0)
                            runOnUiThread {
                                progress.text = "${percent.toLong()}%"
                                progressBar.progress = percent.toInt()
                            }
                    }

                    override fun onStart(size: Long) {
                        index += 1
                        time = System.currentTimeMillis()
                        progress.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE

                        originalSize = getFileSize(size)
                        data.add(
                            index,
                            VideoDetailsModel("", uris[index], originalSize, "", "")
                        )
                        adapter.notifyDataSetChanged()
                        progress.text = ""
                        progressBar.progress = 0
                    }

                    override fun onSuccess(size: Long, path: String?) {
                        progress.visibility = View.GONE
                        progressBar.visibility = View.GONE

                        time = System.currentTimeMillis() - time
                        data[index] = VideoDetailsModel(
                            path,
                            uris[index],
                            originalSize,
                            getFileSize(size),
                            DateUtils.formatElapsedTime(time / 1000)
                        )
                        adapter.notifyDataSetChanged()
                    }

                    override fun onFailure(failureMessage: String) {
                        progress.text = failureMessage
                        Log.wtf("failureMessage", failureMessage)
                    }

                    override fun onCancelled() {
                        Log.wtf("TAG", "compression has been cancelled")
                        // make UI changes, cleanup, etc
                    }
                },
                configureWith = Configuration(
                    quality = VideoQuality.HIGH,
                    frameRate = 24,
                    isMinBitrateCheckEnabled = true,
                )
            )
        }
    }
}
