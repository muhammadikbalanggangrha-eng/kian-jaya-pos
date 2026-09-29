package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CashierManagementScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosNavy
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSlateLight
import com.example.ui.theme.PosTealContainer
import com.example.ui.theme.PosTealPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: PosViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val storeInfo by viewModel.storeInfo.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAdminMenuSheet by remember { mutableStateOf(false) }

    // System Back Button Handling
    BackHandler(enabled = currentUser != null) {
        if (currentScreen != AppScreen.POS) {
            viewModel.navigateTo(AppScreen.POS)
        } else {
            showLogoutDialog = true
        }
    }

    if (currentUser == null || currentScreen == AppScreen.LOGIN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            LoginScreen(viewModel = viewModel)
        }
    } else {
        val user = currentUser!!
        val isAdmin = user.isAdmin

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_navigation_bar")
                ) {
                    // 1. POS Kasir
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.POS,
                        onClick = { viewModel.navigateTo(AppScreen.POS) },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Kasir") },
                        label = { Text("Kasir", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PosTealPrimary,
                            selectedTextColor = PosTealPrimary,
                            indicatorColor = PosTealContainer
                        ),
                        modifier = Modifier.testTag("nav_item_pos")
                    )

                    // 2. Produk (Admin only)
                    if (isAdmin) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.PRODUCTS,
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS) },
                            icon = { Icon(Icons.Default.Inventory2, contentDescription = "Produk") },
                            label = { Text("Produk", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PosTealPrimary,
                                selectedTextColor = PosTealPrimary,
                                indicatorColor = PosTealContainer
                            ),
                            modifier = Modifier.testTag("nav_item_products")
                        )
                    }

                    // 3. Riwayat Transaksi (Both Kasir and Admin)
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ORDERS,
                        onClick = { viewModel.navigateTo(AppScreen.ORDERS) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Riwayat") },
                        label = {
                            Text(
                                if (isAdmin) "Transaksi" else "Riwayat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PosTealPrimary,
                            selectedTextColor = PosTealPrimary,
                            indicatorColor = PosTealContainer
                        ),
                        modifier = Modifier.testTag("nav_item_orders")
                    )

                    // 4. Laporan & Keuangan (Admin only)
                    if (isAdmin) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.REPORTS,
                            onClick = { viewModel.navigateTo(AppScreen.REPORTS) },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = "Laporan") },
                            label = { Text("Laporan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PosTealPrimary,
                                selectedTextColor = PosTealPrimary,
                                indicatorColor = PosTealContainer
                            ),
                            modifier = Modifier.testTag("nav_item_reports")
                        )

                        // 5. Menu Lainnya (Admin only - opens clean sheet for Cashiers, Store Profile, and Logout)
                        val isSecondaryScreen = currentScreen == AppScreen.CASHIERS || currentScreen == AppScreen.SETTINGS
                        NavigationBarItem(
                            selected = isSecondaryScreen,
                            onClick = { showAdminMenuSheet = true },
                            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Menu") },
                            label = { Text("Menu", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PosTealPrimary,
                                selectedTextColor = PosTealPrimary,
                                indicatorColor = PosTealContainer
                            ),
                            modifier = Modifier.testTag("nav_item_more_menu")
                        )
                    } else {
                        // Cashier Logout item
                        NavigationBarItem(
                            selected = false,
                            onClick = { showLogoutDialog = true },
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Keluar", tint = PosRose) },
                            label = { Text("Keluar", fontSize = 11.sp, color = PosRose) },
                            modifier = Modifier.testTag("nav_item_logout")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        AppScreen.POS -> PosScreen(viewModel = viewModel)
                        AppScreen.PRODUCTS -> {
                            if (isAdmin) ProductsScreen(viewModel = viewModel)
                            else PosScreen(viewModel = viewModel)
                        }
                        AppScreen.ORDERS -> OrdersScreen(viewModel = viewModel)
                        AppScreen.REPORTS -> {
                            if (isAdmin) ReportsScreen(viewModel = viewModel)
                            else PosScreen(viewModel = viewModel)
                        }
                        AppScreen.CASHIERS -> {
                            if (isAdmin) CashierManagementScreen(viewModel = viewModel)
                            else PosScreen(viewModel = viewModel)
                        }
                        AppScreen.SETTINGS -> {
                            if (isAdmin) SettingsScreen(viewModel = viewModel)
                            else PosScreen(viewModel = viewModel)
                        }
                        AppScreen.LOGIN -> LoginScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Admin Menu Modal Bottom Sheet
    if (showAdminMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdminMenuSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Menu Administrasi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PosNavy
                )
                Text(
                    text = "${storeInfo?.storeName ?: "KIAN JAYA POS"} • Administrator",
                    style = MaterialTheme.typography.bodySmall,
                    color = PosSlateLight
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Item 1: Kelola Kasir
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAdminMenuSheet = false
                            viewModel.navigateTo(AppScreen.CASHIERS)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFCCFBF1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = null,
                                    tint = PosTealPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Kelola Akun Kasir", fontWeight = FontWeight.Bold, color = PosNavy)
                                Text("Tambah, ubah PIN, atau hapus akses kasir", style = MaterialTheme.typography.bodySmall, color = PosSlateLight)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PosSlateLight)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Item 2: Pengaturan Toko & Nota
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAdminMenuSheet = false
                            viewModel.navigateTo(AppScreen.SETTINGS)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Pengaturan Profil Toko & Struk", fontWeight = FontWeight.Bold, color = PosNavy)
                                Text("Nama usaha, alamat, telp, footer nota", style = MaterialTheme.typography.bodySmall, color = PosSlateLight)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PosSlateLight)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                // Item 3: Keluar / Logout
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAdminMenuSheet = false
                            showLogoutDialog = true
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE4E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = PosRose,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Keluar dari Akun", fontWeight = FontWeight.Bold, color = PosRose)
                            Text("Akhiri sesi kerja saat ini", style = MaterialTheme.typography.bodySmall, color = PosSlateLight)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Keluar dari Akun?", fontWeight = FontWeight.Bold) },
            text = { Text("Anda akan keluar dari sesi ${currentUser?.name ?: ""}. Anda dapat login kembali kapan saja.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosRose)
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
