package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.ui.theme.PosSlateLight
import com.example.ui.theme.PosTealLight
import com.example.ui.theme.PosTealPrimary
import com.example.ui.viewmodel.DailySales

@Composable
fun SalesBarChart(
    dailySales: List<DailySales>,
    modifier: Modifier = Modifier
) {
    if (dailySales.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada data penjualan pada periode ini",
                style = MaterialTheme.typography.bodyMedium,
                color = PosSlateLight
            )
        }
        return
    }

    val maxSales = (dailySales.maxOfOrNull { it.totalSales } ?: 1L).coerceAtLeast(10000L)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tren Pendapatan Harian",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Max: ${CurrencyFormatter.formatRupiah(maxSales)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PosSlateLight
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas for Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bottomLabelHeight = 40f
                val chartHeight = canvasHeight - bottomLabelHeight

                val barCount = dailySales.size
                val barSpacing = canvasWidth / (barCount * 3f)
                val totalSpacing = barSpacing * (barCount + 1)
                val barWidth = (canvasWidth - totalSpacing) / barCount

                // Draw horizontal subtle grid lines (3 lines)
                val gridColor = Color(0xFFE2E8F0)
                for (i in 1..3) {
                    val y = chartHeight * (i / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                }

                // Draw Bars
                dailySales.forEachIndexed { index, sales ->
                    val x = barSpacing + index * (barWidth + barSpacing)
                    val barFraction = (sales.totalSales.toFloat() / maxSales.toFloat()).coerceIn(0.04f, 1.0f)
                    val barHeight = chartHeight * barFraction
                    val y = chartHeight - barHeight

                    val isTopDay = sales.totalSales == maxSales && sales.totalSales > 0

                    val gradient = Brush.verticalGradient(
                        colors = if (isTopDay) {
                            listOf(Color(0xFF0D9488), Color(0xFF14B8A6))
                        } else {
                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                        }
                    )

                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Text for date label using native canvas
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        sales.dayLabel,
                        x + (barWidth / 2f),
                        canvasHeight - 8f,
                        paint
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend / summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val totalOmset = dailySales.sumOf { it.totalSales }
                val totalTrx = dailySales.sumOf { it.orderCount }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total Periode",
                        style = MaterialTheme.typography.bodySmall,
                        color = PosSlateLight
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(totalOmset),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PosTealPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Jumlah Transaksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = PosSlateLight
                    )
                    Text(
                        text = "$totalTrx Transaksi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
