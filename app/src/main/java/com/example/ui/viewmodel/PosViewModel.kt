package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderWithItems
import com.example.data.model.PaymentMethods
import com.example.data.model.Product
import com.example.data.model.ProductCategories
import com.example.data.model.StoreInfo
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.PosRepository
import com.example.ui.util.ReceiptExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppScreen {
    LOGIN,
    POS,
    PRODUCTS,
    ORDERS,
    REPORTS,
    CASHIERS,
    SETTINGS
}

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Long get() = product.sellPrice * quantity
    val totalBuyPrice: Long get() = product.buyPrice * quantity
}

enum class DateFilterPeriod(val label: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari Terakhir"),
    THIS_MONTH("Bulan Ini"),
    ALL("Semua")
}

data class DailySales(
    val dayLabel: String,
    val totalSales: Long,
    val orderCount: Int,
    val timestamp: Long
)

data class TopProductStat(
    val productName: String,
    val category: String,
    val totalQuantitySold: Int,
    val totalRevenue: Long
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = PosRepository(database)
        viewModelScope.launch {
            repository.ensureInitialData()
        }
    }

    // --- Authentication & User State ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    val allCashiers: StateFlow<List<User>> = repository.allCashiers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allUsers: StateFlow<List<User>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun login(username: String, pass: String) {
        viewModelScope.launch {
            _loginError.value = null
            val user = repository.login(username, pass)
            if (user != null) {
                _currentUser.value = user
                _currentScreen.value = AppScreen.POS
            } else {
                _loginError.value = "Username atau Password/PIN salah. Silakan coba lagi."
            }
        }
    }

    fun quickLogin(user: User) {
        _currentUser.value = user
        _loginError.value = null
        _currentScreen.value = AppScreen.POS
    }

    fun logout() {
        _currentUser.value = null
        _cart.value = emptyList()
        _currentScreen.value = AppScreen.LOGIN
    }

    fun navigateTo(screen: AppScreen) {
        val user = _currentUser.value
        // RBAC enforcement: Cashier can ONLY access POS and ORDERS (their own)
        if (user != null && !user.isAdmin) {
            if (screen != AppScreen.POS && screen != AppScreen.ORDERS && screen != AppScreen.LOGIN) {
                // Not authorized
                return
            }
        }
        _currentScreen.value = screen
    }

    // --- Products ---
    val allProducts: StateFlow<List<Product>> = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _posSearchQuery = MutableStateFlow("")
    val posSearchQuery: StateFlow<String> = _posSearchQuery.asStateFlow()

    private val _posSelectedCategory = MutableStateFlow(ProductCategories.ALL)
    val posSelectedCategory: StateFlow<String> = _posSelectedCategory.asStateFlow()

    fun setPosSearchQuery(query: String) {
        _posSearchQuery.value = query
    }

    fun setPosSelectedCategory(category: String) {
        _posSelectedCategory.value = category
    }

    val filteredPosProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _posSearchQuery,
        _posSelectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchCategory = category == ProductCategories.ALL || product.category.equals(category, ignoreCase = true)
            val matchQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.skuBarcode.contains(query, ignoreCase = true)
            matchCategory && matchQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addProduct(
        name: String,
        skuBarcode: String,
        category: String,
        buyPrice: Long,
        sellPrice: Long,
        stock: Int,
        unit: String
    ) {
        viewModelScope.launch {
            repository.insertProduct(
                Product(
                    name = name.trim(),
                    skuBarcode = skuBarcode.trim(),
                    category = category.trim(),
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    stock = stock,
                    unit = unit.trim().ifBlank { "pcs" }
                )
            )
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun addStock(productId: Long, additionalStock: Int) {
        viewModelScope.launch {
            repository.addStock(productId, additionalStock)
        }
    }

    // --- POS Cart & Checkout ---
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _customerName = MutableStateFlow("Pelanggan Umum")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _discountAmount = MutableStateFlow(0L)
    val discountAmount: StateFlow<Long> = _discountAmount.asStateFlow()

    private val _lastCompletedOrder = MutableStateFlow<OrderWithItems?>(null)
    val lastCompletedOrder: StateFlow<OrderWithItems?> = _lastCompletedOrder.asStateFlow()

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setDiscountAmount(discount: Long) {
        _discountAmount.value = discount.coerceAtLeast(0)
    }

    fun addToCart(product: Product) {
        if (product.stock <= 0) return
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            if (existing.quantity < product.stock) {
                current[index] = existing.copy(quantity = existing.quantity + 1)
            }
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cart.value = current
    }

    fun updateCartQuantity(productId: Long, quantity: Int) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (quantity <= 0) {
                current.removeAt(index)
            } else {
                val item = current[index]
                val validQty = quantity.coerceAtMost(item.product.stock)
                current[index] = item.copy(quantity = validQty)
            }
            _cart.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _discountAmount.value = 0L
        _customerName.value = "Pelanggan Umum"
    }

    val cartSubtotal: StateFlow<Long> = _cart.combine(_discountAmount) { items, _ ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val cartTotal: StateFlow<Long> = combine(_cart, _discountAmount) { items, discount ->
        val sub = items.sumOf { it.subtotal }
        (sub - discount).coerceAtLeast(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun processCheckout(
        paymentMethod: String,
        cashReceived: Long,
        onSuccess: (OrderWithItems) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: run {
            onError("Sesi kasir berakhir. Silakan login kembali.")
            return
        }
        val items = _cart.value
        if (items.isEmpty()) {
            onError("Keranjang belanja masih kosong!")
            return
        }

        val subtotal = items.sumOf { it.subtotal }
        val discount = _discountAmount.value
        val total = (subtotal - discount).coerceAtLeast(0L)
        val totalBuy = items.sumOf { it.totalBuyPrice }

        if (paymentMethod == PaymentMethods.CASH && cashReceived < total) {
            onError("Jumlah uang tunai kurang dari total tagihan!")
            return
        }

        val changeGiven = if (paymentMethod == PaymentMethods.CASH) {
            cashReceived - total
        } else {
            0L
        }

        viewModelScope.launch {
            try {
                val orderNumber = repository.generateOrderNumber()
                val order = Order(
                    orderNumber = orderNumber,
                    cashierId = user.id,
                    cashierName = user.name,
                    customerName = _customerName.value.ifBlank { "Pelanggan Umum" },
                    subtotalAmount = subtotal,
                    discountAmount = discount,
                    totalAmount = total,
                    totalBuyPrice = totalBuy,
                    paymentMethod = paymentMethod,
                    cashReceived = if (paymentMethod == PaymentMethods.CASH) cashReceived else total,
                    changeGiven = changeGiven,
                    timestamp = System.currentTimeMillis()
                )

                val orderItems = items.map { cartItem ->
                    OrderItem(
                        orderId = 0, // Assigned by repository
                        productId = cartItem.product.id,
                        productName = cartItem.product.name,
                        quantity = cartItem.quantity,
                        buyPrice = cartItem.product.buyPrice,
                        sellPrice = cartItem.product.sellPrice,
                        subtotal = cartItem.subtotal
                    )
                }

                val savedOrderId = repository.processCheckout(order, orderItems)
                val completedOrder = OrderWithItems(
                    order = order.copy(id = savedOrderId),
                    items = orderItems.map { it.copy(orderId = savedOrderId) }
                )

                _lastCompletedOrder.value = completedOrder
                clearCart()
                onSuccess(completedOrder)
            } catch (e: Exception) {
                onError("Gagal memproses transaksi: ${e.localizedMessage}")
            }
        }
    }

    fun dismissReceiptDialog() {
        _lastCompletedOrder.value = null
    }

    // --- Orders & Transaction History ---
    val allOrdersWithItems: StateFlow<List<OrderWithItems>> = repository.allOrdersWithItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _orderDateFilter = MutableStateFlow(DateFilterPeriod.ALL)
    val orderDateFilter: StateFlow<DateFilterPeriod> = _orderDateFilter.asStateFlow()

    private val _orderCashierFilterId = MutableStateFlow<Long?>(null)
    val orderCashierFilterId: StateFlow<Long?> = _orderCashierFilterId.asStateFlow()

    fun setOrderDateFilter(filter: DateFilterPeriod) {
        _orderDateFilter.value = filter
    }

    fun setOrderCashierFilterId(cashierId: Long?) {
        _orderCashierFilterId.value = cashierId
    }

    val visibleOrders: StateFlow<List<OrderWithItems>> = combine(
        allOrdersWithItems,
        _currentUser,
        _orderDateFilter,
        _orderCashierFilterId
    ) { orders, user, dateFilter, cashierFilter ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        orders.filter { orderWithItems ->
            val order = orderWithItems.order

            // Cashier view constraint: Cashier can ONLY see their own transactions!
            if (user != null && !user.isAdmin) {
                if (order.cashierId != user.id) return@filter false
            } else if (cashierFilter != null && order.cashierId != cashierFilter) {
                return@filter false
            }

            // Date filtering
            when (dateFilter) {
                DateFilterPeriod.ALL -> true
                DateFilterPeriod.TODAY -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    order.timestamp >= cal.timeInMillis
                }
                DateFilterPeriod.LAST_7_DAYS -> {
                    val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
                    order.timestamp >= sevenDaysAgo
                }
                DateFilterPeriod.THIS_MONTH -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    order.timestamp >= cal.timeInMillis
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Reports & Analytics (Admin Only) ---
    private val _reportDateFilter = MutableStateFlow(DateFilterPeriod.LAST_7_DAYS)
    val reportDateFilter: StateFlow<DateFilterPeriod> = _reportDateFilter.asStateFlow()

    fun setReportDateFilter(filter: DateFilterPeriod) {
        _reportDateFilter.value = filter
    }

    val reportOrders: StateFlow<List<OrderWithItems>> = combine(
        allOrdersWithItems,
        _reportDateFilter
    ) { orders, filter ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        orders.filter { orderWithItems ->
            val ts = orderWithItems.order.timestamp
            when (filter) {
                DateFilterPeriod.ALL -> true
                DateFilterPeriod.TODAY -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    ts >= cal.timeInMillis
                }
                DateFilterPeriod.LAST_7_DAYS -> {
                    ts >= (now - 7 * 24 * 60 * 60 * 1000L)
                }
                DateFilterPeriod.THIS_MONTH -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    ts >= cal.timeInMillis
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Sales Chart Data
    val dailySalesData: StateFlow<List<DailySales>> = reportOrders.combine(_reportDateFilter) { orders, _ ->
        val map = linkedMapOf<String, Pair<Long, Int>>() // DayLabel -> (TotalAmount, Count)
        val cal = Calendar.getInstance()

        // Initialize last 7 days or days from orders
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val label = String.format("%02d/%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1)
            map[label] = Pair(0L, 0)
        }

        orders.forEach { orderWithItems ->
            cal.timeInMillis = orderWithItems.order.timestamp
            val label = String.format("%02d/%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
            val current = map[label] ?: Pair(0L, 0)
            map[label] = Pair(current.first + orderWithItems.order.totalAmount, current.second + 1)
        }

        map.map { (label, pair) ->
            DailySales(
                dayLabel = label,
                totalSales = pair.first,
                orderCount = pair.second,
                timestamp = 0L
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Top Selling Products
    val topSellingProducts: StateFlow<List<TopProductStat>> = reportOrders.combine(allProducts) { orders, products ->
        val productMap = products.associateBy { it.id }
        val qtyMap = mutableMapOf<String, Pair<String, Pair<Int, Long>>>() // Name -> (Category, (Qty, Revenue))

        orders.flatMap { it.items }.forEach { item ->
            val existing = qtyMap[item.productName] ?: Pair(
                productMap[item.productId]?.category ?: "Umum",
                Pair(0, 0L)
            )
            val newQty = existing.second.first + item.quantity
            val newRev = existing.second.second + item.subtotal
            qtyMap[item.productName] = Pair(existing.first, Pair(newQty, newRev))
        }

        qtyMap.map { (name, data) ->
            TopProductStat(
                productName = name,
                category = data.first,
                totalQuantitySold = data.second.first,
                totalRevenue = data.second.second
            )
        }.sortedByDescending { it.totalQuantitySold }.take(8)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Cashier Management (Admin Only) ---
    fun addCashier(username: String, pin: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || pin.isBlank() || name.isBlank()) {
                onError("Semua kolom harus diisi!")
                return@launch
            }
            val existing = repository.getUserByUsername(username)
            if (existing != null) {
                onError("Username '$username' sudah digunakan!")
                return@launch
            }
            repository.insertUser(
                User(
                    username = username.trim(),
                    password = pin.trim(),
                    name = name.trim(),
                    role = UserRole.CASHIER.name
                )
            )
            onSuccess()
        }
    }

    fun updateCashier(user: User, newPin: String, newName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateUser(
                user.copy(
                    name = newName.trim().ifBlank { user.name },
                    password = newPin.trim().ifBlank { user.password }
                )
            )
            onSuccess()
        }
    }

    fun deleteCashier(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (user.isAdmin) {
            onError("Akun Administrator utama tidak dapat dihapus!")
            return
        }
        viewModelScope.launch {
            repository.deleteUser(user)
            onSuccess()
        }
    }

    // --- Store Info ---
    val storeInfo: StateFlow<StoreInfo?> = repository.storeInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateStoreInfo(storeName: String, address: String, phone: String, receiptFooter: String) {
        viewModelScope.launch {
            repository.updateStoreInfo(
                StoreInfo(
                    id = 1,
                    storeName = storeName.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    receiptFooter = receiptFooter.trim()
                )
            )
        }
    }

    // --- Exporter ---
    fun exportReportCsv(context: android.content.Context) {
        val orders = reportOrders.value
        val period = _reportDateFilter.value.label
        ReceiptExporter.exportOrdersToCsv(context, orders, period)
    }
}
