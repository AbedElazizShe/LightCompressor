package com.abedelazizshe.lightcompressor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.IOException

/**
 * Created by AbedElaziz Shehadeh on 26 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_SELECT_VIDEO = 0
    }

    private lateinit var path: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setReadStoragePermission()

        fab.setOnClickListener {
            pickVideo()
        }

        cancel.setOnClickListener {
            VideoCompressor.cancel()
        }

        videoLayout.setOnClickListener { VideoPlayerActivity.start(this, path) }
    }

    //Pick a video file from device
    private fun pickVideo() {
        val intent = Intent()
        intent.apply {
            type = "video/*"
            action = Intent.ACTION_PICK
        }
        startActivityForResult(Intent.createChooser(intent, "Select video"), REQUEST_SELECT_VIDEO)
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        mainContents.visibility = View.GONE
        timeTaken.text = ""
        newSize.text = ""

        if (resultCode == Activity.RESULT_OK)
            if (requestCode == REQUEST_SELECT_VIDEO) {
                if (data != null && data.data != null) {
                    val uri = data.data
                    path = getMediaPath(this, uri)
                    val file = File(path)

                    mainContents.visibility = View.VISIBLE
                    GlideApp.with(this).load(uri).into(videoImage)

                    val downloadsPath =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val desFile = File(downloadsPath, "${System.currentTimeMillis()}_${file.name}")
                    if (desFile.exists()) {
                        desFile.delete()
                        try {
                            desFile.createNewFile()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }

                    var time = 0L

                    VideoCompressor.start(
                        path,
                        desFile.path,
                        object : CompressionListener {
                            override fun onProgress(percent: Float) {
                                //Update UI
                                runOnUiThread {
                                    progress.text = "${percent.toLong()}%"
                                    progressBar.progress = percent.toInt()
                                }

                            }

                            override fun onStart() {
                                time = System.currentTimeMillis()
                                progress.visibility = View.VISIBLE
                                progressBar.visibility = View.VISIBLE
                                originalSize.text = "Original size: ${getFileSize(file.length())}"
                            }

                            override fun onSuccess() {
                                val newSizeValue = desFile.length()

                                newSize.text =
                                    "Size after compression: ${getFileSize(newSizeValue)}"

                                time = System.currentTimeMillis() - time
                                timeTaken.text =
                                    "Duration: ${DateUtils.formatElapsedTime(time / 1000)}"

                                path = desFile.path

                                Handler().postDelayed({
                                    progress.visibility = View.GONE
                                    progressBar.visibility = View.GONE
                                }, 50)
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
                        VideoQuality.MEDIUM,
                        isMinBitRateEnabled = false,
                        keepOriginalResolution = false
                    )
                }
            }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setReadStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }
    }
}
