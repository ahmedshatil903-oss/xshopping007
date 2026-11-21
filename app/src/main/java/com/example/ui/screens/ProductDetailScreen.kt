package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.OfficialBadge
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductVisualArtwork
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.FlashAmber
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onBuyNowGoCheckout: () -> Unit
) {
    val allProducts by viewModel.allCatalogProductsUnfiltered.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val reviews by viewModel.activeProductReviews.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val product = remember(allProducts, productId) {
        allProducts.firstOrNull { it.id == productId }
    }

    LaunchedEffect(productId) {
        viewModel.observeReviewsFor(productId)
    }

    if (product == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Product") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { pad ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Text("Product not found")
            }
        }
        return
    }

    val isWishlisted = wishlist.any { it.productId == product.id }
    var selectedSize by remember(product.id) { mutableStateOf(product.sizes.firstOrNull().orEmpty()) }
    var selectedColor by remember(product.id) { mutableStateOf(product.colors.firstOrNull().orEmpty()) }
    var selectedModel by remember(product.id) { mutableStateOf(product.models.firstOrNull().orEmpty()) }
    var quantity by remember(product.id) { mutableLongStateOf(1L) }
    var showReviewDialog by remember { mutableStateOf(false) }

    val similarProducts = remember(allProducts, product) {
        allProducts.filter { it.id != product.id && it.category == product.category }.take(6)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.brand) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWishlist(product) }) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) BrandOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out ${product.title} on X-SHOPPING for ${viewModel.formatMoney(product.effectivePriceUsd)}!"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share product")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.addToCart(product, quantity, selectedSize, selectedColor, selectedModel)
                        },
                        enabled = product.stock > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("add_to_cart_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.addToCart)
                    }
                    Button(
                        onClick = {
                            viewModel.addToCart(product, quantity, selectedSize, selectedColor, selectedModel) {
                                onBuyNowGoCheckout()
                            }
                        },
                        enabled = product.stock > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("buy_now_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite)
                    ) {
                        Text(strings.buyNow)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Multi-Image Gallery Pager
            item {
                val galleryPager = rememberPagerState(pageCount = { 3 })
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                ) {
                    HorizontalPager(state = galleryPager, modifier = Modifier.fillMaxSize()) { page ->
                        val key = when (page) {
                            0 -> product.imageKey
                            1 -> if (product.official) "fashion" else product.imageKey
                            else -> "mobile"
                        }
                        ProductVisualArtwork(
                            imageKey = key,
                            contentDescription = "${product.title} view ${page + 1}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianBlack.copy(alpha = 0.72f)
                    ) {
                        Text(
                            text = "${galleryPager.currentPage + 1} / 3",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Title, Seller, Price & Stock
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OfficialBadge(official = product.official)
                        Text(
                            text = product.sellerName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(text = product.title, style = MaterialTheme.typography.headlineSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = FlashAmber, modifier = Modifier.size(18.dp))
                        Text("${product.rating} • ${product.reviewCount + reviews.size} reviews", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (product.stock > 0) "In Stock (${product.stock})" else "Out of Stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (product.stock > 0) VerifiedEmerald else MaterialTheme.colorScheme.error
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = viewModel.formatMoney(product.effectivePriceUsd),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandOrange
                        )
                        if (product.discountPercent > 0) {
                            Text(
                                text = viewModel.formatMoney(product.priceUsd),
                                style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(color = BrandOrange.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "SAVE ${product.discountPercent}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandOrange,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Variations (Size, Color, Model) & Quantity
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (product.colors.isNotEmpty()) {
                            Text("Color: $selectedColor", style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                product.colors.forEach { c ->
                                    FilterChip(
                                        selected = selectedColor == c,
                                        onClick = { selectedColor = c },
                                        label = { Text(c) }
                                    )
                                }
                            }
                        }
                        if (product.sizes.isNotEmpty()) {
                            Text("Size / Capacity: $selectedSize", style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                product.sizes.forEach { s ->
                                    FilterChip(
                                        selected = selectedSize == s,
                                        onClick = { selectedSize = s },
                                        label = { Text(s) }
                                    )
                                }
                            }
                        }
                        if (product.models.isNotEmpty()) {
                            Text("Edition / Model: $selectedModel", style = MaterialTheme.typography.titleSmall)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                product.models.forEach { m ->
                                    FilterChip(
                                        selected = selectedModel == m,
                                        onClick = { selectedModel = m },
                                        label = { Text(m) }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quantity", style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- }
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                                }
                                Text(
                                    text = quantity.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (quantity < product.stock.coerceAtMost(99L)) quantity++ }
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                                }
                            }
                        }
                    }
                }
            }

            // Delivery & Return Info
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = BrandOrange)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Delivery to ${selectedCountry.flagEmoji} ${selectedCountry.defaultCity}, ${selectedCountry.displayName}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Estimated ${selectedCountry.estimatedDays} via ${selectedCountry.supportedCouriers.first()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null, tint = VerifiedEmerald)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("15-Day International Return Guarantee", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Free return pickup in ${selectedCountry.displayName} • VAT ${selectedCountry.vatPercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Sold by ${product.sellerName}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Category: ${product.category} › ${product.subCategory}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Description
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Product Overview & Specifications", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Customer Reviews
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Customer Reviews (${reviews.size})", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showReviewDialog = true }) {
                            Text("Write Review")
                        }
                    }
                    if (reviews.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Be the first verified shopper to review this item.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        reviews.forEach { rev ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(rev.authorName, style = MaterialTheme.typography.titleSmall)
                                        Text("${rev.rating} ★", color = FlashAmber, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Similar Products
            if (similarProducts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Similar & Frequently Bought Together",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(similarProducts, key = { it.id }) { sim ->
                                ProductCard(
                                    product = sim,
                                    formattedPrice = viewModel.formatMoney(sim.effectivePriceUsd),
                                    formattedOriginalPrice = viewModel.formatMoney(sim.priceUsd),
                                    isWishlisted = wishlist.any { it.productId == sim.id },
                                    onClick = { onOpenProduct(sim.id) },
                                    onToggleWishlist = { viewModel.toggleWishlist(sim) },
                                    modifier = Modifier.width(166.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        var rating by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Write a Product Review") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "$star stars",
                                    tint = if (star <= rating) FlashAmber else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Your experience") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReview(product.id, rating.toLong(), comment)
                        showReviewDialog = false
                    },
                    enabled = comment.trim().length >= 2
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text("Cancel") }
            }
        )
    }
}
