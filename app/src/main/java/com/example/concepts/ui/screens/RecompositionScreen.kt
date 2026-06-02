package com.example.concepts.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.example.concepts.ui.theme.ConceptsTheme
import kotlin.system.measureTimeMillis

@Composable
fun RecompositionScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Recomposition",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

//        BadRecompositionExample()
//        GoodRecompositionExample()
//        BadDependentExample2()
//        GoodDependentExample2()
        BadDependentExample3()
        GoodDependentExample3()
        Example()

    }
}
//@Composable
//fun Example() {
//    var count by remember { mutableIntStateOf(0) }
//
//    Button(onClick = { count++ }) {
//        Text("Click me")
//    }
//}

@Composable
fun Example() {
    var count by remember { mutableIntStateOf(0) }

    CounterButton(onClick = remember(count) { { count++ } })
}

@Composable
fun CounterButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Click me")
    }
}

@Composable
fun BadRecompositionExample() {

    // 🔹 State: changing this triggers recomposition
    var count by remember { mutableIntStateOf(0) }

    // ❌ BAD: This runs EVERY recomposition
    // Put a breakpoint here or log it
    val expensiveValue = calculateExpensiveValue()

    Column {
        Text("Bad Example")
        Text("Count: $count")

        // This value is recalculated every time count changes
        Text("Expensive: $expensiveValue")

        Button(
            onClick = {
                // 👇 This triggers recomposition
                count++
            }
        ) {
            Text("Increment")
        }
    }
}

@Composable
fun GoodRecompositionExample() {

    var count by remember { mutableIntStateOf(0) }

    // ✅ GOOD: This runs ONLY once
    val expensiveValue = remember { calculateExpensiveValue() }

    Column {
        Text("Good Example")

        Text("Count: $count")

        // This value is NOT recalculated on recomposition
        Text("Expensive: $expensiveValue")

        Button(
            onClick = {
                // 👇 Still triggers recomposition
                count++
            }
        ) {
            Text("Increment")
        }
    }
}

// Simulate expensive work

/*fun calculateExpensiveValue(): Long {
    println("❌ Running expensive calculation")
    Thread.sleep(500) // simulate heavy work
    return (1..1_000_000).sumOf { it.toLong() }
}*/

fun calculateExpensiveValue(): Long {
    var result = 0L

    val time = measureTimeMillis {
        result = (1..10_000_000).sumOf { it.toLong() }
    }

    println("❌ Expensive calculation took $time ms")

    return result
}

@Composable
fun BadDependentExample2() {

    var count by remember { mutableIntStateOf(0) }

    // ❌ BAD: recalculates on EVERY recomposition
    // even if count didn't change
    val expensiveValue = calculateBasedOnCount(count)

    Column {
        Text("Bad Dependent Example")
        Text("Count: $count")
        Text("Expensive: $expensiveValue")

        Button(onClick = { count++ }) {
            Text("Increment")
        }
    }
}

@Composable
fun GoodDependentExample2() {

    var count by remember { mutableIntStateOf(0) }

    // ✅ GOOD: recompute ONLY when count changes
    val expensiveValue = remember(count) {
        calculateBasedOnCount(count)
    }

    Column {
        Text("Good Dependent Example")
        Text("Count: $count")
        Text("Expensive: $expensiveValue")

        Button(onClick = { count++ }) {
            Text("Increment")
        }
    }
}

fun calculateBasedOnCount(count: Int): Long {
    val time = measureTimeMillis {
        // simulate heavy work
        (1..10_000_000).sumOf { it + count.toLong() }
    }

    println("❌ Recomputed for count=$count in $time ms")

    return count * 1000L // just return something simple
}

@Composable
fun BadDependentExample3() {

    var count by remember { mutableIntStateOf(0) }

    // 👇 NEW unrelated state
    var toggle by remember { mutableStateOf(false) }

    // ❌ BAD: runs on EVERY recomposition
    val expensiveValue = calculateBasedOnCount(count)

    Column {
        Text("Bad Dependent Example")

        Text("Count: $count")
        Text("Expensive: $expensiveValue")

        Button(onClick = { count++ }) {
            Text("Increment count")
        }

        Button(onClick = { toggle = !toggle }) {
            Text("Toggle (unrelated)")
        }
    }
}

@Composable
fun GoodDependentExample3() {

    var count by remember { mutableIntStateOf(0) }

    // 👇 SAME unrelated state
    var toggle by remember { mutableStateOf(false) }

    // ✅ GOOD: recompute ONLY when count changes
    val expensiveValue = remember(count) {
        calculateBasedOnCount(count)
    }

    Column {
        Text("Good Dependent Example")

        Text("Count: $count")
        Text("Expensive: $expensiveValue")

        Button(onClick = { count++ }) {
            Text("Increment count")
        }

        Button(onClick = { toggle = !toggle }) {
            Text("Toggle (unrelated)")
        }
    }
}


@Preview(
    showBackground = true,
    backgroundColor = 0xFF141218,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
private fun RecompositionScreenPreview() {
    ConceptsTheme(
        darkTheme = true,
        dynamicColor = false
    ) {
        RecompositionScreen(onBack = {})
    }
}
