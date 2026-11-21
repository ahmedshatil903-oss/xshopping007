package com.example

import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.CurrencyCode
import com.example.data.model.GlobalConfig
import com.example.data.model.MarketCountry
import com.example.data.model.MarketplaceCoupons
import com.example.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MarketplaceUnitTests {

    @Test
    fun currencyConversion_formatsCorrectly() {
        val cfg = GlobalConfig(sarRate = 3.75, bdtRate = 120.0)
        assertEquals("SAR 375.00", CurrencyCode.SAR.formatUsd(100.0, cfg))
        assertEquals("$100.00", CurrencyCode.USD.formatUsd(100.0, cfg))
        assertEquals("৳12,000", CurrencyCode.BDT.formatUsd(100.0, cfg))
    }

    @Test
    fun cartItemAndProductDiscount_computesLineTotal() {
        val product = Product(
            id = "p1",
            priceUsd = 200.0,
            discountPercent = 25L
        )
        assertEquals(150.0, product.effectivePriceUsd, 0.001)

        val item = CartItem(
            unitPriceUsd = 200.0,
            discountPercent = 25L,
            quantity = 3L
        )
        assertEquals(450.0, item.lineTotalUsd, 0.001)
    }

    @Test
    fun internationalMarkets_haveDistinctTaxesAndPayments() {
        val sa = MarketCountry.fromCode("SA")
        assertEquals(15.0, sa.vatPercent, 0.01)
        assertEquals(CurrencyCode.SAR, sa.defaultCurrency)
        assert(sa.supportedPaymentMethods.contains("Mada"))
        assert(sa.supportedPaymentMethods.contains("STC Pay"))

        val bd = MarketCountry.fromCode("BD")
        assertEquals(7.5, bd.vatPercent, 0.01)
        assertEquals(4.0, bd.importDutyPercent, 0.01)
        assert(bd.supportedPaymentMethods.any { it.contains("bKash") })
    }

    @Test
    fun couponsAndLocalization_behaveAsExpected() {
        assertNotNull(MarketplaceCoupons.find("saudi20"))
        assertNull(MarketplaceCoupons.find("INVALID_CODE"))

        assertEquals(LayoutDirection.Rtl, AppLanguage.AR.layoutDirection)
        assertEquals(LayoutDirection.Ltr, AppLanguage.EN.layoutDirection)
        assertEquals(LayoutDirection.Ltr, AppLanguage.BN.layoutDirection)
    }
}
