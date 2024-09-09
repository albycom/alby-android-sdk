package com.alby.widget

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AlbyWidgetWebViewInterface(
    private val coroutineScope: CoroutineScope,
    private val bottomSheetState: HideableBottomSheetState
) {

    @JavascriptInterface
    fun postMessage(
        message: String
    ) {
        coroutineScope.launch {
            when (message) {
                "preview-button-clicked" -> {
                    bottomSheetState.expand()
                }

                "widget-rendered" -> {
                    bottomSheetState.show()
                }
            }
        }
    }
}


@Composable
fun AlbyWidgetScreen(brandId: String, productId: String, variantId: String? = null, content: @Composable () -> Unit) {
    val bottomSheetState =
        rememberHideableBottomSheetState(initialValue = HideableBottomSheetValue.Hidden)
    val webViewReference = remember { mutableStateOf<WebView?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(bottomSheetState.targetValue) {
        if (bottomSheetState.targetValue == HideableBottomSheetValue.Expanded || bottomSheetState.targetValue == HideableBottomSheetValue.HalfExpanded) {
            publishEvent(webViewReference.value, "sheet-expanded")
        } else {
            publishEvent(webViewReference.value, "sheet-shrink")
        }
    }

    HideableBottomSheetScaffold(
        bottomSheetState = bottomSheetState,
        bottomSheetContent = { BottomSheet(bottomSheetState, webViewReference, brandId, productId, variantId) },
        bottomSheetStickyItem = { BottomSheetInputText(bottomSheetState, webViewReference) },
        sheetBackgroundColor = Color.White,
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
fun BottomSheet(state: HideableBottomSheetState, webViewReference: MutableState<WebView?>, brandId: String, productId: String, variantId: String? = null) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val heightDP = configuration.screenHeightDp

    val jsInterface = AlbyWidgetWebViewInterface(scope, state)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.DragHandle,
            contentDescription = "Drag handle"
        )
        Box(
            modifier = Modifier
                .background(Color.White)
                .height(calculateHeight(state, heightDP))
                .fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            LazyColumn(userScrollEnabled = true) {
                item {
                    WebViewScreen(jsInterface, webViewReference, brandId, productId, variantId)
                }
            }
        }

    }
}

fun calculateHeight(state: HideableBottomSheetState, screenHeight: Int): Dp {
    if (state.isHidden) {
        return 0.dp
    }

    if (state.isHalfExpanded) {
        return (HideableBottomSheetValue.HalfExpanded.draggableSpaceFraction * screenHeight - 100).dp
    }

    if (state.isExpanded) {
        return (HideableBottomSheetValue.Expanded.draggableSpaceFraction * screenHeight - 100).dp
    }
    return 100.dp
}

@Composable
fun BottomSheetInputText(
    state: HideableBottomSheetState,
    webViewReference: MutableState<WebView?>
) {
    val focusManager = LocalFocusManager.current

    var text by remember { mutableStateOf("") }

    if (state.isExpanded || state.isHalfExpanded) {
        OutlinedTextField(
            value = text,
            singleLine = true,
            onValueChange = { newText -> text = newText },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Ask your question") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    publishEvent(webViewReference.value, text)
                    text = ""
                },
            ),
            trailingIcon = {
                IconButton(onClick = {
                    focusManager.clearFocus()
                    publishEvent(webViewReference.value, text)
                    text = ""
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowCircleUp,
                        contentDescription = "Submit icon"
                    )
                }
            }
        )
    }
}