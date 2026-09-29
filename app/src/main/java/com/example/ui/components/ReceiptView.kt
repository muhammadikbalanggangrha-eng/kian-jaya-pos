package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CurrencyFormatter
import com.example.data.model.OrderWithItems
import com.example.data.model.PaymentMethods
import com.example.data.model.StoreInfo
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosNavy
import com.example.ui.theme.PosTealPrimary
import com.example.ui.util.ReceiptExporter

@Composable
fun ReceiptDialog(
    orderWithItems: OrderWithItems,
    storeInfo: StoreInfo?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val order = orderWithItems.order
    val items = orderWithItems.items
    val receiptText = ReceiptExporter.generateReceiptText(orderWithItems, storeInfo)
    var showThermalPrintDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sukses",
                            tint = PosEmerald,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Transaksi Berhasil!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PosEmerald
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Realistic Thermal Paper View
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFAFAFA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Store details
                        Text(
                            text = storeInfo?.storeName ?: "TOKO BERKAH SEJAHTERA",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = PosNavy
                        )
                        Text(
                            text = storeInfo?.address ?: "Jl. Merdeka No. 45, Jakarta",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Gray
                        )
                        Text(
                            text = "Telp: ${storeInfo?.phone ?: "0812-3456-7890"}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        ReceiptDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Transaction Info
                        ReceiptRow("No. Nota", order.orderNumber)
                        ReceiptRow("Waktu", CurrencyFormatter.formatDate(order.timestamp))
                        ReceiptRow("Kasir", order.cashierName)
                        ReceiptRow("Pelanggan", order.customerName)

                        Spacer(modifier = Modifier.height(8.dp))
                        ReceiptDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items list
                        for (item in items) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PosNavy
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x @${CurrencyFormatter.formatRupiah(item.sellPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(item.subtotal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = PosNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        ReceiptDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Totals
                        ReceiptRow("Subtotal", CurrencyFormatter.formatRupiah(order.subtotalAmount))
                        if (order.discountAmount > 0) {
                            ReceiptRow(
                                "Diskon",
                                "-${CurrencyFormatter.formatRupiah(order.discountAmount)}",
                                isHighlight = true
                            )
                        }
                        ReceiptRow(
                            "TOTAL",
                            CurrencyFormatter.formatRupiah(order.totalAmount),
                            isBold = true,
                            fontSize = 16
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        ReceiptRow("Metode Bayar", order.paymentMethod)
                        if (order.paymentMethod == PaymentMethods.CASH) {
                            ReceiptRow("Uang Diterima", CurrencyFormatter.formatRupiah(order.cashReceived))
                            ReceiptRow("Kembalian", CurrencyFormatter.formatRupiah(order.changeGiven), isBold = true)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        ReceiptDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = storeInfo?.receiptFooter ?: "Terima kasih atas kunjungan Anda!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Direct Thermal Print Button (Bluetooth / USB / Android Print)
                    Button(
                        onClick = { showThermalPrintDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = "Cetak Struk Termal (Bluetooth / USB)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // 2. Share & Dismiss Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ReceiptExporter.shareReceiptAsText(context, receiptText, order.orderNumber)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Bagikan Teks")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PosNavy)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Selesai")
                        }
                    }
                }
            }
        }
    }

    if (showThermalPrintDialog) {
        ThermalPrintDialog(
            orderWithItems = orderWithItems,
            storeInfo = storeInfo,
            onDismiss = { showThermalPrintDialog = false }
        )
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isHighlight: Boolean = false,
    fontSize: Int = 13
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            ),
            color = if (isHighlight) PosTealPrimary else Color(0xFF475569)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            ),
            color = if (isHighlight) PosTealPrimary else PosNavy
        )
    }
}

@Composable
private fun ReceiptDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        ),
        color = Color(0xFFCBD5E1),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
