package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.CatalogSeedProvider
import com.example.data.model.Address
import com.example.data.model.CartItem
import com.example.data.model.GlobalConfig
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> safeSnapshotFlow(
        path: String,
        op: OperationType,
        uid: String,
        flowProvider: () -> Flow<T>
    ): Flow<T> = flow {
        emitAll(
            flowProvider().catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, op, path)
                }
                throw error
            }
        )
    }

    // --- USER PROFILE & PREFERENCES ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        safeSnapshotFlow("users/$userId", OperationType.GET, userId) {
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun ensureUserProfile(displayName: String, email: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val cleanName = displayName.ifBlank { "Global Shopper" }.take(80)
                val refCode = "XSHOP-${uid.take(6).uppercase()}"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "email" to email.take(160),
                    "loyaltyPoints" to 150L,
                    "referralCode" to refCode,
                    "preferredCountry" to "SA",
                    "preferredCurrency" to "SAR",
                    "preferredLanguage" to "en",
                    "darkMode" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        countryCode: String,
        currencyCode: String,
        languageCode: String,
        darkMode: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredCountry" to countryCode,
                    "preferredCurrency" to currencyCode,
                    "preferredLanguage" to languageCode,
                    "darkMode" to darkMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- GLOBAL MARKETPLACE CONFIG ---

    fun observeGlobalConfig(uid: String): Flow<GlobalConfig> =
        safeSnapshotFlow("global_config/marketplace", OperationType.GET, uid) {
            db.collection("global_config").document("marketplace")
                .snapshots()
                .map { snap ->
                    snap.toObject(GlobalConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: GlobalConfig()
                }
        }

    suspend fun updateGlobalConfig(config: GlobalConfig): Result<Unit> {
        val path = "global_config/marketplace"
        return try {
            requireUserId()
            db.collection("global_config").document("marketplace").set(
                mapOf(
                    "sarRate" to config.sarRate.coerceAtLeast(0.01),
                    "aedRate" to config.aedRate.coerceAtLeast(0.01),
                    "bdtRate" to config.bdtRate.coerceAtLeast(0.01),
                    "eurRate" to config.eurRate.coerceAtLeast(0.01),
                    "gbpRate" to config.gbpRate.coerceAtLeast(0.01),
                    "saVatPercent" to config.saVatPercent.coerceIn(0.0, 50.0),
                    "defaultCommissionPercent" to config.defaultCommissionPercent.coerceIn(0.0, 50.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- ADDRESSES ---

    fun observeAddresses(userId: String): Flow<List<Address>> =
        safeSnapshotFlow("users/$userId/addresses", OperationType.LIST, userId) {
            db.collection("users").document(userId).collection("addresses")
                .snapshots()
                .map { it.toObjects(Address::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun saveAddress(address: Address): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val docId = address.id.ifBlank { "addr_${UUID.randomUUID().toString().take(12)}" }
        val path = "users/$uid/addresses/$docId"
        return try {
            require(address.recipientName.trim().length in 1..80) { "Recipient name required" }
            require(address.phone.trim().length in 4..30) { "Valid phone number required" }
            require(address.city.trim().length in 1..80) { "City required" }
            require(address.streetLine.trim().length in 2..200) { "Street address required" }
            val payload = mapOf(
                "userId" to uid,
                "label" to address.label.trim().ifBlank { "Home" }.take(40),
                "recipientName" to address.recipientName.trim().take(80),
                "phone" to address.phone.trim().take(30),
                "countryCode" to address.countryCode,
                "city" to address.city.trim().take(80),
                "streetLine" to address.streetLine.trim().take(200),
                "postalCode" to address.postalCode.trim().take(24),
                "defaultAddress" to address.defaultAddress,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("addresses").document(docId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid/addresses/$addressId"
        return try {
            db.collection("users").document(uid).collection("addresses").document(addressId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- CART ---

    fun observeCart(userId: String): Flow<List<CartItem>> =
        safeSnapshotFlow("users/$userId/cart", OperationType.LIST, userId) {
            db.collection("users").document(userId).collection("cart")
                .snapshots()
                .map { it.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun addToCart(
        product: Product,
        quantity: Long,
        selectedSize: String,
        selectedColor: String,
        selectedModel: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val safeKey = "${product.id}_${selectedSize}_${selectedColor}_${selectedModel}"
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(120)
        val path = "users/$uid/cart/$safeKey"
        return try {
            val docRef = db.collection("users").document(uid).collection("cart").document(safeKey)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val currentQty = existing.getLong("quantity") ?: 1L
                val newQty = (currentQty + quantity).coerceIn(1L, product.stock.coerceAtLeast(1L).coerceAtMost(99L))
                docRef.update(
                    mapOf(
                        "quantity" to newQty,
                        "selectedSize" to selectedSize.take(40),
                        "selectedColor" to selectedColor.take(40),
                        "selectedModel" to selectedModel.take(60),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "productId" to product.id.take(128),
                    "productTitle" to product.title.take(160),
                    "sellerId" to product.sellerId.take(128),
                    "sellerName" to product.sellerName.take(100),
                    "official" to product.official,
                    "unitPriceUsd" to product.priceUsd.coerceAtLeast(0.01),
                    "discountPercent" to product.discountPercent.coerceIn(0L, 90L),
                    "quantity" to quantity.coerceIn(1L, 99L),
                    "selectedSize" to selectedSize.take(40),
                    "selectedColor" to selectedColor.take(40),
                    "selectedModel" to selectedModel.take(60),
                    "imageKey" to product.imageKey.take(80),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, newQty: Long): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid/cart/$cartItemId"
        return try {
            if (newQty <= 0L) {
                db.collection("users").document(uid).collection("cart").document(cartItemId).delete().await()
            } else {
                db.collection("users").document(uid).collection("cart").document(cartItemId)
                    .update(
                        mapOf(
                            "quantity" to newQty.coerceIn(1L, 99L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- WISHLIST & RECENTLY VIEWED ---

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> =
        safeSnapshotFlow("users/$userId/wishlist", OperationType.LIST, userId) {
            db.collection("users").document(userId).collection("wishlist")
                .snapshots()
                .map { it.toObjects(WishlistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun toggleWishlist(product: Product, currentlyInWishlist: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val safeId = product.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val path = "users/$uid/wishlist/$safeId"
        return try {
            val docRef = db.collection("users").document(uid).collection("wishlist").document(safeId)
            if (currentlyInWishlist) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "productId" to product.id.take(128),
                        "productTitle" to product.title.take(160),
                        "unitPriceUsd" to product.priceUsd.coerceAtLeast(0.01),
                        "imageKey" to product.imageKey.take(80),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeRecentlyViewed(userId: String): Flow<List<RecentlyViewedItem>> =
        safeSnapshotFlow("users/$userId/recently_viewed", OperationType.LIST, userId) {
            db.collection("users").document(userId).collection("recently_viewed")
                .snapshots()
                .map { it.toObjects(RecentlyViewedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun recordRecentlyViewed(productId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val safeId = productId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val path = "users/$uid/recently_viewed/$safeId"
        return try {
            db.collection("users").document(uid).collection("recently_viewed").document(safeId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "productId" to productId.take(128),
                        "viewedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PRODUCTS & REVIEWS ---

    fun observeApprovedProducts(uid: String): Flow<List<Product>> =
        safeSnapshotFlow("products", OperationType.LIST, uid) {
            db.collection("products")
                .whereEqualTo("approvalStatus", "APPROVED")
                .snapshots()
                .map { it.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeSellerProducts(sellerId: String): Flow<List<Product>> =
        safeSnapshotFlow("products", OperationType.LIST, sellerId) {
            db.collection("products")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { it.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeAllProductsForAdmin(uid: String): Flow<List<Product>> =
        safeSnapshotFlow("products", OperationType.LIST, uid) {
            db.collection("products")
                .snapshots()
                .map { it.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun createSellerProduct(product: Product, asAdminOfficial: Boolean = false): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val productId = "prod_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "products/$productId"
        return try {
            require(product.title.trim().length in 2..160) { "Product title must be 2–160 characters" }
            require(product.description.trim().length in 4..2000) { "Description must be at least 4 characters" }
            require(product.priceUsd > 0.0 && product.priceUsd <= 100000.0) { "Price must be positive" }
            val payload = mapOf(
                "sellerId" to if (asAdminOfficial) CatalogSeedProvider.OFFICIAL_SELLER_ID else uid,
                "sellerName" to if (asAdminOfficial) CatalogSeedProvider.OFFICIAL_SELLER_NAME else product.sellerName.trim().ifBlank { "Global Store" }.take(100),
                "official" to asAdminOfficial,
                "approvalStatus" to if (asAdminOfficial) "APPROVED" else "PENDING",
                "title" to product.title.trim().take(160),
                "description" to product.description.trim().take(2000),
                "category" to product.category.ifBlank { "Electronics" }.take(60),
                "subCategory" to product.subCategory.ifBlank { "Audio" }.take(60),
                "brand" to product.brand.trim().ifBlank { "X-Partner" }.take(60),
                "priceUsd" to product.priceUsd,
                "discountPercent" to product.discountPercent.coerceIn(0L, 90L),
                "stock" to product.stock.coerceIn(0L, 1000000L),
                "rating" to 5.0,
                "reviewCount" to 0L,
                "imageKey" to product.imageKey.ifBlank { "electronics" }.take(80),
                "sizes" to product.sizes.take(10),
                "colors" to product.colors.take(10),
                "models" to product.models.take(10),
                "availableCountries" to product.availableCountries.ifEmpty { listOf("SA", "AE", "BD", "US", "GB", "EU") }.take(20),
                "featured" to asAdminOfficial,
                "flashSale" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("products").document(productId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateProductApproval(productId: String, approvalStatus: String, featured: Boolean): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId).update(
                mapOf(
                    "approvalStatus" to approvalStatus,
                    "featured" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun seedOfficialCatalogToCloud(): Result<Unit> {
        val path = "products"
        return try {
            requireUserId()
            val batch = db.batch()
            for (product in CatalogSeedProvider.defaultProducts) {
                val docRef = db.collection("products").document(product.id)
                batch.set(
                    docRef,
                    mapOf(
                        "sellerId" to product.sellerId,
                        "sellerName" to product.sellerName,
                        "official" to product.official,
                        "approvalStatus" to "APPROVED",
                        "title" to product.title,
                        "description" to product.description,
                        "category" to product.category,
                        "subCategory" to product.subCategory,
                        "brand" to product.brand,
                        "priceUsd" to product.priceUsd,
                        "discountPercent" to product.discountPercent,
                        "stock" to product.stock,
                        "rating" to product.rating,
                        "reviewCount" to product.reviewCount,
                        "imageKey" to product.imageKey,
                        "sizes" to product.sizes,
                        "colors" to product.colors,
                        "models" to product.models,
                        "availableCountries" to product.availableCountries,
                        "featured" to product.featured,
                        "flashSale" to product.flashSale,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            for (seller in CatalogSeedProvider.defaultSellers) {
                val sRef = db.collection("sellers").document(seller.id)
                batch.set(
                    sRef,
                    mapOf(
                        "ownerId" to seller.ownerId,
                        "storeName" to seller.storeName,
                        "storeDescription" to seller.storeDescription,
                        "sellerType" to seller.sellerType,
                        "official" to seller.official,
                        "status" to seller.status,
                        "countryCode" to seller.countryCode,
                        "rating" to seller.rating,
                        "totalSalesUsd" to seller.totalSalesUsd,
                        "commissionRate" to seller.commissionRate,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeProductReviews(productId: String, uid: String): Flow<List<ProductReview>> =
        safeSnapshotFlow("products/$productId/reviews", OperationType.LIST, uid) {
            db.collection("products").document(productId).collection("reviews")
                .whereEqualTo("productId", productId)
                .snapshots()
                .map { it.toObjects(ProductReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun submitReview(
        productId: String,
        authorName: String,
        rating: Long,
        comment: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val reviewId = "rev_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "products/$productId/reviews/$reviewId"
        return try {
            require(comment.trim().length in 2..600) { "Review must be 2–600 characters" }
            db.collection("products").document(productId).collection("reviews").document(reviewId)
                .set(
                    mapOf(
                        "productId" to productId,
                        "userId" to uid,
                        "authorName" to authorName.trim().ifBlank { "Verified Buyer" }.take(80),
                        "rating" to rating.coerceIn(1L, 5L),
                        "comment" to comment.trim().take(600),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- SELLERS & KYC ---

    fun observeApprovedSellers(uid: String): Flow<List<SellerStore>> =
        safeSnapshotFlow("sellers", OperationType.LIST, uid) {
            db.collection("sellers")
                .whereEqualTo("status", "APPROVED")
                .snapshots()
                .map { it.toObjects(SellerStore::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeMySellerStore(uid: String): Flow<SellerStore?> =
        safeSnapshotFlow("sellers/$uid", OperationType.GET, uid) {
            db.collection("sellers").document(uid)
                .snapshots()
                .map { it.toObject(SellerStore::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeAllSellersForAdmin(uid: String): Flow<List<SellerStore>> =
        safeSnapshotFlow("sellers", OperationType.LIST, uid) {
            db.collection("sellers")
                .snapshots()
                .map { it.toObjects(SellerStore::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun registerSellerStore(
        storeName: String,
        storeDescription: String,
        sellerType: String,
        countryCode: String,
        businessRegNo: String,
        ibanLast4: String,
        contactEmail: String,
        contactPhone: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "sellers/$uid"
        return try {
            require(storeName.trim().length in 2..80) { "Store name must be 2–80 characters" }
            require(storeDescription.trim().length in 2..400) { "Store description required" }
            require(businessRegNo.trim().length in 3..64) { "Verification ID / CR number required" }
            require(ibanLast4.trim().length in 4..8) { "Bank account last 4 digits required" }
            val batch = db.batch()
            val storeRef = db.collection("sellers").document(uid)
            val kycRef = storeRef.collection("private").document("kyc")
            batch.set(
                storeRef,
                mapOf(
                    "ownerId" to uid,
                    "storeName" to storeName.trim().take(80),
                    "storeDescription" to storeDescription.trim().take(400),
                    "sellerType" to if (sellerType == "INDIVIDUAL") "INDIVIDUAL" else "BUSINESS",
                    "official" to false,
                    "status" to "PENDING",
                    "countryCode" to countryCode,
                    "rating" to 5.0,
                    "totalSalesUsd" to 0.0,
                    "commissionRate" to 0.10,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                kycRef,
                mapOf(
                    "ownerId" to uid,
                    "businessRegNo" to businessRegNo.trim().take(64),
                    "ibanLast4" to ibanLast4.trim().take(8),
                    "contactEmail" to contactEmail.trim().take(120),
                    "contactPhone" to contactPhone.trim().take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateSellerStatus(sellerId: String, status: String): Result<Unit> {
        val path = "sellers/$sellerId"
        return try {
            requireUserId()
            db.collection("sellers").document(sellerId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ORDERS, CHECKOUT & SELLER FULFILLMENT ---

    fun observeMyOrders(userId: String): Flow<List<Order>> =
        safeSnapshotFlow("orders", OperationType.LIST, userId) {
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { it.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeAllOrdersForAdmin(uid: String): Flow<List<Order>> =
        safeSnapshotFlow("orders", OperationType.LIST, uid) {
            db.collection("orders")
                .snapshots()
                .map { it.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeSellerSubOrders(sellerId: String): Flow<List<SellerSubOrder>> =
        safeSnapshotFlow("seller_orders", OperationType.LIST, sellerId) {
            db.collection("seller_orders")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { it.toObjects(SellerSubOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun placeOrder(
        cartItems: List<CartItem>,
        cloudProductStockMap: Map<String, Long>,
        countryCode: String,
        currencyCode: String,
        exchangeRate: Double,
        subtotalUsd: Double,
        discountUsd: Double,
        shippingUsd: Double,
        taxUsd: Double,
        totalUsd: Double,
        couponCode: String,
        shippingMethod: String,
        paymentMethod: String,
        shippingAddressSummary: String,
        currentLoyaltyPoints: Long,
        commissionRate: Double
    ): Result<Order> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val orderNumber = "XSH-${(100000..999999).random()}"
        val trackingNumber = "TRK-${countryCode}-${(10000000..99999999).random()}"
        val paymentStatus = if (paymentMethod.contains("Cash on Delivery", ignoreCase = true)) {
            "COD_AUTHORIZED"
        } else {
            "AUTHORIZED"
        }
        val orderStatus = "CONFIRMED"
        val itemsSummary = cartItems.joinToString(", ") { "${it.productTitle} x${it.quantity}" }.take(500)
        val totalUnits = cartItems.sumOf { it.quantity }.coerceAtLeast(1L)

        return try {
            require(cartItems.isNotEmpty()) { "Cart cannot be empty" }
            val batch = db.batch()
            val orderRef = db.collection("orders").document(orderId)
            batch.set(
                orderRef,
                mapOf(
                    "userId" to uid,
                    "orderNumber" to orderNumber,
                    "countryCode" to countryCode,
                    "currencyCode" to currencyCode,
                    "exchangeRate" to exchangeRate,
                    "subtotalUsd" to subtotalUsd,
                    "discountUsd" to discountUsd,
                    "shippingUsd" to shippingUsd,
                    "taxUsd" to taxUsd,
                    "totalUsd" to totalUsd,
                    "couponCode" to couponCode.take(32),
                    "shippingMethod" to shippingMethod.take(60),
                    "trackingNumber" to trackingNumber,
                    "paymentMethod" to paymentMethod.take(60),
                    "paymentStatus" to paymentStatus,
                    "status" to orderStatus,
                    "shippingAddressSummary" to shippingAddressSummary.take(300),
                    "itemCount" to totalUnits,
                    "itemsSummary" to itemsSummary,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Seller-wise sub-orders
            val grouped = cartItems.groupBy { it.sellerId }
            grouped.forEach { (sellerId, sellerItems) ->
                val subId = "sub_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                val gross = sellerItems.sumOf { it.lineTotalUsd }
                val effectiveCommRate = if (sellerItems.first().official) 0.0 else commissionRate
                val comm = gross * effectiveCommRate
                val net = (gross - comm).coerceAtLeast(0.0)
                val subRef = db.collection("seller_orders").document(subId)
                batch.set(
                    subRef,
                    mapOf(
                        "orderId" to orderId,
                        "orderNumber" to orderNumber,
                        "buyerId" to uid,
                        "sellerId" to sellerId.take(128),
                        "sellerName" to sellerItems.first().sellerName.take(100),
                        "grossUsd" to gross,
                        "commissionUsd" to comm,
                        "netUsd" to net,
                        "itemCount" to sellerItems.sumOf { it.quantity }.coerceAtLeast(1L),
                        "itemsSummary" to sellerItems.joinToString(", ") { "${it.productTitle} x${it.quantity}" }.take(500),
                        "courierName" to shippingMethod.take(60),
                        "trackingNumber" to trackingNumber,
                        "status" to orderStatus,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Decrement stock for products stored in Firestore
            for (item in cartItems) {
                val currentStock = cloudProductStockMap[item.productId]
                if (currentStock != null && item.id.isNotBlank()) {
                    val newStock = (currentStock - item.quantity).coerceAtLeast(0L)
                    batch.update(
                        db.collection("products").document(item.productId),
                        mapOf(
                            "stock" to newStock,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (item.id.isNotBlank()) {
                    batch.delete(db.collection("users").document(uid).collection("cart").document(item.id))
                }
            }

            // Award loyalty points (1 point per $1 spent)
            val earnedPoints = totalUsd.toLong().coerceAtLeast(1L)
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "loyaltyPoints" to (currentLoyaltyPoints + earnedPoints),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(
                Order(
                    id = orderId,
                    userId = uid,
                    orderNumber = orderNumber,
                    countryCode = countryCode,
                    currencyCode = currencyCode,
                    exchangeRate = exchangeRate,
                    subtotalUsd = subtotalUsd,
                    discountUsd = discountUsd,
                    shippingUsd = shippingUsd,
                    taxUsd = taxUsd,
                    totalUsd = totalUsd,
                    couponCode = couponCode,
                    shippingMethod = shippingMethod,
                    trackingNumber = trackingNumber,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    status = orderStatus,
                    shippingAddressSummary = shippingAddressSummary,
                    itemCount = totalUnits,
                    itemsSummary = itemsSummary
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "orders/$orderId")
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSellerSubOrderStatus(
        subOrderId: String,
        newStatus: String,
        courierName: String,
        trackingNumber: String
    ): Result<Unit> {
        val path = "seller_orders/$subOrderId"
        return try {
            requireUserId()
            db.collection("seller_orders").document(subOrderId).update(
                mapOf(
                    "status" to newStatus,
                    "courierName" to courierName.take(60),
                    "trackingNumber" to trackingNumber.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PAYOUTS & SUPPORT ---

    fun observeSellerPayouts(sellerId: String): Flow<List<PayoutRequest>> =
        safeSnapshotFlow("payouts", OperationType.LIST, sellerId) {
            db.collection("payouts")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { it.toObjects(PayoutRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    fun observeAllPayoutsForAdmin(uid: String): Flow<List<PayoutRequest>> =
        safeSnapshotFlow("payouts", OperationType.LIST, uid) {
            db.collection("payouts")
                .snapshots()
                .map { it.toObjects(PayoutRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun requestSellerPayout(storeName: String, amountUsd: Double): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val payoutId = "pay_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "payouts/$payoutId"
        return try {
            require(amountUsd > 0.0 && amountUsd <= 500000.0) { "Invalid payout amount" }
            db.collection("payouts").document(payoutId).set(
                mapOf(
                    "sellerId" to uid,
                    "storeName" to storeName.take(100),
                    "amountUsd" to amountUsd,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminProcessPayout(payoutId: String, status: String): Result<Unit> {
        val path = "payouts/$payoutId"
        return try {
            requireUserId()
            db.collection("payouts").document(payoutId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> =
        safeSnapshotFlow("support_tickets", OperationType.LIST, userId) {
            db.collection("support_tickets")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { it.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        }

    suspend fun submitSupportTicket(subject: String, message: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val ticketId = "tkt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "support_tickets/$ticketId"
        return try {
            require(subject.trim().length in 2..120) { "Subject must be 2–120 characters" }
            require(message.trim().length in 4..1000) { "Message must be at least 4 characters" }
            db.collection("support_tickets").document(ticketId).set(
                mapOf(
                    "userId" to uid,
                    "subject" to subject.trim().take(120),
                    "message" to message.trim().take(1000),
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
