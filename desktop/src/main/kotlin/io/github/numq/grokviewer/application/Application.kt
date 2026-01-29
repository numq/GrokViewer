package io.github.numq.grokviewer.application

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import io.github.numq.grokviewer.archive.archiveModule
import io.github.numq.grokviewer.content.contentModule
import io.github.numq.grokviewer.overview.OverviewView
import io.github.numq.grokviewer.overview.overviewModule
import io.github.numq.grokviewer.save.saveModule
import io.github.numq.grokviewer.theme.Theme
import io.github.numq.grokviewer.window.WindowDecoration
import org.koin.core.context.startKoin

const val APP_NAME = "Grok Viewer"

fun main() {
    startKoin { modules(archiveModule, contentModule, overviewModule, saveModule) }

    application {
        Theme(darkTheme = isSystemInDarkTheme()) {
            WindowDecoration(
                title = APP_NAME,
                icon = painterResource("drawable/icon.png"),
                minimumWindowSize = DpSize(512.dp, 512.dp),
                windowContent = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        HorizontalDivider()
                        OverviewView()
                    }
                })
        }
    }
}