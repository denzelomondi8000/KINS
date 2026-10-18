package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.StaffRole
import com.example.data.model.StoreConfig
import com.example.data.model.Worker
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PosViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val storeConfig by viewModel.storeConfig.collectAsState()
    val workers by viewModel.allWorkers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val isEffectivelyOnline by viewModel.isEffectivelyOnline.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Staff Provisioning, 1 = Sales Reports, 2 = Store Settings
    var showProvisionDialog by remember { mutableStateOf(false) }
    var selectedWorkerForPinReset by remember { mutableStateOf<Worker?>(null) }
    var newlyCreatedWorker by remember { mutableStateOf<Worker?>(null) }

    val currency = storeConfig?.currencySymbol ?: "$"
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Store Management Console", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Admin: ${storeConfig?.managerName ?: "Manager"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.POS_REGISTER) },
                        modifier = Modifier.testTag("manager_back_to_pos_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to POS Terminal")
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
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showProvisionDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Issue Worker Login", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("issue_worker_login_fab")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.testTag("manager_dashboard_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Staff Logins (${workers.size})", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sales Reports", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Store & Cloud", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> StaffProvisioningSection(
                    workers = workers,
                    currency = currency,
                    onToggleActive = { worker -> viewModel.toggleWorkerStatus(worker.id, worker.isActive) },
                    onResetPinClick = { worker -> selectedWorkerForPinReset = worker },
                    onDeleteWorker = { worker -> viewModel.deleteWorker(worker.id) }
                )
                1 -> SalesAnalyticsSection(
                    transactions = transactions,
                    workers = workers,
                    currency = currency
                )
                2 -> StoreAndCloudSection(
                    storeConfig = storeConfig,
                    viewModel = viewModel
                )
            }
        }
    }

    // Modal: Issue New Worker Login (Manager gives out login to worker)
    if (showProvisionDialog) {
        ProvisionWorkerDialog(
            onDismiss = { showProvisionDialog = false },
            onProvision = { name, phone, role, pin, discounts, refunds, inventory, reports ->
                viewModel.provisionNewWorker(
                    fullName = name,
                    phone = phone,
                    role = role,
                    pin = pin,
                    canGiveDiscounts = discounts,
                    canProcessRefunds = refunds,
                    canEditInventory = inventory,
                    canViewReports = reports,
                    onSuccess = { created ->
                        showProvisionDialog = false
                        newlyCreatedWorker = created
                    },
                    onError = { /* handled in viewmodel */ }
                )
            }
        )
    }

    // Modal: Display Newly Issued Worker Credentials
    newlyCreatedWorker?.let { worker ->
        AlertDialog(
            onDismissRequest = { newlyCreatedWorker = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = StatusOnline)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Worker Login Credentials Issued!")
                }
            },
            text = {
                Column {
                    Text("Provide these login credentials to the worker:")
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name: ${worker.fullName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Role: ${worker.role.name}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Staff Access Code:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = worker.staffCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Assigned 4-Digit PIN:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = worker.pin,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "The worker can now sign in at the POS terminal using this Staff Code and PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { newlyCreatedWorker = null }) {
                    Text("Done")
                }
            }
        )
    }

    // Modal: Reset Worker PIN
    selectedWorkerForPinReset?.let { worker ->
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedWorkerForPinReset = null },
            title = { Text("Reset PIN for ${worker.fullName}") },
            text = {
                Column {
                    Text("Enter a new 4-digit PIN for staff code ${worker.staffCode}:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("New PIN (min 4 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWorkerPin(worker.id, newPin)
                        selectedWorkerForPinReset = null
                    },
                    enabled = newPin.length >= 4
                ) {
                    Text("Update PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedWorkerForPinReset = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Section 1: Staff Provisioning & Management.
 */
@Composable
private fun StaffProvisioningSection(
    workers: List<Worker>,
    currency: String,
    onToggleActive: (Worker) -> Unit,
    onResetPinClick: (Worker) -> Unit,
    onDeleteWorker: (Worker) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Worker Access Control & PIN Provisioning",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Store Manager has exclusive authority to generate Staff IDs and 4-digit login PINs. Workers cannot register independently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(workers, key = { it.id }) { worker ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (worker.isActive) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worker_card_${worker.staffCode}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(worker.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (worker.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant
                                ) {
                                    Text(
                                        text = worker.role.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Staff Code: ${worker.staffCode} | Phone: ${worker.phone.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Active switch
                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = worker.isActive,
                                onCheckedChange = { onToggleActive(worker) }
                            )
                            Text(
                                text = if (worker.isActive) "Active" else "Suspended",
                                fontSize = 11.sp,
                                color = if (worker.isActive) StatusOnline else StatusError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sales performance metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sales: $currency${String.format(Locale.US, "%.2f", worker.totalSalesAmount)} (${worker.totalTransactionsCount} txns)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Login PIN: ****${worker.pin.takeLast(2)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Permissions tags
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (worker.canGiveDiscounts) PermissionPill("Discounts")
                        if (worker.canProcessRefunds) PermissionPill("Refunds")
                        if (worker.canEditInventory) PermissionPill("Stock Edit")
                        if (worker.canViewReports) PermissionPill("Analytics")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onResetPinClick(worker) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset PIN", fontSize = 13.sp)
                        }
                        IconButton(onClick = { onDeleteWorker(worker) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun PermissionPill(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Modal to provision a brand-new worker.
 */
@Composable
private fun ProvisionWorkerDialog(
    onDismiss: () -> Unit,
    onProvision: (String, String, StaffRole, String, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(StaffRole.CASHIER) }
    var pin by remember { mutableStateOf("1234") }
    var canGiveDiscounts by remember { mutableStateOf(true) }
    var canProcessRefunds by remember { mutableStateOf(false) }
    var canEditInventory by remember { mutableStateOf(false) }
    var canViewReports by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Issue New Worker Login")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Fill in the worker's details. A Staff Code will be generated automatically and linked to this PIN.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Worker Full Name *") },
                    placeholder = { Text("e.g. John Doe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Assigned Role:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(StaffRole.CASHIER, StaffRole.SUPERVISOR, StaffRole.INVENTORY_CLERK).forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = {
                                selectedRole = role
                                if (role == StaffRole.SUPERVISOR) {
                                    canGiveDiscounts = true
                                    canProcessRefunds = true
                                    canEditInventory = true
                                    canViewReports = true
                                } else if (role == StaffRole.INVENTORY_CLERK) {
                                    canEditInventory = true
                                    canGiveDiscounts = false
                                }
                            },
                            label = { Text(role.name.replace("_", " "), fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("Assigned 4-Digit Login PIN *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Permissions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Can Give Discounts")
                    Switch(checked = canGiveDiscounts, onCheckedChange = { canGiveDiscounts = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Can Process Refunds")
                    Switch(checked = canProcessRefunds, onCheckedChange = { canProcessRefunds = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Can Edit Inventory")
                    Switch(checked = canEditInventory, onCheckedChange = { canEditInventory = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Can View Sales Reports")
                    Switch(checked = canViewReports, onCheckedChange = { canViewReports = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onProvision(name, phone, selectedRole, pin, canGiveDiscounts, canProcessRefunds, canEditInventory, canViewReports)
                },
                enabled = name.isNotBlank() && pin.length >= 4
            ) {
                Text("Issue Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Section 2: Sales Analytics & Performance.
 */
@Composable
private fun SalesAnalyticsSection(
    transactions: List<com.example.data.model.TransactionRecord>,
    workers: List<Worker>,
    currency: String
) {
    val totalRevenue = transactions.sumOf { it.totalAmount }
    val totalTxns = transactions.size
    val averageBasket = if (totalTxns > 0) totalRevenue / totalTxns else 0.0

    val cashRevenue = transactions.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
    val cardRevenue = transactions.filter { it.paymentMethod == PaymentMethod.CARD }.sumOf { it.totalAmount }
    val mobileRevenue = transactions.filter { it.paymentMethod == PaymentMethod.MOBILE_MONEY }.sumOf { it.totalAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-level Revenue Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gross Sales", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format(Locale.US, "%s%.2f", currency, totalRevenue),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text("$totalTxns transactions", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Average Basket", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format(Locale.US, "%s%.2f", currency, averageBasket),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text("Per customer", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Payment Methods Breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Revenue by Payment Method", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))

                PaymentRow("Cash Tender", cashRevenue, totalRevenue, currency)
                PaymentRow("Card / Terminal POS", cardRevenue, totalRevenue, currency)
                PaymentRow("Mobile Money / QR", mobileRevenue, totalRevenue, currency)
            }
        }

        // Cashier Performance Leaderboard
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cashier Sales Performance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))

                workers.sortedByDescending { it.totalSalesAmount }.forEach { worker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(worker.fullName, fontWeight = FontWeight.SemiBold)
                            Text("Staff Code: ${worker.staffCode} (${worker.role.name})", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%s%.2f", currency, worker.totalSalesAmount),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("${worker.totalTransactionsCount} txns", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(name: String, amount: Double, total: Double, currency: String) {
    val pct = if (total > 0) (amount / total * 100.0) else 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$name (${String.format(Locale.US, "%.1f", pct)}%)", style = MaterialTheme.typography.bodyMedium)
        Text(String.format(Locale.US, "%s%.2f", currency, amount), fontWeight = FontWeight.Bold)
    }
}

/**
 * Section 3: Store Configuration & Cloud Sync Settings.
 */
@Composable
private fun StoreAndCloudSection(
    storeConfig: StoreConfig?,
    viewModel: PosViewModel
) {
    val isEffectivelyOnline by viewModel.isEffectivelyOnline.collectAsState()
    val simulateOffline by viewModel.simulateOffline.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var storeName by remember(storeConfig) { mutableStateOf(storeConfig?.storeName ?: "") }
    var managerName by remember(storeConfig) { mutableStateOf(storeConfig?.managerName ?: "") }
    var currencySymbol by remember(storeConfig) { mutableStateOf(storeConfig?.currencySymbol ?: "$") }
    var taxRate by remember(storeConfig) { mutableStateOf(storeConfig?.taxRatePercent?.toString() ?: "8.25") }
    var address by remember(storeConfig) { mutableStateOf(storeConfig?.address ?: "") }
    var phone by remember(storeConfig) { mutableStateOf(storeConfig?.phone ?: "") }
    var managerPin by remember(storeConfig) { mutableStateOf(storeConfig?.managerPin ?: "0000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cloud & Offline Diagnostics Card
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cloud & Offline Sync Engine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Simulate Offline Register Mode", fontWeight = FontWeight.SemiBold)
                        Text("Drill / test POS terminal behavior with internet disconnected.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = simulateOffline,
                        onCheckedChange = { viewModel.setSimulateOffline(it) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pending Sync Transactions: $pendingSyncCount")
                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync to Cloud")
                    }
                }
            }
        }

        // Store Profile Form
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Store Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = managerName,
                    onValueChange = { managerName = it },
                    label = { Text("Store Manager Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text("Currency Symbol") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = taxRate,
                        onValueChange = { taxRate = it },
                        label = { Text("Tax Rate (%)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Receipt Store Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Store Contact Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = managerPin,
                    onValueChange = { managerPin = it },
                    label = { Text("Manager Master PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        storeConfig?.let { cfg ->
                            viewModel.updateStoreConfig(
                                cfg.copy(
                                    storeName = storeName,
                                    managerName = managerName,
                                    currencySymbol = currencySymbol,
                                    taxRatePercent = taxRate.toDoubleOrNull() ?: 8.25,
                                    address = address,
                                    phone = phone,
                                    managerPin = managerPin
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Store Settings")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppScreen.CREATE_STORE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Setup / Re-Create POS System Name")
                }
            }
        }
    }
}
