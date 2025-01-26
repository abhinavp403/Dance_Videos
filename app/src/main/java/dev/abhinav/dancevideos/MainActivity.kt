package dev.abhinav.dancevideos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import dev.abhinav.dancevideos.ui.theme.DanceVideosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DanceVideosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        MusicVideoText(
                            header = "Mauja Hi Mauja"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        YoutubeVideoPlayer(
                            youtubeURL = "https://www.youtube.com/watch?v=PaDaoNnOQaM",
                            startSecond = 45f,
                            pauseInterval = 30f
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val URL = "https://www.youtube.com/watch?"
    }
}

@Composable
fun MusicVideoText(
    header: String
){
    Text(
        text = header,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
    )
}

@Composable
fun YoutubeVideoPlayer(
    youtubeURL: String?,
    isPlaying: (Boolean) -> Unit = {},
    startSecond: Float,
    pauseInterval: Float,
    isLoading: (Boolean) -> Unit = {},
    onVideoEnded: () -> Unit = {}
){
    val context = LocalContext.current
    val mLifeCycleOwner = LocalLifecycleOwner.current
    val videoId = splitLinkForVideoId(youtubeURL)
    var player : YouTubePlayer ?= null
    val playerFragment = YouTubePlayerView(context)
    var currentSecond: Float

    val playerStateListener = object : AbstractYouTubePlayerListener() {
        override fun onReady(youTubePlayer: YouTubePlayer) {
            super.onReady(youTubePlayer)
            player = youTubePlayer
            youTubePlayer.cueVideo(videoId, startSecond)
        }

        override fun onStateChange(
            youTubePlayer: YouTubePlayer,
            state: PlayerConstants.PlayerState
        ) {
            super.onStateChange(youTubePlayer, state)
            when(state){
                PlayerConstants.PlayerState.BUFFERING -> {
                    isLoading.invoke(true)
                    isPlaying.invoke(false)
                }
                PlayerConstants.PlayerState.PLAYING -> {
                    isLoading.invoke(false)
                    isPlaying.invoke(true)
                }
                PlayerConstants.PlayerState.ENDED -> {
                    isPlaying.invoke(false)
                    isLoading.invoke(false)
                    onVideoEnded.invoke()
                }
                else -> {}
            }
        }

        override fun onError(
            youTubePlayer: YouTubePlayer,
            error: PlayerConstants.PlayerError
        ) {
            super.onError(youTubePlayer, error)
            Log.e("iFramePlayer Error Reason", "$error")
        }

        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
            currentSecond = second
            if (currentSecond >= startSecond + pauseInterval) {
                isPlaying.invoke(false)
                youTubePlayer.pause()
                onVideoEnded.invoke()
            }
        }
    }

    val playerBuilder = IFramePlayerOptions.Builder().apply {
        controls(1)
        fullscreen(0)
        autoplay(0)
        rel(0)
    }

    AndroidView(
        modifier = Modifier.background(Color.DarkGray),
        factory = {
            playerFragment.apply {
                enableAutomaticInitialization = false
                initialize(playerStateListener, playerBuilder.build())
            }
        }
    )

    DisposableEffect(key1 = Unit, effect = {
        context.findActivity() ?: return@DisposableEffect onDispose {}
        onDispose {
            playerFragment.removeYouTubePlayerListener(playerStateListener)
            playerFragment.release()
            player = null
        }
    })

    DisposableEffect(mLifeCycleOwner) {
        val lifecycle = mLifeCycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    player?.play()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    player?.pause()
                }
                else -> {
                    //
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

private fun splitLinkForVideoId(url: String?): String {
    return (url!!.split("="))[1]
}

fun Context?.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}