package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketplaceCategories
import com.example.data.model.Product
import com.example.ui.components.StatusChip
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerCenterScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    val store by viewModel.mySellerStore.collectAsStateWithLifecycle()
    val sellerProducts by viewModel.mySellerProducts.collectAsStateWithLifecycle()
    val sellerOrders by viewModel.mySellerSubOrders.collectAsStateWithLifecycle()
    val payouts by viewModel.mySellerPayouts.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()

    var showAddProductDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("X-SHOPPING Seller Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (store != null) {
                FloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    containerColor = BrandOrange,
                    contentColor = PureWhite,
                    modifier = Modifier.testTag("seller_add_product_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add product")
                }
            }
        }
    ) { innerPadding ->
        if (store == null) {
            var storeName by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var sellerType by remember { mutableStateOf("BUSINESS") }
            var regNo by remember { mutableStateOf("") }
            var ibanLast4 by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("+966500000000") }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Become an X-SHOPPING Seller", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Register your Individual or Business store for global multi-vendor selling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BUSINESS", "INDIVIDUAL").forEach { type ->
                            FilterChip(
                                selected = sellerType == type,
                                onClick = { sellerType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Store Description & Catalog Focus") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = regNo,
                        onValueChange = { regNo = it },
                        label = { Text("Commercial Registration / National ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = ibanLast4,
                        onValueChange = { ibanLast4 = it.take(4) },
                        label = { Text("Bank IBAN Last 4 Digits (Settlement)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Business Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            viewModel.registerSellerStore(
                                storeName = storeName,
                                storeDescription = description,
                                sellerType = sellerType,
                                countryCode = selectedCountry.code,
                                businessRegNo = regNo,
                                ibanLast4 = ibanLast4,
                                contactPhone = phone
                            )
                        },
                        enabled = storeName.trim().length >= 2 && description.trim().length >= 2 &&
                            regNo.trim().length >= 3 && ibanLast4.trim().length == 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_seller_application_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit Seller Application")
                    }
                }
            }
            return@Scaffold
        }

        val currentStore = store!!
        val grossSales = sellerOrders.sumOf { it.grossUsd }
        val totalCommission = sellerOrders.sumOf { it.commissionUsd }
        val netEarnings = sellerOrders.sumOf { it.netUsd }
        val requestedTotal = payouts.sumOf { it.amountUsd }
        val availableBalance = (netEarnings - requestedTotal).coerceAtLeast(0.0)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentStore.storeName, style = MaterialTheme.typography.titleLarge)
                            StatusChip(currentStore.status)
                        }
                        Text(
                            text = "${currentStore.sellerType} • Market ${currentStore.countryCode} • Commission ${(currentStore.commissionRate * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // KPIs & Wallet
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMiniCard("Gross Sales", viewModel.formatMoney(grossSales), Modifier.weight(1f))
                    KpiMiniCard("Available", viewModel.formatMoney(availableBalance), Modifier.weight(1f))
                    KpiMiniCard("Commission", viewModel.formatMoney(totalCommission), Modifier.weight(1f))
                }
            }

            // Sales Bar Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Order Revenue & Payouts", style = MaterialTheme.typography.titleSmall)
                            OutlinedButton(
                                onClick = {
                                    if (availableBalance > 0.0) {
                                        viewModel.requestSellerPayout(currentStore.storeName, availableBalance)
                                    } else {
                                        viewModel.showMessage("No available earnings to withdraw yet")
                                    }
                                }
                            ) {
                                Text("Withdraw")
                            }
                        }
                        val chartData = remember(sellerOrders) {
                            if (sellerOrders.isEmpty()) listOf(0.2f, 0.45f, 0.3f, 0.7f, 0.55f, 0.85f, 0.65f)
                            else sellerOrders.take(7).map { (it.grossUsd / 500.0).toFloat().coerceIn(0.15f, 1f) }
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                        ) {
                            val barWidth = size.width / (chartData.size * 2f)
                            chartData.forEachIndexed { idx, ratio ->
                                val barHeight = size.height * ratio
                                val x = idx * (barWidth * 2f) + barWidth * 0.5f
                                drawRoundRect(
                                    color = BrandOrange,
                                    topLeft = Offset(x, size.height - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                    }
                }
            }

            // Seller Orders
            item {
                Text("Seller Fulfillment Orders (${sellerOrders.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(sellerOrders, key = { it.id }) { sub ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${sub.orderNumber} • ${sub.itemCount} unit(s)", style = MaterialTheme.typography.titleSmall)
                            StatusChip(sub.status)
                        }
                        Text(sub.itemsSummary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "Net Earnings: ${viewModel.formatMoney(sub.netUsd)} • ${sub.courierName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = VerifiedEmerald
                        )
                        val nextStage = when (sub.status) {
                            "PENDING", "CONFIRMED" -> "PROCESSING"
                            "PROCESSING" -> "PACKED"
                            "PACKED" -> "SHIPPED"
                            "SHIPPED" -> "OUT_FOR_DELIVERY"
                            "OUT_FOR_DELIVERY" -> "DELIVERED"
                            else -> null
                        }
                        if (nextStage != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.advanceSellerSubOrder(sub.id, nextStage, sub.courierName, sub.trackingNumber)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark as ${nextStage.replace("_", " ")}")
                            }
                        }
                    }
                }
            }

            // Seller Products
            item {
                Text("My Store Products (${sellerProducts.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(sellerProducts, key = { it.id }) { prod ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${prod.category} • Stock: ${prod.stock} • ${viewModel.formatMoney(prod.priceUsd)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusChip(prod.approvalStatus)
                    }
                }
            }
        }
    }

    if (showAddProductDialog && store != null) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var priceStr by remember { mutableStateOf("99") }
        var stockStr by remember { mutableStateOf("20") }
        var category by remember { mutableStateOf(MarketplaceCategories.tree.first().name) }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Publish New Product") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Product Title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, minLines = 2)
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Base Price (USD)") }, singleLine = true)
                    OutlinedTextField(value = stockStr, onValueChange = { stockStr = it }, label = { Text("Stock Quantity") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = priceStr.toDoubleOrNull() ?: 49.0
                        val stock = stockStr.toLongOrNull() ?: 10L
                        viewModel.addSellerProduct(
                            Product(
                                sellerName = store!!.storeName,
                                title = title,
                                description = desc,
                                category = category,
                                subCategory = "General",
                                brand = store!!.storeName,
                                priceUsd = price.coerceAtLeast(1.0),
                                stock = stock.coerceAtLeast(1L),
                                imageKey = category.lowercase(),
                                sizes = listOf("Standard"),
                                colors = listOf("Black", "Orange"),
                                models = listOf("2026 Edition")
                            ),
                            asAdminOfficial = false
                        ) { showAddProductDialog = false }
                    },
                    enabled = title.trim().length >= 2 && desc.trim().length >= 4
                ) { Text("Submit Product") }
            },
            dismissButton = { TextButton(onClick = { showAddProductDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
internal fun KpiMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = BrandOrange
            )
        }
    }
}
