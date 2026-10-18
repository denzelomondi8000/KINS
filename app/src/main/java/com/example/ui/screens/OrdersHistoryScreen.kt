package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionRecord
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersHistoryScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val storeConfig by viewModel.storeConfig.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val isEffectivelyOnline by viewModel.isEffectivelyOnline.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val currency = storeConfig?.currencySymbol ?: "$"
    var selectedPaymentFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var viewingReceiptForTxn by remember { mutableStateOf<TransactionRecord?>(null) }

    val filteredTransactions = transactions.filter { txn ->
        val matchesPayment = (selectedPaymentFilter == "ALL") || (txn.paymentMethod.name == selectedPaymentFilter)
        val matchesQuery = searchQuery.isBlank() ||
                txn.id.contains(searchQuery, ignoreCase = true) ||
                txn.cashierName.contains(searchQuery, ignoreCase = true) ||
                txn.cashierStaffCode.contains(searchQuery, ignoreCase = true)
        matchesPayment && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sales & Orders Audit Log", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${transactions.size} Total Orders Recorded", style = MaterialTheme.typography.bodySmall)
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
        modifier = modifier.testTag("orders_history_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Order ID or Cashier...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                val filters = listOf("ALL", "CASH", "CARD", "MOBILE_MONEY")
                items(filters) { filter ->
                    val isSelected = selectedPaymentFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPaymentFilter = filter },
                        label = { Text(filter.replace("_", " ")) }
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions match the criteria.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { txn ->
                        val dateFormatted = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.US).format(Date(txn.timestamp))

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingReceiptForTxn = txn }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(txn.id, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(dateFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%s%.2f", currency, txn.totalAmount),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        // Cloud sync tag
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (txn.syncStatus == SyncStatus.SYNCED) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (txn.syncStatus == SyncStatus.SYNCED) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                                    contentDescription = null,
                                                    tint = if (txn.syncStatus == SyncStatus.SYNCED) Color(0xFF065F46) else Color(0xFF92400E),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (txn.syncStatus == SyncStatus.SYNCED) "Synced" else "Queued",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (txn.syncStatus == SyncStatus.SYNCED) Color(0xFF065F46) else Color(0xFF92400E)
                                                )
                                            }
                                        }
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (txn.paymentMethod) {
                                                PaymentMethod.CASH -> Icons.Default.LocalAtm
                                                PaymentMethod.CARD -> Icons.Default.CreditCard
                                                PaymentMethod.MOBILE_MONEY -> Icons.Default.PhoneAndroid
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(txn.paymentMethod.name, style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("· Cashier: ${txn.cashierName} (${txn.cashierStaffCode})", style = MaterialTheme.typography.bodySmall)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Receipt", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    viewingReceiptForTxn?.let { txn ->
        DigitalReceiptDialog(
            transaction = txn,
            storeConfig = storeConfig,
            currencySymbol = currency,
            onDismiss = { viewingReceiptForTxn = null }
        )
    }
}
