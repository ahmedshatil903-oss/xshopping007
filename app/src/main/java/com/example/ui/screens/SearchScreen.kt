package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductCard
import com.example.ui.state.MarketplaceViewModel

private enum class SortOption(val label: String) {
    FEATURED("Featured"),
    PRICE_ASC("Price: Low to High"),
    PRICE_DESC("Price: High to Low"),
    TOP_RATED("Top Rated")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MarketplaceViewModel,
    initialFilter: String?,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit
) {
    val products by viewModel.catalogProducts.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val wishIds = remember(wishlist) { wishlist.map { it.productId }.toSet() }

    var query by remember(initialFilter) {
        mutableStateOf(if (initialFilter != null && initialFilter != "OFFICIAL") initialFilter else "")
    }
    var officialOnly by remember(initialFilter) { mutableStateOf(initialFilter == "OFFICIAL") }
    var flashOnly by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.FEATURED) }

    val results = remember(products, query, officialOnly, flashOnly, sortOption) {
        val q = query.trim().lowercase()
        val filtered = products.filter { p ->
            val matchQ = q.isBlank() ||
                p.title.lowercase().contains(q) ||
                p.brand.lowercase().contains(q) ||
                p.category.lowercase().contains(q) ||
                p.sellerName.lowercase().contains(q)
            val matchOfficial = !officialOnly || p.official
            val matchFlash = !flashOnly || p.flashSale || p.discountPercent >= 15
            matchQ && matchOfficial && matchFlash
        }
        when (sortOption) {
            SortOption.FEATURED -> filtered.sortedByDescending { it.featured }
            SortOption.PRICE_ASC -> filtered.sortedBy { it.effectivePriceUsd }
            SortOption.PRICE_DESC -> filtered.sortedByDescending { it.effectivePriceUsd }
            SortOption.TOP_RATED -> filtered.sortedByDescending { it.rating }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Marketplace") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search products, brands, categories, sellers...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_input")
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = officialOnly,
                        onClick = { officialOnly = !officialOnly },
                        label = { Text("Official Store") }
                    )
                }
                item {
                    FilterChip(
                        selected = flashOnly,
                        onClick = { flashOnly = !flashOnly },
                        label = { Text("Flash Deals") }
                    )
                }
                items(SortOption.entries) { opt ->
                    FilterChip(
                        selected = sortOption == opt,
                        onClick = { sortOption = opt },
                        label = { Text(opt.label) }
                    )
                }
            }

            if (results.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Filled.SearchOff,
                    title = "No matching products",
                    subtitle = "Try clearing filters or searching another keyword."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val chunked = results.chunked(2)
                    items(chunked) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { product ->
                                ProductCard(
                                    product = product,
                                    formattedPrice = viewModel.formatMoney(product.effectivePriceUsd),
                                    formattedOriginalPrice = viewModel.formatMoney(product.priceUsd),
                                    isWishlisted = wishIds.contains(product.id),
                                    onClick = { onOpenProduct(product.id) },
                                    onToggleWishlist = { viewModel.toggleWishlist(product) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
