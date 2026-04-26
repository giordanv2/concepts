package com.example.concepts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.concepts.ui.screens.ConceptsHomeScreen
import com.example.concepts.ui.screens.DependencyInversionScreen
import com.example.concepts.ui.screens.OpenClosedPrincipleScreen
import com.example.concepts.ui.theme.ConceptsTheme
import com.example.concepts.ui.screens.StateFlowVsSharedFlowScreen

private enum class ConceptDestination {
    Home,
    StateFlowVsSharedFlow,
    DependencyInversion,
    OpenClosedPrinciple
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConceptsTheme {
                ConceptsApp()
            }
        }
    }
}

@Composable
private fun ConceptsApp() {
    var currentDestination by rememberSaveable { mutableStateOf(ConceptDestination.Home) }

    when (currentDestination) {
        ConceptDestination.Home -> ConceptsHomeScreen(
            onOpenStateFlowVsSharedFlow = {
                currentDestination = ConceptDestination.StateFlowVsSharedFlow
            },
            onOpenDependencyInversion = {
                currentDestination = ConceptDestination.DependencyInversion
            },
            onOpenOpenClosedPrinciple = {
                currentDestination = ConceptDestination.OpenClosedPrinciple
            }
        )

        ConceptDestination.StateFlowVsSharedFlow -> StateFlowVsSharedFlowScreen(
            onBack = { currentDestination = ConceptDestination.Home }
        )

        ConceptDestination.DependencyInversion -> DependencyInversionScreen(
            onBack = { currentDestination = ConceptDestination.Home }
        )

        ConceptDestination.OpenClosedPrinciple -> OpenClosedPrincipleScreen(
            onBack = { currentDestination = ConceptDestination.Home }
        )
    }
}
