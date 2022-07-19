package net.matsudamper.zoomableimage

import android.graphics.pdf.PdfDocument.Page
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
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
                                        painter = rememberAsyncImagePainter(model = imageUrl),
                                        contentDescription = null
                                    )
                                }
                                PageType.HiQuality -> {
                                    ZoomableImage(
                                        modifier = Modifier.fillMaxSize(),
                                        painter = rememberAsyncImagePainter(model = imageUrl),
                                        maxZoomLevel = 10f,
                                        contentDescription = null,
                                    )
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
    "https://github.com/matsudamper/ZoomableImage/blob/main/network_resource/picture.jpg?raw=true"
