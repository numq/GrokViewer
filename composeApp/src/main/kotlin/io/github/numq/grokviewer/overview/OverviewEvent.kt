package io.github.numq.grokviewer.overview

sealed interface OverviewEvent {
    data class Error(val message: String) : OverviewEvent
}