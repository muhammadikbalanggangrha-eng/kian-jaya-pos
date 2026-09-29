package com.example.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OrderWithItems
import com.example.data.model.StoreInfo
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosNavy
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSlateLight
import com.example.ui.theme.PosTealPrimary
import com.example.ui.util.printer.BluetoothThermalPrinter
import com.example.ui.util.printer.ConnectedUsbPrinter
import com.example.ui.util.printer.EscPosThermalPrinter
import com.example.ui.util.printer.PairedBluetoothPrinter
import com.example.ui.util.printer.PaperWidth
import com.example.ui.util.printer.PrinterResult
import com.example.ui.util.printer.ThermalPrintDocumentAdapter
import com.example.ui.util.printer.UsbThermalPrinter
import kotlinx.coroutines.launch

@Composable
fun ThermalPrintDialog(
    orderWithItems: OrderWithItems? = null,
    storeInfo: StoreInfo?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bluetooth, 1: USB, 2: System Print
    var selectedPaperWidth by remember { mutableStateOf(PaperWidth.WIDTH_58MM) }

    // Bluetooth states
    var pairedPrinters by remember { mutableStateOf<List<PairedBluetoothPrinter>>(emptyList()) }
    var selectedBluetoothAddress by remember { mutableStateOf<String?>(null) }
    var hasBtPermission by remember { mutableStateOf(BluetoothThermalPrinter.hasBluetoothPermission(context)) }

    // USB states
    var connectedUsbPrinters by remember { mutableStateOf<List<ConnectedUsbPrinter>>(emptyList()) }
    var selectedUsbDevice by remember { mutableStateOf<ConnectedUsbPrinter?>(null) }

    // Operation status
    var isPrinting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(true) }

    // Permission launcher for Android 12+ (BLUETOOTH_CONNECT)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBtPermission = granted
        if (granted) {
            pairedPrinters = BluetoothThermalPrinter.getPairedPrinters(context)
            if (pairedPrinters.isNotEmpty() && selectedBluetoothAddress == null) {
                selectedBluetoothAddress = pairedPrinters.first().address
            }
        } else {
            Toast.makeText(context, "Izin Bluetooth diperlukan untuk menghubungkan printer", Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshBluetoothDevices() {
        if (hasBtPermission) {
            pairedPrinters = BluetoothThermalPrinter.getPairedPrinters(context)
            if (pairedPrinters.isNotEmpty() && selectedBluetoothAddress == null) {
                selectedBluetoothAddress = pairedPrinters.first().address
            }
        }
    }

    fun refreshUsbDevices() {
        connectedUsbPrinters = UsbThermalPrinter.getConnectedPrinters(context)
        if (connectedUsbPrinters.isNotEmpty() && selectedUsbDevice == null) {
            selectedUsbDevice = connectedUsbPrinters.first()
        }
    }

    LaunchedEffect(Unit) {
        refreshBluetoothDevices()
        refreshUsbDevices()
    }

    fun executePrint(isTest: Boolean) {
        isPrinting = true
        statusMessage = null

        coroutineScope.launch {
            val bytes = if (isTest) {
                EscPosThermalPrinter.buildTestPrintBytes(
                    storeName = storeInfo?.storeName ?: "KIAN JAYA POS",
                    paperWidth = selectedPaperWidth
                )
            } else {
                if (orderWithItems == null) {
                    isPrinting = false
                    statusMessage = "Data transaksi tidak tersedia."
                    isSuccess = false
                    return@launch
                }
                EscPosThermalPrinter.buildReceiptBytes(
                    orderWithItems = orderWithItems,
                    storeInfo = storeInfo,
                    paperWidth = selectedPaperWidth
                )
            }

            when (selectedTab) {
                0 -> { // Bluetooth
                    val address = selectedBluetoothAddress
                    if (address == null) {
                        isPrinting = false
                        statusMessage = "Pilih printer Bluetooth terlebih dahulu."
                        isSuccess = false
                        return@launch
                    }

                    val result = BluetoothThermalPrinter.printBytes(context, address, bytes)
                    isPrinting = false
                    when (result) {
                        is PrinterResult.Success -> {
                            statusMessage = result.message
                            isSuccess = true
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                        is PrinterResult.Error -> {
                            statusMessage = result.errorMessage
                            isSuccess = false
                        }
                        PrinterResult.PermissionRequired -> {
                            statusMessage = "Izin Bluetooth diperlukan."
                            isSuccess = false
                        }
                        PrinterResult.BluetoothDisabled -> {
                            statusMessage = "Bluetooth ponsel belum aktif. Aktifkan Bluetooth terlebih dahulu."
                            isSuccess = false
                        }
                    }
                }
                1 -> { // USB
                    val usbPrinter = selectedUsbDevice
                    if (usbPrinter == null) {
                        isPrinting = false
                        statusMessage = "Pilih printer USB yang terhubung."
                        isSuccess = false
                        return@launch
                    }

                    val result = UsbThermalPrinter.printBytes(context, usbPrinter.usbDevice, bytes)
                    isPrinting = false
                    when (result) {
                        is PrinterResult.Success -> {
                            statusMessage = result.message
                            isSuccess = true
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                        is PrinterResult.Error -> {
                            statusMessage = result.errorMessage
                            isSuccess = false
                        }
                        else -> {
                            statusMessage = "Gagal memproses cetak USB."
                            isSuccess = false
                        }
                    }
                }
                2 -> { // System Print
                    isPrinting = false
                    if (orderWithItems != null) {
                        ThermalPrintDocumentAdapter.printReceiptWithSystem(
                            context = context,
                            orderWithItems = orderWithItems,
                            storeInfo = storeInfo,
                            paperWidth = selectedPaperWidth
                        )
                        statusMessage = "Membuka Print Spooler Android..."
                        isSuccess = true
                    } else {
                        statusMessage = "Sistem print siap untuk nota pesanan."
                        isSuccess = true
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFCCFBF1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = PosTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cetak Struk Termal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PosNavy
                            )
                            Text(
                                text = "ESC/POS Bluetooth • USB OTG • Android",
                                style = MaterialTheme.typography.bodySmall,
                                color = PosSlateLight
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Paper Width Selector
                Text(
                    text = "Lebar Kertas Struk:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PosNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedPaperWidth = PaperWidth.WIDTH_58MM },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) Color(0xFFCCFBF1) else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedPaperWidth == PaperWidth.WIDTH_58MM) PosTealPrimary else Color(0xFFCBD5E1)
                        )
                    ) {
                        Text(
                            text = "58 mm (Standar)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) PosNavy else Color.DarkGray
                        )
                    }

                    OutlinedButton(
                        onClick = { selectedPaperWidth = PaperWidth.WIDTH_80MM },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) Color(0xFFCCFBF1) else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedPaperWidth == PaperWidth.WIDTH_80MM) PosTealPrimary else Color(0xFFCBD5E1)
                        )
                    ) {
                        Text(
                            text = "80 mm (Lebar)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) PosNavy else Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connection Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = PosTealPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            refreshBluetoothDevices()
                        },
                        text = { Text("Bluetooth", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            refreshUsbDevices()
                        },
                        text = { Text("USB OTG", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Sistem Cetak", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // Bluetooth Tab
                            if (!hasBtPermission) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = PosSlateLight, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Izin Bluetooth Diperlukan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PosNavy
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.BLUETOOTH)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary)
                                    ) {
                                        Text("Izinkan Akses Bluetooth", fontSize = 12.sp)
                                    }
                                }
                            } else if (pairedPrinters.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = PosSlateLight, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tidak ada printer Bluetooth tersimpan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PosNavy
                                    )
                                    Text(
                                        text = "Lakukan pairing printer termal Anda di Pengaturan Bluetooth HP, lalu tekan tombol Segarkan di bawah.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PosSlateLight,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { refreshBluetoothDevices() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Segarkan Daftar", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(pairedPrinters) { printer ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedBluetoothAddress = printer.address },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (selectedBluetoothAddress == printer.address) Color(0xFFCCFBF1) else Color(0xFFF8FAFC)
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (selectedBluetoothAddress == printer.address) PosTealPrimary else Color(0xFFE2E8F0)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    RadioButton(
                                                        selected = selectedBluetoothAddress == printer.address,
                                                        onClick = { selectedBluetoothAddress = printer.address },
                                                        colors = RadioButtonDefaults.colors(selectedColor = PosTealPrimary)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text(
                                                            text = printer.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = PosNavy,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = printer.address,
                                                            fontSize = 11.sp,
                                                            color = PosSlateLight
                                                        )
                                                    }
                                                }

                                                if (printer.isLikelyPrinter) {
                                                    Surface(
                                                        color = PosEmerald.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Printer POS",
                                                            color = PosEmerald,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // USB Tab
                            if (connectedUsbPrinters.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Usb, contentDescription = null, tint = PosSlateLight, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Printer USB belum terdeteksi",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PosNavy
                                    )
                                    Text(
                                        text = "Gunakan adapter/kabel USB OTG untuk menghubungkan printer termal ke ponsel ini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PosSlateLight,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { refreshUsbDevices() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pindai Ulang USB", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(connectedUsbPrinters) { usb ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedUsbDevice = usb },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (selectedUsbDevice?.usbDevice == usb.usbDevice) Color(0xFFCCFBF1) else Color(0xFFF8FAFC)
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (selectedUsbDevice?.usbDevice == usb.usbDevice) PosTealPrimary else Color(0xFFE2E8F0)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    RadioButton(
                                                        selected = selectedUsbDevice?.usbDevice == usb.usbDevice,
                                                        onClick = { selectedUsbDevice = usb },
                                                        colors = RadioButtonDefaults.colors(selectedColor = PosTealPrimary)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text(
                                                            text = usb.deviceName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = PosNavy
                                                        )
                                                        Text(
                                                            text = "VID: ${usb.vendorId} • PID: ${usb.productId}",
                                                            fontSize = 11.sp,
                                                            color = PosSlateLight
                                                        )
                                                    }
                                                }

                                                if (!usb.hasPermission) {
                                                    Button(
                                                        onClick = { UsbThermalPrinter.requestUsbPermission(context, usb.usbDevice) },
                                                        shape = RoundedCornerShape(6.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary)
                                                    ) {
                                                        Text("Minta Izin", fontSize = 10.sp)
                                                    }
                                                } else {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // System Print Tab
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Print, contentDescription = null, tint = PosTealPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Layanan Cetak Android (Print Spooler)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = PosNavy
                                        )
                                    }
                                    Text(
                                        text = "Mencetak langsung ke printer Wi-Fi, printer jaringan lokal, layanan plugin Mopria/Epson/Canon, atau simpan nota ke format PDF termal berukuran pas.",
                                        fontSize = 12.sp,
                                        color = PosSlateLight
                                    )
                                }
                            }
                        }
                    }
                }

                // Status Message Box (if any)
                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSuccess) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (isSuccess) PosEmerald else PosRose)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccess) PosEmerald else PosRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = if (isSuccess) PosEmerald else PosRose,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Actions: Tes Cetak & Cetak Struk
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { executePrint(isTest = true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("thermal_test_print_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isPrinting
                    ) {
                        Text("Tes Cetak", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { executePrint(isTest = false) },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("thermal_print_receipt_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary),
                        enabled = !isPrinting
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mencetak...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (orderWithItems != null) "Cetak Struk" else "Cetak Tes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
