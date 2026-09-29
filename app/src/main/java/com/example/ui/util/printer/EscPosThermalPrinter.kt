package com.example.ui.util.printer

import com.example.data.model.CurrencyFormatter
import com.example.data.model.OrderWithItems
import com.example.data.model.PaymentMethods
import com.example.data.model.StoreInfo
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

enum class PaperWidth(val charsPerLine: Int, val label: String, val paperWidthMm: Int) {
    WIDTH_58MM(32, "58mm (Kecil - 32 Karakter)", 58),
    WIDTH_80MM(48, "80mm (Besar - 48 Karakter)", 80)
}

object EscPosThermalPrinter {

    private val CHARSET_ASCII = Charset.forName("US-ASCII")
    private val CHARSET_CP850 = Charset.forName("CP850")

    // ESC/POS Command Constants
    val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
    val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
    val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
    val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2
    val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
    val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0
    val CMD_DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x11) // GS ! 0x11
    val CMD_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00) // GS ! 0x00
    val CMD_UNDERLINE_ON = byteArrayOf(0x1B, 0x2D, 0x01) // ESC - 1
    val CMD_UNDERLINE_OFF = byteArrayOf(0x1B, 0x2D, 0x00) // ESC - 0
    val CMD_FEED_3_LINES = byteArrayOf(0x1B, 0x64, 0x03) // ESC d 3
    val CMD_FEED_5_LINES = byteArrayOf(0x1B, 0x64, 0x05) // ESC d 5
    val CMD_PAPER_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 'B' 0
    val CMD_OPEN_DRAWER = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()) // ESC p 0 25 250

    /**
     * Builds ESC/POS thermal receipt byte array for a completed order.
     */
    fun buildReceiptBytes(
        orderWithItems: OrderWithItems,
        storeInfo: StoreInfo?,
        paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        val width = paperWidth.charsPerLine
        val order = orderWithItems.order
        val items = orderWithItems.items
        val storeName = storeInfo?.storeName ?: "KIAN JAYA POS"
        val address = storeInfo?.address ?: "Jl. Merdeka No. 45, Jakarta"
        val phone = storeInfo?.phone ?: "0812-3456-7890"
        val footer = storeInfo?.receiptFooter ?: "Terima kasih atas kunjungan Anda!"

        // 1. Initialize Printer
        stream.write(CMD_INIT)

        // 2. Store Header (Centered, Double size & Bold)
        stream.write(CMD_ALIGN_CENTER)
        stream.write(CMD_DOUBLE_SIZE_ON)
        stream.write(CMD_BOLD_ON)
        writeText(stream, "$storeName\n")
        stream.write(CMD_NORMAL_SIZE)
        stream.write(CMD_BOLD_OFF)

        // Address & Phone
        if (address.isNotBlank()) {
            writeText(stream, "$address\n")
        }
        if (phone.isNotBlank()) {
            writeText(stream, "Telp: $phone\n")
        }

        // Divider
        writeText(stream, "=".repeat(width) + "\n")

        // 3. Order Metadata (Left aligned)
        stream.write(CMD_ALIGN_LEFT)
        writeText(stream, formatTwoColumns("No. Transaksi", order.orderNumber, width) + "\n")
        writeText(stream, formatTwoColumns("Tanggal/Waktu", CurrencyFormatter.formatDate(order.timestamp), width) + "\n")
        writeText(stream, formatTwoColumns("Kasir", order.cashierName, width) + "\n")
        writeText(stream, formatTwoColumns("Pelanggan", order.customerName, width) + "\n")
        writeText(stream, "-".repeat(width) + "\n")

        // 4. Purchased Items
        for (item in items) {
            // Product Name line
            stream.write(CMD_BOLD_ON)
            writeText(stream, "${item.productName}\n")
            stream.write(CMD_BOLD_OFF)

            // Qty x Price and Subtotal line
            val qtyPrice = "  ${item.quantity} x ${CurrencyFormatter.formatRupiah(item.sellPrice)}"
            val subtotal = CurrencyFormatter.formatRupiah(item.subtotal)
            writeText(stream, formatTwoColumns(qtyPrice, subtotal, width) + "\n")
        }

        writeText(stream, "-".repeat(width) + "\n")

        // 5. Total & Payment Breakdown
        writeText(stream, formatTwoColumns("Subtotal", CurrencyFormatter.formatRupiah(order.subtotalAmount), width) + "\n")
        if (order.discountAmount > 0) {
            writeText(stream, formatTwoColumns("Diskon", "-${CurrencyFormatter.formatRupiah(order.discountAmount)}", width) + "\n")
        }

        // Grand Total (Bold)
        stream.write(CMD_BOLD_ON)
        writeText(stream, formatTwoColumns("TOTAL", CurrencyFormatter.formatRupiah(order.totalAmount), width) + "\n")
        stream.write(CMD_BOLD_OFF)

        writeText(stream, formatTwoColumns("Metode Bayar", order.paymentMethod, width) + "\n")
        if (order.paymentMethod == PaymentMethods.CASH) {
            writeText(stream, formatTwoColumns("Tunai Diterima", CurrencyFormatter.formatRupiah(order.cashReceived), width) + "\n")
            stream.write(CMD_BOLD_ON)
            writeText(stream, formatTwoColumns("Kembalian", CurrencyFormatter.formatRupiah(order.changeGiven), width) + "\n")
            stream.write(CMD_BOLD_OFF)
        }

        writeText(stream, "=".repeat(width) + "\n")

        // 6. Footer (Centered)
        stream.write(CMD_ALIGN_CENTER)
        val footerLines = footer.split("\n")
        for (line in footerLines) {
            writeText(stream, "$line\n")
        }
        writeText(stream, "Simpan struk ini sbg bukti bayar\n")

        // 7. Feed lines & Cut Paper
        stream.write(CMD_FEED_5_LINES)
        stream.write(CMD_PAPER_CUT)

        return stream.toByteArray()
    }

    /**
     * Builds ESC/POS thermal test print byte array.
     */
    fun buildTestPrintBytes(
        storeName: String = "KIAN JAYA POS",
        paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        val width = paperWidth.charsPerLine

        stream.write(CMD_INIT)
        stream.write(CMD_ALIGN_CENTER)
        stream.write(CMD_DOUBLE_SIZE_ON)
        stream.write(CMD_BOLD_ON)
        writeText(stream, "$storeName\n")
        stream.write(CMD_NORMAL_SIZE)
        stream.write(CMD_BOLD_OFF)

        writeText(stream, "TES CETAK PRINTER TERMAL\n")
        writeText(stream, "=".repeat(width) + "\n")

        stream.write(CMD_ALIGN_LEFT)
        writeText(stream, formatTwoColumns("Format Kertas", paperWidth.label, width) + "\n")
        writeText(stream, formatTwoColumns("Kolom Karakter", "$width Karakter/Baris", width) + "\n")
        writeText(stream, formatTwoColumns("Status Koneksi", "BERHASIL TERHUBUNG", width) + "\n")
        writeText(stream, "-".repeat(width) + "\n")

        stream.write(CMD_BOLD_ON)
        writeText(stream, centerText("TEKS TEBAL (BOLD) NORMAL", width) + "\n")
        stream.write(CMD_BOLD_OFF)

        writeText(stream, centerText("1234567890!@#$%^&*()_+-=", width) + "\n")
        writeText(stream, "=".repeat(width) + "\n")

        stream.write(CMD_ALIGN_CENTER)
        writeText(stream, "Printer Siap Digunakan untuk Kasir\n")

        stream.write(CMD_FEED_5_LINES)
        stream.write(CMD_PAPER_CUT)

        return stream.toByteArray()
    }

    private fun writeText(stream: ByteArrayOutputStream, text: String) {
        val bytes = text.toByteArray(CHARSET_CP850)
        stream.write(bytes)
    }

    fun formatTwoColumns(left: String, right: String, totalWidth: Int): String {
        val available = totalWidth - left.length - right.length
        return if (available > 0) {
            left + " ".repeat(available) + right
        } else {
            // If text exceeds width, truncate left text gracefully
            val truncatedLeft = if (left.length > (totalWidth - right.length - 2)) {
                left.substring(0, (totalWidth - right.length - 2).coerceAtLeast(1)) + " "
            } else left
            val spaces = (totalWidth - truncatedLeft.length - right.length).coerceAtLeast(1)
            truncatedLeft + " ".repeat(spaces) + right
        }
    }

    fun centerText(text: String, totalWidth: Int): String {
        val trimmed = text.trim()
        if (trimmed.length >= totalWidth) return trimmed
        val pad = (totalWidth - trimmed.length) / 2
        return " ".repeat(pad) + trimmed
    }
}
