package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CreateStoreScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ManagerDashboardScreen
import com.example.ui.screens.OrdersHistoryScreen
import com.example.ui.screens.PosRegisterScreen
import com.example.ui.screens.SyncCenterScreen
import com.example.ui.theme.OmniPOSTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniPOSTheme {
                OmniPOSApp()
            }
        }
    }
}

@Composable
fun OmniPOSApp(viewModel: PosViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentWorker by viewModel.currentWorker.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    // Back button handling for sub-screens
    BackHandler(enabled = currentScreen != AppScreen.LOGIN && currentScreen != AppScreen.CREATE_STORE) {
        when (currentScreen) {
            AppScreen.MANAGER_DASHBOARD,
            AppScreen.INVENTORY,
            AppScreen.ORDERS_HISTORY,
            AppScreen.SYNC_CENTER -> {
                if (currentWorker != null) {
                    viewModel.navigateTo(AppScreen.POS_REGISTER)
                } else {
                    viewModel.navigateTo(AppScreen.LOGIN)
                }
            }
            AppScreen.POS_REGISTER -> {
                viewModel.logout()
            }
            AppScreen.CREATE_STORE -> {
                if (storeConfig?.isConfigured == true) {
                    viewModel.navigateTo(AppScreen.LOGIN)
                }
            }
            AppScreen.LOGIN -> {
                // Exit app
            }
        }
    }

    when (currentScreen) {
        AppScreen.CREATE_STORE -> CreateStoreScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        AppScreen.LOGIN -> LoginScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        AppScreen.POS_REGISTER -> PosRegisterScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        AppScreen.MANAGER_DASHBOARD -> ManagerDashboardScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        AppScreen.INVENTORY -> InventoryScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        AppScreen.ORDERS_HISTORY -> OrdersHistoryScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        AppScreen.SYNC_CENTER -> SyncCenterScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
    }
}
