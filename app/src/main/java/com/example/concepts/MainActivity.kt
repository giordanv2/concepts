package com.example.concepts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.concepts.ui.screens.StateFlowVsSharedFlowScreen
import com.example.concepts.ui.theme.ConceptsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConceptsTheme {
                StateFlowVsSharedFlowScreen()
            }
        }
    }
}
