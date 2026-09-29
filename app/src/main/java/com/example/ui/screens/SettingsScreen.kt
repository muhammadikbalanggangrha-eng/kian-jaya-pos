package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ThermalPrintDialog
import com.example.ui.theme.PosNavy
import com.example.ui.theme.PosSlateLight
import com.example.ui.theme.PosTealPrimary
import com.example.ui.viewmodel.PosViewModel

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storeInfo by viewModel.storeInfo.collectAsState()

    var storeName by remember(storeInfo) { mutableStateOf(storeInfo?.storeName ?: "KIAN JAYA POS") }
    var address by remember(storeInfo) { mutableStateOf(storeInfo?.address ?: "Jl. Merdeka No. 45, Jakarta") }
    var phone by remember(storeInfo) { mutableStateOf(storeInfo?.phone ?: "0812-3456-7890") }
    var footer by remember(storeInfo) {
        mutableStateOf(storeInfo?.receiptFooter ?: "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar.")
    }
    var showThermalPrintDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.POS) },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke POS")
                }
                Column {
                    Text(
                        text = "Pengaturan Toko & Struk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PosNavy
                    )
                    Text(
                        text = "Informasi Profil Usaha & Format Nota Kasir",
                        style = MaterialTheme.typography.bodySmall,
                        color = PosSlateLight
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = PosTealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Profil Usaha / Toko",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PosNavy
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_store_name_input")
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat Toko") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor Telepon / WhatsApp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = footer,
                        onValueChange = { footer = it },
                        label = { Text("Catatan Kaki Struk (Footer)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            viewModel.updateStoreInfo(storeName, address, phone, footer)
                            Toast.makeText(context, "Profil toko berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Perubahan Profil", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Thermal Printer Integration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = PosTealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Printer Termal & Struk Kasir",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PosNavy
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Text(
                        text = "Aplikasi mendukung printer termal Bluetooth (SPP) dan USB OTG (ESC/POS) untuk ukuran kertas 58mm & 80mm. Anda dapat melakukan tes cetak dan memilih printer aktif di sini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PosSlateLight,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { showThermalPrintDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hubungkan & Tes Cetak Printer", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showThermalPrintDialog) {
                ThermalPrintDialog(
                    orderWithItems = null,
                    storeInfo = storeInfo,
                    onDismiss = { showThermalPrintDialog = false }
                )
            }

            // System info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PosSlateLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Tentang Aplikasi KIAN JAYA POS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PosNavy
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Text(
                        text = "• Versi: 1.0 (Produksi Siap Deploy)\n" +
                                "• Arsitektur: Jetpack Compose M3 + Room SQLite Local DB\n" +
                                "• Hak Akses: Multi-user RBAC (Admin & Kasir)\n" +
                                "• Ekspor Data: Format CSV / Excel terstandarisasi\n" +
                                "• Cetak Struk: Standar format printer thermal 58/80mm",
                        style = MaterialTheme.typography.bodySmall,
                        color = PosSlateLight,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
