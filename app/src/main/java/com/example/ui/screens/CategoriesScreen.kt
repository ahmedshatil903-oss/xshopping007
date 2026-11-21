package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketplaceCategories
import com.example.ui.components.ProductCard
import com.example.ui.components.categoryIconFor
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.theme.BrandOrange

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    viewModel: MarketplaceViewModel,
    initialCategory: String?,
    onOpenProduct: (String) -> Unit,
    onBrowseCategory: (String) -> Unit
) {
    val products by viewModel.catalogProducts.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val wishIds = remember(wishlist) { wishlist.map { it.productId }.toSet() }

    var expandedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory ?: MarketplaceCategories.tree.first().name)
    }
    var activeSubCategory by remember { mutableStateOf<String?>(null) }

    val filteredProducts = remember(products, expandedCategory, activeSubCategory) {
        products.filter { prod ->
            val catMatch = prod.category.equals(expandedCategory, ignoreCase = true)
            val subMatch = activeSubCategory == null || prod.subCategory.equals(activeSubCategory, ignoreCase = true)
            catMatch && subMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Global Departments & Categories",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Tap any department to expand subcategories and browse matching products.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(MarketplaceCategories.tree) { node ->
            val isExpanded = expandedCategory == node.name
            val countInCat = products.count { it.category.equals(node.name, ignoreCase = true) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_node_${node.name}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (isExpanded) BrandOrange else MaterialTheme.colorScheme.outline
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                expandedCategory = if (isExpanded) "" else node.name
                                activeSubCategory = null
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = categoryIconFor(node.iconKey),
                                        contentDescription = node.name,
                                        tint = BrandOrange,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(text = node.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${node.subCategories.size} subcategories • $countInCat live items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Expand category"
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                node.subCategories.forEach { sub ->
                                    val subSelected = activeSubCategory == sub
                                    AssistChip(
                                        onClick = {
                                            activeSubCategory = if (subSelected) null else sub
                                        },
                                        label = {
                                            Text(
                                                text = sub,
                                                color = if (subSelected) BrandOrange else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    )
                                }
                                AssistChip(
                                    onClick = { onBrowseCategory(node.name) },
                                    label = { Text("Search All ${node.name}", color = BrandOrange) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (expandedCategory.isNotBlank() && filteredProducts.isNotEmpty()) {
            item {
                Text(
                    text = "In $expandedCategory" + (activeSubCategory?.let { " › $it" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            val chunked = filteredProducts.chunked(2)
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
