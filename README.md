[![Languages](https://img.shields.io/badge/languages-Kotlin-orange.svg?maxAge=2592000)](https://github.com/albycom/alby_widget_android)
[![Apache License](http://img.shields.io/badge/license-APACHE2-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0.html)
![GitHub Tag](https://img.shields.io/github/v/tag/albycom/alby_widget_android)
![Maven Central Version](https://img.shields.io/maven-central/v/com.alby.widget/alby-widget)

## Installation
AlbyWidget for Android requires a SDK 23+ and Jetpack Compose.

### Gradle Kotlin
```
implementation("com.alby.widget:alby-widget:0.6.0")
```

### Gradle
```
implementation 'com.alby.widget:alby-widget:0.6.0'
```

### Apache Maven
```xml
<dependency>
    <groupId>com.alby.widget</groupId>
    <artifactId>alby-widget</artifactId>
    <version>0.6.0</version>
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

## WebView Storage and Multiple Brand IDs

The Alby SDK is built on WebViews. By default, WebView storage (cookies, localStorage, etc.) is shared across all Brand IDs. For most implementations, this is not an issue as apps typically use a single Brand ID.

If you have the uncommon need to use multiple Brand IDs simultaneously in the same app and require isolated storage states (for example, during development or in a multi-tenant application):

```kotlin
// In your Application or Activity
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    WebView.setDataDirectorySuffix(brandId) // Or any other partition strategy
}

// Then initialize the SDK
AlbySDK.initialize(brandId)
```

Note: This scenario is rare and typically only needed if you're building a multi-tenant application or testing different Brand IDs in development. Most applications will use a single Brand ID in production.


## Managing WebView Data

You can clear all Alby-related data (cookies, cache, localStorage) using:

```kotlin
// Clear all Alby data - this will reset user state
AlbySDK.clearAlbyData(context)  // If SDK not initialized
// or
AlbySDK.clearAlbyData()         // If SDK is initialized

// Example in Activity:
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Clear data before initialization if needed
        AlbySDK.clearAlbyData(this)
        
        // Initialize SDK
        AlbySDK.initialize("your-brand-id", this)
        
        // Later, can clear without context
        AlbySDK.clearAlbyData()
    }
}
```

**Note:** Clearing data will reset all user state including chat history, preferences, and any stored information. Use this when you want to completely reset the widget's state or when switching users.

## Components

### AlbyWidgetScreen
The `AlbyWidgetScreen` is a component that displays the Alby widget inside a sheet (modal). This is ideal for cases where you want the widget to appear in an overlay or pop-up format, giving users the option to engage with the widget without leaving the current screen.

1. Import the alby widget
```kotlin
import com.alby.widget.AlbyWidgetScreen
```
2. Go to the Activity where you want to place the widget and wrap your existing screen with our widget and pass in the required `brandId`, `productId` and `widgetId` parameters:
```kotlin
AlbyWidgetScreen(
    brandId = "your-brand-id", 
    productId = "your-product-id", 
    widgetId = "your-widget-id",
    onThreadIdChanged = { newThreadId ->
        // Handle the new thread ID here
        // newThreadId will be null if the conversation is reset/cleared
        println("Thread ID changed to: $newThreadId")
    }
) {
    YourScreenGoesHere()
}
```

3. Optional parameters:
```kotlin
AlbyWidgetScreen(
    brandId = "your-brand-id",
    productId = "your-product-id",
    widgetId = "your-widget-id",
    threadId = "existing-thread-id", // Restore a previous conversation
    bottomOffset = 56.dp, // Add padding for bottom navigation/bars
    testId = "your-test-id",
    testVersion = "your-test-version",
    testDescription = "your-test-description",
    onThreadIdChanged = { newThreadId -> 
        // Handle thread ID changes
    },
    onWidgetRendered = {
        // Called when the widget has finished rendering
        println("Widget is ready!")
    }
) {
    YourScreenGoesHere()
}
```

### AlbyInlineWidget
The `AlbyInlineWidget` is a component that allows embedding the Alby widget directly into your app's UI. It's perfect for inline use on any page, like product details or brand-specific screens, where the widget integrates seamlessly within the existing view hierarchy.

1. Import the alby widget
```kotlin
import com.alby.widget.AlbyInlineWidget
```

2. In the Composable function where you want to place the widget, add the `AlbyInlineWidget` component:
```kotlin
AlbyInlineWidget(
    modifier = Modifier.padding(24.dp),
    brandId = "your-brand-id",
    productId = "your-product-id",
    widgetId = "your-widget-id",
    threadId = "existing-thread-id", // Optional: restore a previous conversation
    onThreadIdChanged = { newThreadId ->
        // Handle the new thread ID here
        // newThreadId will be null if the conversation is reset/cleared
        println("Thread ID changed to: $newThreadId")
    }
)
```

3. Optional parameters:
```kotlin
AlbyInlineWidget(
    modifier = Modifier.padding(24.dp),
    brandId = "your-brand-id",
    productId = "your-product-id",
    widgetId = "your-widget-id",
    threadId = "existing-thread-id",
    testId = "your-test-id",
    testVersion = "your-test-version",
    testDescription = "your-test-description",
    onThreadIdChanged = { newThreadId -> 
        // Handle thread ID changes
    },
    onWidgetRendered = {
        // Called when the widget has finished rendering
        println("Widget is ready!")
    }
)
```

## Conversation Management
Both `AlbyWidgetScreen` and `AlbyInlineWidget` support conversation persistence through thread IDs:

1. **Restoring Conversations**: Pass an existing thread ID to continue a previous conversation:
```kotlin
threadId = "existing-thread-id"
```

2. **Tracking Thread Changes**: Listen for thread ID changes to persist conversations:
```kotlin
onThreadIdChanged = { newThreadId ->
    if (newThreadId != null) {
        // Save the thread ID for later use
        saveThreadId(newThreadId)
    } else {
        // Conversation was reset/cleared
        clearSavedThreadId()
    }
}
```

## Event Tracking
The SDK also provides an API to sending purchase data and other events via HTTP requests.

### Usage
1. Use the sendPurchasePixel method to send a purchase pixel request:
```kotlin
AlbySDK.sendPurchasePixel(
    orderId = 12345, // Order ID (String or Number)
    orderTotal = 99.99, // Order total (Float or Number)
    variantIds = listOf("A123", 456), // List of variant IDs (String or Number)
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
