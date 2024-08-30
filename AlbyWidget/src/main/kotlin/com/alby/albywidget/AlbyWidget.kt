package com.alby.widget

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
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
                    Log.d("widget", "Widget rendered")
                    bottomSheetState.show()
                }
            }
        }
    }
}


@Composable
fun AlbyWidgetScreen(content: @Composable () -> Unit) {
    val bottomSheetState =
        rememberHideableBottomSheetState(initialValue = HideableBottomSheetValue.Hidden)
    val webViewReference = remember { mutableStateOf<WebView?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(bottomSheetState.targetValue) {
        if (bottomSheetState.targetValue == HideableBottomSheetValue.Expanded || bottomSheetState.targetValue == HideableBottomSheetValue.HalfExpanded) {
            publishEvent(webViewReference.value, "sheet-expanded")
        }
    }

    HideableBottomSheetScaffold(
        bottomSheetState = bottomSheetState,
        bottomSheetContent = { BottomSheet(bottomSheetState, webViewReference) },
        bottomSheetStickyItem = { BottomSheetInputText(bottomSheetState, webViewReference) },
        sheetBackgroundColor = Color.White,
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
fun BottomSheet(state: HideableBottomSheetState, webViewReference: MutableState<WebView?>) {
    val scope = rememberCoroutineScope()

    val jsInterface = AlbyWidgetWebViewInterface(scope, state)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.DragHandle,
            contentDescription = stringResource(id = androidx.compose.ui.R.string.close_drawer)
        )
        WebViewScreen(jsInterface, webViewReference)
    }
}

@Composable
fun BottomSheetInputText(
    state: HideableBottomSheetState,
    webViewReference: MutableState<WebView?>
) {
    var text by remember { mutableStateOf("") }

    if (state.isExpanded || state.isHalfExpanded) {
        OutlinedTextField(
            value = text,
            onValueChange = { newText -> text = newText },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Ask your question") },
            trailingIcon = {
                IconButton(onClick = {
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

fun submitMessage(message: String) {

}