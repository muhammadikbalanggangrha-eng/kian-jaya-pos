package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val skuBarcode: String = "",
    val category: String,
    val buyPrice: Long, // Harga Beli / Modal
    val sellPrice: Long, // Harga Jual
    val stock: Int,
    val unit: String = "pcs",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean get() = stock in 1..5
    val isOutOfStock: Boolean get() = stock <= 0
    val marginProfit: Long get() = sellPrice - buyPrice
}

object ProductCategories {
    val ALL = "Semua"
    val defaultCategories = listOf(
        "Semua",
        "Makanan",
        "Minuman",
        "Snack",
        "Sembako",
        "Kebutuhan Rumah",
        "Lainnya"
    )
}
