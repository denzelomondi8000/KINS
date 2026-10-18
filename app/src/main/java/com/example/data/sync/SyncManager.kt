package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SyncResult {
    data class Success(val message: String, val syncedTxnCount: Int) : SyncResult()
    data class Failure(val errorMessage: String) : SyncResult()
}

class SyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isDeviceOnline = MutableStateFlow(checkInitialConnectivity())
    val isDeviceOnline: StateFlow<Boolean> = _isDeviceOnline

    private val _simulateOffline = MutableStateFlow(false)
    val simulateOffline: StateFlow<Boolean> = _simulateOffline

    // Effective online state: True only if device is actually connected and simulateOffline is false
    val isEffectivelyOnline: StateFlow<Boolean> = combine(
        _isDeviceOnline,
        _simulateOffline
    ) { deviceOnline, simOffline ->
        deviceOnline && !simOffline
    }.stateIn(scope, SharingStarted.Eagerly, checkInitialConnectivity())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isDeviceOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isDeviceOnline.value = false
                }
            }
        )
    }

    fun setSimulateOffline(simulate: Boolean) {
        _simulateOffline.value = simulate
    }

    suspend fun triggerCloudSync(): SyncResult {
        if (!isEffectivelyOnline.value) {
            val failure = SyncResult.Failure("Terminal is in Offline Mode. Connect to the internet or disable offline simulation to sync with Cloud Hub.")
            _lastSyncResult.value = failure
            return failure
        }

        _isSyncing.value = true
        return try {
            // Emulate cloud sync handshakes and transaction packet uploads
            val pendingTxns = database.transactionDao().getPendingSyncTransactions()
            delay(1200) // Realistic cloud round-trip delay

            if (pendingTxns.isNotEmpty()) {
                val txnIds = pendingTxns.map { it.id }
                database.transactionDao().markTransactionsSynced(txnIds)
            }
            database.productDao().markAllProductsSynced()
            val now = System.currentTimeMillis()
            database.storeConfigDao().updateLastSyncTimestamp(now)

            val success = SyncResult.Success(
                message = if (pendingTxns.isNotEmpty()) {
                    "Cloud sync complete: ${pendingTxns.size} transactions backed up to Cloud Store Hub."
                } else {
                    "Cloud sync complete: All transactions, stock counts, and staff records are up to date."
                },
                syncedTxnCount = pendingTxns.size
            )
            _lastSyncResult.value = success
            success
        } catch (e: Exception) {
            val failure = SyncResult.Failure("Cloud sync failed: ${e.localizedMessage ?: "Network error"}")
            _lastSyncResult.value = failure
            failure
        } finally {
            _isSyncing.value = false
        }
    }
}
