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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusChip
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    val sellers by viewModel.adminSellers.collectAsStateWithLifecycle()
    val products by viewModel.adminProducts.collectAsStateWithLifecycle()
    val orders by viewModel.adminOrders.collectAsStateWithLifecycle()
    val payouts by viewModel.adminPayouts.collectAsStateWithLifecycle()
    val globalConfig by viewModel.globalConfig.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!viewModel.isAdminUser) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyStateView(
                    icon = Icons.Filled.AdminPanelSettings,
                    title = "Super Admin Access Required",
                    subtitle = "Signed-in account (${viewModel.currentUserEmail}) does not hold Super Admin permissions in Firestore security rules."
                )
            }
            return@Scaffold
        }

        val gmv = orders.sumOf { it.totalUsd }
        val platformCommission = gmv * (globalConfig.defaultCommissionPercent / 100.0)
        val pendingSellers = sellers.filter { it.status == "PENDING" }
        val pendingProducts = products.filter { it.approvalStatus == "PENDING" }

        var sarRate by remember(globalConfig) { mutableStateOf(globalConfig.sarRate.toString()) }
        var vatPercent by remember(globalConfig) { mutableStateOf(globalConfig.saVatPercent.toString()) }
        var commPercent by remember(globalConfig) { mutableStateOf(globalConfig.defaultCommissionPercent.toString()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMiniCard("Platform GMV", viewModel.formatMoney(gmv), Modifier.weight(1f))
                    KpiMiniCard("Commissions", viewModel.formatMoney(platformCommission), Modifier.weight(1f))
                    KpiMiniCard("Total Orders", orders.size.toString(), Modifier.weight(1f))
                }
            }

            item {
                Button(
                    onClick = { viewModel.adminSeedCatalog() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = PureWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Sync Official Store Catalog to Cloud Firestore")
                }
            }

            // Global Config Editor
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Global Exchange, VAT & Commission Rules", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sarRate,
                                onValueChange = { sarRate = it },
                                label = { Text("SAR / USD") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = vatPercent,
                                onValueChange = { vatPercent = it },
                                label = { Text("SA VAT %") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = commPercent,
                                onValueChange = { commPercent = it },
                                label = { Text("Comm %") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.adminUpdateGlobalConfig(
                                    globalConfig.copy(
                                        sarRate = sarRate.toDoubleOrNull() ?: 3.75,
                                        saVatPercent = vatPercent.toDoubleOrNull() ?: 15.0,
                                        defaultCommissionPercent = commPercent.toDoubleOrNull() ?: 10.0
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Global Marketplace Config")
                        }
                    }
                }
            }

            // Pending Seller Approvals
            item {
                Text("Seller Approvals (${pendingSellers.size} pending)", style = MaterialTheme.typography.titleMedium)
            }
            items(sellers, key = { "adm_sel_${it.id}" }) { seller ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(seller.storeName, style = MaterialTheme.typography.titleSmall)
                            StatusChip(seller.status)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.adminApproveSeller(seller.id, "APPROVED") }) {
                                Text("Approve")
                            }
                            OutlinedButton(onClick = { viewModel.adminApproveSeller(seller.id, "SUSPENDED") }) {
                                Text("Suspend")
                            }
                        }
                    }
                }
            }

            // Pending Product Approvals
            item {
                Text("Product Moderation (${pendingProducts.size} pending)", style = MaterialTheme.typography.titleMedium)
            }
            items(products.take(15), key = { "adm_prod_${it.id}" }) { prod ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(prod.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            StatusChip(prod.approvalStatus)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.adminApproveProduct(prod.id, "APPROVED", true) }) {
                                Text("Approve & Feature")
                            }
                            OutlinedButton(onClick = { viewModel.adminApproveProduct(prod.id, "REJECTED", false) }) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }

            // Payouts
            if (payouts.isNotEmpty()) {
                item {
                    Text("Seller Payout Requests (${payouts.size})", style = MaterialTheme.typography.titleMedium)
                }
                items(payouts, key = { "adm_pay_${it.id}" }) { pay ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pay.storeName, style = MaterialTheme.typography.titleSmall)
                                Text(viewModel.formatMoney(pay.amountUsd), color = BrandOrange)
                            }
                            if (pay.status == "PENDING") {
                                Button(onClick = { viewModel.adminProcessPayout(pay.id, "PAID") }) {
                                    Text("Mark Paid")
                                }
                            } else {
                                StatusChip(pay.status)
                            }
                        }
                    }
                }
            }
        }
    }
}
