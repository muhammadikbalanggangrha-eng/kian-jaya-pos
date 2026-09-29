package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_info")
data class StoreInfo(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "KIAN JAYA POS",
    val address: String = "Jl. Merdeka No. 45, Jakarta",
    val phone: String = "0812-3456-7890",
    val receiptFooter: String = "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar."
)
