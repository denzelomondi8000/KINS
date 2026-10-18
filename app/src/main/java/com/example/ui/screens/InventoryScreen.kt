package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.StaffRole
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PosViewModel
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val currentWorker by viewModel.currentWorker.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val isEffectivelyOnline by viewModel.isEffectivelyOnline.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val currency = storeConfig?.currencySymbol ?: "$"
    val canEdit = currentWorker?.role == StaffRole.STORE_MANAGER || currentWorker?.canEditInventory == true

    var showLowStockOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var productToRestock by remember { mutableStateOf<Product?>(null) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    val displayedProducts = allProducts.filter { product ->
        val matchesStock = !showLowStockOnly || (product.stockQuantity <= product.lowStockThreshold)
        val matchesQuery = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.sku.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true)
        matchesStock && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inventory & Stock Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${allProducts.size} Products (${lowStockProducts.size} Low Stock)", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.POS_REGISTER) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    SyncStatusBadge(
                        isEffectivelyOnline = isEffectivelyOnline,
                        pendingSyncCount = pendingSyncCount,
                        isSyncing = isSyncing,
                        onBadgeClick = { viewModel.navigateTo(AppScreen.SYNC_CENTER) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (canEdit) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add New Product", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("add_product_fab")
                )
            }
        },
        modifier = modifier.testTag("inventory_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search & Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, SKU or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilterChip(
                    selected = showLowStockOnly,
                    onClick = { showLowStockOnly = !showLowStockOnly },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (lowStockProducts.isNotEmpty()) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusOffline, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Low Stock (${lowStockProducts.size})")
                        }
                    }
                )
            }

            if (displayedProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No products match the criteria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedProducts, key = { it.id }) { product ->
                        val isOut = product.stockQuantity <= 0
                        val isLow = product.stockQuantity in 1..product.lowStockThreshold

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            "Category: ${product.category} | SKU: ${product.sku} | Barcode: ${product.barcode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Stock pill
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isOut -> Color(0xFFFEE2E2)
                                            isLow -> Color(0xFFFEF3C7)
                                            else -> Color(0xFFD1FAE5)
                                        }
                                    ) {
                                        Text(
                                            text = "${product.stockQuantity} ${product.unit}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when {
                                                isOut -> Color(0xFF991B1B)
                                                isLow -> Color(0xFF92400E)
                                                else -> Color(0xFF065F46)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Retail: $currency${String.format(Locale.US, "%.2f", product.sellingPrice)} (Cost: $currency${String.format(Locale.US, "%.2f", product.costPrice)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val margin = if (product.sellingPrice > 0) ((product.sellingPrice - product.costPrice) / product.sellingPrice * 100.0) else 0.0
                                        Text(
                                            "Margin: ${String.format(Locale.US, "%.1f", margin)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusOnline
                                        )
                                    }

                                    if (canEdit) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { productToRestock = product },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Restock", fontSize = 12.sp)
                                            }
                                            IconButton(onClick = { productToEdit = product }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            IconButton(onClick = { viewModel.deleteProduct(product.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Modal: Quick Restock Dialog
    productToRestock?.let { product ->
        var qtyToAdd by remember { mutableStateOf("10") }
        AlertDialog(
            onDismissRequest = { productToRestock = null },
            title = { Text("Restock: ${product.name}") },
            text = {
                Column {
                    Text("Current stock: ${product.stockQuantity} ${product.unit}")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = qtyToAdd,
                        onValueChange = { if (it.all { c -> c.isDigit() }) qtyToAdd = it },
                        label = { Text("Quantity to Add") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 25, 50).forEach { amount ->
                            OutlinedButton(onClick = { qtyToAdd = "$amount" }) {
                                Text("+$amount")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyToAdd.toIntOrNull() ?: 0
                        if (qty > 0) {
                            viewModel.restockProduct(product.id, qty)
                        }
                        productToRestock = null
                    }
                ) {
                    Text("Confirm Restock")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToRestock = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Add or Edit Product Dialog
    if (showAddDialog || productToEdit != null) {
        val existing = productToEdit
        var name by remember { mutableStateOf(existing?.name ?: "") }
        var category by remember { mutableStateOf(existing?.category ?: "Beverages") }
        var sku by remember { mutableStateOf(existing?.sku ?: "SKU-${(100..999).random()}") }
        var barcode by remember { mutableStateOf(existing?.barcode ?: "${(7890000..7899999).random()}") }
        var costPrice by remember { mutableStateOf(existing?.costPrice?.toString() ?: "1.50") }
        var sellingPrice by remember { mutableStateOf(existing?.sellingPrice?.toString() ?: "3.99") }
        var stockQty by remember { mutableStateOf(existing?.stockQuantity?.toString() ?: "20") }
        var unit by remember { mutableStateOf(existing?.unit ?: "pcs") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                productToEdit = null
            },
            title = { Text(if (existing != null) "Edit Product" else "Add New Product") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("Cost Price") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("Selling Price *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = stockQty,
                        onValueChange = { stockQty = it },
                        label = { Text("Stock Quantity *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val product = Product(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            category = category,
                            sku = sku,
                            barcode = barcode,
                            costPrice = costPrice.toDoubleOrNull() ?: 1.0,
                            price = sellingPrice.toDoubleOrNull() ?: 2.0,
                            stockLevel = stockQty.toIntOrNull() ?: 0,
                            unit = unit
                        )
                        if (existing != null) {
                            viewModel.updateProduct(product) { productToEdit = null }
                        } else {
                            viewModel.addProduct(product) { showAddDialog = false }
                        }
                    },
                    enabled = name.isNotBlank() && sellingPrice.isNotBlank()
                ) {
                    Text(if (existing != null) "Save Changes" else "Add Product")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    productToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
