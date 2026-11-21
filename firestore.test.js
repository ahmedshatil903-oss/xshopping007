const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_buyer_123";
const BOB_UID = "bob_seller_456";
const ADMIN_UID = "admin_user_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").where("approvalStatus", "==", "APPROVED").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("sellers").where("status", "==", "APPROVED").get());
});

test("User profile & subcollections: owner access works, cross-user & ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      loyaltyPoints: 100,
      referralCode: "XSHOP-ALICE",
      preferredCountry: "SA",
      preferredCurrency: "SAR",
      preferredLanguage: "en",
      darkMode: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());

  // Owner cart & address subcollections
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1").set({
      userId: ALICE_UID,
      label: "Home",
      recipientName: "Alice",
      phone: "+966501234567",
      countryCode: "SA",
      city: "Riyadh",
      streetLine: "Olaya Street 42",
      postalCode: "12211",
      defaultAddress: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("cart_1").set({
      userId: ALICE_UID,
      productId: "prod_1",
      productTitle: "Wireless Headphones",
      sellerId: BOB_UID,
      sellerName: "Bob Audio",
      official: false,
      unitPriceUsd: 199.0,
      discountPercent: 15,
      quantity: 2,
      selectedSize: "Standard",
      selectedColor: "Obsidian",
      selectedModel: "Pro",
      imageKey: "headphones",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").get());
});

test("Seller store & product approval: seller cannot self-approve, admin can approve", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "ahmedshatil903@gmail.com", email_verified: true }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext("spoof_1", { email: "ahmedshatil903@gmail.com", email_verified: false }).firestore();
  const now = new Date(Date.now() - 2000);

  // Seller cannot create an already APPROVED or official store
  await assertFails(
    bobDb.collection("sellers").doc(BOB_UID).set({
      ownerId: BOB_UID,
      storeName: "Bob Store",
      storeDescription: "Quality electronics",
      sellerType: "BUSINESS",
      official: false,
      status: "APPROVED",
      countryCode: "SA",
      rating: 4.8,
      totalSalesUsd: 0.0,
      commissionRate: 0.1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seller creates PENDING store + private KYC
  await assertSucceeds(
    bobDb.collection("sellers").doc(BOB_UID).set({
      ownerId: BOB_UID,
      storeName: "Bob Store",
      storeDescription: "Quality electronics",
      sellerType: "BUSINESS",
      official: false,
      status: "PENDING",
      countryCode: "SA",
      rating: 5.0,
      totalSalesUsd: 0.0,
      commissionRate: 0.1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("sellers").doc(BOB_UID).collection("private").doc("kyc").set({
      ownerId: BOB_UID,
      businessRegNo: "CR-1010998877",
      ibanLast4: "4421",
      contactEmail: "bob@example.com",
      contactPhone: "+966550001122",
      updatedAt: now,
    })
  );

  // Buyer cannot read Bob's private KYC
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("sellers").doc(BOB_UID).collection("private").doc("kyc").get());

  // Spoofed unverified admin email cannot approve
  await assertFails(
    spoofAdminDb.collection("sellers").doc(BOB_UID).update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Verified bootstrapped admin approves seller
  await assertSucceeds(
    adminDb.collection("sellers").doc(BOB_UID).update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Seller creates PENDING product
  await assertSucceeds(
    bobDb.collection("products").doc("prod_bob_1").set({
      sellerId: BOB_UID,
      sellerName: "Bob Store",
      official: false,
      approvalStatus: "PENDING",
      title: "Studio Mic",
      description: "Broadcast quality USB-C condenser microphone.",
      category: "Electronics",
      subCategory: "Audio",
      brand: "SonicLab",
      priceUsd: 129.0,
      discountPercent: 10,
      stock: 25,
      rating: 4.9,
      reviewCount: 0,
      imageKey: "audio",
      sizes: ["Standard"],
      colors: ["Black", "Orange"],
      models: ["USB-C"],
      availableCountries: ["SA", "AE", "US"],
      featured: false,
      flashSale: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered product queries succeed; unfiltered fail for non-admin
  await assertSucceeds(aliceDb.collection("products").where("approvalStatus", "==", "APPROVED").get());
  await assertSucceeds(bobDb.collection("products").where("sellerId", "==", BOB_UID).get());
  await assertFails(aliceDb.collection("products").get());
});

test("Orders & Seller sub-orders: buyer checkout and seller fulfillment isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1001").set({
      userId: ALICE_UID,
      orderNumber: "XSH-1001",
      countryCode: "SA",
      currencyCode: "SAR",
      exchangeRate: 3.75,
      subtotalUsd: 100.0,
      discountUsd: 10.0,
      shippingUsd: 12.0,
      taxUsd: 13.5,
      totalUsd: 115.5,
      couponCode: "XGLOBAL10",
      shippingMethod: "Aramex Priority",
      trackingNumber: "ARX99887766",
      paymentMethod: "Mada",
      paymentStatus: "AUTHORIZED",
      status: "CONFIRMED",
      shippingAddressSummary: "Alice, Olaya St, Riyadh, SA",
      itemCount: 1,
      itemsSummary: "Wireless Headphones x1",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("seller_orders").doc("sub_1001").set({
      orderId: "ord_1001",
      orderNumber: "XSH-1001",
      buyerId: ALICE_UID,
      sellerId: BOB_UID,
      sellerName: "Bob Store",
      grossUsd: 100.0,
      commissionUsd: 10.0,
      netUsd: 90.0,
      itemCount: 1,
      itemsSummary: "Wireless Headphones x1",
      courierName: "Aramex Priority",
      trackingNumber: "ARX99887766",
      status: "CONFIRMED",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Buyer can list own orders; unfiltered fails
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());

  // Seller can list & advance own seller sub-order
  await assertSucceeds(bobDb.collection("seller_orders").where("sellerId", "==", BOB_UID).get());
  await assertSucceeds(
    bobDb.collection("seller_orders").doc("sub_1001").update({
      status: "SHIPPED",
      trackingNumber: "ARX99887766",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Seller cannot tamper with netUsd payout amount
  await assertFails(
    bobDb.collection("seller_orders").doc("sub_1001").update({
      netUsd: 9999.0,
      updatedAt: new Date(Date.now() - 400),
    })
  );
});
