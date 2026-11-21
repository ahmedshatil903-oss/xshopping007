package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val loyaltyPoints: Long = 0L,
    val referralCode: String = "",
    val preferredCountry: String = "SA",
    val preferredCurrency: String = "SAR",
    val preferredLanguage: String = "en",
    val darkMode: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Address(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val recipientName: String = "",
    val phone: String = "",
    val countryCode: String = "SA",
    val city: String = "",
    val streetLine: String = "",
    val postalCode: String = "",
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val official: Boolean = false,
    val unitPriceUsd: Double = 0.0,
    val discountPercent: Long = 0L,
    val quantity: Long = 1L,
    val selectedSize: String = "",
    val selectedColor: String = "",
    val selectedModel: String = "",
    val imageKey: String = "electronics",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val discountedUnitPriceUsd: Double
        get() = unitPriceUsd * (1.0 - (discountPercent.coerceIn(0L, 90L) / 100.0))

    val lineTotalUsd: Double
        get() = discountedUnitPriceUsd * quantity.coerceAtLeast(1L)
}

@Keep
@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val unitPriceUsd: Double = 0.0,
    val imageKey: String = "electronics",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RecentlyViewedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    @ServerTimestamp val viewedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SellerStore(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val storeName: String = "",
    val storeDescription: String = "",
    val sellerType: String = "BUSINESS",
    val official: Boolean = false,
    val status: String = "PENDING",
    val countryCode: String = "SA",
    val rating: Double = 5.0,
    val totalSalesUsd: Double = 0.0,
    val commissionRate: Double = 0.10,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SellerPrivateKyc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val businessRegNo: String = "",
    val ibanLast4: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val official: Boolean = false,
    val approvalStatus: String = "APPROVED",
    val title: String = "",
    val description: String = "",
    val category: String = "Electronics",
    val subCategory: String = "Audio",
    val brand: String = "X-Tech",
    val priceUsd: Double = 0.0,
    val discountPercent: Long = 0L,
    val stock: Long = 0L,
    val rating: Double = 4.5,
    val reviewCount: Long = 0L,
    val imageKey: String = "electronics",
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val models: List<String> = emptyList(),
    val availableCountries: List<String> = listOf("SA", "AE", "BD", "US", "GB", "EU"),
    val featured: Boolean = false,
    val flashSale: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectivePriceUsd: Double
        get() = priceUsd * (1.0 - (discountPercent.coerceIn(0L, 90L) / 100.0))
}

@Keep
@IgnoreExtraProperties
data class ProductReview(
    @DocumentId val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val countryCode: String = "SA",
    val currencyCode: String = "SAR",
    val exchangeRate: Double = 3.75,
    val subtotalUsd: Double = 0.0,
    val discountUsd: Double = 0.0,
    val shippingUsd: Double = 0.0,
    val taxUsd: Double = 0.0,
    val totalUsd: Double = 0.0,
    val couponCode: String = "",
    val shippingMethod: String = "",
    val trackingNumber: String = "",
    val paymentMethod: String = "",
    val paymentStatus: String = "AUTHORIZED",
    val status: String = "CONFIRMED",
    val shippingAddressSummary: String = "",
    val itemCount: Long = 1L,
    val itemsSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SellerSubOrder(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val grossUsd: Double = 0.0,
    val commissionUsd: Double = 0.0,
    val netUsd: Double = 0.0,
    val itemCount: Long = 1L,
    val itemsSummary: String = "",
    val courierName: String = "",
    val trackingNumber: String = "",
    val status: String = "CONFIRMED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PayoutRequest(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val storeName: String = "",
    val amountUsd: Double = 0.0,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GlobalConfig(
    @DocumentId val id: String = "marketplace",
    val sarRate: Double = 3.75,
    val aedRate: Double = 3.67,
    val bdtRate: Double = 119.50,
    val eurRate: Double = 0.92,
    val gbpRate: Double = 0.79,
    val saVatPercent: Double = 15.0,
    val defaultCommissionPercent: Double = 10.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
