package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CatalogSeedProvider
import com.example.data.model.Address
import com.example.data.model.AppLanguage
import com.example.data.model.AppStrings
import com.example.data.model.CartItem
import com.example.data.model.CurrencyCode
import com.example.data.model.GlobalConfig
import com.example.data.model.MarketCountry
import com.example.data.model.MarketplaceCoupons
import com.example.data.model.Order
import com.example.data.model.PayoutRequest
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.RecentlyViewedItem
import com.example.data.model.SellerStore
import com.example.data.model.SellerSubOrder
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.data.remote.MarketplaceRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartPricingSummary(
    val subtotalUsd: Double = 0.0,
    val discountUsd: Double = 0.0,
    val shippingUsd: Double = 0.0,
    val taxUsd: Double = 0.0,
    val totalUsd: Double = 0.0,
    val appliedCouponCode: String = "",
    val couponMessage: String? = null
)

class MarketplaceViewModel(
    private val repository: MarketplaceRepository,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserEmail: String,
    val isAdminUser: Boolean
) : ViewModel() {

    private companion object {
        const val TAG = "MarketplaceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _selectedCountry = MutableStateFlow(MarketCountry.SA)
    val selectedCountry: StateFlow<MarketCountry> = _selectedCountry.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(CurrencyCode.SAR)
    val selectedCurrency: StateFlow<CurrencyCode> = _selectedCurrency.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.EN)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _couponCodeInput = MutableStateFlow("")
    val couponCodeInput: StateFlow<String> = _couponCodeInput.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _activeProductIdForReviews = MutableStateFlow<String?>(null)

    private fun <T> Flow<T>.toUiStateFlow(initial: UiState<T> = UiState.Loading): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream error", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load data"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initial)

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Profile error", error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val globalConfig: StateFlow<GlobalConfig> = repository.observeGlobalConfig(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(GlobalConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), GlobalConfig())

    val addresses: StateFlow<List<Address>> = repository.observeAddresses(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val cartItems: StateFlow<List<CartItem>> = repository.observeCart(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val wishlistItems: StateFlow<List<WishlistItem>> = repository.observeWishlist(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val recentlyViewed: StateFlow<List<RecentlyViewedItem>> = repository.observeRecentlyViewed(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val cloudProductsFlow: Flow<List<Product>> = repository.observeApprovedProducts(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }

    val cloudProductStockMap: StateFlow<Map<String, Long>> = cloudProductsFlow
        .map { list -> list.associate { it.id to it.stock } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    val catalogProducts: StateFlow<List<Product>> = combine(
        cloudProductsFlow,
        _selectedCountry
    ) { cloud, country ->
        val merged = linkedMapOf<String, Product>()
        CatalogSeedProvider.defaultProducts.forEach { merged[it.id] = it }
        cloud.forEach { merged[it.id] = it }
        merged.values.filter { product ->
            product.availableCountries.isEmpty() || product.availableCountries.contains(country.code)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CatalogSeedProvider.defaultProducts)

    val allCatalogProductsUnfiltered: StateFlow<List<Product>> = cloudProductsFlow
        .map { cloud ->
            val merged = linkedMapOf<String, Product>()
            CatalogSeedProvider.defaultProducts.forEach { merged[it.id] = it }
            cloud.forEach { merged[it.id] = it }
            merged.values.toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CatalogSeedProvider.defaultProducts)

    val featuredSellers: StateFlow<List<SellerStore>> = repository.observeApprovedSellers(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .map { cloud ->
            val merged = linkedMapOf<String, SellerStore>()
            CatalogSeedProvider.defaultSellers.forEach { merged[it.id] = it }
            cloud.forEach { merged[it.id] = it }
            merged.values.toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CatalogSeedProvider.defaultSellers)

    val myOrders: StateFlow<UiState<List<Order>>> = repository.observeMyOrders(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .toUiStateFlow()

    val mySellerStore: StateFlow<SellerStore?> = repository.observeMySellerStore(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val mySellerProducts: StateFlow<List<Product>> = repository.observeSellerProducts(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val mySellerSubOrders: StateFlow<List<SellerSubOrder>> = repository.observeSellerSubOrders(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val mySellerPayouts: StateFlow<List<PayoutRequest>> = repository.observeSellerPayouts(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val supportTickets: StateFlow<List<SupportTicket>> = repository.observeSupportTickets(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Admin streams (only active when isAdminUser == true)
    val adminSellers: StateFlow<List<SellerStore>> = if (isAdminUser) {
        repository.observeAllSellersForAdmin(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val adminProducts: StateFlow<List<Product>> = if (isAdminUser) {
        repository.observeAllProductsForAdmin(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val adminOrders: StateFlow<List<Order>> = if (isAdminUser) {
        repository.observeAllOrdersForAdmin(currentUserId)
            .catch { emit(emptyList()) }
            .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val adminPayouts: StateFlow<List<PayoutRequest>> = if (isAdminUser) {
        repository.observeAllPayoutsForAdmin(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProductReviews: StateFlow<List<ProductReview>> = _activeProductIdForReviews
        .flatMapLatest { pid ->
            if (pid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeProductReviews(pid, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val cartPricing: StateFlow<CartPricingSummary> = combine(
        cartItems,
        _selectedCountry,
        _couponCodeInput,
        globalConfig
    ) { items, country, couponCode, cfg ->
        if (items.isEmpty()) {
            CartPricingSummary()
        } else {
            val subtotal = items.sumOf { it.lineTotalUsd }
            val coupon = MarketplaceCoupons.find(couponCode)
            var discount = 0.0
            var appliedCode = ""
            var couponMsg: String? = null
            if (couponCode.isNotBlank()) {
                if (coupon == null) {
                    couponMsg = "Invalid coupon code"
                } else if (subtotal < coupon.minSpendUsd) {
                    couponMsg = "Min spend $${coupon.minSpendUsd.toInt()} required for ${coupon.code}"
                } else {
                    discount = subtotal * (coupon.discountPercent / 100.0)
                    appliedCode = coupon.code
                    couponMsg = "${coupon.code} applied (-${coupon.discountPercent}%)"
                }
            }
            val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
            val shipping = if (afterDiscount >= country.freeShippingThresholdUsd) 0.0 else country.baseShippingUsd
            val effectiveVat = if (country == MarketCountry.SA) cfg.saVatPercent else country.vatPercent
            val taxRate = (effectiveVat + country.importDutyPercent) / 100.0
            val tax = afterDiscount * taxRate
            val total = afterDiscount + shipping + tax
            CartPricingSummary(
                subtotalUsd = subtotal,
                discountUsd = discount,
                shippingUsd = shipping,
                taxUsd = tax,
                totalUsd = total,
                appliedCouponCode = appliedCode,
                couponMessage = couponMsg
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CartPricingSummary())

    val strings: StateFlow<AppStrings> = _selectedLanguage
        .map { AppStrings.forLanguage(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppStrings.forLanguage(AppLanguage.EN))

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserName, currentUserEmail)
            userProfile.collect { profile ->
                if (profile != null) {
                    _selectedCountry.value = MarketCountry.fromCode(profile.preferredCountry)
                    _selectedCurrency.value = CurrencyCode.fromCode(profile.preferredCurrency)
                    _selectedLanguage.value = AppLanguage.fromCode(profile.preferredLanguage)
                    _darkMode.value = profile.darkMode
                }
            }
        }
    }

    fun formatMoney(amountUsd: Double): String =
        _selectedCurrency.value.formatUsd(amountUsd, globalConfig.value)

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun selectCountry(country: MarketCountry, syncCurrency: Boolean = true) {
        _selectedCountry.value = country
        val nextCurrency = if (syncCurrency) country.defaultCurrency else _selectedCurrency.value
        _selectedCurrency.value = nextCurrency
        persistPreferences()
    }

    fun selectCurrency(currency: CurrencyCode) {
        _selectedCurrency.value = currency
        persistPreferences()
    }

    fun selectLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
        persistPreferences()
    }

    fun toggleDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        persistPreferences()
    }

    private fun persistPreferences() {
        viewModelScope.launch {
            repository.updateUserPreferences(
                countryCode = _selectedCountry.value.code,
                currencyCode = _selectedCurrency.value.code,
                languageCode = _selectedLanguage.value.code,
                darkMode = _darkMode.value
            )
        }
    }

    fun applyCoupon(code: String) {
        _couponCodeInput.value = code.trim().uppercase()
    }

    fun observeReviewsFor(productId: String) {
        _activeProductIdForReviews.value = productId
        viewModelScope.launch {
            repository.recordRecentlyViewed(productId)
        }
    }

    fun addToCart(
        product: Product,
        quantity: Long,
        size: String,
        color: String,
        model: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.addToCart(product, quantity, size, color, model)
            if (result.isSuccess) {
                _snackbarMessage.value = "Added ${product.title.take(28)} to cart"
                onSuccess()
            } else {
                _snackbarMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Unable to add to cart"
            }
        }
    }

    fun updateCartQty(cartItemId: String, newQty: Long) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, newQty)
        }
    }

    fun toggleWishlist(product: Product) {
        val inWishlist = wishlistItems.value.any { it.productId == product.id }
        viewModelScope.launch {
            val res = repository.toggleWishlist(product, inWishlist)
            if (res.isSuccess) {
                _snackbarMessage.value = if (inWishlist) "Removed from wishlist" else "Saved to wishlist"
            }
        }
    }

    fun submitReview(productId: String, rating: Long, comment: String) {
        viewModelScope.launch {
            val res = repository.submitReview(productId, currentUserName, rating, comment)
            _snackbarMessage.value = if (res.isSuccess) "Review posted!" else res.exceptionOrNull()?.localizedMessage ?: "Failed to post review"
        }
    }

    fun saveAddress(address: Address, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveAddress(address)
            if (res.isSuccess) {
                _snackbarMessage.value = "Address saved"
                onSuccess()
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Invalid address"
            }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            repository.deleteAddress(addressId)
        }
    }

    fun placeOrder(
        shippingMethod: String,
        paymentMethod: String,
        shippingAddressSummary: String,
        extraCourierFeeUsd: Double,
        onOrderPlaced: (Order) -> Unit,
        onError: (String) -> Unit
    ) {
        val items = cartItems.value
        val pricing = cartPricing.value
        if (items.isEmpty()) {
            onError("Your cart is empty")
            return
        }
        viewModelScope.launch {
            val country = _selectedCountry.value
            val currency = _selectedCurrency.value
            val cfg = globalConfig.value
            val finalShippingUsd = pricing.shippingUsd + extraCourierFeeUsd
            val finalTotalUsd = pricing.totalUsd + extraCourierFeeUsd
            val points = userProfile.value?.loyaltyPoints ?: 0L
            val res = repository.placeOrder(
                cartItems = items,
                cloudProductStockMap = cloudProductStockMap.value,
                countryCode = country.code,
                currencyCode = currency.code,
                exchangeRate = currency.rateFrom(cfg),
                subtotalUsd = pricing.subtotalUsd,
                discountUsd = pricing.discountUsd,
                shippingUsd = finalShippingUsd,
                taxUsd = pricing.taxUsd,
                totalUsd = finalTotalUsd,
                couponCode = pricing.appliedCouponCode,
                shippingMethod = shippingMethod,
                paymentMethod = paymentMethod,
                shippingAddressSummary = shippingAddressSummary,
                currentLoyaltyPoints = points,
                commissionRate = cfg.defaultCommissionPercent / 100.0
            )
            res.onSuccess { order ->
                _couponCodeInput.value = ""
                onOrderPlaced(order)
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Order checkout failed")
            }
        }
    }

    fun updateCustomerOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            val res = repository.updateOrderStatus(orderId, newStatus)
            _snackbarMessage.value = if (res.isSuccess) "Order updated to $newStatus" else res.exceptionOrNull()?.localizedMessage ?: "Update failed"
        }
    }

    fun registerSellerStore(
        storeName: String,
        storeDescription: String,
        sellerType: String,
        countryCode: String,
        businessRegNo: String,
        ibanLast4: String,
        contactPhone: String
    ) {
        viewModelScope.launch {
            val res = repository.registerSellerStore(
                storeName = storeName,
                storeDescription = storeDescription,
                sellerType = sellerType,
                countryCode = countryCode,
                businessRegNo = businessRegNo,
                ibanLast4 = ibanLast4,
                contactEmail = currentUserEmail.ifBlank { "seller@x-shopping.global" },
                contactPhone = contactPhone
            )
            _snackbarMessage.value = if (res.isSuccess) {
                "Seller application submitted for admin verification!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Registration failed"
            }
        }
    }

    fun addSellerProduct(product: Product, asAdminOfficial: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.createSellerProduct(product, asAdminOfficial)
            if (res.isSuccess) {
                _snackbarMessage.value = if (asAdminOfficial) "Published to Official Store" else "Product submitted for approval"
                onSuccess()
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to create product"
            }
        }
    }

    fun advanceSellerSubOrder(subOrderId: String, nextStatus: String, courierName: String, trackingNumber: String) {
        viewModelScope.launch {
            val res = repository.updateSellerSubOrderStatus(subOrderId, nextStatus, courierName, trackingNumber)
            _snackbarMessage.value = if (res.isSuccess) "Fulfillment updated: $nextStatus" else res.exceptionOrNull()?.localizedMessage ?: "Failed to update fulfillment"
        }
    }

    fun requestSellerPayout(storeName: String, amountUsd: Double) {
        viewModelScope.launch {
            val res = repository.requestSellerPayout(storeName, amountUsd)
            _snackbarMessage.value = if (res.isSuccess) "Payout request submitted" else res.exceptionOrNull()?.localizedMessage ?: "Payout request failed"
        }
    }

    fun submitSupportTicket(subject: String, message: String) {
        viewModelScope.launch {
            val res = repository.submitSupportTicket(subject, message)
            _snackbarMessage.value = if (res.isSuccess) "Support ticket created" else res.exceptionOrNull()?.localizedMessage ?: "Failed to submit ticket"
        }
    }

    // Admin Operations
    fun adminApproveSeller(sellerId: String, status: String) {
        viewModelScope.launch {
            val res = repository.adminUpdateSellerStatus(sellerId, status)
            _snackbarMessage.value = if (res.isSuccess) "Seller status -> $status" else res.exceptionOrNull()?.localizedMessage ?: "Action failed"
        }
    }

    fun adminApproveProduct(productId: String, status: String, featured: Boolean) {
        viewModelScope.launch {
            val res = repository.adminUpdateProductApproval(productId, status, featured)
            _snackbarMessage.value = if (res.isSuccess) "Product -> $status" else res.exceptionOrNull()?.localizedMessage ?: "Action failed"
        }
    }

    fun adminProcessPayout(payoutId: String, status: String) {
        viewModelScope.launch {
            val res = repository.adminProcessPayout(payoutId, status)
            _snackbarMessage.value = if (res.isSuccess) "Payout -> $status" else res.exceptionOrNull()?.localizedMessage ?: "Action failed"
        }
    }

    fun adminUpdateGlobalConfig(config: GlobalConfig) {
        viewModelScope.launch {
            val res = repository.updateGlobalConfig(config)
            _snackbarMessage.value = if (res.isSuccess) "Global marketplace config saved" else res.exceptionOrNull()?.localizedMessage ?: "Save failed"
        }
    }

    fun adminSeedCatalog() {
        viewModelScope.launch {
            val res = repository.seedOfficialCatalogToCloud()
            _snackbarMessage.value = if (res.isSuccess) "Official Store catalog & sellers synced to Cloud Firestore" else res.exceptionOrNull()?.localizedMessage ?: "Seed failed"
        }
    }
}
