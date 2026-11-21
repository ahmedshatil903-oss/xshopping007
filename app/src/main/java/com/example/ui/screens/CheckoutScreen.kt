package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Address
import com.example.data.model.Order
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onOrderConfirmedGoHome: () -> Unit
) {
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    val pricing by viewModel.cartPricing.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()

    var currentStep by remember { mutableIntStateOf(0) } // 0: Address, 1: Shipping, 2: Payment, 3: Confirm
    var selectedAddressId by remember { mutableStateOf("") }
    var selectedCourier by remember(selectedCountry) { mutableStateOf(selectedCountry.supportedCouriers.first()) }
    var selectedPayment by remember(selectedCountry) { mutableStateOf(selectedCountry.supportedPaymentMethods.first()) }
    var paymentAuthorizedConfirmed by remember { mutableStateOf(false) }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var checkoutError by remember { mutableStateOf<String?>(null) }
    var confirmedOrder by remember { mutableStateOf<Order?>(null) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    val activeAddress = remember(addresses, selectedAddressId) {
        addresses.firstOrNull { it.id == selectedAddressId } ?: addresses.firstOrNull()
    }
    val extraCourierFeeUsd = if (selectedCourier.contains("DHL") || selectedCourier.contains("FedEx")) 4.50 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (confirmedOrder != null) "Order Confirmed" else "Secure Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (confirmedOrder != null) {
            val ord = confirmedOrder!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Order Confirmed",
                    tint = VerifiedEmerald,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text("Order Placed Successfully!", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Order ${ord.orderNumber} • Tracking ${ord.trackingNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandOrange
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryRow("Courier", ord.shippingMethod)
                        SummaryRow("Payment", "${ord.paymentMethod} (${ord.paymentStatus})")
                        SummaryRow("Ship To", ord.shippingAddressSummary)
                        SummaryRow("Total Paid", viewModel.formatMoney(ord.totalUsd), BrandOrange)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onOrderConfirmedGoHome,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue Shopping")
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stepper Indicator
            item {
                val steps = listOf("Address", "Shipping", "Payment", "Review")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    steps.forEachIndexed { idx, label ->
                        val active = idx == currentStep
                        val done = idx < currentStep
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { if (idx <= currentStep) currentStep = idx },
                            color = when {
                                active -> BrandOrange
                                done -> VerifiedEmerald.copy(alpha = 0.16f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = "${idx + 1}. $label",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    active -> PureWhite
                                    done -> VerifiedEmerald
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            // Step 0: Delivery Address
            if (currentStep == 0) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BrandOrange)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Select Delivery Address", style = MaterialTheme.typography.titleMedium)
                                }
                                TextButton(onClick = { showAddAddressDialog = true }) {
                                    Text("+ Add New")
                                }
                            }
                            if (addresses.isEmpty()) {
                                Text(
                                    text = "No saved addresses yet. Add an international shipping address to continue.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { showAddAddressDialog = true },
                                    modifier = Modifier.fillMaxWidth().testTag("checkout_add_address_button")
                                ) {
                                    Text("Add Shipping Address")
                                }
                            } else {
                                addresses.forEach { addr ->
                                    val selected = activeAddress?.id == addr.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedAddressId = addr.id },
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, if (selected) BrandOrange else MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = selected, onClick = { selectedAddressId = addr.id })
                                            Column {
                                                Text("${addr.label} • ${addr.recipientName}", style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    text = "${addr.streetLine}, ${addr.city}, ${addr.countryCode} • ${addr.phone}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = { currentStep = 1 },
                                enabled = activeAddress != null,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Continue to Shipping")
                            }
                        }
                    }
                }
            }

            // Step 1: Shipping Courier
            if (currentStep == 1) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = BrandOrange)
                                Spacer(Modifier.width(6.dp))
                                Text("Shipping Method (${selectedCountry.displayName})", style = MaterialTheme.typography.titleMedium)
                            }
                            selectedCountry.supportedCouriers.forEach { courier ->
                                val isExpress = courier.contains("DHL") || courier.contains("FedEx")
                                val surcharge = if (isExpress) 4.50 else 0.0
                                val selected = selectedCourier == courier
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedCourier = courier },
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (selected) BrandOrange else MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selected = selected, onClick = { selectedCourier = courier })
                                            Column {
                                                Text(courier, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    text = if (isExpress) "Priority Tracked • ${selectedCountry.estimatedDays}" else "Standard Tracked • ${selectedCountry.estimatedDays}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (surcharge == 0.0) "Included" else "+${viewModel.formatMoney(surcharge)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BrandOrange
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { currentStep = 2 },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Continue to Payment")
                            }
                        }
                    }
                }
            }

            // Step 2: Payment Method & Authorization Gate
            if (currentStep == 2) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = BrandOrange)
                                Spacer(Modifier.width(6.dp))
                                Text("Regional Payment Method (${selectedCountry.code})", style = MaterialTheme.typography.titleMedium)
                            }
                            selectedCountry.supportedPaymentMethods.forEach { method ->
                                val selected = selectedPayment == method
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedPayment = method
                                            paymentAuthorizedConfirmed = false
                                        },
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (selected) BrandOrange else MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = selected, onClick = {
                                            selectedPayment = method
                                            paymentAuthorizedConfirmed = false
                                        })
                                        Column {
                                            Text(method, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = if (method.contains("Cash on Delivery")) {
                                                    "Authorized COD verification upon delivery"
                                                } else {
                                                    "Tokenized gateway authorization (raw card never stored)"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { paymentAuthorizedConfirmed = !paymentAuthorizedConfirmed },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = paymentAuthorizedConfirmed,
                                    onCheckedChange = { paymentAuthorizedConfirmed = it }
                                )
                                Text(
                                    text = "Authorize $selectedPayment payment for ${viewModel.formatMoney(pricing.totalUsd + extraCourierFeeUsd)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = { currentStep = 3 },
                                enabled = paymentAuthorizedConfirmed,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Review Order Summary")
                            }
                        }
                    }
                }
            }

            // Step 3: Order Summary & Place Order
            if (currentStep == 3) {
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
                            Text("Final Order Summary", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${cartItems.sumOf { it.quantity }} item(s) • Ship to ${activeAddress?.city}, ${activeAddress?.countryCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider()
                            SummaryRow("Courier", selectedCourier)
                            SummaryRow("Payment", selectedPayment)
                            SummaryRow("Subtotal", viewModel.formatMoney(pricing.subtotalUsd))
                            if (pricing.discountUsd > 0) {
                                SummaryRow("Coupon (${pricing.appliedCouponCode})", "-${viewModel.formatMoney(pricing.discountUsd)}", VerifiedEmerald)
                            }
                            SummaryRow("Shipping", viewModel.formatMoney(pricing.shippingUsd + extraCourierFeeUsd))
                            SummaryRow("VAT & Duties", viewModel.formatMoney(pricing.taxUsd))
                            HorizontalDivider()
                            SummaryRow("Total Payable", viewModel.formatMoney(pricing.totalUsd + extraCourierFeeUsd), BrandOrange)

                            if (checkoutError != null) {
                                Text(
                                    text = checkoutError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val addr = activeAddress ?: return@Button
                                    isPlacingOrder = true
                                    checkoutError = null
                                    val summary = "${addr.recipientName}, ${addr.streetLine}, ${addr.city}, ${addr.countryCode}"
                                    viewModel.placeOrder(
                                        shippingMethod = selectedCourier,
                                        paymentMethod = selectedPayment,
                                        shippingAddressSummary = summary,
                                        extraCourierFeeUsd = extraCourierFeeUsd,
                                        onOrderPlaced = { order ->
                                            isPlacingOrder = false
                                            confirmedOrder = order
                                        },
                                        onError = { err ->
                                            isPlacingOrder = false
                                            checkoutError = err
                                        }
                                    )
                                },
                                enabled = !isPlacingOrder && paymentAuthorizedConfirmed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("place_order_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isPlacingOrder) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = PureWhite, strokeWidth = 2.dp)
                                } else {
                                    Text("Confirm & Place Order")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAddressDialog) {
        var label by remember { mutableStateOf("Home") }
        var recipient by remember { mutableStateOf(viewModel.currentUserName) }
        var phone by remember { mutableStateOf("+966501234567") }
        var city by remember { mutableStateOf(selectedCountry.defaultCity) }
        var street by remember { mutableStateOf("") }
        var postal by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("New Delivery Address (${selectedCountry.displayName})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home / Office)") }, singleLine = true)
                    OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
                    OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Street, District & Building") }, singleLine = true)
                    OutlinedTextField(value = postal, onValueChange = { postal = it }, label = { Text("Postal / ZIP Code") }, singleLine = true)
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
                                streetLine = street,
                                postalCode = postal,
                                defaultAddress = true
                            )
                        ) {
                            showAddAddressDialog = false
                        }
                    },
                    enabled = recipient.isNotBlank() && phone.length >= 4 && city.isNotBlank() && street.length >= 2
                ) {
                    Text("Save Address")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddressDialog = false }) { Text("Cancel") }
            }
        )
    }
}
