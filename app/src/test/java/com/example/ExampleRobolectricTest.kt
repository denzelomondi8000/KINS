package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StaffRole
import com.example.data.repository.PosRepository
import com.example.data.sync.SyncManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PosRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val syncManager = SyncManager(context, database)
        repository = PosRepository(database, syncManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OmniPOS", appName)
    }

    @Test
    fun `create pos system name form saves to Room with 0000 PIN and product schema`() = runBlocking {
        // Manager creates a POS system with custom name and default PIN 0000
        val config = repository.createPosSystem(
            storeName = "Denzel's Superstore",
            managerName = "Store Manager",
            managerPin = "0000",
            currencySymbol = "$",
            taxRatePercent = 8.0
        )

        assertEquals("Denzel's Superstore", config.storeName)
        assertEquals("0000", config.managerPin)
        assertTrue(config.isConfigured)

        // Authenticate with 0000
        val isManagerAuth = repository.authenticateManager("0000")
        assertTrue(isManagerAuth)

        // Verify products are in Room database with ID, name, price, and stock levels
        val coldBrew = repository.findProductByBarcodeOrSku("7891001")
        assertNotNull(coldBrew)
        assertEquals("Cold Brew Coffee 330ml", coldBrew?.name)
        assertEquals(3.49, coldBrew?.price ?: 0.0, 0.001)
        assertEquals(45, coldBrew?.stockLevel)
    }

    @Test
    fun `manager provisions worker and worker authenticates with pin`() = runBlocking {
        // Manager provisions a new cashier
        val provisionedWorker = repository.provisionNewWorker(
            fullName = "Jane Cashier",
            phone = "+1 555-0987",
            role = StaffRole.CASHIER,
            pin = "5555",
            canGiveDiscounts = true,
            canProcessRefunds = false,
            canEditInventory = false,
            canViewReports = false
        )

        assertNotNull(provisionedWorker.staffCode)
        assertEquals("Jane Cashier", provisionedWorker.fullName)

        // Authenticate with correct PIN
        val authSuccess = repository.authenticateWorker(provisionedWorker.staffCode, "5555")
        assertTrue(authSuccess.isSuccess)
        assertEquals("Jane Cashier", authSuccess.getOrNull()?.fullName)

        // Authenticate with incorrect PIN
        val authFailure = repository.authenticateWorker(provisionedWorker.staffCode, "9999")
        assertTrue(authFailure.isFailure)
    }

    @Test
    fun `checkout decrements inventory and records transaction`() = runBlocking {
        val worker = repository.provisionNewWorker(
            fullName = "Test Cashier",
            phone = "+1 555-1234",
            role = StaffRole.CASHIER,
            pin = "1111",
            canGiveDiscounts = true,
            canProcessRefunds = false,
            canEditInventory = false,
            canViewReports = false
        )

        val product = Product(
            id = UUID.randomUUID().toString(),
            sku = "SKU-TEST-01",
            barcode = "999001",
            name = "Test Sparkling Water",
            category = "Beverages",
            costPrice = 1.00,
            price = 2.50,
            stockLevel = 20
        )
        repository.addProduct(product)

        val cart = listOf(
            CartItem(product = product, quantity = 2)
        )

        val checkoutResult = repository.processCheckout(
            cashier = worker,
            items = cart,
            paymentMethod = PaymentMethod.CASH,
            amountTendered = 10.00
        )

        assertTrue(checkoutResult.isSuccess)
        val txn = checkoutResult.getOrNull()
        assertNotNull(txn)
        assertEquals(worker.staffCode, txn?.cashierStaffCode)

        // Check that stock was decremented from 20 to 18
        val updatedProduct = repository.findProductByBarcodeOrSku(product.barcode)
        assertNotNull(updatedProduct)
        assertEquals(18, updatedProduct?.stockLevel)
    }
}
