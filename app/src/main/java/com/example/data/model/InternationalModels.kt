package com.example.data.model

import androidx.compose.ui.unit.LayoutDirection
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

enum class MarketCountry(
    val code: String,
    val displayName: String,
    val defaultCity: String,
    val flagEmoji: String,
    val defaultCurrency: CurrencyCode,
    val vatPercent: Double,
    val importDutyPercent: Double,
    val freeShippingThresholdUsd: Double,
    val baseShippingUsd: Double,
    val estimatedDays: String,
    val supportedPaymentMethods: List<String>,
    val supportedCouriers: List<String>
) {
    SA(
        code = "SA",
        displayName = "Saudi Arabia",
        defaultCity = "Riyadh",
        flagEmoji = "🇸🇦",
        defaultCurrency = CurrencyCode.SAR,
        vatPercent = 15.0,
        importDutyPercent = 0.0,
        freeShippingThresholdUsd = 80.0,
        baseShippingUsd = 7.50,
        estimatedDays = "1–2 Business Days",
        supportedPaymentMethods = listOf("Mada", "STC Pay", "Apple Pay / Google Pay", "Visa / Mastercard", "Cash on Delivery"),
        supportedCouriers = listOf("X-SHOPPING Express", "Aramex Priority", "DHL Express")
    ),
    AE(
        code = "AE",
        displayName = "United Arab Emirates",
        defaultCity = "Dubai",
        flagEmoji = "🇦🇪",
        defaultCurrency = CurrencyCode.AED,
        vatPercent = 5.0,
        importDutyPercent = 0.0,
        freeShippingThresholdUsd = 90.0,
        baseShippingUsd = 8.00,
        estimatedDays = "1–3 Business Days",
        supportedPaymentMethods = listOf("Apple Pay / Google Pay", "Visa / Mastercard", "Cash on Delivery"),
        supportedCouriers = listOf("Aramex Priority", "DHL Express", "FedEx International")
    ),
    BD(
        code = "BD",
        displayName = "Bangladesh",
        defaultCity = "Dhaka",
        flagEmoji = "🇧🇩",
        defaultCurrency = CurrencyCode.BDT,
        vatPercent = 7.5,
        importDutyPercent = 4.0,
        freeShippingThresholdUsd = 75.0,
        baseShippingUsd = 5.50,
        estimatedDays = "2–4 Business Days",
        supportedPaymentMethods = listOf("bKash / Nagad Wallet", "Visa / Mastercard", "Google Pay", "Cash on Delivery"),
        supportedCouriers = listOf("X-SHOPPING Courier", "DHL Express", "FedEx International")
    ),
    US(
        code = "US",
        displayName = "United States",
        defaultCity = "New York",
        flagEmoji = "🇺🇸",
        defaultCurrency = CurrencyCode.USD,
        vatPercent = 8.25,
        importDutyPercent = 0.0,
        freeShippingThresholdUsd = 75.0,
        baseShippingUsd = 9.00,
        estimatedDays = "2–4 Business Days",
        supportedPaymentMethods = listOf("Google Pay", "Stripe Card (Visa / MC / Amex)", "PayPal"),
        supportedCouriers = listOf("UPS Ground", "FedEx Priority", "DHL Express")
    ),
    GB(
        code = "GB",
        displayName = "United Kingdom",
        defaultCity = "London",
        flagEmoji = "🇬🇧",
        defaultCurrency = CurrencyCode.GBP,
        vatPercent = 20.0,
        importDutyPercent = 2.0,
        freeShippingThresholdUsd = 100.0,
        baseShippingUsd = 10.00,
        estimatedDays = "2–4 Business Days",
        supportedPaymentMethods = listOf("Google Pay", "Adyen Card (Visa / Mastercard)", "PayPal"),
        supportedCouriers = listOf("DHL Express", "FedEx International", "UPS Express")
    ),
    EU(
        code = "EU",
        displayName = "European Union",
        defaultCity = "Berlin",
        flagEmoji = "🇪🇺",
        defaultCurrency = CurrencyCode.EUR,
        vatPercent = 19.0,
        importDutyPercent = 2.5,
        freeShippingThresholdUsd = 110.0,
        baseShippingUsd = 11.00,
        estimatedDays = "3–5 Business Days",
        supportedPaymentMethods = listOf("Adyen Card (Visa / Mastercard)", "Google Pay", "PayPal"),
        supportedCouriers = listOf("DHL Europlus", "UPS Standard", "FedEx International")
    );

    companion object {
        fun fromCode(code: String): MarketCountry =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SA
    }
}

enum class CurrencyCode(val code: String, val symbol: String) {
    SAR("SAR", "SAR "),
    USD("USD", "$"),
    BDT("BDT", "৳"),
    AED("AED", "AED "),
    EUR("EUR", "€"),
    GBP("GBP", "£");

    fun rateFrom(config: GlobalConfig): Double = when (this) {
        SAR -> config.sarRate
        USD -> 1.0
        BDT -> config.bdtRate
        AED -> config.aedRate
        EUR -> config.eurRate
        GBP -> config.gbpRate
    }

    fun formatUsd(amountUsd: Double, config: GlobalConfig): String {
        val converted = amountUsd * rateFrom(config)
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (this@CurrencyCode == BDT) 0 else 2
            maximumFractionDigits = if (this@CurrencyCode == BDT) 0 else 2
        }
        val value = if (this == BDT) converted.roundToLong() else converted
        return "$symbol${nf.format(value)}"
    }

    companion object {
        fun fromCode(code: String): CurrencyCode =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SAR
    }
}

