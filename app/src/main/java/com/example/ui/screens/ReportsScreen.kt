package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.ui.components.SalesBarChart
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosNavy
import com.example.ui.theme.PosSlateLight
import com.example.ui.theme.PosTealContainer
import com.example.ui.theme.PosTealPrimary
import com.example.ui.viewmodel.DateFilterPeriod
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.TopProductStat

@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orders by viewModel.reportOrders.collectAsState()
    val dateFilter by viewModel.reportDateFilter.collectAsState()
    val dailySales by viewModel.dailySalesData.collectAsState()
    val topProducts by viewModel.topSellingProducts.collectAsState()

    val totalRevenue = orders.sumOf { it.order.totalAmount }
    val totalCost = orders.sumOf { it.order.totalBuyPrice }
    val totalProfit = totalRevenue - totalCost
    val totalTransactions = orders.size
    val totalItemsSold = orders.sumOf { it.items.sumOf { item -> item.quantity } }

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
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Laporan & Analitik Penjualan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PosNavy
                        )
                        Text(
                            text = "Dashboard Keuangan Toko",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosSlateLight
                        )
                    }

                    // Export CSV Button
                    Button(
                        onClick = {
                            if (orders.isEmpty()) {
                                Toast.makeText(context, "Tidak ada data transaksi untuk diekspor", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.exportReportCsv(context)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary),
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor CSV")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date Period Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateFilterPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = dateFilter == period,
                            onClick = { viewModel.setReportDateFilter(period) },
                            label = { Text(period.label) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosTealContainer,
                                selectedLabelColor = PosTealPrimary
                            )
                        )
                    }
                }
            }
        }

        // Scrollable Report Dashboard
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Summary Grid (4 cards in 2 rows)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Total Pendapatan",
                        value = CurrencyFormatter.formatRupiah(totalRevenue),
                        icon = Icons.Default.MonetizationOn,
                        iconColor = PosTealPrimary,
                        backgroundColor = Color(0xFFF0FDFA),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Keuntungan Bersih (Laba)",
                        value = CurrencyFormatter.formatRupiah(totalProfit),
                        icon = Icons.Default.Savings,
                        iconColor = PosEmerald,
                        backgroundColor = Color(0xFFF0FDF4),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Jumlah Transaksi",
                        value = "$totalTransactions Nota",
                        icon = Icons.Default.Receipt,
                        iconColor = Color(0xFF3B82F6),
                        backgroundColor = Color(0xFFEFF6FF),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Produk Terjual",
                        value = "$totalItemsSold Unit",
                        icon = Icons.Default.ShoppingBag,
                        iconColor = PosAmber,
                        backgroundColor = Color(0xFFFFFBEB),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sales Trend Bar Chart
            item {
                SalesBarChart(dailySales = dailySales)
            }

            // Top Selling Products Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = PosAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Ringkasan Barang Paling Laku (Top Products)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PosNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (topProducts.isEmpty()) {
                            Text(
                                text = "Belum ada produk terjual pada periode ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PosSlateLight,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            topProducts.forEachIndexed { index, productStat ->
                                TopProductItemRow(rank = index + 1, stat = productStat)
                                if (index < topProducts.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = PosSlateLight
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PosNavy
            )
        }
    }
}

@Composable
fun TopProductItemRow(
    rank: Int,
    stat: TopProductStat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Rank Badge
            val rankBgColor = when (rank) {
                1 -> Color(0xFFFEF3C7) // Gold
                2 -> Color(0xFFF1F5F9) // Silver
                3 -> Color(0xFFFFEDD5) // Bronze
                else -> Color(0xFFF8FAFC)
            }
            val rankTextColor = when (rank) {
                1 -> Color(0xFFB45309)
                2 -> Color(0xFF475569)
                3 -> Color(0xFFC2410C)
                else -> PosSlateLight
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(rankBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = rankTextColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = stat.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PosNavy
                )
                Text(
                    text = stat.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = PosSlateLight
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${stat.totalQuantitySold} Terjual",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = PosTealPrimary
            )
            Text(
                text = CurrencyFormatter.formatRupiah(stat.totalRevenue),
                style = MaterialTheme.typography.labelSmall,
                color = PosSlateLight
            )
        }
    }
}
