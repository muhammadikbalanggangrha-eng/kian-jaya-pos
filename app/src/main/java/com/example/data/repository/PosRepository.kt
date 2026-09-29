package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.dao.OrderDao
import com.example.data.dao.ProductDao
import com.example.data.dao.StoreInfoDao
import com.example.data.dao.UserDao
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.model.Product
import com.example.data.model.StoreInfo
import com.example.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PosRepository(
    private val database: AppDatabase
) {
    private val userDao: UserDao = database.userDao()
    private val productDao: ProductDao = database.productDao()
    private val orderDao: OrderDao = database.orderDao()
    private val storeInfoDao: StoreInfoDao = database.storeInfoDao()

    // --- User & Auth ---
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allCashiers: Flow<List<User>> = userDao.getAllCashiers()

    suspend fun login(username: String, password: String): User? = withContext(Dispatchers.IO) {
        userDao.login(username.trim(), password.trim())
    }

    suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username.trim())
    }

    suspend fun insertUser(user: User): Long = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) = withContext(Dispatchers.IO) {
        userDao.deleteUser(user)
    }

    suspend fun ensureInitialData() = withContext(Dispatchers.IO) {
        if (userDao.countUsers() == 0) {
            com.example.data.populateInitialData(database)
        }
    }

    // --- Products ---
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()

    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)

    fun getProductsByCategory(category: String): Flow<List<Product>> = productDao.getProductsByCategory(category)

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun addStock(productId: Long, additionalStock: Int) = withContext(Dispatchers.IO) {
        productDao.addStock(productId, additionalStock)
    }

    // --- Orders & Checkout ---
    val allOrdersWithItems: Flow<List<OrderWithItems>> = orderDao.getAllOrdersWithItems()

    fun getOrdersByCashier(cashierId: Long): Flow<List<OrderWithItems>> =
        orderDao.getOrdersWithItemsByCashier(cashierId)

    fun getOrderById(orderId: Long): Flow<OrderWithItems?> =
        orderDao.getOrderWithItemsById(orderId)

    suspend fun generateOrderNumber(): String {
        val dateFormat = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
        val datePart = dateFormat.format(Date())
        val randomSuffix = Random.nextInt(100, 999)
        return "TRX-$datePart-$randomSuffix"
    }

    /**
     * Executes order transaction:
     * 1. Inserts the order header
     * 2. Inserts all order line items
     * 3. Automatically decrements the stock for each sold product in the inventory
     */
    suspend fun processCheckout(order: Order, items: List<OrderItem>): Long = withContext(Dispatchers.IO) {
        val orderId = orderDao.insertOrder(order)
        val itemsWithOrderId = items.map { it.copy(orderId = orderId) }
        orderDao.insertOrderItems(itemsWithOrderId)

        // Automatically reduce stock for each sold product
        for (item in itemsWithOrderId) {
            productDao.reduceStock(item.productId, item.quantity)
        }
        orderId
    }

    // --- Store Info ---
    val storeInfo: Flow<StoreInfo?> = storeInfoDao.getStoreInfo()

    suspend fun updateStoreInfo(storeInfo: StoreInfo) = withContext(Dispatchers.IO) {
        storeInfoDao.insertOrUpdateStoreInfo(storeInfo)
    }
}
