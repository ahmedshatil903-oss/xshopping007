package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Product
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.FlashAmber
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VerifiedEmerald

@Composable
fun XShoppingBrandMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 30.dp else 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(if (compact) 18.dp else 22.dp)) {
                val w = size.width
                val h = size.height
                drawLine(
                    color = BrandOrange,
                    start = Offset(w * 0.16f, h * 0.16f),
                    end = Offset(w * 0.84f, h * 0.84f),
                    strokeWidth = w * 0.16f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = PureWhite,
                    start = Offset(w * 0.84f, h * 0.16f),
                    end = Offset(w * 0.16f, h * 0.84f),
                    strokeWidth = w * 0.16f,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "X-",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = if (compact) 17.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BrandOrange
                )
                Text(
                    text = "SHOPPING",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = if (compact) 17.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (!compact) {
                Text(
                    text = "GLOBAL MARKETPLACE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun categoryIconFor(key: String): ImageVector = when (key.lowercase()) {
    "headphones", "audio", "electronics" -> Icons.Filled.Headphones
    "smartwatch", "wearables" -> Icons.Filled.Watch
    "mobile" -> Icons.Filled.PhoneIphone
    "computers" -> Icons.Filled.LaptopMac
    "fashion" -> Icons.Filled.ShoppingBag
    "home" -> Icons.Filled.Kitchen
    "furniture" -> Icons.Filled.Chair
    "beauty" -> Icons.Filled.Spa
    "health" -> Icons.Filled.MedicalServices
    "sports" -> Icons.Filled.FitnessCenter
    "toys" -> Icons.Filled.SportsEsports
    "auto" -> Icons.Filled.DirectionsCar
    "books" -> Icons.AutoMirrored.Filled.MenuBook
    "grocery" -> Icons.Filled.LocalGroceryStore
    else -> Icons.Filled.WorkOutline
}

@Composable
fun ProductVisualArtwork(
    imageKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    when (imageKey.lowercase()) {
        "headphones" -> {
            Image(
                painter = painterResource(id = R.drawable.img_product_headphones),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        "smartwatch" -> {
            Image(
                painter = painterResource(id = R.drawable.img_product_smartwatch),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        "mobile" -> {
            Image(
                painter = painterResource(id = R.drawable.img_banner_flash_sale),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        "fashion", "accessories" -> {
            Image(
                painter = painterResource(id = R.drawable.img_banner_official_store),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        else -> {
            val palette = when (imageKey.lowercase()) {
                "computers" -> listOf(Color(0xFF141820), Color(0xFF283142), BrandOrange)
                "home", "furniture" -> listOf(Color(0xFF2B231E), Color(0xFF473A30), BrandOrange)
                "sports" -> listOf(Color(0xFF132229), Color(0xFF1E3A47), BrandOrange)
                "grocery" -> listOf(Color(0xFF1E291B), Color(0xFF34472D), FlashAmber)
                "beauty" -> listOf(Color(0xFF2B1C24), Color(0xFF4D313F), BrandOrange)
                "auto" -> listOf(Color(0xFF17191E), Color(0xFF2F3440), BrandOrange)
                else -> listOf(Color(0xFF181A20), Color(0xFF2E323D), BrandOrange)
            }
            Box(
                modifier = modifier.background(
                    Brush.linearGradient(colors = listOf(palette[0], palette[1]))
                ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = palette[2].copy(alpha = 0.22f),
                        radius = size.minDimension * 0.42f,
                        center = Offset(size.width * 0.78f, size.height * 0.22f)
                    )
                    drawCircle(
                        color = PureWhite.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.55f,
                        center = Offset(size.width * 0.2f, size.height * 0.85f)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = PureWhite.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.22f)),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIconFor(imageKey),
                            contentDescription = contentDescription,
                            tint = PureWhite,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfficialBadge(
    official: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (official) BrandOrange else VerifiedEmerald.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (official) Icons.Filled.Verified else Icons.Filled.CheckCircle,
                contentDescription = if (official) "Official X-SHOPPING Store" else "Verified Seller",
                tint = if (official) PureWhite else VerifiedEmerald,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = if (official) "OFFICIAL" else "VERIFIED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (official) PureWhite else VerifiedEmerald
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    formattedPrice: String,
    formattedOriginalPrice: String,
    isWishlisted: Boolean,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("product_card_${product.id}")
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            ) {
                ProductVisualArtwork(
                    imageKey = product.imageKey,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize()
                )
                if (product.discountPercent > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = BrandOrange
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("wishlist_btn_${product.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ObsidianBlack.copy(alpha = 0.55f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                                tint = if (isWishlisted) BrandOrange else PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OfficialBadge(official = product.official)
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = FlashAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${product.rating} (${product.reviewCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedPrice,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.discountPercent > 0) {
                        Text(
                            text = formattedOriginalPrice,
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val bgColor = when (status.uppercase()) {
        "DELIVERED", "APPROVED", "PAID", "AUTHORIZED", "RESOLVED" -> VerifiedEmerald.copy(alpha = 0.14f)
        "SHIPPED", "OUT_FOR_DELIVERY", "PROCESSING", "PACKED", "CONFIRMED" -> BrandOrange.copy(alpha = 0.14f)
        "CANCELLED", "REJECTED", "SUSPENDED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        else -> FlashAmber.copy(alpha = 0.16f)
    }
    val fgColor = when (status.uppercase()) {
        "DELIVERED", "APPROVED", "PAID", "AUTHORIZED", "RESOLVED" -> VerifiedEmerald
        "SHIPPED", "OUT_FOR_DELIVERY", "PROCESSING", "PACKED", "CONFIRMED" -> BrandOrange
        "CANCELLED", "REJECTED", "SUSPENDED" -> MaterialTheme.colorScheme.error
        else -> FlashAmber
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = status.replace("_", " "),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fgColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
