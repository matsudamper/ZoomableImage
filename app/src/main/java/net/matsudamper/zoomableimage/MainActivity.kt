package net.matsudamper.zoomableimage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import net.matsudamper.zoomableimage.ui.theme.ZoomableImageTheme

internal class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            ZoomableImageTheme {
                var type by remember {
                    mutableStateOf(PageType.Normal)
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomAppBar(Modifier.fillMaxWidth()) {
                            BottomNavigationItem(
                                selected = type == PageType.Normal,
                                onClick = {
                                    type = PageType.Normal
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowLeft,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(text = "Normal")
                                },
                            )
                            BottomNavigationItem(
                                selected = type == PageType.HiQuality,
                                onClick = {
                                    type = PageType.HiQuality
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(text = "HiQuality")
                                },
                            )
                        }
                    },
                    content = { padding ->
                        Surface(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize()
                        ) {
                            when (type) {
                                PageType.Normal -> {
                                    var imageZoom by remember {
                                        mutableStateOf(1f)
                                    }
                                    var imageOffset by remember {
                                        mutableStateOf(Offset.Zero)
                                    }
                                    Image(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = imageZoom
                                                scaleY = imageZoom
                                                translationX = imageOffset.x
                                                translationY = imageOffset.y
                                            }
                                            .pointerInput(Unit) {
                                                detectTransformGestures { _, pan, zoom, _ ->
                                                    imageZoom *= zoom
                                                    imageOffset += pan
                                                }
                                            },
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(imageUrl)
                                                .size(Size.ORIGINAL)
                                                .build()
                                        ),
                                        contentDescription = null
                                    )
                                }
                                PageType.HiQuality -> {
                                    Zoomable(state = rememberZoomableState()) {
                                        Image(
                                            modifier = Modifier,
                                            painter = rememberAsyncImagePainter(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(imageUrl)
                                                    .size(Size.ORIGINAL)
                                                    .build()
                                            ),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

public enum class PageType {
    Normal,
    HiQuality,
    ;
}

private val imageUrl =
    "https://images.pexels.com/photos/2832034/pexels-photo-2832034.jpeg"
