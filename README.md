[![Languages](https://img.shields.io/badge/languages-Kotlin-orange.svg?maxAge=2592000)](https://github.com/albycom/alby_widget_android)
[![Apache License](http://img.shields.io/badge/license-APACHE2-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0.html)
![GitHub Tag](https://img.shields.io/github/v/tag/albycom/alby_widget_android)
![Maven Central Version](https://img.shields.io/maven-central/v/com.alby.widget/alby-widget)

## Installation
AlbyWidget for Android requires a SDK 23+ and Jetpack Compose.

### Gradle Kotlin
```
implementation("com.alby.widget:alby-widget:0.2.1")
```

### Gradle
```
implementation 'com.alby.widget:alby-widget:0.2.1'
```

### Apache Maven
```xml
<dependency>
    <groupId>com.alby.widget</groupId>
    <artifactId>alby-widget</artifactId>
    <version>0.2.1</version>
</dependency>
```

## Setup and Configuration
This SDK only works with Jetpack Compose. First of all, make sure that you have internet permission enabled for your app inside your `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Prerequisites  

1. Make sure you have an Alby account - if you don't, go to https://alby.com and create one.
2. Get your brand id - this is an organization id that represents your brand
3. Get your widget id - this is a unique id that you can get in the widgets embed page inside the alby UI.


## Components

### AlbyWidgetScreen
The `AlbyWidgetScreen` is a component that displays the Alby widget inside a sheet (modal). This is ideal for cases where you want the widget to appear in an overlay or pop-up format, giving users the option to engage with the widget without leaving the current screen.

1. Import the alby widget `import com.alby.widget.AlbyWidgetScreen`
2. Go to the Activity where you want to place the widget and wrap your existing screen with our widget and pass in the required `brandId`, `productId` and `widgetId` parameters:
```
AlbyWidgetScreen(brandId = "your-brand-id", productId = "your-product-id", widgetId = "your-widget-id" ) {
 YourScreenGoesHere()
}
```

The default placement will be in the bottom of the screen. If you have a bottom bar or something similar, make sure you add place the
bottom sheet around your tab and that you pass the padding for the bottom bar to the widget so it stays on top of the bottom bar.

#### Example Usage

```kotlin
class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // setting up the individual tabs
            val homeTab = TabBarItem(title = "Home", selectedIcon = Icons.Filled.Home, unselectedIcon = Icons.Outlined.Home)
            val alertsTab = TabBarItem(title = "Alerts", selectedIcon = Icons.Filled.Notifications, unselectedIcon = Icons.Outlined.Notifications, badgeAmount = 7)
            val settingsTab = TabBarItem(title = "Settings", selectedIcon = Icons.Filled.Settings, unselectedIcon = Icons.Outlined.Settings)
            val moreTab = TabBarItem(title = "More", selectedIcon = Icons.Filled.List, unselectedIcon = Icons.Outlined.List)

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
                    Scaffold(bottomBar = { TabView(tabBarItems, navController) })  {
                        innerPadding ->
                        val bottomPadding = innerPadding.calculateBottomPadding() + 10.dp

                        NavHost(navController = navController, startDestination = homeTab.title) {
                            composable(homeTab.title) {
                                AlbyWidgetScreen(brandId = "your brand id", productId = "your product id", widgetId = "your widget id", bottomOffset = bottomPadding) {
                                    Text(homeTab.title)
                                }

                            }
                            composable(alertsTab.title) {
                                Text(alertsTab.title)
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
```

### AlbyInlineWidget
The `AlbyInlineWidget` is a component that allows embedding the Alby widget directly into your app's UI. It’s perfect for inline use on any page, like product details or brand-specific screens, where the widget integrates seamlessly within the existing view hierarchy.

1. Import the alby widget `import com.alby.widget.AlbyInlineWidget`
2. In the Composable function where you want to place the widget, add the `AlbyInlineWidget` component and pass in the required `brandId`, `productId` and `widgetId` parameters:
```
AlbyInlineWidget(
    brandId = "your brand id",
    productId = "your product id",
    widgetId = "your widget id"
)
```

#### Example Usage
```kotlin
Column(
    modifier = Modifier
        .padding(innerPadding),
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    AlbyInlineWidget(
        brandId = "your brand id",
        productId = "your product id",
        widgetId = "your widget id",
        modifier = Modifier.padding(24.dp),
    )

}
```