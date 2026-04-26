package com.example.concepts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.concepts.ui.theme.ConceptsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Product(
    val id: String,
    val name: String
)

// BAD EXAMPLE:
// This single repository keeps being edited whenever the app adds a new strategy.
// The ViewModel depends on this concrete repository, so the extension point is poor.
class BadProductRepository {
    suspend fun getProducts(mode: String): List<Product> {
        return when (mode) {
            "online_only" -> fetchOnlineOnly()
            "offline_first" -> fetchOfflineFirst()
            "bluetooth_sync" -> fetchBluetoothSync()
            else -> emptyList()
        }
    }

    private suspend fun fetchOnlineOnly(): List<Product> {
        delay(100)
        return listOf(
            Product("1", "Online Coffee"),
            Product("2", "Online Muffin")
        )
    }

    private suspend fun fetchOfflineFirst(): List<Product> {
        delay(100)
        return listOf(
            Product("3", "Offline Espresso"),
            Product("4", "Offline Croissant")
        )
    }

    private suspend fun fetchBluetoothSync(): List<Product> {
        delay(100)
        return listOf(
            Product("5", "Bluetooth Sandwich"),
            Product("6", "Bluetooth Juice")
        )
    }
}

class BadProductViewModel : ViewModel() {
    private val repository = BadProductRepository()

    var uiState by mutableStateOf<List<Product>>(emptyList())
        private set

    fun loadProducts() {
        viewModelScope.launch {
            uiState = repository.getProducts("offline_first")
        }
    }
}

// GOOD EXAMPLE:
// OCP is applied at the repository boundary, like NIA.
// The UI and ViewModel depend on ProductRepository, and we can add new repository
// implementations without modifying the ViewModel or Compose screen.
interface ProductRepository {
    suspend fun getProducts(): List<Product>
}

// These are collaborators, not alternatives.
// OfflineFirstProductRepository uses all of them together, the same way NIA repositories do.
class ProductNetworkDataSource {
    suspend fun fetchProducts(): List<Product> {
        delay(100)
        return listOf(
            Product("1", "Network Coffee"),
            Product("2", "Network Muffin")
        )
    }
}

class ProductDao {
    private var cachedProducts: List<Product> = emptyList()

    suspend fun getProducts(): List<Product> {
        delay(50)
        return cachedProducts
    }

    suspend fun insertProducts(products: List<Product>) {
        delay(50)
        cachedProducts = products
    }
}

class ProductPreferencesDataSource {
    fun shouldUseOfflineFirst(): Boolean {
        return true
    }
}

class ProductNotifier {
    fun notifySyncComplete() {
    }
}

// Repository implementation 1:
// This looks much closer to NIA: several concrete collaborators, all working together.
class OfflineFirstProductRepository(
    private val network: ProductNetworkDataSource,
    private val productDao: ProductDao,
    private val preferences: ProductPreferencesDataSource,
    private val notifier: ProductNotifier
) : ProductRepository {
    override suspend fun getProducts(): List<Product> {
        val cachedProducts = productDao.getProducts()

        if (cachedProducts.isEmpty() || preferences.shouldUseOfflineFirst()) {
            val networkProducts = network.fetchProducts()
            productDao.insertProducts(networkProducts)
            notifier.notifySyncComplete()
        }

        return productDao.getProducts()
    }
}

// Repository implementation 2:
// New behavior is added by introducing a new repository class, not by modifying the old one.
class OnlineOnlyProductRepository(
    private val network: ProductNetworkDataSource
) : ProductRepository {
    override suspend fun getProducts(): List<Product> {
        return network.fetchProducts()
    }
}

// Repository implementation 3:
// Another extension point for a different environment like POS hardware sync.
class BluetoothSyncProductRepository : ProductRepository {
    override suspend fun getProducts(): List<Product> {
        delay(100)
        return listOf(
            Product("7", "Bluetooth Sandwich"),
            Product("8", "Bluetooth Juice")
        )
    }
}

class ProductListViewModel(
    private val repository: ProductRepository
) : ViewModel() {
    var uiState by mutableStateOf<List<Product>>(emptyList())
        private set

    fun loadProducts() {
        viewModelScope.launch {
            uiState = repository.getProducts()
        }
    }
}

object ProductRepositoryServiceLocator {
    private val network = ProductNetworkDataSource()
    private val productDao = ProductDao()
    private val preferences = ProductPreferencesDataSource()
    private val notifier = ProductNotifier()

