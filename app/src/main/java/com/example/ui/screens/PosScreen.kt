package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.PaymentMethods
import com.example.data.model.Product
import com.example.data.model.ProductCategories
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosNavy
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSlateLight
import com.example.ui.theme.PosTealContainer
import com.example.ui.theme.PosTealPrimary
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val storeInfo by viewModel.storeInfo.collectAsState()
    val products by viewModel.filteredPosProducts.collectAsState()
    val searchQuery by viewModel.posSearchQuery.collectAsState()
    val selectedCategory by viewModel.posSelectedCategory.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val cartSubtotal by viewModel.cartSubtotal.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val lastCompletedOrder by viewModel.lastCompletedOrder.collectAsState()

    var isCartSheetOpen by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethods.CASH) }
    var cashReceivedInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("0") }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Calculate cash received and change
    val cashReceived = cashReceivedInput.toLongOrNull() ?: 0L
    val changeGiven = (cashReceived - cartTotal).coerceAtLeast(0L)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = storeInfo?.storeName ?: "KIAN JAYA POS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PosNavy
                            )
                            Text(
                                text = "Kasir: ${currentUser?.name ?: "Petugas"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PosTealPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Cart Button with badge
                        BadgedBox(
                            badge = {
                                if (cart.isNotEmpty()) {
                                    Badge(containerColor = PosRose) {
                                        Text("${cart.sumOf { it.quantity }}")
                                    }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = { isCartSheetOpen = true },
                                modifier = Modifier.testTag("pos_cart_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Keranjang Belanja",
                                    tint = PosNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setPosSearchQuery(it) },
                        placeholder = { Text("Cari produk atau barcode SKU...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = PosSlateLight)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setPosSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("pos_search_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProductCategories.defaultCategories.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setPosSelectedCategory(category) },
                                label = { Text(category) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosTealContainer,
                                    selectedLabelColor = PosTealPrimary
                                ),
                                modifier = Modifier.testTag("filter_chip_$category")
                            )
                        }
                    }
                }
            }

            // Product Grid
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PosSlateLight,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada produk yang cocok",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PosSlateLight
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products, key = { it.id }) { product ->
                        val cartItem = cart.firstOrNull { it.product.id == product.id }
                        ProductCard(
                            product = product,
                            quantityInCart = cartItem?.quantity ?: 0,
                            onAddToCart = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }

        // Floating Bottom Cart Bar
        AnimatedVisibility(
            visible = cart.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = PosNavy,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${cart.sumOf { it.quantity }} Barang di Keranjang",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(cartTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { isCartSheetOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary),
                        modifier = Modifier.testTag("pos_open_checkout_sheet_button")
                    ) {
                        Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bayar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Modal Bottom Sheet: Cart & Checkout
        if (isCartSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isCartSheetOpen = false },
                sheetState = bottomSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keranjang & Pembayaran",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PosNavy
                        )
                        OutlinedButton(
                            onClick = { viewModel.clearCart() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PosRose)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kosongkan", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Items list
                    cart.forEach { item ->
                        CartItemRow(
                            item = item,
                            onQuantityChange = { newQty ->
                                viewModel.updateCartQuantity(item.product.id, newQty)
                            },
                            onRemove = {
                                viewModel.removeFromCart(item.product.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Customer Name Input
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.setCustomerName(it) },
                        label = { Text("Nama Pelanggan (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Discount Input
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = {
                            discountInput = it
                            val disc = it.toLongOrNull() ?: 0L
                            viewModel.setDiscountAmount(disc)
                        },
                        label = { Text("Diskon Transaksi (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Totals Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = PosSlateLight)
                        Text(CurrencyFormatter.formatRupiah(cartSubtotal), fontWeight = FontWeight.SemiBold)
                    }
                    if (discountAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Diskon", color = PosRose)
                            Text("-${CurrencyFormatter.formatRupiah(discountAmount)}", color = PosRose, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Tagihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            CurrencyFormatter.formatRupiah(cartTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PosTealPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Method Options
                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethods.list.forEach { method ->
                            val isSelected = selectedPaymentMethod == method
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPaymentMethod = method
                                    if (method != PaymentMethods.CASH) {
                                        cashReceivedInput = cartTotal.toString()
                                    }
                                },
                                label = { Text(method) },
                                leadingIcon = {
                                    when (method) {
                                        PaymentMethods.CASH -> Icon(Icons.Default.LocalAtm, contentDescription = null)
                                        PaymentMethods.QRIS -> Icon(Icons.Default.QrCode, contentDescription = null)
                                        else -> Icon(Icons.Default.Payment, contentDescription = null)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosTealContainer,
                                    selectedLabelColor = PosTealPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Cash Specific Calculation & Quick Buttons
                    if (selectedPaymentMethod == PaymentMethods.CASH) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Uang Tunai Diterima",
                            style = MaterialTheme.typography.labelMedium,
                            color = PosSlateLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick cash chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cashReceivedInput = cartTotal.toString() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Uang Pas")
                            }
                            listOf(20000L, 50000L, 100000L, 200000L).forEach { amount ->
                                if (amount >= cartTotal) {
                                    OutlinedButton(
                                        onClick = { cashReceivedInput = amount.toString() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(CurrencyFormatter.formatRupiah(amount))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = cashReceivedInput,
                            onValueChange = { cashReceivedInput = it },
                            label = { Text("Jumlah Uang Tunai (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pos_cash_received_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Kembalian Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (cashReceived >= cartTotal) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (cashReceived >= cartTotal) PosEmerald else PosRose
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (cashReceived >= cartTotal) "Kembalian" else "Kurang",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cashReceived >= cartTotal) PosEmerald else PosRose
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(
                                        if (cashReceived >= cartTotal) changeGiven else (cartTotal - cashReceived)
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cashReceived >= cartTotal) PosEmerald else PosRose
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Checkout Button
                    Button(
                        onClick = {
                            val cashAmount = if (selectedPaymentMethod == PaymentMethods.CASH) {
                                cashReceivedInput.toLongOrNull() ?: 0L
                            } else {
                                cartTotal
                            }

                            viewModel.processCheckout(
                                paymentMethod = selectedPaymentMethod,
                                cashReceived = cashAmount,
                                onSuccess = {
                                    isCartSheetOpen = false
                                    cashReceivedInput = ""
                                    discountInput = "0"
                                    Toast.makeText(context, "Transaksi berhasil diproses!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("pos_process_payment_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary)
                    ) {
                        Text(
                            text = "Konfirmasi & Cetak Nota (${CurrencyFormatter.formatRupiah(cartTotal)})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Receipt Dialog on Transaction Complete
        lastCompletedOrder?.let { completedOrder ->
            ReceiptDialog(
                orderWithItems = completedOrder,
                storeInfo = storeInfo,
                onDismiss = {
                    viewModel.dismissReceiptDialog()
                }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    quantityInCart: Int,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = product.stock > 0, onClick = onAddToCart)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.stock <= 0) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (product.stock > 0) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Category & Stock Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = PosNavy
                    )
                }

                // Stock indicator
                val stockColor = when {
                    product.stock <= 0 -> PosRose
                    product.stock <= 5 -> PosAmber
                    else -> PosEmerald
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = stockColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (product.stock <= 0) "Habis" else "${product.stock} ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = stockColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (product.stock <= 0) Color.Gray else PosNavy
            )

            if (product.skuBarcode.isNotBlank()) {
                Text(
                    text = "SKU: ${product.skuBarcode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosSlateLight
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price & Add to Cart button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = CurrencyFormatter.formatRupiah(product.sellPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (product.stock <= 0) Color.Gray else PosTealPrimary
                )

                if (product.stock > 0) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (quantityInCart > 0) PosTealPrimary else PosTealContainer)
                            .clickable(onClick = onAddToCart),
                        contentAlignment = Alignment.Center
                    ) {
                        if (quantityInCart > 0) {
                            Text(
                                text = "$quantityInCart",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah ke keranjang",
                                tint = PosTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PosNavy
                )
                Text(
                    text = "${CurrencyFormatter.formatRupiah(item.product.sellPrice)} / ${item.product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PosSlateLight
                )
            }

            // Qty Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (item.quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                        contentDescription = "Kurang",
                        tint = if (item.quantity == 1) PosRose else PosNavy,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    enabled = item.quantity < item.product.stock,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        tint = if (item.quantity < item.product.stock) PosNavy else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = CurrencyFormatter.formatRupiah(item.subtotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = PosTealPrimary
            )
        }
    }
}
