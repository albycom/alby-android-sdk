package com.example.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.NestedScrollView
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alby.widget.AlbyInlineWidget
import com.alby.widget.AlbySDK
import com.alby.widget.AlbyWidgetScreen
import com.example.sample.ui.theme.AlbyWidgetTheme

data class TabBarItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeAmount: Int? = null
)

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val brandId = "your-brand-id"
        val productId = "your-product-id"
        val widgetId = "your-widget-id"

        AlbySDK.clearAlbyData(this)
        AlbySDK.initialize(brandId, this)

        setContent {
            // setting up the individual tabs
            val homeTab = TabBarItem(
                title = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home
            )
            val alertsTab = TabBarItem(
                title = "Alerts",
                selectedIcon = Icons.Filled.Notifications,
                unselectedIcon = Icons.Outlined.Notifications,
                badgeAmount = 7
            )
            val settingsTab = TabBarItem(
                title = "Settings",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings
            )
            val moreTab = TabBarItem(
                title = "More",
                selectedIcon = Icons.Filled.List,
                unselectedIcon = Icons.Outlined.List
            )

            // creating a list of all the tabs
            val tabBarItems = listOf(homeTab, alertsTab, settingsTab, moreTab)

            // creating our navController
            val navController = rememberNavController()

            AlbyWidgetTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(bottomBar = { TabView(tabBarItems, navController) }) { innerPadding ->
                        val bottomPadding = innerPadding.calculateBottomPadding() + 10.dp

                        NavHost(navController = navController, startDestination = homeTab.title) {
                            composable(homeTab.title) {
                                AlbyWidgetScreen(
                                    productId = productId,
                                    widgetId = widgetId,
                                    bottomOffset = bottomPadding,
                                    onWidgetRendered = {
                                        // Called when the widget has finished rendering
                                        println("Widget is ready!")
                                    }
                                ) {
                                    LazyColumn {
                                        items(1) {
                                            Text(homeTab.title)
                                        }
                                    }

                                }

                            }
                            composable(alertsTab.title) {
                                ProductPageWithInlineWidget(
                                    productId = productId,
                                    widgetId = widgetId,
                                    contentPadding = innerPadding
                                )
                            }
                            composable(settingsTab.title) {
                                Text(settingsTab.title)
                            }
                            composable(moreTab.title) {
                                MoreView()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// This is a wrapper view that allows us to easily and cleanly
// reuse this component in any future project
@Composable
fun TabView(tabBarItems: List<TabBarItem>, navController: NavController) {
    var selectedTabIndex by rememberSaveable {
        mutableStateOf(0)
    }

    NavigationBar {
        // looping over each tab to generate the views and navigation for each item
        tabBarItems.forEachIndexed { index, tabBarItem ->
            NavigationBarItem(
                selected = selectedTabIndex == index,
                onClick = {
                    selectedTabIndex = index
                    navController.navigate(tabBarItem.title)
                },
                icon = {
                    TabBarIconView(
                        isSelected = selectedTabIndex == index,
                        selectedIcon = tabBarItem.selectedIcon,
                        unselectedIcon = tabBarItem.unselectedIcon,
                        title = tabBarItem.title,
                        badgeAmount = tabBarItem.badgeAmount
                    )
                },
                label = { Text(tabBarItem.title) })
        }
    }
}

// This component helps to clean up the API call from our TabView above,
// but could just as easily be added inside the TabView without creating this custom component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabBarIconView(
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    title: String,
    badgeAmount: Int? = null
) {
    BadgedBox(badge = { TabBarBadgeView(badgeAmount) }) {
        Icon(
            imageVector = if (isSelected) {
                selectedIcon
            } else {
                unselectedIcon
            },
            contentDescription = title
        )
    }
}

// This component helps to clean up the API call from our TabBarIconView above,
// but could just as easily be added inside the TabBarIconView without creating this custom component
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TabBarBadgeView(count: Int? = null) {
    if (count != null) {
        Badge {
            Text(count.toString())
        }
    }
}
// end of the reusable components that can be copied over to any new projects
// ----------------------------------------

@Composable
fun ProductPageWithInlineWidget(
    productId: String,
    widgetId: String,
    contentPadding: PaddingValues
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            NestedScrollView(context).apply {
                isFillViewport = true
                addView(
                    ComposeView(context).apply {
                        setViewCompositionStrategy(
                            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                        )
                        setContent {
                            AlbyWidgetTheme {
                                ProductPageBody(productId, widgetId, contentPadding)
                            }
                        }
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        }
    )
}

@Composable
private fun ProductPageBody(
    productId: String,
    widgetId: String,
    contentPadding: PaddingValues
) {
    val similarItems = listOf(
        "Anybody Tall Lush Jersey Long Sleeve Top",
        "Goodnight Kiss Ladies Merry Henley PJ Set",
        "MUK LUKS Tall Waffle Quarter Zip Set",
        "Quilted Knit Jacket",
        "Stretch Denim Straight Leg Jean"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Product $productId", style = MaterialTheme.typography.titleLarge)
            Text(
                "Inline widget in a scrolling page. If the chat has no overflow, dragging it scrolls this page. Once it overflows, the chat keeps the drag.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Text(
            "Garment Specification",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        repeat(8) { index ->
            Text(
                "Spec detail line ${index + 1}: fabric, fit, and care information.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        AlbyInlineWidget(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(horizontal = 8.dp),
            productId = productId,
            widgetId = widgetId,
            onThreadIdChanged = { newThreadId ->
                println("Thread ID changed to: $newThreadId")
            },
            onWidgetRendered = {
                println("Widget is ready!")
            },
        )
        Text(
            "Shop Similar Items",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        similarItems.forEach { item ->
            Text(
                item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        repeat(12) { index ->
            Text(
                "More product content ${index + 1}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Add to Cart")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// This was added to demonstrate that we are infact changing views when we click a new tab
@Composable
fun MoreView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Thing 1")
        Text("Thing 2")
        Text("Thing 3")
        Text("Thing 4")
        Text("Thing 5")

        Button(
            onClick = {
                // Call the AlbyPurchasePixel method
                AlbySDK.sendPurchasePixel(
                    orderId = 12345,
                    orderTotal = 99.99,
                    variantIds = listOf("A123", 456),
                    currency = "USD"
                )
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Call Purchase Pixel")
        }

        Button(
            onClick = {

                AlbySDK.sendAddToCartEvent(99, "123", "USD", "3")
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Send Add to Cart Event")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AlbyWidgetTheme {
        MoreView()
    }
}