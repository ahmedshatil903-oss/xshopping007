package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.OfficialBadge
import com.example.ui.components.ProductVisualArtwork
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VerifiedEmerald

@Composable
fun CartScreen(
    viewModel: MarketplaceViewModel,
    onContinueShopping: () -> Unit,
    onProceedToCheckout: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val pricing by viewModel.cartPricing.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val couponInput by viewModel.couponCodeInput.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()

    var couponField by remember(couponInput) { mutableStateOf(couponInput) }

    if (cartItems.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyStateView(
                icon = Icons.Filled.ShoppingCart,
                title = "Your X-SHOPPING Cart is Empty",
                subtitle = "Explore Official Store & verified global sellers to add items."
            )
            Button(
                onClick = onContinueShopping,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Explore Global Deals")
            }
        }
        return
    }

    val sellerGroups = remember(cartItems) { cartItems.groupBy { it.sellerName } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Shopping Cart (${cartItems.sumOf { it.quantity }})", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Grouped by seller • Shipping to ${selectedCountry.flagEmoji} ${selectedCountry.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        sellerGroups.forEach { (sellerName, items) ->
            item(key = "header_$sellerName") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OfficialBadge(official = items.first().official)
                    Text(sellerName, style = MaterialTheme.typography.titleSmall)
                }
            }
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductVisualArtwork(
                            imageKey = item.imageKey,
                            contentDescription = item.productTitle,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.productTitle,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val variantText = listOf(item.selectedColor, item.selectedSize, item.selectedModel)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            if (variantText.isNotBlank()) {
                                Text(
                                    text = variantText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = viewModel.formatMoney(item.lineTotalUsd),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandOrange
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartQty(item.id, item.quantity - 1) }
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                                }
                                Text(item.quantity.toString(), style = MaterialTheme.typography.titleSmall)
                                IconButton(
                                    onClick = { viewModel.updateCartQty(item.id, item.quantity + 1) }
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.updateCartQty(item.id, 0) }
                                ) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove item",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Coupon Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Apply Promo Coupon", style = MaterialTheme.typography.titleSmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = couponField,
                            onValueChange = { couponField = it },
                            placeholder = { Text("XGLOBAL10 / WELCOME15 / SAUDI20") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = { viewModel.applyCoupon(couponField) }) {
                            Text("Apply")
                        }
                    }
                    if (pricing.couponMessage != null) {
                        Text(
                            text = pricing.couponMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pricing.appliedCouponCode.isNotBlank()) VerifiedEmerald else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // International Tax, Shipping & Total Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryRow("Subtotal", viewModel.formatMoney(pricing.subtotalUsd))
                    if (pricing.discountUsd > 0) {
                        SummaryRow("Coupon Discount", "-${viewModel.formatMoney(pricing.discountUsd)}", VerifiedEmerald)
                    }
                    SummaryRow(
                        "Shipping (${selectedCountry.code})",
                        if (pricing.shippingUsd == 0.0) "FREE" else viewModel.formatMoney(pricing.shippingUsd)
                    )
                    SummaryRow(
                        "VAT (${selectedCountry.vatPercent}%) & Duty (${selectedCountry.importDutyPercent}%)",
                        viewModel.formatMoney(pricing.taxUsd)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Payable Total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = viewModel.formatMoney(pricing.totalUsd),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = BrandOrange
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onProceedToCheckout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("proceed_to_checkout_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite)
                    ) {
                        Text(strings.checkout, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}
