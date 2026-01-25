package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OverviewFeature(reducer: OverviewReducer) : Feature<OverviewState, OverviewCommand, OverviewEvent> by Feature(
    initialState = OverviewState(),
    scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer = reducer,
    OverviewCommand.Initialize
)