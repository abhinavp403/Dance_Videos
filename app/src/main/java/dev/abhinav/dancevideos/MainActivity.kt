package dev.abhinav.dancevideos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import dev.abhinav.dancevideos.MainActivity.Companion.URL
import dev.abhinav.dancevideos.ui.theme.DanceVideosTheme

val song = listOf (
    Song(
        name = "Mauja Hi Mauja",
        videoId = splitLinkForVideoId(URL + "v=PaDaoNnOQaM"),
        startSecond = 45f,
        pauseInterval = 30f
    ),
    Song(
        name = "Ek Pal Ka Jeena",
        videoId = splitLinkForVideoId(URL + "v=aGbPyM6lzBs"),
        startSecond = 60f,
        pauseInterval = 43f
    ),
    Song(
        name = "Senorita",
        videoId = splitLinkForVideoId(URL + "v=2Z0Put0teCM"),
        startSecond = 120f,
        pauseInterval = 20f
    ),
    Song(
        name = "Badtameez Dil",
        videoId = splitLinkForVideoId(URL + "v=II2EO3Nw4m0"),
        startSecond = 103f,
        pauseInterval = 20f
    ),
    Song(
        name = "Bhool Bhulaiyaa",
        videoId = splitLinkForVideoId(URL + "v=B9_nql5xBFo"),
        startSecond = 125f,
        pauseInterval = 17f
    ),
)

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
                    Column {
                        YoutubeVideoPlayer(songList = song)
                    }
                }
            }
        }
    }

    companion object {
        const val URL = "https://www.youtube.com/watch?"
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
    songList: List<Song>,
    isPlaying: (Boolean) -> Unit = {},
    isLoading: (Boolean) -> Unit = {},
    onVideoEnded: () -> Unit = {}
){
    val context = LocalContext.current
    val activity = context.findActivity()
    val mLifeCycleOwner = LocalLifecycleOwner.current
    var currentSecond: Float
    var activeFullscreenPlayer by remember { mutableStateOf<String?>(null) }
    val fullscreenContainers = remember { mutableStateMapOf<String, FrameLayout>() }
    val players = remember { mutableStateMapOf<String, YouTubePlayer>() }
    val videoIds = songList.map { it.videoId }

    Box(Modifier.fillMaxSize()) {
        if (activeFullscreenPlayer != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                factory = { fullscreenContainers[activeFullscreenPlayer]!! }
            )

            // Add BackHandler for fullscreen mode
            BackHandler(enabled = true) {
                players[activeFullscreenPlayer]?.toggleFullscreen()
                players.values.forEach { it.pause() }
                players.clear()
                fullscreenContainers.clear()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(videoIds.size) { index ->
                    MusicVideoText(
                        header = song[index].name
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        factory = { context ->
                            val youTubePlayerView = YouTubePlayerView(context).apply {
                                enableAutomaticInitialization = false
                                fullscreenContainers[videoIds[index]] = FrameLayout(context)

                                val playerOptions = IFramePlayerOptions.Builder()
                                    .controls(1)
                                    .fullscreen(1) // Enable fullscreen button
                                    .autoplay(0)
                                    .rel(0)
                                    .build()

                                addFullscreenListener(object : FullscreenListener {
                                    override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                                        activeFullscreenPlayer = videoIds[index]

                                        // Detach the fullscreen view from its current parent
                                        (fullscreenView.parent as? ViewGroup)?.removeView(fullscreenView)

                                        // Add fullscreen view to its container
                                        fullscreenContainers[videoIds[index]]?.apply {
                                            visibility = View.VISIBLE
                                            removeAllViews()
                                            addView(fullscreenView)
                                        }

                                        // Switch to landscape orientation
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        players[videoIds[index]]?.play()
                                    }

                                    override fun onExitFullscreen() {
                                        activeFullscreenPlayer = null

                                        // Clear fullscreen container
                                        fullscreenContainers[videoIds[index]]?.apply {
                                            removeAllViews()
                                            visibility = View.GONE
                                        }

                                        // Switch back to portrait orientation
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                })

                                initialize(object : AbstractYouTubePlayerListener() {
                                    override fun onReady(youTubePlayer: YouTubePlayer) {
                                        players[videoIds[index]] = youTubePlayer
                                        youTubePlayer.cueVideo(videoIds[index], songList[index].startSecond)
                                    }

                                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                        super.onError(youTubePlayer, error)
                                        Log.e("iFramePlayer Error Reason", "$error")
                                    }

                                    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
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

                                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                        currentSecond = second
                                        if (currentSecond >= songList[index].startSecond + songList[index].pauseInterval) {
                                            isPlaying.invoke(false)
                                            youTubePlayer.pause()
                                            onVideoEnded.invoke()
                                            youTubePlayer.seekTo(songList[index].startSecond)
                                        }
                                    }
                                }, playerOptions)
                            }

                            youTubePlayerView
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                players.values.forEach { it.pause() }
                players.clear()
                fullscreenContainers.clear()
            }
        }

        DisposableEffect(mLifeCycleOwner) {
            val lifecycle = mLifeCycleOwner.lifecycle
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> players.values.forEach { it.play() }
                    Lifecycle.Event.ON_PAUSE -> players.values.forEach { it.pause() }
                    else -> {}
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
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