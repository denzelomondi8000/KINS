package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.HeldCart
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StaffRole
import com.example.data.model.SyncStatus
import com.example.ui.components.BarcodeScannerSimulatorDialog
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosRegisterScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val currentWorker by viewModel.currentWorker.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val isEffectivelyOnline by viewModel.isEffectivelyOnline.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val products by viewModel.filteredProducts.collectAsState()
    val allProductsList by viewModel.allProducts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val orderDiscountPercent by viewModel.orderDiscountPercent.collectAsState()
    val heldCarts by viewModel.heldCarts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val activeReceipt by viewModel.activeReceipt.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val currency = storeConfig?.currencySymbol ?: "$"
    val taxRate = (storeConfig?.taxRatePercent ?: 8.25) / 100.0

    // Cart calculations
    val cartSubtotal = cartItems.sumOf { it.lineTotal }
    val cartDiscount = cartSubtotal * (orderDiscountPercent / 100.0)
    val discountedSubtotal = (cartSubtotal - cartDiscount).coerceAtLeast(0.0)
    val cartTax = discountedSubtotal * taxRate
    val cartTotal = discountedSubtotal + cartTax
    val totalCartUnits = cartItems.sumOf { it.quantity }

    var showBarcodeDialog by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showHeldCartsDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val categories = listOf("All", "Beverages", "Bakery", "Snacks", "Produce", "Dairy", "Household", "Personal Care")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = storeConfig?.storeName ?: "OmniPOS Store",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cashier: ${currentWorker?.fullName ?: "Staff"} [${currentWorker?.staffCode ?: ""}]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentWorker?.role == StaffRole.STORE_MANAGER || currentWorker?.role == StaffRole.SUPERVISOR) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = currentWorker?.role?.name ?: "",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    SyncStatusBadge(
                        isEffectivelyOnline = isEffectivelyOnline,
                        pendingSyncCount = pendingSyncCount,
                        isSyncing = isSyncing,
                        onBadgeClick = { viewModel.navigateTo(AppScreen.SYNC_CENTER) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    // Inventory catalog shortcut
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.INVENTORY) },
                        modifier = Modifier.testTag("nav_inventory_button")
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Inventory")
                    }

                    // Orders history shortcut
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.ORDERS_HISTORY) },
                        modifier = Modifier.testTag("nav_orders_history_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Orders History")
                    }

                    // Manager dashboard (if authorized)
                    if (currentWorker?.role == StaffRole.STORE_MANAGER || currentWorker?.canViewReports == true) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.MANAGER_DASHBOARD) },
                            modifier = Modifier.testTag("nav_manager_dashboard_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Manager Dashboard", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Clock out / Switch Staff
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Switch Staff / Clock Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Persistent Bottom Cart Bar
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showCartSheet = true }
                            .padding(4.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (totalCartUnits > 0) {
                                    Badge { Text("$totalCartUnits") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = String.format(Locale.US, "%s%.2f", currency, cartTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (totalCartUnits == 0) "Cart is empty" else "$totalCartUnits items (Tap to review)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Hold cart button
                        if (heldCarts.isNotEmpty()) {
                            BadgedBox(badge = { Badge { Text("${heldCarts.size}") } }) {
                                OutlinedButton(
                                    onClick = { showHeldCartsDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Held (${heldCarts.size})", fontSize = 13.sp)
                                }
                            }
                        } else if (cartItems.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.holdCurrentCart() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hold", fontSize = 13.sp)
                            }
                        }

                        // Charge / Checkout Button
                        Button(
                            onClick = { showCheckoutSheet = true },
                            enabled = cartItems.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .testTag("pos_checkout_button")
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Charge $currency${String.format(Locale.US, "%.2f", cartTotal)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag("pos_register_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Barcode Gun Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search product name, SKU or barcode...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_search_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = { showBarcodeDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("barcode_scanner_trigger")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode Gun")
                }
            }

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedCategory(category) },
                        label = { Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Products Catalog Grid
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching products found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try clearing filters or search term.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductPosCard(
                            product = product,
                            currency = currency,
                            onAddToCart = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }
    }

    // Barcode Simulator Dialog
    if (showBarcodeDialog) {
        BarcodeScannerSimulatorDialog(
            products = allProductsList,
            onBarcodeScanned = { code ->
                viewModel.scanBarcodeOrSku(code)
            },
            onDismiss = { showBarcodeDialog = false }
        )
    }

    // Digital Receipt Dialog upon Checkout
    activeReceipt?.let { receipt ->
        DigitalReceiptDialog(
            transaction = receipt,
            storeConfig = storeConfig,
            currencySymbol = currency,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }

    // Bottom Sheet: Detailed Cart Review & Adjustments
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Sale Cart ($totalCartUnits items)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        if (currentWorker?.canGiveDiscounts == true) {
                            TextButton(onClick = { showDiscountDialog = true }) {
                                Text(if (orderDiscountPercent > 0) "${orderDiscountPercent.toInt()}% Off" else "Add Discount")
                            }
                        }
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear cart", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cart is currently empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(cartItems) { item ->
                            CartItemRow(
                                item = item,
                                currency = currency,
                                onIncrement = { viewModel.updateCartItemQuantity(item.product.id, 1) },
                                onDecrement = { viewModel.updateCartItemQuantity(item.product.id, -1) },
                                onRemove = { viewModel.removeCartItem(item.product.id) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary breakdown
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%s%.2f", currency, cartSubtotal))
                    }
                    if (cartDiscount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount (${orderDiscountPercent.toInt()}%)", color = StatusOnline)
                            Text(String.format(Locale.US, "-%s%.2f", currency, cartDiscount), color = StatusOnline)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tax / VAT (${storeConfig?.taxRatePercent ?: 8.25}%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%s%.2f", currency, cartTax))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Due", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            String.format(Locale.US, "%s%.2f", currency, cartTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showCartSheet = false
                            showCheckoutSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Proceed to Payment ($currency${String.format(Locale.US, "%.2f", cartTotal)})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Checkout Sheet: Payment Methods, Tender Amount & Change Due
    if (showCheckoutSheet) {
        CheckoutSheet(
            totalDue = cartTotal,
            currency = currency,
            onDismiss = { showCheckoutSheet = false },
            onCompletePayment = { paymentMethod, tendered, customerNote ->
                showCheckoutSheet = false
                viewModel.checkout(
                    paymentMethod = paymentMethod,
                    amountTendered = tendered,
                    customerInfo = customerNote,
                    onSuccess = { /* Receipt will pop up via activeReceipt StateFlow */ },
                    onError = { err -> /* handled in viewmodel statusMessage */ }
                )
            }
        )
    }

    // Held / Parked Carts Dialog
    if (showHeldCartsDialog) {
        AlertDialog(
            onDismissRequest = { showHeldCartsDialog = false },
            title = { Text("Parked / Held Customer Carts") },
            text = {
                Column {
                    if (heldCarts.isEmpty()) {
                        Text("No held carts currently.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(heldCarts) { held ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(held.customerNote, fontWeight = FontWeight.Bold)
                                            Text("${held.items.sumOf { it.quantity }} items · Total $currency${String.format(Locale.US, "%.2f", held.items.sumOf { it.lineTotal })}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row {
                                            FilledTonalButton(onClick = {
                                                viewModel.resumeHeldCart(held)
                                                showHeldCartsDialog = false
                                            }) {
                                                Text("Resume")
                                            }
                                            IconButton(onClick = { viewModel.deleteHeldCart(held) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Discard")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHeldCartsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Discount percentage input dialog
    if (showDiscountDialog) {
        var discountInput by remember { mutableStateOf(if (orderDiscountPercent > 0) orderDiscountPercent.toInt().toString() else "10") }
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Apply Order Discount") },
            text = {
                Column {
                    Text("Enter discount percentage for this customer:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Discount (%)") },
                        trailingIcon = { Text("%", modifier = Modifier.padding(end = 12.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20).forEach { pct ->
                            OutlinedButton(onClick = { discountInput = "$pct" }) {
                                Text("$pct%")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pct = discountInput.toDoubleOrNull() ?: 0.0
                    viewModel.setOrderDiscountPercent(pct)
                    showDiscountDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setOrderDiscountPercent(0.0)
                    showDiscountDialog = false
                }) {
                    Text("Remove Discount")
                }
            }
        )
    }
}

/**
 * Clean, interactive Product Card for fast POS tapping.
 */
@Composable
fun ProductPosCard(
    product: Product,
    currency: String,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = product.stockQuantity in 1..product.lowStockThreshold

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 2.dp),
        modifier = modifier
            .testTag("product_card_${product.sku}")
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) { onAddToCart() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Category & Stock Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isOutOfStock) {
                    Text(
                        text = "Out of Stock",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                } else if (isLowStock) {
                    Text(
                        text = "Only ${product.stockQuantity} left",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusOffline
                    )
                } else {
                    Text(
                        text = "${product.stockQuantity} ${product.unit}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )

            Text(
                text = "SKU: ${product.sku}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price & 1-tap Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "%s%.2f", currency, product.sellingPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = CircleShape,
                    color = if (isOutOfStock) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isOutOfStock) { onAddToCart() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Line item row in the active cart sheet.
 */
@Composable
fun CartItemRow(
    item: CartItem,
    currency: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                String.format(Locale.US, "%s%.2f / %s", currency, item.product.sellingPrice, item.product.unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Qty controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilledTonalButton(
                onClick = onDecrement,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }

            Text(
                text = "${item.quantity}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )

            FilledTonalButton(
                onClick = onIncrement,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = String.format(Locale.US, "%s%.2f", currency, item.lineTotal),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End
        )

        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Fast checkout modal with payment method selection, tender suggestions, and change calculation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    totalDue: Double,
    currency: String,
    onDismiss: () -> Unit,
    onCompletePayment: (PaymentMethod, Double, String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var amountTenderedStr by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalDue)) }
    var customerNote by remember { mutableStateOf("") }

    val tendered = amountTenderedStr.toDoubleOrNull() ?: 0.0
    val changeDue = (tendered - totalDue).coerceAtLeast(0.0)
    val isTenderSufficient = (selectedMethod != PaymentMethod.CASH) || (tendered >= totalDue)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Checkout & Tender",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total to Pay:", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = String.format(Locale.US, "%s%.2f", currency, totalDue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Tabs
            Text("Select Payment Method:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodButton(
                    method = PaymentMethod.CASH,
                    label = "Cash",
                    icon = Icons.Default.LocalAtm,
                    isSelected = selectedMethod == PaymentMethod.CASH,
                    onClick = {
                        selectedMethod = PaymentMethod.CASH
                        amountTenderedStr = String.format(Locale.US, "%.2f", totalDue)
                    },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodButton(
                    method = PaymentMethod.CARD,
                    label = "Card POS",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedMethod == PaymentMethod.CARD,
                    onClick = {
                        selectedMethod = PaymentMethod.CARD
                        amountTenderedStr = String.format(Locale.US, "%.2f", totalDue)
                    },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodButton(
                    method = PaymentMethod.MOBILE_MONEY,
                    label = "Mobile Pay",
                    icon = Icons.Default.PhoneAndroid,
                    isSelected = selectedMethod == PaymentMethod.MOBILE_MONEY,
                    onClick = {
                        selectedMethod = PaymentMethod.MOBILE_MONEY
                        amountTenderedStr = String.format(Locale.US, "%.2f", totalDue)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedMethod == PaymentMethod.CASH) {
                // Quick Bill suggestions e.g. Exact, Next 5, 10, 20, 50, 100
                Text("Quick Tender Amount:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val bills = listOf(
                        totalDue,
                        kotlin.math.ceil(totalDue / 5.0) * 5.0,
                        kotlin.math.ceil(totalDue / 10.0) * 10.0,
                        20.0,
                        50.0,
                        100.0
                    ).filter { it >= totalDue }.distinct()

                    items(bills) { bill ->
                        OutlinedButton(
                            onClick = { amountTenderedStr = String.format(Locale.US, "%.2f", bill) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (bill == totalDue) "Exact ($currency${String.format(Locale.US, "%.2f", bill)})" else "$currency${bill.toInt()}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountTenderedStr,
                    onValueChange = { amountTenderedStr = it },
                    label = { Text("Amount Tendered by Customer") },
                    leadingIcon = { Text(currency, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Change calculation banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (changeDue >= 0 && tendered >= totalDue) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (tendered >= totalDue) "Change to Return:" else "Amount Still Due:",
                            fontWeight = FontWeight.Bold,
                            color = if (tendered >= totalDue) Color(0xFF065F46) else Color(0xFF991B1B)
                        )
                        Text(
                            text = if (tendered >= totalDue) {
                                String.format(Locale.US, "%s%.2f", currency, changeDue)
                            } else {
                                String.format(Locale.US, "%s%.2f", currency, totalDue - tendered)
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (tendered >= totalDue) Color(0xFF065F46) else Color(0xFF991B1B)
                        )
                    }
                }
            } else if (selectedMethod == PaymentMethod.CARD) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Tap / Insert Card into Connected PIN Pad", fontWeight = FontWeight.Bold)
                        Text("EMV Chip, NFC contactless and magnetic stripe transactions supported offline and synced.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Customer Scans QR / Enters Till Number", fontWeight = FontWeight.Bold)
                        Text("M-Pesa, Apple Pay, Google Pay or UPI prompt dispatched to customer phone.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customerNote,
                onValueChange = { customerNote = it },
                label = { Text("Customer Info / Phone (Optional)") },
                singleLine = true,
                placeholder = { Text("e.g. John Doe (+1 555-019)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onCompletePayment(selectedMethod, tendered, customerNote)
                },
                enabled = isTenderSufficient,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("complete_payment_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Complete Sale & Issue Receipt", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentMethodButton(
    method: PaymentMethod,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
