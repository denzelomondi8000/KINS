package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncResult
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncCenterScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val storeConfig by viewModel.storeConfig.collectAsState()
    val isEffectivelyOnline by viewModel.isEffectivelyOnline.collectAsState()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsState()
    val simulateOffline by viewModel.simulateOffline.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val workers by viewModel.allWorkers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var showExportSummary by remember { mutableStateOf(false) }

    val lastSyncTimeFormatted = storeConfig?.lastCloudSyncTimestamp?.let {
        SimpleDateFormat("MMM dd, yyyy · HH:mm:ss", Locale.US).format(Date(it))
    } ?: "Never"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud & Offline Sync Hub", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.POS_REGISTER) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("sync_center_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Status Banner Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEffectivelyOnline) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isEffectivelyOnline) Color(0xFF065F46) else Color(0xFF92400E),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isEffectivelyOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isEffectivelyOnline) "Terminal Online & Connected" else "Terminal in Offline Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isEffectivelyOnline) Color(0xFF065F46) else Color(0xFF92400E)
                        )
                        Text(
                            text = if (isEffectivelyOnline) {
                                if (pendingSyncCount > 0) "$pendingSyncCount transactions ready to sync" else "All catalog & sales data fully synchronized"
                            } else {
                                "Staff can ring up sales with 100% offline capability"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEffectivelyOnline) Color(0xFF047857) else Color(0xFFB45309)
                        )
                    }
                }
            }

            // Sync Queue & Actions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Cloud Sync Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pending Transactions to Upload:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$pendingSyncCount sales",
                            fontWeight = FontWeight.Bold,
                            color = if (pendingSyncCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Cloud Sync Timestamp:", style = MaterialTheme.typography.bodyMedium)
                        Text(lastSyncTimeFormatted, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing with Cloud Store Hub...")
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync to Cloud Now")
                        }
                    }

                    // Display last sync result alert
                    lastSyncResult?.let { result ->
                        Spacer(modifier = Modifier.height(10.dp))
                        when (result) {
                            is SyncResult.Success -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFD1FAE5),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF065F46), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(result.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF065F46))
                                    }
                                }
                            }
                            is SyncResult.Failure -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF991B1B), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(result.errorMessage, style = MaterialTheme.typography.bodySmall, color = Color(0xFF991B1B))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Connectivity Drill & Offline Simulation Card
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Offline Drill & Network Controls", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Test that cashiers and store staff can ring up sales, print receipts, and manage inventory when the physical network is disconnected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDeviceOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (isDeviceOnline) StatusOnline else StatusOffline
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Device Physical Connectivity:")
                        }
                        Text(
                            text = if (isDeviceOnline) "Connected" else "No Connection",
                            fontWeight = FontWeight.Bold,
                            color = if (isDeviceOnline) StatusOnline else StatusOffline
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Offline Mode", fontWeight = FontWeight.SemiBold)
                            Text("Force the terminal to operate in disconnected mode.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = simulateOffline,
                            onCheckedChange = { viewModel.setSimulateOffline(it) }
                        )
                    }
                }
            }

            // Store Snapshot & Cloud Export Card
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Store Backup & Export", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Export a comprehensive JSON snapshot of the local database including active catalog, staff rosters, and transaction logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showExportSummary = !showExportSummary },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showExportSummary) "Hide Backup Snapshot" else "Generate Store Backup Snapshot")
                    }

                    if (showExportSummary) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("SNAPSHOT SUMMARY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Store: ${storeConfig?.storeName ?: "OmniPOS"}")
                                Text("• Total Catalog Items: ${products.size} SKUs")
                                Text("• Provisioned Workers: ${workers.size} active accounts")
                                Text("• Total Sales Recorded: ${transactions.size} orders (${storeConfig?.currencySymbol}${String.format(Locale.US, "%.2f", transactions.sumOf { it.totalAmount })})")
                                Text("• Pending Cloud Sync: $pendingSyncCount items")
                                Text("• Local Engine: SQLite Room + KSP (Encrypted Schema)")
                            }
                        }
                    }
                }
            }
        }
    }
}
