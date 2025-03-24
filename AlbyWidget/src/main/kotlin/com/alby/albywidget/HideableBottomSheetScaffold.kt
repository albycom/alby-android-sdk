package com.alby.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dropShadow
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HideableBottomSheetScaffold(
    bottomSheetState: HideableBottomSheetState,
    bottomSheetContent: @Composable BoxScope.() -> Unit,
    bottomSheetStickyItem: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetShape: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    sheetBackgroundColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    var layoutHeight by remember { mutableIntStateOf(0) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current;
    val imePadding = WindowInsets.ime.asPaddingValues() // Handle keyboard insets
    val isFirstRenderSheet = remember { mutableStateOf(true) }
    val isFirstRenderSheetContent = remember { mutableStateOf(true) }


    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(imePadding)
            .onSizeChanged {
                if (isFirstRenderSheet.value) {
                    layoutHeight = it.height
                    if (layoutHeight > 0 && sheetHeight > 0) {
                        bottomSheetState.updateAnchors(layoutHeight, sheetHeight, density)
                    }
                    isFirstRenderSheet.value = false
                }

            }

    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .offset {
                    val yOffset = bottomSheetState
                        .requireOffset()
                        .roundToInt()
                    IntOffset(x = 0, y = yOffset)
                }
                .anchoredDraggable<HideableBottomSheetValue>(
                    state = bottomSheetState.draggableState,
                    orientation = Orientation.Vertical
                )
                .doubleShadowDrop(
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    firstOffset = (-1).dp,
                    secondOffset = (-4).dp,
                    firstBlur = 3.dp,
                    secondBlur = 12.dp,
                    firstSpread = 0.dp,
                    secondSpread = 3.dp,
                )
                .background(sheetBackgroundColor, sheetShape)
        ) {
            Box(
                modifier = Modifier
                    .onSizeChanged {
                        if (isFirstRenderSheetContent.value) {
                            sheetHeight = it.height
                            if (layoutHeight > 0 && sheetHeight > 0) {
                                bottomSheetState.updateAnchors(layoutHeight, sheetHeight, density)
                            }
                            isFirstRenderSheetContent.value = false
                        }
                    },
                content = bottomSheetContent
            )
        }
        if (bottomSheetState.isExpanded || bottomSheetState.isHalfExpanded) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.White)
                        .padding(16.dp),
                    content = bottomSheetStickyItem
                )
            }
        }
    }
}


fun Modifier.doubleShadowDrop(
    shape: Shape,
    firstOffset: Dp,
    secondOffset: Dp,
    firstBlur: Dp,
    secondBlur: Dp,
    firstSpread: Dp,
    secondSpread: Dp,
) = this
    .dropShadow(shape, Color.Black.copy(0.3f), firstBlur, firstOffset, firstOffset, firstSpread)
    .dropShadow(
        shape,
        Color.White.copy(0.15f),
        secondBlur,
        secondOffset,
        secondOffset,
        secondSpread
    )
