package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Product
import com.example.data.model.StoreConfig
import com.example.data.model.TransactionRecord
import com.example.data.model.Worker
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreConfigDao {
    @Query("SELECT * FROM store_config WHERE storeId = 'STORE_MAIN' LIMIT 1")
    fun getStoreConfig(): Flow<StoreConfig?>

    @Query("SELECT * FROM store_config WHERE storeId = 'STORE_MAIN' LIMIT 1")
    suspend fun getStoreConfigOnce(): StoreConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStoreConfig(config: StoreConfig)

    @Query("UPDATE store_config SET lastCloudSyncTimestamp = :timestamp WHERE storeId = 'STORE_MAIN'")
    suspend fun updateLastSyncTimestamp(timestamp: Long)

    @Query("DELETE FROM store_config")
    suspend fun clearStoreConfig()
}

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY role ASC, fullName ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    suspend fun getWorkerById(id: String): Worker?

    @Query("SELECT * FROM workers WHERE staffCode = :staffCode LIMIT 1")
    suspend fun getWorkerByStaffCode(staffCode: String): Worker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkers(workers: List<Worker>)

    @Update
    suspend fun updateWorker(worker: Worker)

    @Query("UPDATE workers SET pin = :newPin WHERE id = :workerId")
    suspend fun updateWorkerPin(workerId: String, newPin: String)

    @Query("UPDATE workers SET isActive = :isActive WHERE id = :workerId")
    suspend fun updateWorkerStatus(workerId: String, isActive: Boolean)

    @Query("UPDATE workers SET lastLoginAt = :timestamp WHERE id = :workerId")
    suspend fun updateWorkerLastLogin(workerId: String, timestamp: Long)

    @Query("UPDATE workers SET totalSalesAmount = totalSalesAmount + :amount, totalTransactionsCount = totalTransactionsCount + 1 WHERE staffCode = :staffCode")
    suspend fun incrementWorkerSales(staffCode: String, amount: Double)

    @Query("DELETE FROM workers WHERE id = :workerId")
    suspend fun deleteWorker(workerId: String)

    @Query("DELETE FROM workers")
    suspend fun clearWorkers()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY category ASC, name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockLevel <= lowStockThreshold ORDER BY stockLevel ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode OR sku = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET stockLevel = stockLevel - :quantityPurchased, syncStatus = 'PENDING_SYNC', updatedAt = :timestamp WHERE id = :productId")
    suspend fun decrementStock(productId: String, quantityPurchased: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockLevel = stockLevel + :quantityAdded, syncStatus = 'PENDING_SYNC', updatedAt = :timestamp WHERE id = :productId")
    suspend fun restockProduct(productId: String, quantityAdded: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET syncStatus = 'SYNCED' WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun markAllProductsSynced()

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: String)

    @Query("DELETE FROM products")
    suspend fun clearProducts()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_SYNC' ORDER BY timestamp ASC")
    suspend fun getPendingSyncTransactions(): List<TransactionRecord>

    @Query("SELECT COUNT(*) FROM transactions WHERE syncStatus = 'PENDING_SYNC'")
    fun getPendingSyncCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE cashierStaffCode = :staffCode ORDER BY timestamp DESC")
    fun getTransactionsByCashier(staffCode: String): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionRecord)

    @Query("UPDATE transactions SET syncStatus = 'SYNCED' WHERE id IN (:transactionIds)")
    suspend fun markTransactionsSynced(transactionIds: List<String>)

    @Query("UPDATE transactions SET syncStatus = 'SYNCED'")
    suspend fun markAllTransactionsSynced()
}
