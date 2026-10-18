package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Role of the staff member in the shop.
 */
enum class StaffRole {
    STORE_MANAGER,
    SUPERVISOR,
    CASHIER,
    INVENTORY_CLERK
}

/**
 * Sync status for offline-first architecture.
 */
enum class SyncStatus {
    SYNCED,
    PENDING_SYNC
}

/**
 * Payment method used at checkout.
 */
enum class PaymentMethod {
    CASH,
    CARD,
    MOBILE_MONEY
}

/**
 * Store configuration and Manager master credentials.
 */
@Entity(tableName = "store_config")
data class StoreConfig(
    @PrimaryKey val storeId: String = "STORE_MAIN",
    val storeName: String = "",
    val managerName: String = "Store Manager",
    val managerEmail: String = "",
    val managerPin: String = "0000", // Default master PIN is 0000
    val currencySymbol: String = "$",
    val taxRatePercent: Double = 8.0,
    val address: String = "",
    val phone: String = "",
    val isOnlineSyncEnabled: Boolean = true,
    val isConfigured: Boolean = false,
    val lastCloudSyncTimestamp: Long = System.currentTimeMillis(),
    val registeredAt: Long = System.currentTimeMillis()
)

/**
 * Worker / Staff entity provisioned exclusively by the Store Manager.
 */
@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey val id: String, // UUID
    val staffCode: String,      // e.g. "CASH-101", "MGR-001"
    val fullName: String,
    val phone: String = "",
    val role: StaffRole = StaffRole.CASHIER,
    val pin: String,            // 4-digit numeric login PIN issued by manager
    val isActive: Boolean = true,
    val canGiveDiscounts: Boolean = true,
    val canProcessRefunds: Boolean = false,
    val canEditInventory: Boolean = false,
    val canViewReports: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val totalSalesAmount: Double = 0.0,
    val totalTransactionsCount: Int = 0
)

/**
 * Room database schema to store product information including:
 * ID, name, price, and stock levels for offline checkout support.
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,  // Product ID
    val name: String,            // Product name
    val price: Double,           // Product selling price
    val stockLevel: Int,         // Current stock level for offline checkout
    val sku: String = "",
    val barcode: String = "",
    val category: String = "General",
    val costPrice: Double = 0.0,
    val lowStockThreshold: Int = 5,
    val unit: String = "pcs",
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Computed aliases for UI compatibility
    val sellingPrice: Double get() = price
    val stockQuantity: Int get() = stockLevel
}

/**
 * Line item in a completed transaction or active cart.
 */
data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val itemDiscountPercent: Double = 0.0
) {
    val unitPriceAfterDiscount: Double
        get() = product.price * (1.0 - itemDiscountPercent / 100.0)

    val lineTotal: Double
        get() = unitPriceAfterDiscount * quantity
}

/**
 * Completed POS sales transaction.
 */
@Entity(tableName = "transactions")
data class TransactionRecord(
    @PrimaryKey val id: String, // e.g. "TXN-2026-1001"
    val cashierStaffCode: String,
    val cashierName: String,
    val subtotal: Double,
    val orderDiscountAmount: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val amountTendered: Double,
    val changeGiven: Double,
    val customerInfo: String = "Walk-in Customer",
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
    val totalItemsCount: Int,
    val itemsSummary: String
)

/**
 * Parked / Held cart so cashier can serve another customer without losing items.
 */
data class HeldCart(
    val id: String,
    val customerNote: String,
    val items: List<CartItem>,
    val timestamp: Long = System.currentTimeMillis()
)
