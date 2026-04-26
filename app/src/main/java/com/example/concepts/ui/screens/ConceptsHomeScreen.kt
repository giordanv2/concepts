package com.example.concepts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.concepts.ui.theme.ConceptsTheme

@Composable
fun ConceptsHomeScreen(
    onOpenStateFlowVsSharedFlow: () -> Unit,
    onOpenDependencyInversion: () -> Unit,
    onOpenOpenClosedPrinciple: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Android Concepts Playground",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Each screen is isolated in its own file so you can practice concepts one by one, like a small study app.",
            style = MaterialTheme.typography.bodyLarge
        )

        ConceptEntryCard(
            title = "StateFlow vs SharedFlow",
            description = "Interactive demo for state, events, replay behavior, and snackbar restoration after rotation.",
            buttonLabel = "Open concept",
            onClick = onOpenStateFlowVsSharedFlow
        )

        ConceptEntryCard(
            title = "Dependency Inversion Principle",
            description = "Production-style example showing tight coupling versus abstraction-based design, with comments.",
            buttonLabel = "Open concept",
            onClick = onOpenDependencyInversion
        )

        ConceptEntryCard(
            title = "Open/Closed Principle",
            description = "Same checkout example, but focused on being open for extension and closed for modification.",
            buttonLabel = "Open concept",
            onClick = onOpenOpenClosedPrinciple
        )
    }
}

@Composable
private fun ConceptEntryCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onClick) {
                Text(buttonLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConceptsHomeScreenPreview() {
    ConceptsTheme {
        ConceptsHomeScreen(
            onOpenStateFlowVsSharedFlow = {},
            onOpenDependencyInversion = {},
            onOpenOpenClosedPrinciple = {}
        )
    }
}
