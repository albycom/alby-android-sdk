package com.alby.widget

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AlbyWidgetWebViewInterface(
    private val coroutineScope: CoroutineScope,
    private val bottomSheetState: HideableBottomSheetState,
    private val isLoading: MutableState<Boolean>,
    private val isLoadingText: MutableState<String>
) {

    @JavascriptInterface
    fun postMessage(
        message: String
    ) {
        coroutineScope.launch {
            with(message) {
                when {
                    message == "preview-button-clicked" -> {
                        bottomSheetState.expand()
                    }

                    message == "widget-rendered" -> {
                        bottomSheetState.show()
                    }

                    contains("streaming-message") -> {
                        isLoading.value = true
                        val replacedResult = message.replace("streaming-message:", "")
                        isLoadingText.value = replacedResult
                    }

                    contains("streaming-finished") -> {
                        isLoading.value = false
                        isLoadingText.value = ""
                    }
                }
            }
        }
    }
}


@Composable
fun AlbyWidgetScreen(
    brandId: String,
    productId: String,
    variantId: String? = null,
    content: @Composable () -> Unit
) {
    val bottomSheetState =
        rememberHideableBottomSheetState(initialValue = HideableBottomSheetValue.Hidden)

    val webViewReference = remember { mutableStateOf<WebView?>(null) }
    val isLoading = remember { mutableStateOf(false) }
    val isLoadingText = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val jsInterface =
        AlbyWidgetWebViewInterface(coroutineScope, bottomSheetState, isLoading, isLoadingText)

    LaunchedEffect(bottomSheetState.targetValue) {
        Log.d("state", bottomSheetState.targetValue.toString())
        if (bottomSheetState.targetValue == HideableBottomSheetValue.HalfExpanded || bottomSheetState.targetValue == HideableBottomSheetValue.Expanded) {
            publishEvent(webViewReference.value, "sheet-expanded")
        } else {
            publishEvent(webViewReference.value, "sheet-shrink")
        }
    }

    HideableBottomSheetScaffold(
        bottomSheetState = bottomSheetState,
        bottomSheetContent = {
            BottomSheet(
                bottomSheetState,
                webViewReference,
                jsInterface,
                brandId,
                productId,
                variantId
            )
        },
        bottomSheetStickyItem = {
            BottomSheetInputText(
                webViewReference,
                isLoading,
                isLoadingText
            )
        },
        sheetBackgroundColor = Color.White,
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
fun BottomSheet(
    state: HideableBottomSheetState,
    webViewReference: MutableState<WebView?>,
    webViewInterface: AlbyWidgetWebViewInterface,
    brandId: String,
    productId: String,
    variantId: String? = null
) {
    val configuration = LocalConfiguration.current
    val heightDP = configuration.screenHeightDp


    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter
        ) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Drag handle",
                tint = Color(121, 116, 126, 255),
            )
        }
        if(state.isExpanded || state.isHalfExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Text("Powered by", color = Color(147, 157, 175, 255), fontSize = 11.sp)
                    Box(
                        modifier = Modifier.height(13.dp).padding(start = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.alby_logo),
                            contentDescription = "Alby logo",
                            modifier = Modifier.fillMaxHeight(),
                            contentScale = ContentScale.FillHeight,
                        )
                    }
                }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close alby widget",
                        tint = Color(121, 116, 126, 255)
                    )
                }
            }
            HorizontalDivider(color = Color(229, 231, 235, 255))
        }

        Box(
            modifier = Modifier
                .background(Color.White)
                .height(calculateHeight(state, heightDP))
                .fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            LazyColumn(userScrollEnabled = true) {
                item {
                    WebViewScreen(webViewInterface, webViewReference, brandId, productId, variantId)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetInputText(
    webViewReference: MutableState<WebView?>,
    isLoading: MutableState<Boolean>,
    isLoadingText: MutableState<String>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    val colors =
        OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(107, 114, 128, 255),
            focusedBorderColor = Color(107, 114, 128, 255),
            disabledContainerColor = Color(229, 231, 235, 255),
            disabledBorderColor = Color.Transparent
        )

    var text by remember { mutableStateOf("") }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val placeholderText = isLoadingText.value.ifEmpty { "Ask any question about this product" }
    val placeholderPadding = if (isLoading.value) {
        24.dp
    } else {
        0.dp
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = text,
            onValueChange = { newText -> text = newText },
            modifier = Modifier
                .weight(1f)
                .focusable(),
            textStyle = TextStyle(fontSize = 14.sp, color = Color(17, 25, 40, 255)),
            singleLine = true,
            enabled = !isLoading.value,
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    publishEvent(webViewReference.value, text)
                    text = ""
                },
            ),
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = text,
                placeholder = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (isLoading.value) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.CenterStart),
                                color = Color(96, 96, 96, 153),
                                strokeWidth = 1.dp
                            )

                        }
                        Text(
                            placeholderText,
                            color = Color(96, 96, 96, 153),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = placeholderPadding)
                        )
                    }

                },
                innerTextField = innerTextField,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                colors = colors,
                enabled = !isLoading.value,
                interactionSource = interactionSource,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = !isLoading.value,
                        isError = false,
                        interactionSource = interactionSource,
                        shape = RoundedCornerShape(12.dp),
                        colors = colors,
                        modifier = Modifier.fillMaxSize(),
                        focusedBorderThickness = 1.dp,
                        unfocusedBorderThickness = 1.dp
                    )
                },
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    top = 7.dp,
                    bottom = 7.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
            )
        }

        if (isFocused && text.isNotEmpty()) {
            Spacer(modifier = Modifier.width(10.dp))
            FloatingActionButton(
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier = Modifier
                    .height(36.dp)
                    .width(36.dp),

                containerColor = Color(17, 25, 40, 255),
                shape = RoundedCornerShape(100),
                onClick = {
                    focusManager.clearFocus()
                    publishEvent(webViewReference.value, text)
                    text = ""
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Submit icon",
                    tint = Color(255, 255, 255, 255),
                    modifier = Modifier
                        .height(14.dp)
                        .width(14.dp)

                )
            }
        }

    }

}