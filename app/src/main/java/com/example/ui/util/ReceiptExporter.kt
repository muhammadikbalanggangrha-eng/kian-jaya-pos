package com.example.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.CurrencyFormatter
import com.example.data.model.OrderWithItems
import com.example.data.model.StoreInfo
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptExporter {

    fun generateReceiptText(orderWithItems: OrderWithItems, storeInfo: StoreInfo?): String {
        val storeName = storeInfo?.storeName ?: "KIAN JAYA POS"
        val address = storeInfo?.address ?: "Jl. Merdeka No. 45, Jakarta"
        val phone = storeInfo?.phone ?: "0812-3456-7890"
        val footer = storeInfo?.receiptFooter ?: "Terima kasih atas kunjungan Anda!"
        val order = orderWithItems.order
        val items = orderWithItems.items

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine(centerText(storeName, 32))
        sb.appendLine(centerText(address, 32))
        sb.appendLine(centerText("Telp: $phone", 32))
        sb.appendLine("================================")
        sb.appendLine("No     : ${order.orderNumber}")
        sb.appendLine("Waktu  : ${CurrencyFormatter.formatDate(order.timestamp)}")
        sb.appendLine("Kasir  : ${order.cashierName}")
        sb.appendLine("Plg    : ${order.customerName}")
        sb.appendLine("--------------------------------")

        for (item in items) {
            sb.appendLine(item.productName)
            val qtyPrice = "  ${item.quantity} x ${CurrencyFormatter.formatRupiah(item.sellPrice)}"
            val subtotal = CurrencyFormatter.formatRupiah(item.subtotal)
            sb.appendLine(formatTwoColumns(qtyPrice, subtotal, 32))
        }

        sb.appendLine("--------------------------------")
        sb.appendLine(formatTwoColumns("Subtotal", CurrencyFormatter.formatRupiah(order.subtotalAmount), 32))
        if (order.discountAmount > 0) {
            sb.appendLine(formatTwoColumns("Diskon", "-${CurrencyFormatter.formatRupiah(order.discountAmount)}", 32))
        }
        sb.appendLine(formatTwoColumns("TOTAL", CurrencyFormatter.formatRupiah(order.totalAmount), 32))
        sb.appendLine(formatTwoColumns("Metode Bayar", order.paymentMethod, 32))
        if (order.paymentMethod == "TUNAI") {
            sb.appendLine(formatTwoColumns("Tunai Diterima", CurrencyFormatter.formatRupiah(order.cashReceived), 32))
            sb.appendLine(formatTwoColumns("Kembalian", CurrencyFormatter.formatRupiah(order.changeGiven), 32))
        }
        sb.appendLine("================================")
        sb.appendLine(centerText(footer, 32))
        sb.appendLine("================================")

        return sb.toString()
    }

    private fun centerText(text: String, width: Int): String {
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            if (line.length >= width) line
            else {
                val padding = (width - line.length) / 2
                " ".repeat(padding) + line
            }
        }
    }

    private fun formatTwoColumns(left: String, right: String, totalWidth: Int): String {
        val spaces = totalWidth - left.length - right.length
        return if (spaces > 0) left + " ".repeat(spaces) + right else "$left $right"
    }

    fun shareReceiptAsText(context: Context, receiptText: String, orderNumber: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Struk Pembelian $orderNumber")
            putExtra(Intent.EXTRA_TEXT, receiptText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Struk"))
    }

    /**
     * Generates a CSV file containing sales report orders and triggers Android system share intent
     */
    fun exportOrdersToCsv(context: Context, orders: List<OrderWithItems>, reportPeriodName: String): Boolean {
        try {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "Laporan_Penjualan_${dateFormat.format(Date())}.csv"
            val file = File(context.cacheDir, filename)

            FileWriter(file).use { writer ->
                // CSV Header with BOM for Excel UTF-8 compatibility
                writer.write("\uFEFF")
                writer.write("No,Nomor Transaksi,Tanggal,Kasir,Pelanggan,Metode Bayar,Subtotal,Diskon,Total Belanja,Modal (HPP),Laba Kotor,Rincian Produk\n")

                orders.forEachIndexed { index, orderWithItems ->
                    val o = orderWithItems.order
                    val dateStr = CurrencyFormatter.formatDate(o.timestamp)
                    val itemsSummary = orderWithItems.items.joinToString(" | ") {
                        "${it.productName} (${it.quantity}x @${it.sellPrice})"
                    }.replace("\"", "\"\"")

                    val row = listOf(
                        (index + 1).toString(),
                        "\"${o.orderNumber}\"",
                        "\"$dateStr\"",
                        "\"${o.cashierName}\"",
                        "\"${o.customerName}\"",
                        "\"${o.paymentMethod}\"",
                        o.subtotalAmount.toString(),
                        o.discountAmount.toString(),
                        o.totalAmount.toString(),
                        o.totalBuyPrice.toString(),
                        o.totalProfit.toString(),
                        "\"$itemsSummary\""
                    ).joinToString(",")

                    writer.write(row + "\n")
                }
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan $reportPeriodName")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Ekspor Laporan Penjualan (CSV)"))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
