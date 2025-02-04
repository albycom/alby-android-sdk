package com.example.sample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

        AlbySDK.initialize("c8866843-ce73-496e-b14e-73be14e6450a", this)

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
                                    brandId = "c8866843-ce73-496e-b14e-73be14e6450a",
                                    productId = "100037",
                                    widgetId = "15b194d9-2641-41f2-a0d7-e258d73d0709",
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
                                Column(
                                    modifier = Modifier
                                        .padding(innerPadding),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    AlbyInlineWidget(
                                        brandId = "c8866843-ce73-496e-b14e-73be14e6450a",
                                        modifier = Modifier.padding(24.dp),
                                        productId = "100037",
                                        onThreadIdChanged = { newThreadId ->
                                            // Handle the new thread ID here
                                            println("Thread ID changed to: $newThreadId")
                                        },
                                        onWidgetRendered = {
                                            // Called when the widget has finished rendering
                                            println("Widget is ready!")
                                        }
                                    )
                                }
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