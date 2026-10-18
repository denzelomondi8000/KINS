package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.data.model.StoreConfig
import com.example.data.model.TransactionRecord
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Visual badge showing current Online / Offline status and pending sync count.
 */
@Composable
fun SyncStatusBadge(
    isEffectivelyOnline: Boolean,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSyncing -> MaterialTheme.colorScheme.primaryContainer
        !isEffectivelyOnline -> Color(0xFFFEF3C7)
        pendingSyncCount > 0 -> Color(0xFFEFF6FF)
        else -> Color(0xFFD1FAE5)
    }

    val contentColor = when {
        isSyncing -> MaterialTheme.colorScheme.onPrimaryContainer
        !isEffectivelyOnline -> Color(0xFF92400E)
        pendingSyncCount > 0 -> Color(0xFF1E40AF)
        else -> Color(0xFF065F46)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        contentColor = contentColor,
        modifier = modifier
            .testTag("sync_status_badge")
            .clip(RoundedCornerShape(20.dp))
            .clickable { onBadgeClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (isSyncing) {
                val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing)
                    ),
                    label = "rotation"
                )
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Syncing",
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Syncing...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            } else if (!isEffectivelyOnline) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline Mode",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (pendingSyncCount > 0) "Offline ($pendingSyncCount pending)" else "Offline Terminal",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Online Synced",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (pendingSyncCount > 0) "Online ($pendingSyncCount queuing)" else "Online · Synced",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * On-screen numeric keypad for entering PINs or tender amounts.
 */
@Composable
fun NumericPinPad(
    pinLength: Int,
    currentPin: String,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // PIN bullets display
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth()
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < currentPin.length
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .border(
                            1.dp,
                            if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3x4 grid for keys
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "DEL")
        )

        for (row in keys) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 5.dp)
            ) {
                for (key in row) {
                    when (key) {
                        "DEL" -> {
                            FilledTonalButton(
                                onClick = onBackspace,
                                modifier = Modifier
                                    .size(72.dp, 56.dp)
                                    .testTag("pinpad_backspace"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete")
                            }
                        }
                        "C" -> {
                            FilledTonalButton(
                                onClick = onClear,
                                modifier = Modifier
                                    .size(72.dp, 56.dp)
                                    .testTag("pinpad_clear"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("CLR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    if (currentPin.length < pinLength) {
                                        onDigitClick(key)
                                    }
                                },
                                modifier = Modifier
                                    .size(72.dp, 56.dp)
                                    .testTag("pinpad_key_$key"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onSubmit,
            enabled = submitEnabled && currentPin.length >= 4,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(50.dp)
                .testTag("pinpad_submit_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Enter Terminal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Barcode simulator dialog allowing cashiers to simulate scanning an item barcode.
 */
@Composable
fun BarcodeScannerSimulatorDialog(
    products: List<Product>,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Barcode Scanner Gun")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Scan or enter a product Barcode / SKU to ring up item immediately:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label = { Text("Barcode or SKU") },
                    singleLine = true,
                    placeholder = { Text("e.g. 7891001 or BEV-001") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_barcode_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Quick Barcode Scan Simulation:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Quick buttons for first 6 products
                products.take(6).forEach { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onBarcodeScanned(product.barcode)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(product.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Barcode: ${product.barcode} | SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Scan",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (manualBarcode.isNotBlank()) {
                        onBarcodeScanned(manualBarcode.trim())
                        onDismiss()
                    }
                },
                enabled = manualBarcode.isNotBlank()
            ) {
                Text("Ring Up")
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
 * Realistic Digital Thermal Receipt Dialog.
 */
@Composable
fun DigitalReceiptDialog(
    transaction: TransactionRecord,
    storeConfig: StoreConfig?,
    currencySymbol: String = "$",
    onDismiss: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(transaction.timestamp))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("digital_receipt_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Receipt Header
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusOnline,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Payment Approved",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StatusOnline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = storeConfig?.storeName ?: "OmniPOS Superstore",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = storeConfig?.address ?: "Marketplace Terminal #1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = storeConfig?.phone ?: "+1 (800) 555-0199",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Rows
                ReceiptMetadataRow("Order ID:", transaction.id)
                ReceiptMetadataRow("Date/Time:", dateStr)
                ReceiptMetadataRow("Cashier:", "${transaction.cashierName} (${transaction.cashierStaffCode})")
                ReceiptMetadataRow("Customer:", transaction.customerInfo)
                ReceiptMetadataRow(
                    label = "Sync Status:",
                    value = if (transaction.syncStatus.name == "SYNCED") "Synced to Cloud ☁️" else "Queued Locally ⏳"
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Line items summary
                Text(
                    text = "ITEMS PURCHASED (${transaction.totalItemsCount})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = transaction.itemsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Financial Breakdown
                ReceiptAmountRow("Subtotal", String.format(Locale.US, "%s%.2f", currencySymbol, transaction.subtotal))
                if (transaction.orderDiscountAmount > 0) {
                    ReceiptAmountRow(
                        "Discount",
                        String.format(Locale.US, "-%s%.2f", currencySymbol, transaction.orderDiscountAmount),
                        color = StatusOnline
                    )
                }
                ReceiptAmountRow("Tax / VAT", String.format(Locale.US, "%s%.2f", currencySymbol, transaction.taxAmount))

                Spacer(modifier = Modifier.height(6.dp))
                ReceiptAmountRow(
                    "TOTAL",
                    String.format(Locale.US, "%s%.2f", currencySymbol, transaction.totalAmount),
                    isBold = true,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                ReceiptAmountRow("Payment Method", transaction.paymentMethod.name)
                ReceiptAmountRow("Tendered", String.format(Locale.US, "%s%.2f", currencySymbol, transaction.amountTendered))
                ReceiptAmountRow("Change Returned", String.format(Locale.US, "%s%.2f", currencySymbol, transaction.changeGiven))

                Spacer(modifier = Modifier.height(16.dp))

                // Stylized Barcode pattern
                Text(
                    text = "||| | ||||| || |||| ||| ||||| |||| ||",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.id,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Thank you for shopping with us!\nRetain this receipt for any exchanges.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next Sale")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReceiptAmountRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
    }
}
