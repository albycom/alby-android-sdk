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
This SDK only works with Jetpack Compose.

1. Make sure you have an Alby account - if you don't, go to https://alby.com and create one.
2. Get your brand id
3. Import the alby widget `import com.alby.widget.AlbyWidgetScreen`
3. Go to the Activity where you want to place the widget and wrap your existing screen with our widget
```
AlbyWidgetScreen(brandId = "your-brand-id", productId ="your-product-id" ) {
 YourScreenGoesHere()
}
```

The default placement will be in the bottom of the screen. If you have a bottom bar or something similar, make sure you add a bottom
offset. In the example below we are moving the alby bottom sheet 50 points upwards.

```
AlbyWidgetScreen(brandId = "your-brand-id", productId ="your-product-id", bottomOffset = 20 ) {
 YourScreenGoesHere()
}
```

### Example
```kotlin
AlbyWidgetScreen(brandId = "your-brand-id", productId ="your-product-id" ) {
    DetailScreen(
        navigator = destinationsNavigator,
        viewModel = viewModel,
        onHideSystemUI = {
            hideSystemBars()
        }
    )
}
```