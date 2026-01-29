package io.github.numq.grokviewer.application

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.numq.grokviewer.archive.archiveModule
import io.github.numq.grokviewer.content.contentModule
import io.github.numq.grokviewer.overview.OverviewView
import io.github.numq.grokviewer.overview.overviewModule
import io.github.numq.grokviewer.save.saveModule
import io.github.numq.grokviewer.theme.Theme
import org.koin.core.context.startKoin
import java.awt.Color
import java.awt.Dimension

const val APP_NAME = "Grok Viewer"

fun main() {
    startKoin { modules(archiveModule, contentModule, overviewModule, saveModule) }

    application {
        val windowState = rememberWindowState()

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = APP_NAME,
            undecorated = true,
            transparent = true,
            resizable = true,
        ) {
            window.background = Color(MaterialTheme.colorScheme.background.toArgb())

            LaunchedEffect(windowState) {
                window.minimumSize = Dimension(512, 512)
            }

            Theme(darkTheme = isSystemInDarkTheme()) {
                Surface {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WindowDraggableArea(
                                modifier = Modifier.weight(1f).pointerInput(windowState) {
                                    detectTapGestures(onDoubleTap = {
                                        windowState.placement = when (windowState.placement) {
                                            WindowPlacement.Fullscreen -> WindowPlacement.Floating

                                            else -> WindowPlacement.Fullscreen
                                        }
                                    })
                                }) {
                                Text(
                                    text = APP_NAME,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            IconButton(onClick = {
                                windowState.isMinimized = !windowState.isMinimized
                            }, modifier = Modifier.fillMaxHeight(1f).aspectRatio(1f), shape = RectangleShape) {
                                Icon(imageVector = Icons.Default.Minimize, contentDescription = null)
                            }
                            IconButton(onClick = {
                                windowState.placement = when (windowState.placement) {
                                    WindowPlacement.Fullscreen -> WindowPlacement.Floating

                                    else -> WindowPlacement.Fullscreen
                                }
                            }, modifier = Modifier.fillMaxHeight(1f).aspectRatio(1f), shape = RectangleShape) {
                                Icon(
                                    imageVector = when (windowState.placement) {
                                        WindowPlacement.Fullscreen -> Icons.Default.FullscreenExit

                                        else -> Icons.Default.Fullscreen
                                    }, contentDescription = null
                                )
                            }
                            IconButton(
                                onClick = ::exitApplication,
                                modifier = Modifier.fillMaxHeight(1f).aspectRatio(1f),
                                shape = RectangleShape
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            }
                        }
                        HorizontalDivider()
                        OverviewView()
                    }
                }
            }
        }
    }
}