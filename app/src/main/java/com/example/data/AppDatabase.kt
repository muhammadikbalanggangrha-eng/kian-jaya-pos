package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.OrderDao
import com.example.data.dao.ProductDao
import com.example.data.dao.StoreInfoDao
import com.example.data.dao.UserDao
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.PaymentMethods
import com.example.data.model.Product
import com.example.data.model.StoreInfo
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Product::class,
        Order::class,
        OrderItem::class,
        StoreInfo::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun storeInfoDao(): StoreInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasir_pos_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }
    }
}

suspend fun populateInitialData(database: AppDatabase) {
    val userDao = database.userDao()
    val productDao = database.productDao()
    val storeInfoDao = database.storeInfoDao()
    val orderDao = database.orderDao()

    // 1. Initial Store Info
    storeInfoDao.insertOrUpdateStoreInfo(
        StoreInfo(
            id = 1,
            storeName = "KIAN JAYA POS",
            address = "Jl. Merdeka No. 45, Jakarta Pusat",
            phone = "0812-3456-7890",
            receiptFooter = "Terima kasih telah berbelanja di Kian Jaya POS!\nBarang yang sudah dibeli tidak dapat ditukar."
        )
    )

    // 2. Initial Users (Admin & Kasir)
    val defaultUsers = listOf(
        User(
            id = 1,
            username = "admin",
            password = "123",
            name = "Pemilik Toko (Admin)",
            role = UserRole.ADMIN.name
        ),
        User(
            id = 2,
            username = "kasir1",
            password = "123",
            name = "Kasir 1 (Budi Santoso)",
            role = UserRole.CASHIER.name
        ),
        User(
            id = 3,
            username = "kasir2",
            password = "123",
            name = "Kasir 2 (Siti Rahma)",
            role = UserRole.CASHIER.name
        )
    )
    userDao.insertUsers(defaultUsers)

    // 3. Initial Products
    val defaultProducts = listOf(
        Product(
            id = 1,
            name = "Kopi Susu Gula Aren",
            skuBarcode = "8991001",
            category = "Minuman",
            buyPrice = 10000,
            sellPrice = 18000,
            stock = 45,
            unit = "cup"
        ),
        Product(
            id = 2,
            name = "Teh Melati Manis",
            skuBarcode = "8991002",
            category = "Minuman",
            buyPrice = 3000,
            sellPrice = 6000,
            stock = 60,
            unit = "cup"
        ),
        Product(
            id = 3,
            name = "Air Mineral 600ml",
            skuBarcode = "8991003",
            category = "Minuman",
            buyPrice = 2500,
            sellPrice = 4000,
            stock = 80,
            unit = "botol"
        ),
        Product(
            id = 4,
            name = "Nasi Goreng Spesial",
            skuBarcode = "8992001",
            category = "Makanan",
            buyPrice = 15000,
            sellPrice = 25000,
            stock = 30,
            unit = "porsi"
        ),
        Product(
            id = 5,
            name = "Mie Goreng Jawa",
            skuBarcode = "8992002",
            category = "Makanan",
            buyPrice = 13000,
            sellPrice = 22000,
            stock = 25,
            unit = "porsi"
        ),
        Product(
            id = 6,
            name = "Ayam Geprek Sambal Bawang",
            skuBarcode = "8992003",
            category = "Makanan",
            buyPrice = 12000,
            sellPrice = 20000,
            stock = 20,
            unit = "porsi"
        ),
        Product(
            id = 7,
            name = "Keripik Singkong Balado",
            skuBarcode = "8993001",
            category = "Snack",
            buyPrice = 8000,
            sellPrice = 14000,
            stock = 35,
            unit = "bungkus"
        ),
        Product(
            id = 8,
            name = "Roti Coklat Keju",
            skuBarcode = "8993002",
            category = "Snack",
            buyPrice = 5000,
            sellPrice = 9000,
            stock = 15,
            unit = "pcs"
        ),
        Product(
            id = 9,
            name = "Minyak Goreng 1L",
            skuBarcode = "8994001",
            category = "Sembako",
            buyPrice = 16000,
            sellPrice = 19500,
            stock = 4, // low stock test
            unit = "pouch"
        ),
        Product(
            id = 10,
            name = "Beras Premium 5kg",
            skuBarcode = "8994002",
            category = "Sembako",
            buyPrice = 65000,
            sellPrice = 75000,
            stock = 12,
            unit = "sak"
        ),
        Product(
            id = 11,
            name = "Gula Pasir 1kg",
            skuBarcode = "8994003",
            category = "Sembako",
            buyPrice = 14500,
            sellPrice = 17500,
            stock = 0, // out of stock test
            unit = "kg"
        )
    )
    productDao.insertProducts(defaultProducts)

    // 4. Initial Sample Orders (Over the last few days so reports chart has historical data)
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L

    val sampleOrders = listOf(
        Triple(now - (3 * dayMillis), 2L, "Kasir 1 (Budi Santoso)"),
        Triple(now - (2 * dayMillis), 3L, "Kasir 2 (Siti Rahma)"),
        Triple(now - (1 * dayMillis), 2L, "Kasir 1 (Budi Santoso)"),
        Triple(now - (4 * 3600 * 1000L), 2L, "Kasir 1 (Budi Santoso)")
    )

    sampleOrders.forEachIndexed { index, (time, cashierId, cashierName) ->
        val orderNo = "TRX-${String.format("%04d", index + 1001)}"
        val orderId = orderDao.insertOrder(
            Order(
                orderNumber = orderNo,
                cashierId = cashierId,
                cashierName = cashierName,
                customerName = "Pelanggan #${index + 1}",
                subtotalAmount = 67000,
                discountAmount = 0,
                totalAmount = 67000,
                totalBuyPrice = 40000,
                paymentMethod = if (index % 2 == 0) PaymentMethods.CASH else PaymentMethods.QRIS,
                cashReceived = 70000,
                changeGiven = 3000,
                timestamp = time
            )
        )
        val items = listOf(
            OrderItem(
                orderId = orderId,
                productId = 1,
                productName = "Kopi Susu Gula Aren",
                quantity = 2,
                buyPrice = 10000,
                sellPrice = 18000,
                subtotal = 36000
            ),
            OrderItem(
                orderId = orderId,
                productId = 4,
                productName = "Nasi Goreng Spesial",
                quantity = 1,
                buyPrice = 15000,
                sellPrice = 25000,
                subtotal = 25000
            ),
            OrderItem(
                orderId = orderId,
                productId = 2,
                productName = "Teh Melati Manis",
                quantity = 1,
                buyPrice = 3000,
                sellPrice = 6000,
                subtotal = 6000
            )
        )
        orderDao.insertOrderItems(items)
    }
}
