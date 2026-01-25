package io.github.numq.grokviewer.application

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.window.singleWindowApplication
import io.github.numq.grokviewer.archive.archiveModule
import io.github.numq.grokviewer.content.contentModule
import io.github.numq.grokviewer.overview.OverviewView
import io.github.numq.grokviewer.overview.overviewModule
import io.github.numq.grokviewer.theme.Theme
import org.koin.core.context.startKoin

const val TITLE = "Grok Viewer"

fun main() {
    startKoin { modules(archiveModule, contentModule, overviewModule) }

    singleWindowApplication(title = TITLE) {
        Theme(darkTheme = isSystemInDarkTheme()) {
            OverviewView()
        }
    }
}