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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.concepts.ui.theme.ConceptsTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// BAD EXAMPLE:
// This ViewModel directly creates its network client and depends on a concrete implementation.
// In a real codebase this makes testing, swapping implementations, and reuse much harder.
class BadCheckoutViewModel() : ViewModel() {
    private val paymentGateway = StripePaymentGateway()
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    suspend fun placeOrder(totalInCents: Int) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        try {
            val receiptId = paymentGateway.charge(totalInCents)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    confirmationMessage = "Order placed. Receipt: $receiptId"
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Payment failed"
                )
            }
        }
    }
}

// Low-level implementation detail.
// The problem is not that this class exists. The problem is that high-level code depends on it directly.
class StripePaymentGateway {
    suspend fun charge(totalInCents: Int): String {
        return "stripe_$totalInCents"
    }
}

// GOOD EXAMPLE:
// Both the ViewModel and the concrete gateway now depend on an abstraction.
// This follows the Dependency Inversion Principle:
// high-level policy depends on an interface, not a low-level implementation.
interface PaymentGateway {
    suspend fun charge(totalInCents: Int): String
}

class StripePaymentGatewayImpl : PaymentGateway {
    override suspend fun charge(totalInCents: Int): String {
        return "stripe_$totalInCents"
    }
}

// This fake is the kind of thing teams use in tests, previews, or demo builds.
class FakePaymentGateway : PaymentGateway {
    override suspend fun charge(totalInCents: Int): String {
        return "fake_success_receipt"
    }
}

class PlaceOrderUseCase(
    private val paymentGateway: PaymentGateway
) {
    suspend operator fun invoke(totalInCents: Int): String {
        return paymentGateway.charge(totalInCents)
    }
}

// Simple service locator for the sample app.
// In a production codebase this could be replaced by Hilt, Koin, or a larger app container.
object CheckoutServiceLocator {
    // Pick the concrete implementation for this environment here.
    // Swap to FakePaymentGateway() for tests, previews, or demo builds.
    private val paymentGateway: PaymentGateway = StripePaymentGatewayImpl()

    private val placeOrderUseCase: PlaceOrderUseCase = PlaceOrderUseCase(
        paymentGateway = paymentGateway
    )

    fun provideCheckoutViewModel(): CheckoutViewModel {
        return CheckoutViewModel(
            placeOrderUseCase = placeOrderUseCase
        )
    }

    fun activeGatewayName(): String {
        return paymentGateway::class.simpleName ?: "UnknownPaymentGateway"
    }
}

class CheckoutViewModel(
    private val placeOrderUseCase: PlaceOrderUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    suspend fun placeOrder(totalInCents: Int) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        try {
            val receiptId = placeOrderUseCase(totalInCents)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    confirmationMessage = "Order placed. Receipt: $receiptId"
                )
            }
        } catch (exception: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Payment failed"
                )
            }
        }
    }
}

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val confirmationMessage: String? = null,
    val errorMessage: String? = null
)

private val badExampleCode = """
class BadCheckoutViewModel : ViewModel() {
    // Bad: high-level app logic directly knows about Stripe.
    private val paymentGateway = StripePaymentGateway()

    suspend fun placeOrder(totalInCents: Int) {
        val receiptId = paymentGateway.charge(totalInCents)
        // Update UI state...
    }
}
""".trimIndent()

private val goodExampleCode = """
interface PaymentGateway {
    suspend fun charge(totalInCents: Int): String
}

class PlaceOrderUseCase(
    private val paymentGateway: PaymentGateway
) {
    suspend operator fun invoke(totalInCents: Int): String {
        return paymentGateway.charge(totalInCents)
    }
}

class CheckoutViewModel(
    private val placeOrderUseCase: PlaceOrderUseCase
) : ViewModel() {
    suspend fun placeOrder(totalInCents: Int) {
        val receiptId = placeOrderUseCase(totalInCents)
        // Update UI state...
    }
}

object CheckoutServiceLocator {
    private val paymentGateway: PaymentGateway = StripePaymentGatewayImpl()

    private val placeOrderUseCase = PlaceOrderUseCase(
        paymentGateway = paymentGateway
    )

    fun provideCheckoutViewModel(): CheckoutViewModel {
        return CheckoutViewModel(placeOrderUseCase)
    }
}
""".trimIndent()

@Composable
fun DependencyInversionScreen(
    onBack: () -> Unit
) {
    // The ViewModel still depends on abstractions only.
    // The service locator chooses the real concrete implementation for this screen.
    val checkoutViewModel = remember { CheckoutServiceLocator.provideCheckoutViewModel() }
    val activeGatewayName = remember { CheckoutServiceLocator.activeGatewayName() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Dependency Inversion Principle",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "High-level modules should not depend on low-level modules. Both should depend on abstractions. In Android, that usually means ViewModels and use cases depend on interfaces, not directly on Retrofit, Room, Stripe, Firebase, or specific SDK classes.",
            style = MaterialTheme.typography.bodyLarge
        )

        SummaryCard(
            title = "Concrete implementation in use",
            body = "This screen currently builds `CheckoutViewModel` from the service locator using `$activeGatewayName`. The ViewModel itself still only knows about `PlaceOrderUseCase`, and the use case only knows about `PaymentGateway`."
        )

        SummaryCard(
            title = "Why teams care about it",
            body = "Without dependency inversion, feature code becomes hard to test, harder to replace, and more expensive to change. A payment provider swap or a local-data fallback can ripple through UI and business logic instead of staying isolated."
        )

        SummaryCard(
            title = "What it looks like when not used",
            body = "A ViewModel creates concrete classes itself with `StripePaymentGateway()` or `RetrofitUserApi()`. That means the ViewModel now knows too much about infrastructure and cannot easily work with a fake, a cache-backed version, or a second provider."
        )

        CodeCard(
            title = "Bad: ViewModel depends on concrete payment SDK wrapper",
            explanation = "This is common in beginner code and sometimes slips into production during fast feature work. It works, but it couples your app policy directly to one implementation.",
            code = badExampleCode
        )

        CodeCard(
            title = "Good: ViewModel and use case depend on PaymentGateway",
            explanation = "This is closer to what you will see in company codebases. The app logic depends on an interface, and the implementation can be swapped in composition root or DI setup.",
            code = goodExampleCode
        )

        HorizontalDivider()

        SummaryCard(
            title = "Real production reading tip",
            body = "When you read code at work, look for where objects are constructed. If a ViewModel says `private val api = RealApi()` or `private val repo = SqlRepo()`, that is usually a DIP smell. If it accepts `UserRepository` or `PaymentGateway` in the constructor, that is usually the healthier design."
        )

        SummaryCard(
            title = "Where to look in this file",
            body = "Read `BadCheckoutViewModel` first, then compare it to `CheckoutViewModel`, `PlaceOrderUseCase`, `PaymentGateway`, and `CheckoutServiceLocator`. The key difference is that the service locator chooses the implementation, not the ViewModel."
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    body: String
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CodeCard(
    title: String,
    explanation: String,
    code: String
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
                text = explanation,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DependencyInversionScreenPreview() {
    ConceptsTheme {
        DependencyInversionScreen(onBack = {})
    }
}
