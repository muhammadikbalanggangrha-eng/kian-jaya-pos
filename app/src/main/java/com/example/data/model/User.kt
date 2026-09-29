package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    CASHIER
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val password: String, // PIN or password
    val name: String,
    val role: String, // "ADMIN" or "CASHIER"
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN.name
}
