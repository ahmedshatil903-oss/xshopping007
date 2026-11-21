package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.MarketCountry
import com.example.data.model.MarketplaceCategories
import com.example.data.model.Product
import com.example.data.model.SellerStore
import com.example.ui.components.OfficialBadge
import com.example.ui.components.ProductCard
import com.example.ui.components.XShoppingBrandMark
import com.example.ui.components.categoryIconFor
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.FlashAmber
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MarketplaceViewModel,
    onOpenSearch: (String?) -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenCart: () -> Unit,
    onOpenCategories: (String) -> Unit
) {
    val products by viewModel.catalogProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.allCatalogProductsUnfiltered.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val recentlyViewed by viewModel.recentlyViewed.collectAsStateWithLifecycle()
    val sellers by viewModel.featuredSellers.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val ordersState by viewModel.myOrders.collectAsStateWithLifecycle()

    var showCountrySheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableLongStateOf(14_290L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            countdownSeconds = if (countdownSeconds > 0) countdownSeconds - 1 else 14_290L
        }
    }

    val cartBadgeCount = cartItems.sumOf { it.quantity }.toInt()
    val wishIds = remember(wishlist) { wishlist.map { it.productId }.toSet() }
    val flashProducts = remember(products) { products.filter { it.flashSale || it.discountPercent >= 15 } }
    val officialProducts = remember(products) { products.filter { it.official } }
    val recentlyViewedProducts = remember(recentlyViewed, allProducts) {
        val map = allProducts.associateBy { it.id }
        recentlyViewed.sortedByDescending { it.viewedAt?.seconds ?: 0L }.mapNotNull { map[it.productId] }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header: Brand + Country Selector + Notifications + Cart
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XShoppingBrandMark(compact = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showCountrySheet = true }
                                .testTag("country_selector_button"),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Delivery location",
                                    tint = BrandOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${selectedCountry.flagEmoji} ${selectedCountry.defaultCity}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Change country",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showNotificationsSheet = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                        IconButton(
                            onClick = onOpenCart,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_cart_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (cartBadgeCount > 0) {
                                        Badge(containerColor = BrandOrange, contentColor = PureWhite) {
                                            Text(cartBadgeCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingCart,
                                    contentDescription = "Shopping cart"
                                )
                            }
                        }
                    }
                }

                // Search bar button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenSearch(null) }
                        .testTag("home_search_bar"),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = BrandOrange
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = strings.searchHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Promotional Hero Carousel
        item {
            val pagerState = rememberPagerState(pageCount = { 2 })
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val isFlash = page == 0
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenSearch(null) }
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isFlash) R.drawable.img_banner_flash_sale else R.drawable.img_banner_official_store
                            ),
                            contentDescription = if (isFlash) "Global Flash Sale" else "X-SHOPPING Official Store",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ObsidianBlack.copy(alpha = 0.88f), ObsidianBlack.copy(alpha = 0.25f))
                                    )
                                )
                                .padding(18.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = BrandOrange,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isFlash) "FLASH SALE • UP TO 30% OFF" else "X-SHOPPING OFFICIAL STORE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PureWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = if (isFlash) "Global Tech & Audio Deals" else "100% Authentic Flagship Warehouse",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureWhite
                                )
                                Text(
                                    text = if (isFlash) {
                                        val h = countdownSeconds / 3600
                                        val m = (countdownSeconds % 3600) / 60
                                        val s = countdownSeconds % 60
                                        "Ends in ${"%02d:%02d:%02d".format(h, m, s)} • Use code SAUDI20"
                                    } else {
                                        "Same/Next-day dispatch to ${selectedCountry.defaultCity}, ${selectedCountry.displayName}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureWhite.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(2) { idx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == idx) 18.dp else 7.dp, 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == idx) BrandOrange
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }
            }
        }

        // Category Quick Navigation
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(MarketplaceCategories.tree) { node ->
                    Column(
                        modifier = Modifier
                            .width(74.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenCategories(node.name) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = categoryIconFor(node.iconKey),
                                    contentDescription = node.name,
                                    tint = BrandOrange,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Flash Sale Rail
        if (flashProducts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = strings.flashSale,
                        icon = Icons.Filled.Bolt,
                        actionLabel = "See All",
                        onAction = { onOpenSearch(null) }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(flashProducts, key = { "flash_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                formattedPrice = viewModel.formatMoney(product.effectivePriceUsd),
                                formattedOriginalPrice = viewModel.formatMoney(product.priceUsd),
                                isWishlisted = wishIds.contains(product.id),
                                onClick = { onOpenProduct(product.id) },
                                onToggleWishlist = { viewModel.toggleWishlist(product) },
                                modifier = Modifier.width(172.dp)
                            )
                        }
                    }
                }
            }
        }

        // Official X-SHOPPING Store Rail
        if (officialProducts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = strings.officialStore,
                        icon = Icons.Filled.Verified,
                        actionLabel = "Official Catalog",
                        onAction = { onOpenSearch("OFFICIAL") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(officialProducts, key = { "official_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                formattedPrice = viewModel.formatMoney(product.effectivePriceUsd),
                                formattedOriginalPrice = viewModel.formatMoney(product.priceUsd),
                                isWishlisted = wishIds.contains(product.id),
                                onClick = { onOpenProduct(product.id) },
                                onToggleWishlist = { viewModel.toggleWishlist(product) },
                                modifier = Modifier.width(172.dp)
                            )
                        }
                    }
                }
            }
        }

        // Featured Seller Stores
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = strings.featuredSellers,
                    icon = Icons.Filled.Storefront,
                    actionLabel = null,
                    onAction = {}
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sellers, key = { it.id }) { seller ->
                        SellerSpotlightCard(
                            seller = seller,
                            onClick = { onOpenSearch(seller.storeName) }
                        )
                    }
                }
            }
        }

        // Recently Viewed
        if (recentlyViewedProducts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = strings.recentlyViewed,
                        icon = Icons.Filled.Star,
                        actionLabel = null,
                        onAction = {}
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyViewedProducts.take(8), key = { "recent_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                formattedPrice = viewModel.formatMoney(product.effectivePriceUsd),
                                formattedOriginalPrice = viewModel.formatMoney(product.priceUsd),
                                isWishlisted = wishIds.contains(product.id),
                                onClick = { onOpenProduct(product.id) },
                                onToggleWishlist = { viewModel.toggleWishlist(product) },
                                modifier = Modifier.width(164.dp)
                            )
                        }
                    }
                }
            }
        }

        // Trending Worldwide 2-column grid rows
        item {
            SectionHeader(
                title = "${strings.trendingNow} (${selectedCountry.displayName})",
                icon = Icons.Filled.Star,
                actionLabel = "Filter",
                onAction = { onOpenSearch(null) }
            )
        }
        val chunked = products.chunked(2)
        items(chunked) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { product ->
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
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (showCountrySheet) {
        ModalBottomSheet(onDismissRequest = { showCountrySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select Delivery Country & Market",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Catalog availability, VAT/import taxes, couriers & local payments adapt automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                MarketCountry.entries.forEach { country ->
                    val isSelected = country == selectedCountry
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.selectCountry(country, syncCurrency = true)
                                showCountrySheet = false
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandOrange else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${country.flagEmoji} ${country.displayName} (${country.defaultCity})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${country.defaultCurrency.code} • VAT ${country.vatPercent}% • ${country.estimatedDays}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandOrange
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(onDismissRequest = { showNotificationsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Marketplace Alerts", style = MaterialTheme.typography.titleLarge)
                val orders = (ordersState as? UiState.Success)?.data.orEmpty()
                if (orders.isNotEmpty()) {
                    orders.take(3).forEach { ord ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Order ${ord.orderNumber} • ${ord.status}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Tracking: ${ord.trackingNumber} via ${ord.shippingMethod}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Saudi Launch & GCC Flash Sale", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Use coupon SAUDI20 for 20% off orders over $100 USD.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BrandOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = BrandOrange,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SellerSpotlightCard(
    seller: SellerStore,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(230.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OfficialBadge(official = seller.official)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Seller rating",
                        tint = FlashAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${seller.rating}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text(
                text = seller.storeName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = seller.storeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
