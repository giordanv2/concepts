package com.example.concepts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.concepts.ui.theme.ConceptsTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConceptsTheme {
                EventConceptApp()
            }
        }
    }
}

data class ScreenUiState(
    val tapCount: Int = 0,
    val lastAction: String = "Nothing triggered yet"
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
}

class EventConceptViewModel : ViewModel() {
    private val _screenState = MutableStateFlow(ScreenUiState())
    val screenState: StateFlow<ScreenUiState> = _screenState.asStateFlow()

    // WRONG: StateFlow keeps the latest value.
    // If the collector restarts after rotation or recreation, it receives this event again.
    private val _wrongEvent = MutableStateFlow<UiEvent?>(null)
    val wrongEvent: StateFlow<UiEvent?> = _wrongEvent.asStateFlow()

    // RIGHT: SharedFlow is a better fit for one-time events like snackbars or navigation.
    // With replay = 0 (the default), new collectors do not receive old events.
    private val _rightEvent = MutableSharedFlow<UiEvent>()
    val rightEvent: SharedFlow<UiEvent> = _rightEvent.asSharedFlow()

    fun incrementState() {
        _screenState.update { state ->
            state.copy(
                tapCount = state.tapCount + 1,
                lastAction = "StateFlow state updated correctly"
            )
        }
    }

    fun triggerWrongSnackbar() {
        val message = "Wrong event fired at tap #${_screenState.value.tapCount}"
        _screenState.update { it.copy(lastAction = "Wrong event emitted with StateFlow") }
        _wrongEvent.value = UiEvent.ShowMessage(message)
        print(message)
    }

    suspend fun triggerRightSnackbar() {
        val message = "Right event fired at tap #${_screenState.value.tapCount}"
        _screenState.update { it.copy(lastAction = "Right event emitted with SharedFlow") }
        _rightEvent.emit(UiEvent.ShowMessage(message))
    }

    fun consumeWrongEvent() {
        // Clearing the StateFlow event avoids repeats, but it is easy to forget and noisy to maintain.
        _wrongEvent.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventConceptApp(
    viewModel: EventConceptViewModel = viewModel()
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val wrongEvent by viewModel.wrongEvent.collectAsStateWithLifecycle()
    val wrongSnackbarHostState = remember { SnackbarHostState() }
    val rightSnackbarHostState = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showCollectors by rememberSaveable { mutableStateOf(true) }

    // This collector shows the common mistake:
    // because wrongEvent is a StateFlow, the latest event is replayed when collection starts again.
    LaunchedEffect(wrongEvent, showCollectors) {
        if (!showCollectors) return@LaunchedEffect

        val event = wrongEvent as? UiEvent.ShowMessage ?: return@LaunchedEffect
        wrongSnackbarHostState.showSnackbar(event.message)
    }

    // SharedFlow is collected as a stream of one-off events.
    // Restarting this collector does not replay the previous snackbar event.
    LaunchedEffect(showCollectors) {
        if (!showCollectors) return@LaunchedEffect

        viewModel.rightEvent.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> rightSnackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SnackbarHost(hostState = wrongSnackbarHostState)
                SnackbarHost(hostState = rightSnackbarHostState)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "StateFlow vs SharedFlow",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Use the app to trigger an event, then turn collectors off and on again. The wrong StateFlow snackbar replays. The SharedFlow snackbar does not.",
                style = MaterialTheme.typography.bodyLarge
            )

            StateSummaryCard(
                tapCount = screenState.tapCount,
                lastAction = screenState.lastAction,
                onIncrement = viewModel::incrementState
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showCollectors,
                    onClick = { showCollectors = !showCollectors },
                    label = {
                        Text(if (showCollectors) "Collectors ON" else "Collectors OFF")
                    }
                )
                Text(
                    text = "Toggle this to simulate the UI stopping and starting collection.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            DemoCard(
                title = "Wrong: event stored in StateFlow",
                description = "Tap the button, dismiss the snackbar, then toggle collectors OFF and ON. Because StateFlow keeps the last value, the snackbar comes back.",
                accentLabel = "Replays on restart",
                accentColor = MaterialTheme.colorScheme.errorContainer,
                onTrigger = viewModel::triggerWrongSnackbar,
                secondaryActionLabel = "Clear wrong event",
                onSecondaryAction = viewModel::consumeWrongEvent
            )

            DemoCard(
                title = "Right: event emitted with SharedFlow",
                description = "Tap the button, dismiss the snackbar, then toggle collectors OFF and ON. The old event is gone, so the snackbar stays dismissed.",
                accentLabel = "One-time event",
                accentColor = MaterialTheme.colorScheme.primaryContainer,
                onTrigger = {
                    scope.launch {
                        viewModel.triggerRightSnackbar()
                    }
                },
                secondaryActionLabel = null,
                onSecondaryAction = null
            )

            HorizontalDivider()

            Text(
                text = "Rule of thumb",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "State = long-lived UI data to render.\nEvent = one-time action like snackbar, toast, or navigation.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun StateSummaryCard(
    tapCount: Int,
    lastAction: String,
    onIncrement: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Proper StateFlow usage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Counter: $tapCount",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Last action: $lastAction",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onIncrement) {
                Text("Update screen state")
            }
        }
    }
}

@Composable
private fun DemoCard(
    title: String,
    description: String,
    accentLabel: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onTrigger: () -> Unit,
    secondaryActionLabel: String?,
    onSecondaryAction: (() -> Unit)?
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(accentColor, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = accentLabel,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTrigger) {
                    Text("Trigger snackbar")
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    TextButton(onClick = onSecondaryAction) {
                        Text(secondaryActionLabel)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventConceptPreview() {
    ConceptsTheme {
        EventConceptAppPreview()
    }
}

@Composable
private fun EventConceptAppPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCollectors by remember { mutableStateOf(true) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "StateFlow vs SharedFlow",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Preview of the concept screen.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (showCollectors) "Collectors ON" else "Collectors OFF")
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { showCollectors = !showCollectors }) {
                    Text("Toggle")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            StateSummaryCard(
                tapCount = 2,
                lastAction = "Preview state",
                onIncrement = {}
            )
            DemoCard(
                title = "Wrong: event stored in StateFlow",
                description = "This demo replays the last event.",
                accentLabel = "Replays on restart",
                accentColor = MaterialTheme.colorScheme.errorContainer,
                onTrigger = {},
                secondaryActionLabel = "Clear wrong event",
                onSecondaryAction = {}
            )
            DemoCard(
                title = "Right: event emitted with SharedFlow",
                description = "This demo only emits to active collectors.",
                accentLabel = "One-time event",
                accentColor = MaterialTheme.colorScheme.primaryContainer,
                onTrigger = {},
                secondaryActionLabel = null,
                onSecondaryAction = null
            )
        }
    }
}
