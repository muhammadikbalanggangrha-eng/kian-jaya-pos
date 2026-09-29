package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.OrderWithItems
import com.example.data.model.PaymentMethods
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosNavy
import com.example.ui.theme.PosSlateLight
import com.example.ui.theme.PosTealContainer
import com.example.ui.theme.PosTealPrimary
import com.example.ui.viewmodel.DateFilterPeriod
import com.example.ui.viewmodel.PosViewModel

@Composable
fun OrdersScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val storeInfo by viewModel.storeInfo.collectAsState()
    val orders by viewModel.visibleOrders.collectAsState()
    val selectedDateFilter by viewModel.orderDateFilter.collectAsState()
    val selectedCashierFilterId by viewModel.orderCashierFilterId.collectAsState()
    val allCashiers by viewModel.allCashiers.collectAsState()

    var selectedOrderForReceipt by remember { mutableStateOf<OrderWithItems?>(null) }

    val totalRevenue = orders.sumOf { it.order.totalAmount }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (currentUser?.isAdmin == true) "Riwayat Semua Transaksi" else "Riwayat Transaksi Saya",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PosNavy
                            )
                            Text(
                                text = "${orders.size} transaksi • Total: ${CurrencyFormatter.formatRupiah(totalRevenue)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PosTealPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateFilterPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedDateFilter == period,
                                onClick = { viewModel.setOrderDateFilter(period) },
                                label = { Text(period.label) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosTealContainer,
                                    selectedLabelColor = PosTealPrimary
                                )
                            )
                        }
                    }

                    // Admin only: Filter by Cashier
                    if (currentUser?.isAdmin == true && allCashiers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kasir:", style = MaterialTheme.typography.labelSmall, color = PosSlateLight)
                            FilterChip(
                                selected = selectedCashierFilterId == null,
                                onClick = { viewModel.setOrderCashierFilterId(null) },
                                label = { Text("Semua Kasir") },
                                shape = RoundedCornerShape(16.dp)
                            )
                            allCashiers.forEach { cashier ->
                                FilterChip(
                                    selected = selectedCashierFilterId == cashier.id,
                                    onClick = { viewModel.setOrderCashierFilterId(cashier.id) },
                                    label = { Text(cashier.name) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Orders List
            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = PosSlateLight,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada transaksi pada periode ini",
                            color = PosSlateLight
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(orders, key = { it.order.id }) { orderWithItems ->
                        OrderCard(
                            orderWithItems = orderWithItems,
                            onClick = { selectedOrderForReceipt = orderWithItems }
                        )
                    }
                }
            }
        }

        // Receipt Modal Dialog
        selectedOrderForReceipt?.let { orderWithItems ->
            ReceiptDialog(
                orderWithItems = orderWithItems,
                storeInfo = storeInfo,
                onDismiss = { selectedOrderForReceipt = null }
            )
        }
    }
}

@Composable
fun OrderCard(
    orderWithItems: OrderWithItems,
    onClick: () -> Unit
) {
    val order = orderWithItems.order
    val items = orderWithItems.items

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("order_card_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PosNavy
                    )
                    Text(
                        text = CurrencyFormatter.formatDate(order.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = PosSlateLight
                    )
                }

                // Payment method badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (order.paymentMethod == PaymentMethods.CASH) Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                ) {
                    Text(
                        text = order.paymentMethod,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (order.paymentMethod == PaymentMethods.CASH) Color(0xFF2563EB) else PosEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Items Preview
            val itemsPreview = items.joinToString(", ") { "${it.productName} (${it.quantity}x)" }
            Text(
                text = itemsPreview,
                style = MaterialTheme.typography.bodySmall,
                color = PosSlateLight,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PosSlateLight
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${order.cashierName} • ${order.customerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PosSlateLight
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PosTealPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PosSlateLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