enum class AppLanguage(
    val code: String,
    val label: String,
    val layoutDirection: LayoutDirection
) {
    EN("en", "English", LayoutDirection.Ltr),
    AR("ar", "العربية", LayoutDirection.Rtl),
    BN("bn", "বাংলা", LayoutDirection.Ltr);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

data class CategoryNode(
    val name: String,
    val subCategories: List<String>,
    val iconKey: String
)

object MarketplaceCategories {
    val tree: List<CategoryNode> = listOf(
        CategoryNode("Electronics", listOf("Audio", "Wearables", "Cameras", "Smart Home"), "electronics"),
        CategoryNode("Mobile Phones", listOf("Flagship Phones", "Tablets", "Chargers"), "mobile"),
        CategoryNode("Computers", listOf("Laptops", "Monitors", "Keyboards", "Accessories"), "computers"),
        CategoryNode("Fashion", listOf("Men's Fashion", "Women's Fashion", "Kids' Fashion", "Watches"), "fashion"),
        CategoryNode("Home & Kitchen", listOf("Coffee & Espresso", "Appliances", "Furniture", "Cookware"), "home"),
        CategoryNode("Beauty & Personal Care", listOf("Skincare", "Fragrances", "Grooming"), "beauty"),
        CategoryNode("Health & Household", listOf("Supplements", "Air Purifiers", "First Aid"), "health"),
        CategoryNode("Sports & Outdoors", listOf("Running", "Fitness Gear", "Camping"), "sports"),
        CategoryNode("Toys & Games", listOf("STEM Kits", "Board Games", "Consoles"), "toys"),
        CategoryNode("Automotive", listOf("Dash Cams", "Car Care", "EV Chargers"), "auto"),
        CategoryNode("Books", listOf("Tech & Business", "Fiction", "Design"), "books"),
        CategoryNode("Grocery", listOf("Specialty Coffee", "Dates & Honey", "Snacks"), "grocery"),
        CategoryNode("Accessories", listOf("Bags & Backpacks", "Travel Gear", "Sunglasses"), "accessories")
    )
}

data class CouponDefinition(
    val code: String,
    val discountPercent: Int,
    val description: String,
    val minSpendUsd: Double
)

object MarketplaceCoupons {
    val active = listOf(
        CouponDefinition("XGLOBAL10", 10, "10% off international orders", 30.0),
        CouponDefinition("WELCOME15", 15, "15% off first global purchase", 50.0),
        CouponDefinition("SAUDI20", 20, "20% off Riyadh & GCC launch promo", 100.0)
    )

    fun find(code: String): CouponDefinition? =
        active.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
}

data class AppStrings(
    val tagline: String,
    val home: String,
    val categories: String,
    val cart: String,
    val account: String,
    val searchHint: String,
    val flashSale: String,
    val officialStore: String,
    val featuredSellers: String,
    val trendingNow: String,
    val recentlyViewed: String,
    val addToCart: String,
    val buyNow: String,
    val checkout: String,
    val sellerCenter: String,
    val adminPortal: String,
    val deliverTo: String,
    val signOut: String
) {
    companion object {
        fun forLanguage(lang: AppLanguage): AppStrings = when (lang) {
            AppLanguage.EN -> AppStrings(
                tagline = "Your Global Shopping Destination",
                home = "Home",
                categories = "Categories",
                cart = "Cart",
                account = "Account",
                searchHint = "Search global products, brands, stores...",
                flashSale = "Flash Sale",
                officialStore = "X-SHOPPING Official",
                featuredSellers = "Featured Global Sellers",
                trendingNow = "Trending Worldwide",
                recentlyViewed = "Recently Viewed",
                addToCart = "Add to Cart",
                buyNow = "Buy Now",
                checkout = "Proceed to Checkout",
                sellerCenter = "Seller Center",
                adminPortal = "Admin Console",
                deliverTo = "Deliver to",
                signOut = "Sign Out"
            )
            AppLanguage.AR -> AppStrings(
                tagline = "وجهتك العالمية للتسوق",
                home = "الرئيسية",
                categories = "الأقسام",
                cart = "السلة",
                account = "حسابي",
                searchHint = "ابحث عن المنتجات والعلامات التجارية...",
                flashSale = "عروض خاطفة",
                officialStore = "متجر X-SHOPPING الرسمي",
                featuredSellers = "بائعون مميزون",
                trendingNow = "الأكثر رواجاً",
                recentlyViewed = "شوهدت مؤخراً",
                addToCart = "أضف إلى السلة",
                buyNow = "اشترِ الآن",
                checkout = "إتمام الشراء",
                sellerCenter = "مركز البائع",
                adminPortal = "لوحة الإدارة",
                deliverTo = "التوصيل إلى",
                signOut = "تسجيل الخروج"
            )
            AppLanguage.BN -> AppStrings(
                tagline = "আপনার গ্লোবাল শপিং গন্তব্য",
                home = "হোম",
                categories = "ক্যাটাগরি",
                cart = "কার্ট",
                account = "অ্যাকাউন্ট",
                searchHint = "গ্লোবাল পণ্য, ব্র্যান্ড ও স্টোর খুঁজুন...",
                flashSale = "ফ্ল্যাশ সেল",
                officialStore = "X-SHOPPING অফিশিয়াল",
                featuredSellers = "সেরা বিক্রেতা",
                trendingNow = "ট্রেন্ডিং পণ্য",
                recentlyViewed = "সম্প্রতি দেখা",
                addToCart = "কার্টে যোগ করুন",
                buyNow = "এখনই কিনুন",
                checkout = "চেকআউট করুন",
                sellerCenter = "সেলার সেন্টার",
                adminPortal = "অ্যাডমিন প্যানেল",
                deliverTo = "ডেলিভারি",
                signOut = "সাইন আউট"
            )
        }
    }
}
