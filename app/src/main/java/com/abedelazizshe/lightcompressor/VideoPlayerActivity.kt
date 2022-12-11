package com.abedelazizshe.lightcompressor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_video_player.*
import java.io.File

/**
 * Created by AbedElaziz Shehadeh on 26 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var exoPlayer: SimpleExoPlayer
    private var uri = ""

    companion object {
        fun start(context: Context, uri: String?) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
                .putExtra("uri", uri)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        intent?.extras?.let {
            uri = it.getString("uri", "")
        }
        initializePlayer()
    }

    private fun initializePlayer() {

        val trackSelector = DefaultTrackSelector(this)
        val loadControl = DefaultLoadControl()
        val rendererFactory = DefaultRenderersFactory(this)

        exoPlayer = SimpleExoPlayer.Builder(this, rendererFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()
    }

    private fun play(uri: Uri) {

        val userAgent = Util.getUserAgent(this, getString(R.string.app_name))
        val mediaSource = ProgressiveMediaSource
            .Factory(DefaultDataSourceFactory(this, userAgent))
            .createMediaSource(uri)

        ep_video_view.player = exoPlayer

        exoPlayer.prepare(mediaSource)
        exoPlayer.playWhenReady = true
    }

    override fun onStart() {
        super.onStart()
        playVideo()
    }

    private fun playVideo() {
        val file = File(uri)
        val localUri = Uri.fromFile(file)
        play(localUri)
    }

    override fun onStop() {
        super.onStop()
        exoPlayer.stop()
        exoPlayer.release()
    }
}
