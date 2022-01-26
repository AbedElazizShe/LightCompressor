package com.abedelazizshe.lightcompressor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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
    private val uris = mutableListOf<Uri>()
    private val adapter = RecyclerViewAdapter(mutableListOf()) {
        VideoPlayerActivity.start(this, it.playableVideoPath)
    }
    private val intentPickVideo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        reset()
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data?.clipData != null) {
                for (i in 0 until it.data!!.clipData!!.itemCount) {
                    val videoItem = it.data!!.clipData!!.getItemAt(i)
                    uris.add(videoItem.uri)
                }
                processVideo()
            } else if (it.data?.data != null) {
                uris.add(it.data!!.data!!)
                processVideo()
            }
        }
    }

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

        findViewById<RecyclerView>(R.id.recyclerview).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MainActivity.adapter
        }
    }

    //Pick a video file from device
    private fun pickVideo() {
        val intent = Intent().apply {
            type = "video/*"
            action = Intent.ACTION_PICK
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        intentPickVideo.launch(Intent.createChooser(intent, "Select video"))
    }

    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                intentPickVideo.launch(takeVideoIntent)
            }
        }
    }

    private fun reset() {
        uris.clear()
        adapter.clear()
        mainContents.visibility = View.GONE
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
            VideoCompressor.start(
                context = applicationContext,
                uris = uris,
                isStreamable = true,
                saveAt = Environment.DIRECTORY_MOVIES,
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {
                        //Update UI
                        if (percent <= 100 && percent.toInt() % 5 == 0)
                            runOnUiThread {
                                adapter.updateItem(index, "", uris[index], "", percent)
                            }
                    }

                    override fun onStart(index: Int) {
                        adapter.videos.add(
                            index,
                            VideoDetailsModel("", uris[index], "")
                        )
                        adapter.notifyItemInserted(index)
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        adapter.updateItem(index, path, uris[index], getFileSize(size), 100F)
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        Log.wtf("failureMessage", failureMessage)
                    }

                    override fun onCancelled(index: Int) {
                        Log.wtf("TAG", "compression has been cancelled")
                        // make UI changes, cleanup, etc
                    }
                },
                configureWith = Configuration(
                    quality = VideoQuality.LOW,
                    isMinBitrateCheckEnabled = true,
                )
            )
        }
    }
}