    // Like NIA, the ViewModel depends on the repository interface.
    // The concrete repository is selected here.
    private val repository: ProductRepository = OfflineFirstProductRepository(
        network = network,
        productDao = productDao,
        preferences = preferences,
        notifier = notifier
    )

    fun provideProductListViewModel(): ProductListViewModel {
        return ProductListViewModel(repository)
    }

    fun activeRepositoryName(): String {
        return repository::class.simpleName ?: "UnknownRepository"
    }
}

private val badExampleCode = """
class BadProductRepository {
    suspend fun getProducts(mode: String): List<Product> {
        return when (mode) {
            "online_only" -> fetchOnlineOnly()
            "offline_first" -> fetchOfflineFirst()
            "bluetooth_sync" -> fetchBluetoothSync()
            else -> emptyList()
        }
    }
}

class BadProductViewModel : ViewModel() {
    private val repository = BadProductRepository()

    fun loadProducts() {
        viewModelScope.launch {
            val products = repository.getProducts("offline_first")
        }
    }
}
""".trimIndent()

private val goodExampleCode = """
interface ProductRepository {
    suspend fun getProducts(): List<Product>
}

class OfflineFirstProductRepository(
    private val network: ProductNetworkDataSource,
    private val productDao: ProductDao,
    private val preferences: ProductPreferencesDataSource,
    private val notifier: ProductNotifier
) : ProductRepository {
    override suspend fun getProducts(): List<Product> {
        // read db, sync network, notify, return db
        return productDao.getProducts()
    }
}

class OnlineOnlyProductRepository(
    private val network: ProductNetworkDataSource
) : ProductRepository {
    override suspend fun getProducts(): List<Product> {
        return network.fetchProducts()
    }
}

class ProductListViewModel(
    private val repository: ProductRepository
) : ViewModel() {
    fun loadProducts() {
        viewModelScope.launch {
            val products = repository.getProducts()
        }
    }
}
""".trimIndent()

@Composable
fun OpenClosedPrincipleScreen(
    onBack: () -> Unit
) {
    val viewModel = remember { ProductRepositoryServiceLocator.provideProductListViewModel() }
    val activeRepositoryName = remember { ProductRepositoryServiceLocator.activeRepositoryName() }
    val products = viewModel.uiState

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
            text = "Open/Closed Principle",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This version mirrors the NIA shape more closely. OCP is applied at the repository interface boundary: the ViewModel and UI depend on `ProductRepository`, while different repository implementations can be added without changing them.",
            style = MaterialTheme.typography.bodyLarge
        )

        SummaryCard(
            title = "Why this looks like NIA",
            body = "The active repository on this screen is `$activeRepositoryName`. Like NIA, one repository implementation can coordinate multiple concrete collaborators internally, such as network, database, preferences, and notifier."
        )

        SummaryCard(
            title = "Important distinction",
            body = "Inside `OfflineFirstProductRepository`, the concrete collaborators are not interchangeable strategies. They are cooperative dependencies in a workflow. OCP is not about forcing an interface over every collaborator. It is about keeping the ViewModel and UI closed to repository strategy changes."
        )

        CodeCard(
            title = "Bad: one repository keeps getting modified",
            explanation = "This version violates OCP because adding a new mode means editing the same repository class again and again.",
            code = badExampleCode
        )

        CodeCard(
            title = "Good: multiple repositories behind one interface",
            explanation = "This is the NIA-style shape. Add `BluetoothSyncProductRepository` or `OnlineOnlyProductRepository` as a new class, and the ViewModel stays unchanged because it only knows `ProductRepository`.",
            code = goodExampleCode
        )

        HorizontalDivider()

        SummaryCard(
            title = "What changes when a new behavior is added",
            body = "To add a new fetch strategy, create another repository implementation such as `BluetoothSyncProductRepository` and wire it in the service locator or DI graph. `ProductListViewModel` and the Compose screen do not change."
        )

        Button(onClick = { viewModel.loadProducts() }) {
            Text("Load Products")
        }

        ProductListCard(products = products)
    }
}

@Composable
private fun ProductListCard(products: List<Product>) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Compose Screen Output",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (products.isEmpty()) {
                Text(
                    text = "Tap \"Load Products\" to fetch items from the active repository implementation.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(products) { product ->
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
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
private fun OpenClosedPrincipleScreenPreview() {
    ConceptsTheme {
        OpenClosedPrincipleScreen(onBack = {})
    }
}
