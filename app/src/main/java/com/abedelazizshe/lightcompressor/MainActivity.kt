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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
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
        data.clear()
        adapter.notifyDataSetChanged()
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
    private fun processVideo() {
        mainContents.visibility = View.VISIBLE

        GlobalScope.launch {
            VideoCompressor.start(
                context = applicationContext,
                uris,
                isStreamable = false,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies,
                    videoName = "compressed_video"
                ),
//                appSpecificStorageConfiguration = AppSpecificStorageConfiguration(
//                    videoName = "compressed_video",
//                ),
                configureWith = Configuration(
                    quality = VideoQuality.LOW,
                    isMinBitrateCheckEnabled = true,
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {
                        //Update UI
                        if (percent <= 100 && percent.toInt() % 5 == 0)
                            runOnUiThread {
                                data[index] = VideoDetailsModel(
                                    "",
                                    uris[index],
                                    "",
                                    percent
                                )
                                adapter.notifyDataSetChanged()
                            }
                    }

                    override fun onStart(index: Int) {
                        data.add(
                            index,
                            VideoDetailsModel("", uris[index], "")
                        )
                        adapter.notifyDataSetChanged()
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        data[index] = VideoDetailsModel(
                            path,
                            uris[index],
                            getFileSize(size),
                            100F
                        )
                        adapter.notifyDataSetChanged()
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        Log.wtf("failureMessage", failureMessage)
                    }

                    override fun onCancelled(index: Int) {
                        Log.wtf("TAG", "compression has been cancelled")
                        // make UI changes, cleanup, etc
                    }
                },
            )
        }
    }
}
