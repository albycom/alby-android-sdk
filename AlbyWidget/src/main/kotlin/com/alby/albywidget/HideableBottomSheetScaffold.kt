package com.alby.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
    content: @Composable ColumnScope.() -> Unit
) {

    var layoutHeight by remember { mutableIntStateOf(0) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current;

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .onSizeChanged {
                layoutHeight = it.height
                if (layoutHeight > 0 && sheetHeight > 0) {
                    bottomSheetState.updateAnchors(layoutHeight, sheetHeight, density)
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            content()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
                .offset {
                    val yOffset = bottomSheetState
                        .requireOffset()
                        .roundToInt()
                    IntOffset(x = 0, y = yOffset)
                }
                .anchoredDraggable(
                    state = bottomSheetState.draggableState,
                    orientation = Orientation.Vertical
                )
                .background(sheetBackgroundColor, sheetShape)
                .padding(vertical = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .onSizeChanged {
                        sheetHeight = it.height
                        if (layoutHeight > 0 && sheetHeight > 0) {
                            bottomSheetState.updateAnchors(layoutHeight, sheetHeight, density)
                        }
                    },
                content = bottomSheetContent
            )
        }
        if (!bottomSheetState.isHidden) {
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