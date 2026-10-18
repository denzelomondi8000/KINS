package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StaffRole
import com.example.data.model.StoreConfig
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionRecord
import com.example.data.model.Worker
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PosRepository(
    private val database: AppDatabase,
    val syncManager: SyncManager
) {
    private val storeConfigDao = database.storeConfigDao()
    private val workerDao = database.workerDao()
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()

    val storeConfigFlow: Flow<StoreConfig?> = storeConfigDao.getStoreConfig()
    val allWorkersFlow: Flow<List<Worker>> = workerDao.getAllWorkers()
    val allProductsFlow: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProductsFlow: Flow<List<Product>> = productDao.getLowStockProducts()
    val allTransactionsFlow: Flow<List<TransactionRecord>> = transactionDao.getAllTransactions()
    val pendingSyncCountFlow: Flow<Int> = transactionDao.getPendingSyncCount()

    suspend fun getStoreConfig(): StoreConfig? = withContext(Dispatchers.IO) {
        storeConfigDao.getStoreConfigOnce()
    }

    suspend fun saveStoreConfig(config: StoreConfig) = withContext(Dispatchers.IO) {
        storeConfigDao.saveStoreConfig(config)
    }

    /**
     * Creates a new POS system from the Create Page form.
     * Sets the custom store name, manager name, PIN (default 0000), and populates
     * the Room database with product information (ID, name, price, stock levels)
     * for seamless offline checkout.
     */
    suspend fun createPosSystem(
        storeName: String,
        managerName: String,
        managerPin: String = "0000",
        currencySymbol: String = "$",
        taxRatePercent: Double = 8.0,
        address: String = "Main Branch Counter",
        phone: String = ""
    ): StoreConfig = withContext(Dispatchers.IO) {
        val pin = managerPin.ifBlank { "0000" }
        val config = StoreConfig(
            storeId = "STORE_MAIN",
            storeName = storeName.trim(),
            managerName = managerName.trim().ifBlank { "Store Manager" },
            managerPin = pin,
            currencySymbol = currencySymbol.trim().ifBlank { "$" },
            taxRatePercent = taxRatePercent,
            address = address.trim(),
            phone = phone.trim(),
            isConfigured = true,
            isOnlineSyncEnabled = true,
            lastCloudSyncTimestamp = System.currentTimeMillis(),
            registeredAt = System.currentTimeMillis()
        )
        storeConfigDao.saveStoreConfig(config)

        // Seed initial products in Room database with ID, name, price, and stock levels for offline checkout
        seedInitialCatalogIfEmpty()

        // Seed initial staff workers provisioned by the manager
        seedInitialWorkersIfEmpty()

        config
    }

    private suspend fun seedInitialCatalogIfEmpty() {
        val existingProducts = productDao.getProductById("BEV-001")
        if (existingProducts == null) {
            val starterProducts = listOf(
                // Beverages
                Product(id = "BEV-001", name = "Cold Brew Coffee 330ml", price = 3.49, stockLevel = 45, sku = "BEV-001", barcode = "7891001", category = "Beverages", costPrice = 1.20, lowStockThreshold = 10, unit = "can"),
                Product(id = "BEV-002", name = "Sparkling Spring Water 500ml", price = 1.79, stockLevel = 80, sku = "BEV-002", barcode = "7891002", category = "Beverages", costPrice = 0.50, lowStockThreshold = 15, unit = "bottle"),
                Product(id = "BEV-003", name = "Pure Orange Juice 1L", price = 4.25, stockLevel = 28, sku = "BEV-003", barcode = "7891003", category = "Beverages", costPrice = 1.80, lowStockThreshold = 8, unit = "bottle"),
                Product(id = "BEV-004", name = "Organic Green Tea 400ml", price = 2.50, stockLevel = 34, sku = "BEV-004", barcode = "7891004", category = "Beverages", costPrice = 0.90, lowStockThreshold = 10, unit = "bottle"),

                // Bakery & Snacks
                Product(id = "BAK-001", name = "Artisan Sourdough Loaf", price = 4.50, stockLevel = 18, sku = "BAK-001", barcode = "7892001", category = "Bakery", costPrice = 1.50, lowStockThreshold = 5, unit = "loaf"),
                Product(id = "BAK-002", name = "Butter Croissant (2-Pack)", price = 3.25, stockLevel = 22, sku = "BAK-002", barcode = "7892002", category = "Bakery", costPrice = 1.20, lowStockThreshold = 6, unit = "pack"),
                Product(id = "SNK-001", name = "Sea Salt Kettle Chips 150g", price = 2.69, stockLevel = 65, sku = "SNK-001", barcode = "7893001", category = "Snacks", costPrice = 0.85, lowStockThreshold = 12, unit = "bag"),
                Product(id = "SNK-002", name = "Roasted Almonds 200g", price = 5.49, stockLevel = 30, sku = "SNK-002", barcode = "7893002", category = "Snacks", costPrice = 2.20, lowStockThreshold = 8, unit = "pack"),
                Product(id = "SNK-003", name = "Dark Chocolate 72% Bar 100g", price = 2.89, stockLevel = 42, sku = "SNK-003", barcode = "7893003", category = "Snacks", costPrice = 1.10, lowStockThreshold = 10, unit = "bar"),

                // Produce & Dairy
                Product(id = "GRO-001", name = "Organic Avocados (4-Pack)", price = 4.99, stockLevel = 14, sku = "GRO-001", barcode = "7894001", category = "Produce", costPrice = 2.00, lowStockThreshold = 5, unit = "net"),
                Product(id = "GRO-002", name = "Fresh Crisp Apples 1kg", price = 3.89, stockLevel = 25, sku = "GRO-002", barcode = "7894002", category = "Produce", costPrice = 1.40, lowStockThreshold = 8, unit = "kg"),
                Product(id = "GRO-003", name = "Whole Milk Gallon (3.8L)", price = 4.49, stockLevel = 20, sku = "GRO-003", barcode = "7894003", category = "Dairy", costPrice = 2.30, lowStockThreshold = 6, unit = "jug"),
                Product(id = "GRO-004", name = "Farm Fresh Eggs (12-ct)", price = 3.99, stockLevel = 32, sku = "GRO-004", barcode = "7894004", category = "Dairy", costPrice = 1.90, lowStockThreshold = 10, unit = "carton"),

                // Household & Personal Care
                Product(id = "HOU-001", name = "Eco Paper Towels (3-Roll)", price = 5.99, stockLevel = 24, sku = "HOU-001", barcode = "7895001", category = "Household", costPrice = 2.40, lowStockThreshold = 6, unit = "pack"),
                Product(id = "HOU-002", name = "Lavender Dish Soap 750ml", price = 3.49, stockLevel = 15, sku = "HOU-002", barcode = "7895002", category = "Household", costPrice = 1.30, lowStockThreshold = 5, unit = "bottle"),
                Product(id = "PER-001", name = "Botanical Hand Wash 300ml", price = 3.99, stockLevel = 8, sku = "PER-001", barcode = "7896001", category = "Personal Care", costPrice = 1.50, lowStockThreshold = 5, unit = "bottle")
            )
            productDao.insertProducts(starterProducts)
        }
    }

    private suspend fun seedInitialWorkersIfEmpty() {
        val existingWorkers = workerDao.getWorkerByStaffCode("CASH-101")
        if (existingWorkers == null) {
            val workers = listOf(
                Worker(
                    id = UUID.randomUUID().toString(),
                    staffCode = "CASH-101",
                    fullName = "Alice Miller",
                    phone = "+1 555-0101",
                    role = StaffRole.CASHIER,
                    pin = "1234",
                    isActive = true,
                    canGiveDiscounts = true,
                    canProcessRefunds = false,
                    canEditInventory = false,
                    canViewReports = false,
                    totalSalesAmount = 340.50,
                    totalTransactionsCount = 12
                ),
                Worker(
                    id = UUID.randomUUID().toString(),
                    staffCode = "CASH-102",
                    fullName = "David Chen",
                    phone = "+1 555-0102",
                    role = StaffRole.CASHIER,
                    pin = "2345",
                    isActive = true,
                    canGiveDiscounts = false,
                    canProcessRefunds = false,
                    canEditInventory = false,
                    canViewReports = false,
                    totalSalesAmount = 180.25,
                    totalTransactionsCount = 6
                )
            )
            workerDao.insertWorkers(workers)
        }
    }

    // --- Authentication ---
    suspend fun authenticateWorker(staffCode: String, pin: String): Result<Worker> = withContext(Dispatchers.IO) {
        val worker = workerDao.getWorkerByStaffCode(staffCode.trim().uppercase())
            ?: return@withContext Result.failure(Exception("Staff Code '$staffCode' not found. Please contact your Store Manager."))

        if (!worker.isActive) {
            return@withContext Result.failure(Exception("This staff account is currently deactivated by the Store Manager."))
        }

        if (worker.pin != pin.trim()) {
            return@withContext Result.failure(Exception("Incorrect PIN. Please re-enter or ask your Manager to reset it."))
        }

        workerDao.updateWorkerLastLogin(worker.id, System.currentTimeMillis())
        Result.success(worker)
    }

    suspend fun authenticateManager(pin: String): Boolean = withContext(Dispatchers.IO) {
        val config = storeConfigDao.getStoreConfigOnce()
        val expectedPin = config?.managerPin?.ifBlank { "0000" } ?: "0000"
        expectedPin == pin.trim()
    }

    // --- Worker Management (Manager Only) ---
    suspend fun provisionNewWorker(
        fullName: String,
        phone: String,
        role: StaffRole,
        pin: String,
        canGiveDiscounts: Boolean,
        canProcessRefunds: Boolean,
        canEditInventory: Boolean,
        canViewReports: Boolean
    ): Worker = withContext(Dispatchers.IO) {
        val prefix = when (role) {
            StaffRole.CASHIER -> "CASH"
            StaffRole.SUPERVISOR -> "SUP"
            StaffRole.INVENTORY_CLERK -> "INV"
            StaffRole.STORE_MANAGER -> "MGR"
        }
        val randomDigits = (100..999).random()
        val generatedCode = "$prefix-$randomDigits"

        val worker = Worker(
            id = UUID.randomUUID().toString(),
            staffCode = generatedCode,
            fullName = fullName.trim(),
            phone = phone.trim(),
            role = role,
            pin = pin.trim(),
            isActive = true,
            canGiveDiscounts = canGiveDiscounts,
            canProcessRefunds = canProcessRefunds,
            canEditInventory = canEditInventory,
            canViewReports = canViewReports,
            createdAt = System.currentTimeMillis()
        )
        workerDao.insertWorker(worker)
        worker
    }

    suspend fun updateWorkerPin(workerId: String, newPin: String) = withContext(Dispatchers.IO) {
        workerDao.updateWorkerPin(workerId, newPin.trim())
    }

    suspend fun setWorkerActiveStatus(workerId: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        workerDao.updateWorkerStatus(workerId, isActive)
    }

    suspend fun deleteWorker(workerId: String) = withContext(Dispatchers.IO) {
        workerDao.deleteWorker(workerId)
    }

    // --- Product & Inventory Management ---
    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)

    suspend fun findProductByBarcodeOrSku(code: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductByBarcode(code.trim())
    }

    suspend fun addProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun restockProduct(productId: String, quantityAdded: Int) = withContext(Dispatchers.IO) {
        productDao.restockProduct(productId, quantityAdded)
    }

    suspend fun deleteProduct(productId: String) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(productId)
    }

    // --- POS Sales Checkout (100% Offline Support via Room) ---
    suspend fun processCheckout(
        cashier: Worker,
        items: List<CartItem>,
        paymentMethod: PaymentMethod,
        amountTendered: Double,
        orderDiscountPercent: Double = 0.0,
        customerInfo: String = "Walk-in Customer"
    ): Result<TransactionRecord> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) {
            return@withContext Result.failure(Exception("Cart is empty!"))
        }

        val config = storeConfigDao.getStoreConfigOnce() ?: StoreConfig()
        val taxRate = config.taxRatePercent / 100.0

        val subtotal = items.sumOf { it.lineTotal }
        val discountAmount = subtotal * (orderDiscountPercent / 100.0)
        val discountedSubtotal = (subtotal - discountAmount).coerceAtLeast(0.0)
        val taxAmount = discountedSubtotal * taxRate
        val total = discountedSubtotal + taxAmount

        val changeGiven = (amountTendered - total).coerceAtLeast(0.0)

        val datePrefix = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val randomSuffix = (1000..9999).random()
        val txnId = "TXN-$datePrefix-$randomSuffix"

        val isOnline = syncManager.isEffectivelyOnline.value
        val syncStatus = if (isOnline) SyncStatus.SYNCED else SyncStatus.PENDING_SYNC

        val itemsSummary = items.joinToString(", ") { "${it.product.name} x${it.quantity}" }

        val transaction = TransactionRecord(
            id = txnId,
            cashierStaffCode = cashier.staffCode,
            cashierName = cashier.fullName,
            subtotal = subtotal,
            orderDiscountAmount = discountAmount,
            taxAmount = taxAmount,
            totalAmount = total,
            paymentMethod = paymentMethod,
            amountTendered = amountTendered,
            changeGiven = changeGiven,
            customerInfo = customerInfo.ifBlank { "Walk-in Customer" },
            timestamp = System.currentTimeMillis(),
            syncStatus = syncStatus,
            totalItemsCount = items.sumOf { it.quantity },
            itemsSummary = itemsSummary
        )

        // 1. Record transaction in Room database
        transactionDao.insertTransaction(transaction)

        // 2. Decrement stock level in Room database for offline checkout
        for (item in items) {
            productDao.decrementStock(item.product.id, item.quantity)
        }

        // 3. Increment cashier stats
        workerDao.incrementWorkerSales(cashier.staffCode, total)

        Result.success(transaction)
    }

    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        syncManager.triggerCloudSync()
    }
}
