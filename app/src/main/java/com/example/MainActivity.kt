package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.remote.AuthManager
import com.example.data.remote.AuthManager.authStateFlow
import com.example.data.remote.MarketplaceRepository
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SellerCenterScreen
import com.example.ui.state.MarketplaceViewModel
import com.example.ui.theme.XShoppingTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CART = "cart"
    const val ACCOUNT = "account"
    const val SEARCH = "search"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CHECKOUT = "checkout"
    const val SELLER_CENTER = "seller_center"
    const val ADMIN_DASHBOARD = "admin_dashboard"
}

private data class TopTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        XShoppingTheme(darkTheme = true) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            val isAdmin = user.email.equals("ahmedshatil903@gmail.com", ignoreCase = true) && user.isEmailVerified
            val viewModel: MarketplaceViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MarketplaceViewModel(
                            repository = MarketplaceRepository(db),
                            currentUserId = user.uid,
                            currentUserName = user.displayName ?: "Global Shopper",
                            currentUserEmail = user.email ?: "",
                            isAdminUser = isAdmin
                        )
                    }
                }
            )
            XShoppingRootShell(viewModel = viewModel)
        }
    }
}

@Composable
private fun XShoppingRootShell(viewModel: MarketplaceViewModel) {
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val language by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage!!)
            viewModel.clearSnackbar()
        }
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Routes.HOME

    val tabs = remember(strings) {
        listOf(
            TopTab(Routes.HOME, strings.home, Icons.Filled.Home, Icons.Outlined.Home),
            TopTab(Routes.CATEGORIES, strings.categories, Icons.Filled.Category, Icons.Outlined.Category),
            TopTab(Routes.CART, strings.cart, Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
            TopTab(Routes.ACCOUNT, strings.account, Icons.Filled.Person, Icons.Outlined.Person)
        )
    }
    val showTopLevelNav = currentRoute in tabs.map { it.route } || currentRoute.startsWith(Routes.CATEGORIES)
    val cartBadgeCount = cartItems.sumOf { it.quantity }.toInt()

    XShoppingTheme(darkTheme = darkMode) {
        CompositionLocalProvider(LocalLayoutDirection provides language.layoutDirection) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showTopLevelNav && !isWideScreen) {
                            NavigationBar {
                                tabs.forEach { tab ->
                                    val selected = currentRoute == tab.route ||
                                        (tab.route == Routes.CATEGORIES && currentRoute.startsWith(Routes.CATEGORIES))
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(Routes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
                                            if (tab.route == Routes.CART && cartBadgeCount > 0) {
                                                BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                                    Icon(icon, contentDescription = tab.label)
                                                }
                                            } else {
                                                Icon(icon, contentDescription = tab.label)
                                            }
                                        },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showTopLevelNav && isWideScreen) {
                            NavigationRail {
                                tabs.forEach { tab ->
                                    val selected = currentRoute == tab.route ||
                                        (tab.route == Routes.CATEGORIES && currentRoute.startsWith(Routes.CATEGORIES))
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(Routes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
                                            if (tab.route == Routes.CART && cartBadgeCount > 0) {
                                                BadgedBox(badge = { Badge { Text(cartBadgeCount.toString()) } }) {
                                                    Icon(icon, contentDescription = tab.label)
                                                }
                                            } else {
                                                Icon(icon, contentDescription = tab.label)
                                            }
                                        },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            NavHost(
                                navController = navController,
                                startDestination = Routes.HOME
                            ) {
                                composable(Routes.HOME) {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onOpenSearch = { filter ->
                                            val route = if (filter.isNullOrBlank()) Routes.SEARCH else "${Routes.SEARCH}?filter=$filter"
                                            navController.navigate(route)
                                        },
                                        onOpenProduct = { pid -> navController.navigate("product/$pid") },
                                        onOpenCart = { navController.navigate(Routes.CART) },
                                        onOpenCategories = { cat ->
                                            navController.navigate("${Routes.CATEGORIES}?cat=$cat")
                                        }
                                    )
                                }
                                composable(
                                    route = "${Routes.CATEGORIES}?cat={cat}",
                                    arguments = listOf(navArgument("cat") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { entry ->
                                    val cat = entry.arguments?.getString("cat")
                                    CategoriesScreen(
                                        viewModel = viewModel,
                                        initialCategory = cat,
                                        onOpenProduct = { pid -> navController.navigate("product/$pid") },
                                        onBrowseCategory = { c -> navController.navigate("${Routes.SEARCH}?filter=$c") }
                                    )
                                }
                                composable(Routes.CATEGORIES) {
                                    CategoriesScreen(
                                        viewModel = viewModel,
                                        initialCategory = null,
                                        onOpenProduct = { pid -> navController.navigate("product/$pid") },
                                        onBrowseCategory = { c -> navController.navigate("${Routes.SEARCH}?filter=$c") }
                                    )
                                }
                                composable(Routes.CART) {
                                    CartScreen(
                                        viewModel = viewModel,
                                        onContinueShopping = { navController.navigate(Routes.HOME) },
                                        onProceedToCheckout = { navController.navigate(Routes.CHECKOUT) }
                                    )
                                }
                                composable(Routes.ACCOUNT) {
                                    AccountScreen(
                                        viewModel = viewModel,
                                        onOpenSellerCenter = { navController.navigate(Routes.SELLER_CENTER) },
                                        onOpenAdminDashboard = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                                        onOpenProduct = { pid -> navController.navigate("product/$pid") }
                                    )
                                }
                                composable(
                                    route = "${Routes.SEARCH}?filter={filter}",
                                    arguments = listOf(navArgument("filter") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { entry ->
                                    SearchScreen(
                                        viewModel = viewModel,
                                        initialFilter = entry.arguments?.getString("filter"),
                                        onBack = { navController.popBackStack() },
                                        onOpenProduct = { pid -> navController.navigate("product/$pid") }
                                    )
                                }
                                composable(Routes.SEARCH) {
                                    SearchScreen(
                                        viewModel = viewModel,
                                        initialFilter = null,
                                        onBack = { navController.popBackStack() },
                                        onOpenProduct = { pid -> navController.navigate("product/$pid") }
                                    )
                                }
                                composable(
                                    route = Routes.PRODUCT_DETAIL,
                                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                                ) { entry ->
                                    val pid = entry.arguments?.getString("productId") ?: ""
                                    ProductDetailScreen(
                                        productId = pid,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() },
                                        onOpenProduct = { nextId -> navController.navigate("product/$nextId") },
                                        onBuyNowGoCheckout = { navController.navigate(Routes.CHECKOUT) }
                                    )
                                }
                                composable(Routes.CHECKOUT) {
                                    CheckoutScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() },
                                        onOrderConfirmedGoHome = {
                                            navController.navigate(Routes.HOME) {
                                                popUpTo(Routes.HOME) { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable(Routes.SELLER_CENTER) {
                                    SellerCenterScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(Routes.ADMIN_DASHBOARD) {
                                    AdminDashboardScreen(
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
