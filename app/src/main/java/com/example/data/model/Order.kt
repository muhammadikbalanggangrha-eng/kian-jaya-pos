package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderNumber"], unique = true),
        Index(value = ["cashierId"]),
        Index(value = ["timestamp"])
    ]
)
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val cashierId: Long,
    val cashierName: String,
    val customerName: String = "Pelanggan Umum",
    val subtotalAmount: Long,
    val discountAmount: Long = 0,
    val totalAmount: Long,
    val totalBuyPrice: Long, // Total modal HPP
    val paymentMethod: String, // "TUNAI", "QRIS", "TRANSFER", "DEBIT"
    val cashReceived: Long,
    val changeGiven: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalProfit: Long get() = totalAmount - totalBuyPrice
}

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val buyPrice: Long,
    val sellPrice: Long,
    val subtotal: Long
)

data class OrderWithItems(
    @Embedded val order: Order,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItem>
)

object PaymentMethods {
    const val CASH = "TUNAI"
    const val QRIS = "QRIS"
    const val TRANSFER = "TRANSFER"
    const val DEBIT = "DEBIT"

    val list = listOf(CASH, QRIS, TRANSFER, DEBIT)
}

object CurrencyFormatter {
    private val localeID = Locale("in", "ID")
    private val format = NumberFormat.getCurrencyInstance(localeID).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(amount: Long): String {
        return format.format(amount).replace("Rp", "Rp ")
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", localeID)
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", localeID)
        return sdf.format(Date(timestamp))
    }

    fun formatDay(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, dd MMM", localeID)
        return sdf.format(Date(timestamp))
    }
}
