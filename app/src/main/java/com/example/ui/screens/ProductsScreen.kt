package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.Product
import com.example.data.model.ProductCategories
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosNavy
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSlateLight
import com.example.ui.theme.PosTealContainer
import com.example.ui.theme.PosTealPrimary
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProductCategories.ALL) }

    // Dialog states
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productToAddStock by remember { mutableStateOf<Product?>(null) }

    // Filter products
    val filteredProducts = allProducts.filter { product ->
        val matchCat = selectedCategory == ProductCategories.ALL || product.category.equals(selectedCategory, ignoreCase = true)
        val matchSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.skuBarcode.contains(searchQuery, ignoreCase = true)
        matchCat && matchSearch
    }

    val lowStockCount = allProducts.count { it.stock in 1..5 }
    val outOfStockCount = allProducts.count { it.stock <= 0 }

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
                                text = "Manajemen Produk & Stok",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PosNavy
                            )
                            Text(
                                text = "Total ${allProducts.size} item produk terdaftar",
                                style = MaterialTheme.typography.bodySmall,
                                color = PosSlateLight
                            )
                        }

                        Button(
                            onClick = { isAddDialogOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary),
                            modifier = Modifier.testTag("admin_add_product_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah")
                        }
                    }

                    // Stock alert banner
                    if (lowStockCount > 0 || outOfStockCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosAmber)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = PosAmber, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Peringatan Stok: $outOfStockCount habis, $lowStockCount menipis (<=5)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari produk atau barcode SKU...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = PosSlateLight)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
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
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PosTealContainer,
                                    selectedLabelColor = PosTealPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada produk ditemukan",
                        color = PosSlateLight
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductAdminCard(
                            product = product,
                            onEdit = { productToEdit = product },
                            onDelete = { productToDelete = product },
                            onAddStock = { productToAddStock = product }
                        )
                    }
                }
            }
        }

        // Add Product Dialog
        if (isAddDialogOpen) {
            ProductFormDialog(
                title = "Tambah Produk Baru",
                initialProduct = null,
                onDismiss = { isAddDialogOpen = false },
                onSave = { name, sku, category, buy, sell, stock, unit ->
                    viewModel.addProduct(name, sku, category, buy, sell, stock, unit)
                    isAddDialogOpen = false
                    Toast.makeText(context, "Produk '$name' berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Product Dialog
        productToEdit?.let { product ->
            ProductFormDialog(
                title = "Edit Produk",
                initialProduct = product,
                onDismiss = { productToEdit = null },
                onSave = { name, sku, category, buy, sell, stock, unit ->
                    viewModel.updateProduct(
                        product.copy(
                            name = name,
                            skuBarcode = sku,
                            category = category,
                            buyPrice = buy,
                            sellPrice = sell,
                            stock = stock,
                            unit = unit
                        )
                    )
                    productToEdit = null
                    Toast.makeText(context, "Produk berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Quick Add Stock Dialog
        productToAddStock?.let { product ->
            var additionalStockInput by remember { mutableStateOf("10") }
            AlertDialog(
                onDismissRequest = { productToAddStock = null },
                title = { Text("Tambah Stok Produk") },
                text = {
                    Column {
                        Text(
                            text = "${product.name} (Stok Saat Ini: ${product.stock} ${product.unit})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = additionalStockInput,
                            onValueChange = { additionalStockInput = it },
                            label = { Text("Jumlah Stok Ditambahkan") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val addQty = additionalStockInput.toIntOrNull() ?: 0
                            if (addQty > 0) {
                                viewModel.addStock(product.id, addQty)
                                Toast.makeText(context, "Berhasil menambah $addQty stok", Toast.LENGTH_SHORT).show()
                            }
                            productToAddStock = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary)
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToAddStock = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        productToDelete?.let { product ->
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("Hapus Produk?") },
                text = {
                    Text("Apakah Anda yakin ingin menghapus produk '${product.name}'? Data produk yang dihapus tidak dapat dipulihkan.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProduct(product)
                            productToDelete = null
                            Toast.makeText(context, "Produk '${product.name}' dihapus", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PosRose)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun ProductAdminCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddStock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        if (product.skuBarcode.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SKU: ${product.skuBarcode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PosSlateLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PosNavy
                    )
                }

                // Stock Badge
                val stockColor = when {
                    product.stock <= 0 -> PosRose
                    product.stock <= 5 -> PosAmber
                    else -> PosEmerald
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stockColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Stok: ${product.stock} ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = stockColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Harga Beli (Modal)", style = MaterialTheme.typography.labelSmall, color = PosSlateLight)
                    Text(CurrencyFormatter.formatRupiah(product.buyPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Harga Jual", style = MaterialTheme.typography.labelSmall, color = PosSlateLight)
                    Text(CurrencyFormatter.formatRupiah(product.sellPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PosTealPrimary)
                }
                Column {
                    Text("Margin Laba", style = MaterialTheme.typography.labelSmall, color = PosSlateLight)
                    Text("+${CurrencyFormatter.formatRupiah(product.marginProfit)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PosEmerald)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddStock,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Stok", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PosNavy, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = PosRose, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    title: String,
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onSave: (name: String, sku: String, category: String, buy: Long, sell: Long, stock: Int, unit: String) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.skuBarcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Makanan") }
    var buyPriceInput by remember { mutableStateOf(initialProduct?.buyPrice?.toString() ?: "") }
    var sellPriceInput by remember { mutableStateOf(initialProduct?.sellPrice?.toString() ?: "") }
    var stockInput by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "10") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Makanan", "Minuman", "Snack", "Sembako", "Kebutuhan Rumah", "Lainnya")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Barcode / SKU (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyPriceInput,
                        onValueChange = { buyPriceInput = it },
                        label = { Text("Harga Modal (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPriceInput,
                        onValueChange = { sellPriceInput = it },
                        label = { Text("Harga Jual (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { stockInput = it },
                        label = { Text("Stok Awal *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan (pcs, kg)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buy = buyPriceInput.toLongOrNull() ?: 0L
                    val sell = sellPriceInput.toLongOrNull() ?: 0L
                    val stock = stockInput.toIntOrNull() ?: 0
                    if (name.isNotBlank() && sell > 0) {
                        onSave(name, sku, category, buy, sell, stock, unit)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PosTealPrimary)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
