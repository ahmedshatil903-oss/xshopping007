package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Address
import com.example.data.model.AppLanguage
import com.example.data.model.CurrencyCode
import com.example.data.model.MarketCountry
import com.example.data.remote.AuthManager
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusChip
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.PureWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
    viewModel: MarketplaceViewModel,
    onOpenSellerCenter: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onOpenProduct: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val ordersState by viewModel.myOrders.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val tickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()

    var showSupportDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    val orders = (ordersState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header + Loyalty & Referral
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BrandOrange,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = viewModel.currentUserName.take(1).uppercase().ifBlank { "X" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureWhite
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(viewModel.currentUserName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = viewModel.currentUserEmail.ifBlank { "Google Account Verified" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Loyalty, contentDescription = null, tint = BrandOrange)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Loyalty Balance", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "${profile?.loyaltyPoints ?: 150} pts",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BrandOrange
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val code = profile?.referralCode ?: "XSHOP-GLOBAL"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Shop globally on X-SHOPPING with my referral code $code!"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share referral"))
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Refer & Earn")
                        }
                    }
                }
            }
        }

        // Seller Center & Super Admin Portals
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenSellerCenter)
                        .testTag("open_seller_center_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BrandOrange)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = BrandOrange)
                        Text(strings.sellerCenter, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Manage store, inventory & payouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenAdminDashboard)
                        .testTag("open_admin_portal_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = BrandOrange)
                        Text(strings.adminPortal, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (viewModel.isAdminUser) "Super Admin active" else "Role-protected console",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // International Settings: Country, Currency, Language (RTL), Dark Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("International & Display Settings", style = MaterialTheme.typography.titleMedium)

                    Text("Country Market", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MarketCountry.entries.forEach { c ->
                            FilterChip(
                                selected = selectedCountry == c,
                                onClick = { viewModel.selectCountry(c, syncCurrency = true) },
                                label = { Text("${c.flagEmoji} ${c.code}") }
                            )
                        }
                    }

                    Text("Currency", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CurrencyCode.entries.forEach { cur ->
                            FilterChip(
                                selected = selectedCurrency == cur,
                                onClick = { viewModel.selectCurrency(cur) },
                                label = { Text(cur.code) }
                            )
                        }
                    }

                    Text("Language (Arabic enables RTL)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { viewModel.selectLanguage(lang) },
                                label = { Text(lang.label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode Theme", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                }
            }
        }

        // Orders & Live Tracking
        item {
            Text("My Orders & Tracking (${orders.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (orders.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Filled.Inventory2,
                    title = "No orders placed yet",
                    subtitle = "Your multi-vendor order history and live courier tracking appear here."
                )
            }
        } else {
            items(orders, key = { it.id }) { order ->
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.orderNumber, style = MaterialTheme.typography.titleSmall)
                            StatusChip(order.status)
                        }
                        Text(
                            text = order.itemsSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${order.shippingMethod} • ${order.trackingNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandOrange
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total: ${viewModel.formatMoney(order.totalUsd)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (order.status in listOf("PENDING", "CONFIRMED")) {
                                TextButton(onClick = { viewModel.updateCustomerOrderStatus(order.id, "CANCELLED") }) {
                                    Text("Cancel Order")
                                }
                            } else if (order.status == "DELIVERED") {
                                TextButton(onClick = { viewModel.updateCustomerOrderStatus(order.id, "RETURNED") }) {
                                    Text("Request Return")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wishlist Quick List
        item {
            Text("Saved Wishlist (${wishlist.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (wishlist.isNotEmpty()) {
            items(wishlist, key = { it.id }) { w ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenProduct(w.productId) },
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(w.productTitle, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(viewModel.formatMoney(w.unitPriceUsd), style = MaterialTheme.typography.labelLarge, color = BrandOrange)
                    }
                }
            }
        }

        // Saved Addresses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Addresses (${addresses.size})", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAddAddressDialog = true }) { Text("+ Add") }
            }
        }
        items(addresses, key = { it.id }) { addr ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BrandOrange)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${addr.label} • ${addr.recipientName}", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${addr.streetLine}, ${addr.city}, ${addr.countryCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.deleteAddress(addr.id) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete address")
                    }
                }
            }
        }

        // Support & Sign Out
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showSupportDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Support (${tickets.size})")
                }
                Button(
                    onClick = {
                        AuthManager.signOut(credentialManager, scope)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = PureWhite)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.signOut)
                }
            }
        }
    }

    if (showSupportDialog) {
        var subject by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Customer Support") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe your issue") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitSupportTicket(subject, message)
                        showSupportDialog = false
                    },
                    enabled = subject.trim().length >= 2 && message.trim().length >= 4
                ) { Text("Send Ticket") }
            },
            dismissButton = { TextButton(onClick = { showSupportDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddAddressDialog) {
        var label by remember { mutableStateOf("Home") }
        var recipient by remember { mutableStateOf(viewModel.currentUserName) }
        var phone by remember { mutableStateOf("+966501234567") }
        var city by remember { mutableStateOf(selectedCountry.defaultCity) }
        var street by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("Add Shipping Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, singleLine = true)
                    OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
                    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Street Line") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveAddress(
                            Address(
                                label = label,
                                recipientName = recipient,
                                phone = phone,
                                countryCode = selectedCountry.code,
                                city = city,
                                streetLine = street
                            )
                        ) { showAddAddressDialog = false }
                    },
                    enabled = recipient.isNotBlank() && phone.length >= 4 && city.isNotBlank() && street.length >= 2
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddAddressDialog = false }) { Text("Cancel") } }
        )
    }
}
