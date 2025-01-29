[![Languages](https://img.shields.io/badge/languages-Kotlin-orange.svg?maxAge=2592000)](https://github.com/albycom/alby_widget_android)
[![Apache License](http://img.shields.io/badge/license-APACHE2-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0.html)
![GitHub Tag](https://img.shields.io/github/v/tag/albycom/alby_widget_android)
![Maven Central Version](https://img.shields.io/maven-central/v/com.alby.widget/alby-widget)

## Installation
AlbyWidget for Android requires a SDK 29+ and Jetpack Compose.

### Gradle Kotlin
```
implementation("com.alby.widget:alby-widget:0.0.15")
```

### Gradle
```
implementation 'com.alby.widget:alby-widget:0.0.15'
```

### Apache Maven
```xml
<dependency>
    <groupId>com.alby.widget</groupId>
    <artifactId>alby-widget</artifactId>
    <version>0.0.15</version>
</dependency>
```

## Setup and Configuration
This SDK only works with Jetpack Compose. First of all, make sure that you have internet permission enabled for your app inside your `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Prerequisites  

1. Brand ID - This is an organization identifier that represents your brand
2. Widget ID - This is a unique identifier for the alby widget that you can get in the widgets embed page inside the alby UI.


## Initialization
The SDK must be initialized with the unique identifier for your alby account (Brand ID) and a context.

AlbySDK.initialize() must be called before any other SDK methods can be invoked. We recommend initializing from the earliest point in your application code, such as the Application.onCreate() method.

```kotlin
// Application subclass 
import android.app.Application
import com.alby.widget.AlbySDK

class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        /* ... */
        
        // Initialize is required before invoking any other Alby SDK functionality 
        AlbySDK.initialize("your-brand-id", this)
    }
}
```

## Components

### AlbyWidgetScreen
The `AlbyWidgetScreen` is a component that displays the Alby widget inside a sheet (modal). This is ideal for cases where you want the widget to appear in an overlay or pop-up format, giving users the option to engage with the widget without leaving the current screen.

1. Import the alby widget
```kotlin
import com.alby.widget.AlbyWidgetScreen
```
2. Go to the Activity where you want to place the widget and wrap your existing screen with our widget and pass in the required `brandId`, `productId` and `widgetId` parameters:
```kotlin
AlbyWidgetScreen(brandId = "your-brand-id", productId = "your-product-id", widgetId = "your-widget-id" ) {
 YourScreenGoesHere()
}
```
3. Optional: You can pass in A/B test parameters to the widget by passing in the `testId`, `testVersion` and `testDescription` parameters:
```kotlin
AlbyWidgetScreen(brandId = "your-brand-id", productId = "your-product-id", widgetId = "your-widget-id", testId = "your-test-id", testVersion = "your-test-version", testDescription = "your-test-description" ) {
 YourScreenGoesHere()
}
```

The default placement will be in the bottom of the screen. If you have a bottom bar or something similar, make sure you add place the
bottom sheet around your tab and that you pass the padding for the bottom bar to the widget so it stays on top of the bottom bar.

### AlbyInlineWidget
The `AlbyInlineWidget` is a component that allows embedding the Alby widget directly into your app's UI. It's perfect for inline use on any page, like product details or brand-specific screens, where the widget integrates seamlessly within the existing view hierarchy.

1. Import the alby widget
```kotlin
import com.alby.widget.AlbyInlineWidget
```
2. In the Composable function where you want to place the widget, add the `AlbyInlineWidget` component and pass in the required `brandId`, `productId` and `widgetId` parameters:
```kotlin
AlbyInlineWidget(
    brandId = "your-brand-id",
    productId = "your-product-id",
    widgetId = "your-widget-id"
)
```
3. Optional: You can pass in A/B test parameters to the widget by passing in the `testId`, `testVersion` and `testDescription` parameters:
```kotlin
AlbyInlineWidget(
    brandId = "your-brand-id",
    productId = "your-product-id",
    widgetId = "your-widget-id",
    testId = "your-test-id",
    testVersion = "your-test-version",
    testDescription = "your-test-description"
)
```

## Event Tracking
The SDK also provides an API to sending purchase data and other events via HTTP requests.

### Usage
1. Use the sendPurchasePixel method to send a purchase pixel request:
```kotlin
AlbySDK.sendPurchasePixel(
    orderId = 12345, // Order ID (String or Number)
    orderTotal = 99.99, // Order total (Float or Number)
    productIds = listOf("A123", 456), // List of product IDs (String or Number)
    currency = "USD" // Currency of the order
)
```

2. Use the sendAddToCartEvent method to send an add to cart event:
```kotlin
AlbySDK.sendAddToCartEvent(
    price = 99.99, // Price of the item
    variantId = "A123", // Variant ID of the item
    currency = "USD", // Currency of the item
    quantity = "1" // Quantity of the item
)
```