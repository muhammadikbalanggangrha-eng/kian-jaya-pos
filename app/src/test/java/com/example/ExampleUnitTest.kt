package com.example

import com.example.data.model.CurrencyFormatter
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.ui.viewmodel.CartItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyFormatter.formatRupiah(50000L)
        assert(formatted.contains("50.000"))
    }

    @Test
    fun testCartItemCalculation() {
        val product = Product(
            id = 1,
            name = "Kopi Susu",
            category = "Minuman",
            buyPrice = 8000,
            sellPrice = 15000,
            stock = 20
        )
        val cartItem = CartItem(product = product, quantity = 3)
        assertEquals(45000L, cartItem.subtotal)
        assertEquals(24000L, cartItem.totalBuyPrice)
    }

    @Test
    fun testOrderProfitCalculation() {
        val order = Order(
            orderNumber = "TRX-TEST-001",
            cashierId = 1L,
            cashierName = "Kasir 1",
            subtotalAmount = 50000L,
            discountAmount = 5000L,
            totalAmount = 45000L,
            totalBuyPrice = 25000L,
            paymentMethod = "TUNAI",
            cashReceived = 50000L,
            changeGiven = 5000L
        )
        assertEquals(20000L, order.totalProfit)
    }
}
