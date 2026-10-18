package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.HeldCart
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StaffRole
import com.example.data.model.StoreConfig
import com.example.data.model.TransactionRecord
import com.example.data.model.Worker
import com.example.data.repository.PosRepository
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    CREATE_STORE, // Setup POS system name form
    LOGIN,
    POS_REGISTER,
    MANAGER_DASHBOARD,
    INVENTORY,
    ORDERS_HISTORY,
    SYNC_CENTER
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val syncManager = SyncManager(application, database, viewModelScope)
    val repository = PosRepository(database, syncManager)

    // Current Screen: Defaults to CREATE_STORE if no store is configured
    private val _currentScreen = MutableStateFlow(AppScreen.CREATE_STORE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active Staff / Manager Session
    private val _currentWorker = MutableStateFlow<Worker?>(null)
    val currentWorker: StateFlow<Worker?> = _currentWorker.asStateFlow()

    private val _isManagerLoggedIn = MutableStateFlow(false)
    val isManagerLoggedIn: StateFlow<Boolean> = _isManagerLoggedIn.asStateFlow()

    // Store Configuration
    val storeConfig: StateFlow<StoreConfig?> = repository.storeConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Workers & Products from Room
    val allWorkers: StateFlow<List<Worker>> = repository.allWorkersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<Product>> = repository.allProductsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProductsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionRecord>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Online / Offline State
    val isEffectivelyOnline = syncManager.isEffectivelyOnline
    val isDeviceOnline = syncManager.isDeviceOnline
    val simulateOffline = syncManager.simulateOffline
    val isSyncing = syncManager.isSyncing
    val lastSyncResult = syncManager.lastSyncResult

    // POS Catalog Search & Category Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesCategory = (category == "All" || product.category.equals(category, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.sku.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _orderDiscountPercent = MutableStateFlow(0.0)
    val orderDiscountPercent: StateFlow<Double> = _orderDiscountPercent.asStateFlow()

    private val _customerNote = MutableStateFlow("")
    val customerNote: StateFlow<String> = _customerNote.asStateFlow()

    // Held Carts
    private val _heldCarts = MutableStateFlow<List<HeldCart>>(emptyList())
    val heldCarts: StateFlow<List<HeldCart>> = _heldCarts.asStateFlow()

    // Completed Transaction / Receipt Dialog
    private val _activeReceipt = MutableStateFlow<TransactionRecord?>(null)
    val activeReceipt: StateFlow<TransactionRecord?> = _activeReceipt.asStateFlow()

    // User Feedback / Snackbars
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = repository.getStoreConfig()
            if (existing != null && existing.isConfigured && existing.storeName.isNotBlank()) {
                _currentScreen.value = AppScreen.LOGIN
            } else {
                _currentScreen.value = AppScreen.CREATE_STORE
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // --- POS System Creation (Create Page) ---
    fun createPosSystem(
        storeName: String,
        managerName: String,
        managerPin: String = "0000",
        currency: String = "$",
        taxRate: Double = 8.0,
        address: String = "Main Retail Terminal #1",
        phone: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val pin = managerPin.ifBlank { "0000" }
            val config = repository.createPosSystem(
                storeName = storeName,
                managerName = managerName,
                managerPin = pin,
                currencySymbol = currency,
                taxRatePercent = taxRate,
                address = address,
                phone = phone
            )

            // Auto-login manager
            _isManagerLoggedIn.value = true
            _currentWorker.value = Worker(
                id = "MGR-MASTER",
                staffCode = "MGR-001",
                fullName = config.managerName,
                role = StaffRole.STORE_MANAGER,
                pin = pin,
                isActive = true,
                canGiveDiscounts = true,
                canProcessRefunds = true,
                canEditInventory = true,
                canViewReports = true
            )
            _statusMessage.value = "POS System '${config.storeName}' created with Manager PIN $pin!"
            _currentScreen.value = AppScreen.POS_REGISTER
            onSuccess()
        }
    }

    // --- Authentication ---
    fun loginStaffWithPin(staffCode: String, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.authenticateWorker(staffCode, pin)
            result.onSuccess { worker ->
                _currentWorker.value = worker
                _isManagerLoggedIn.value = (worker.role == StaffRole.STORE_MANAGER)
                _currentScreen.value = AppScreen.POS_REGISTER
                onSuccess()
            }.onFailure { error ->
                onError(error.message ?: "Authentication failed")
            }
        }
    }

    fun loginManagerWithMasterPin(pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.authenticateManager(pin)
            if (success) {
                _isManagerLoggedIn.value = true
                val managerWorker = Worker(
                    id = "MGR-MASTER",
                    staffCode = "MGR-001",
                    fullName = storeConfig.value?.managerName ?: "Store Manager",
                    role = StaffRole.STORE_MANAGER,
                    pin = pin,
                    isActive = true,
                    canGiveDiscounts = true,
                    canProcessRefunds = true,
                    canEditInventory = true,
                    canViewReports = true
                )
                _currentWorker.value = managerWorker
                _currentScreen.value = AppScreen.MANAGER_DASHBOARD
                onSuccess()
            } else {
                onError("Incorrect Manager Master PIN (default is 0000)")
            }
        }
    }

    fun logout() {
        _currentWorker.value = null
        _isManagerLoggedIn.value = false
        _cartItems.value = emptyList()
        _currentScreen.value = AppScreen.LOGIN
    }

    // --- Cart Actions ---
    fun addToCart(product: Product) {
        if (product.stockLevel <= 0) {
            _statusMessage.value = "${product.name} is out of stock!"
            return
        }

        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentList[index]
            if (existing.quantity >= product.stockLevel) {
                _statusMessage.value = "Cannot exceed available stock (${product.stockLevel})!"
                return
            }
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = currentList
    }

    fun updateCartItemQuantity(productId: String, delta: Int) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val existing = currentList[index]
            val newQty = existing.quantity + delta
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else if (newQty > existing.product.stockLevel) {
                _statusMessage.value = "Cannot exceed available stock (${existing.product.stockLevel})!"
                return
            } else {
                currentList[index] = existing.copy(quantity = newQty)
            }
            _cartItems.value = currentList
        }
    }

    fun removeCartItem(productId: String) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _orderDiscountPercent.value = 0.0
        _customerNote.value = ""
    }

    fun setOrderDiscountPercent(percent: Double) {
        _orderDiscountPercent.value = percent.coerceIn(0.0, 100.0)
    }

    fun setCustomerNote(note: String) {
        _customerNote.value = note
    }

    fun holdCurrentCart(note: String = "") {
        if (_cartItems.value.isEmpty()) return
        val held = HeldCart(
            id = UUID.randomUUID().toString(),
            customerNote = note.ifBlank { "Held #${_heldCarts.value.size + 1}" },
            items = _cartItems.value
        )
        _heldCarts.value = _heldCarts.value + held
        clearCart()
        _statusMessage.value = "Cart parked (${held.customerNote})"
    }

    fun resumeHeldCart(heldCart: HeldCart) {
        _cartItems.value = heldCart.items
        _heldCarts.value = _heldCarts.value.filter { it.id != heldCart.id }
        _statusMessage.value = "Resumed cart (${heldCart.customerNote})"
    }

    fun deleteHeldCart(heldCart: HeldCart) {
        _heldCarts.value = _heldCarts.value.filter { it.id != heldCart.id }
    }

    // --- Checkout & Payment (Offline-first via Room) ---
    fun checkout(
        paymentMethod: PaymentMethod,
        amountTendered: Double,
        customerInfo: String,
        onSuccess: (TransactionRecord) -> Unit,
        onError: (String) -> Unit
    ) {
        val cashier = _currentWorker.value
        if (cashier == null) {
            onError("No active staff logged in")
            return
        }

        viewModelScope.launch {
            val result = repository.processCheckout(
                cashier = cashier,
                items = _cartItems.value,
                paymentMethod = paymentMethod,
                amountTendered = amountTendered,
                orderDiscountPercent = _orderDiscountPercent.value,
                customerInfo = customerInfo
            )

            result.onSuccess { txn ->
                _activeReceipt.value = txn
                clearCart()
                onSuccess(txn)
            }.onFailure { error ->
                onError(error.message ?: "Checkout failed")
            }
        }
    }

    fun dismissReceipt() {
        _activeReceipt.value = null
    }

    fun showReceiptForTransaction(transaction: TransactionRecord) {
        _activeReceipt.value = transaction
    }

    // --- Filters & Barcode Scan ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun scanBarcodeOrSku(code: String) {
        viewModelScope.launch {
            val product = repository.findProductByBarcodeOrSku(code)
            if (product != null) {
                addToCart(product)
                _statusMessage.value = "Added: ${product.name}"
            } else {
                _statusMessage.value = "No product found with Barcode/SKU: $code"
            }
        }
    }

    // --- Worker Provisioning (Manager Only) ---
    fun provisionNewWorker(
        fullName: String,
        phone: String,
        role: StaffRole,
        pin: String,
        canGiveDiscounts: Boolean,
        canProcessRefunds: Boolean,
        canEditInventory: Boolean,
        canViewReports: Boolean,
        onSuccess: (Worker) -> Unit,
        onError: (String) -> Unit
    ) {
        if (pin.length < 4) {
            onError("PIN must be at least 4 digits")
            return
        }
        viewModelScope.launch {
            try {
                val worker = repository.provisionNewWorker(
                    fullName = fullName,
                    phone = phone,
                    role = role,
                    pin = pin,
                    canGiveDiscounts = canGiveDiscounts,
                    canProcessRefunds = canProcessRefunds,
                    canEditInventory = canEditInventory,
                    canViewReports = canViewReports
                )
                _statusMessage.value = "Worker created: ${worker.fullName} (Staff Code: ${worker.staffCode})"
                onSuccess(worker)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to create worker")
            }
        }
    }

    fun updateWorkerPin(workerId: String, newPin: String) {
        if (newPin.length < 4) {
            _statusMessage.value = "PIN must be at least 4 digits"
            return
        }
        viewModelScope.launch {
            repository.updateWorkerPin(workerId, newPin)
            _statusMessage.value = "PIN updated successfully"
        }
    }

    fun toggleWorkerStatus(workerId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.setWorkerActiveStatus(workerId, !currentStatus)
            _statusMessage.value = if (!currentStatus) "Staff account activated" else "Staff account deactivated"
        }
    }

    fun deleteWorker(workerId: String) {
        viewModelScope.launch {
            repository.deleteWorker(workerId)
            _statusMessage.value = "Worker removed"
        }
    }

    // --- Product / Inventory ---
    fun addProduct(product: Product, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addProduct(product)
            _statusMessage.value = "Product added to catalog"
            onSuccess()
        }
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateProduct(product)
            _statusMessage.value = "Product updated"
            onSuccess()
        }
    }

    fun restockProduct(productId: String, qty: Int) {
        viewModelScope.launch {
            repository.restockProduct(productId, qty)
            _statusMessage.value = "Added $qty units to stock"
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _statusMessage.value = "Product deleted"
        }
    }

    // --- Sync & Store Config ---
    fun triggerCloudSync() {
        viewModelScope.launch {
            val result = repository.syncNow()
            when (result) {
                is SyncResult.Success -> _statusMessage.value = result.message
                is SyncResult.Failure -> _statusMessage.value = result.errorMessage
            }
        }
    }

    fun setSimulateOffline(simulate: Boolean) {
        syncManager.setSimulateOffline(simulate)
        _statusMessage.value = if (simulate) "Simulated Offline Mode enabled" else "Online connectivity restored"
    }

    fun updateStoreConfig(config: StoreConfig) {
        viewModelScope.launch {
            repository.saveStoreConfig(config)
            _statusMessage.value = "Store settings saved"
        }
    }
}
