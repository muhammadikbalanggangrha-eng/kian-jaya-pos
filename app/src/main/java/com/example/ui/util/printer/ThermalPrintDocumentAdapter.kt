package com.example.ui.util.printer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.model.CurrencyFormatter
import com.example.data.model.OrderWithItems
import com.example.data.model.PaymentMethods
import com.example.data.model.StoreInfo
import java.io.FileOutputStream
import java.io.IOException

class ThermalPrintDocumentAdapter(
    private val context: Context,
    private val orderWithItems: OrderWithItems,
    private val storeInfo: StoreInfo?,
    private val paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
) : PrintDocumentAdapter() {

    private var pdfDocument: PdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        metadata: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("Struk_${orderWithItems.order.orderNumber}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val order = orderWithItems.order
        val items = orderWithItems.items
        val storeName = storeInfo?.storeName ?: "KIAN JAYA POS"
        val address = storeInfo?.address ?: "Jl. Merdeka No. 45, Jakarta"
        val phone = storeInfo?.phone ?: "0812-3456-7890"
        val footer = storeInfo?.receiptFooter ?: "Terima kasih atas kunjungan Anda!"

        // 58mm = ~200 points wide, 80mm = ~280 points wide in standard 72 DPI print points
        val pageWidth = if (paperWidth == PaperWidth.WIDTH_58MM) 210 else 280
        val baseHeight = 350 + (items.size * 32)
        val pageHeight = baseHeight.coerceAtLeast(400)

        pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument!!.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background white
        canvas.drawColor(Color.WHITE)

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        val paintBold = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintHeader = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val paintCenter = Paint().apply {
            color = Color.DKGRAY
            textSize = 8f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val paintDash = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }

        val centerX = pageWidth / 2f
        val margin = 10f
        val rightX = pageWidth - margin
        var y = 24f

        // 1. Store Header
        canvas.drawText(storeName, centerX, y, paintHeader)
        y += 14f

        if (address.isNotBlank()) {
            canvas.drawText(address, centerX, y, paintCenter)
            y += 12f
        }
        if (phone.isNotBlank()) {
            canvas.drawText("Telp: $phone", centerX, y, paintCenter)
            y += 12f
        }

        // Divider
        y += 4f
        canvas.drawLine(margin, y, rightX, y, paintDash)
        y += 14f

        // 2. Order Metadata
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("No: ${order.orderNumber}", margin, y, paintText)
        y += 12f
        canvas.drawText("Tgl: ${CurrencyFormatter.formatDate(order.timestamp)}", margin, y, paintText)
        y += 12f
        canvas.drawText("Kasir: ${order.cashierName}", margin, y, paintText)
        y += 12f
        canvas.drawText("Pelanggan: ${order.customerName}", margin, y, paintText)
        y += 6f

        // Divider
        canvas.drawLine(margin, y, rightX, y, paintDash)
        y += 14f

        // 3. Purchased Items
        for (item in items) {
            paintBold.textAlign = Paint.Align.LEFT
            canvas.drawText(item.productName, margin, y, paintBold)
            y += 12f

            paintText.textAlign = Paint.Align.LEFT
            canvas.drawText(" ${item.quantity} x ${CurrencyFormatter.formatRupiah(item.sellPrice)}", margin, y, paintText)

            paintBold.textAlign = Paint.Align.RIGHT
            canvas.drawText(CurrencyFormatter.formatRupiah(item.subtotal), rightX, y, paintBold)
            y += 14f
        }

        // Divider
        canvas.drawLine(margin, y, rightX, y, paintDash)
        y += 14f

        // 4. Totals Breakdown
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("Subtotal", margin, y, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyFormatter.formatRupiah(order.subtotalAmount), rightX, y, paintText)
        y += 13f

        if (order.discountAmount > 0) {
            paintText.textAlign = Paint.Align.LEFT
            canvas.drawText("Diskon", margin, y, paintText)
            paintText.textAlign = Paint.Align.RIGHT
            canvas.drawText("-${CurrencyFormatter.formatRupiah(order.discountAmount)}", rightX, y, paintText)
            y += 13f
        }

        paintBold.textAlign = Paint.Align.LEFT
        paintBold.textSize = 11f
        canvas.drawText("TOTAL", margin, y, paintBold)
        paintBold.textAlign = Paint.Align.RIGHT
        canvas.drawText(CurrencyFormatter.formatRupiah(order.totalAmount), rightX, y, paintBold)
        paintBold.textSize = 9.5f
        y += 14f

        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("Metode: ${order.paymentMethod}", margin, y, paintText)
        y += 12f

        if (order.paymentMethod == PaymentMethods.CASH) {
            paintText.textAlign = Paint.Align.LEFT
            canvas.drawText("Tunai: ${CurrencyFormatter.formatRupiah(order.cashReceived)}", margin, y, paintText)
            y += 12f
            paintBold.textAlign = Paint.Align.LEFT
            canvas.drawText("Kembalian: ${CurrencyFormatter.formatRupiah(order.changeGiven)}", margin, y, paintBold)
            y += 14f
        }

        // Divider
        canvas.drawLine(margin, y, rightX, y, paintDash)
        y += 16f

        // 5. Footer Note
        val footerLines = footer.split("\n")
        for (fLine in footerLines) {
            canvas.drawText(fLine, centerX, y, paintCenter)
            y += 11f
        }
        canvas.drawText("Simpan struk sbg bukti pembayaran", centerX, y, paintCenter)

        pdfDocument!!.finishPage(page)

        try {
            FileOutputStream(destination.fileDescriptor).use { output ->
                pdfDocument!!.writeTo(output)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        } finally {
            pdfDocument?.close()
            pdfDocument = null
        }
    }

    companion object {
        fun printReceiptWithSystem(
            context: Context,
            orderWithItems: OrderWithItems,
            storeInfo: StoreInfo?,
            paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
        ) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val adapter = ThermalPrintDocumentAdapter(context, orderWithItems, storeInfo, paperWidth)
            val printAttributes = PrintAttributes.Builder().build()
            printManager.print("Struk_${orderWithItems.order.orderNumber}", adapter, printAttributes)
        }
    }
}
