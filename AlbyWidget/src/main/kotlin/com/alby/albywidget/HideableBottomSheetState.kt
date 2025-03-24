package com.alby.widget

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.min

enum class HideableBottomSheetValue {
    Hidden,
    HalfExpanded,
    Expanded,
    Initial;

    val draggableSpaceFraction: Float
        get() = when (this) {
            Hidden -> -1f
            Initial -> 0.09f
            HalfExpanded -> 0.6f
            Expanded -> 1f
        }
}

object HideableBottomSheetDefaults {
    val AnimationSpec = SpringSpec<Float>()
    val DecayAnimationSpec:  DecayAnimationSpec<Float> = exponentialDecay()

    val PositionalThreshold = { distance: Float -> distance * 0.2f }

    val VelocityThreshold = { 125f }
}

@OptIn(ExperimentalFoundationApi::class)
@Stable
class HideableBottomSheetState(
    initialValue: HideableBottomSheetValue,
    private val animationSpec: AnimationSpec<Float> = HideableBottomSheetDefaults.AnimationSpec,
    private val decayAnimationSpec: DecayAnimationSpec<Float> = HideableBottomSheetDefaults.DecayAnimationSpec,
    private val confirmValueChange: (HideableBottomSheetValue) -> Boolean = { true }
) {
    val draggableState = AnchoredDraggableState(
        initialValue = initialValue,
        positionalThreshold = HideableBottomSheetDefaults.PositionalThreshold,
        velocityThreshold = HideableBottomSheetDefaults.VelocityThreshold,
        animationSpec = animationSpec,
        confirmValueChange = confirmValueChange
    )

    /**
     * The current value of the [HideableBottomSheetState].
     */
    val currentValue: HideableBottomSheetValue
        get() = draggableState.currentValue

    val targetValue: HideableBottomSheetValue
        get() = draggableState.targetValue

    /**
     * Whether the bottom sheet is visible.
     */
    val isVisible: Boolean
        get() = currentValue != HideableBottomSheetValue.Hidden

    /**
     * Whether the bottom sheet is expanded.
     */
    val isExpanded: Boolean
        get() = currentValue == HideableBottomSheetValue.Expanded

    /**
     * Whether the bottom sheet is half expanded.
     */
    val isHalfExpanded: Boolean
        get() = currentValue == HideableBottomSheetValue.HalfExpanded

    /**
     * Whether the bottom sheet is hidden.
     */
    val isHidden: Boolean
        get() = currentValue == HideableBottomSheetValue.Hidden

    private val hasHalfExpandedState: Boolean
        get() = draggableState.anchors.hasAnchorFor(HideableBottomSheetValue.HalfExpanded)

    private val hasInitialState: Boolean
        get() = draggableState.anchors.hasAnchorFor(HideableBottomSheetValue.Initial)

    /**
     * Show the bottom sheet with animation and suspend until it's shown.
     * If the sheet is taller than 50% of the parent's height, the bottom sheet will be half expanded.
     * Otherwise, it will be fully expanded.
     */
    suspend fun show() {
        val targetValue = when {
            hasInitialState -> HideableBottomSheetValue.Initial
            else -> HideableBottomSheetValue.HalfExpanded
        }
        animateTo(targetValue)
    }

    /**
     * Expand the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun expand() {
        if (draggableState.anchors.hasAnchorFor(HideableBottomSheetValue.Expanded)) {
            animateTo(HideableBottomSheetValue.Expanded)
        }
    }

    /**
     * Half expand the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun halfExpand() {
        if (draggableState.anchors.hasAnchorFor(HideableBottomSheetValue.HalfExpanded)) {
            animateTo(HideableBottomSheetValue.HalfExpanded)
        }
    }

    /**
     * Hide the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun hide() {
        animateTo(HideableBottomSheetValue.Hidden)
    }

    fun requireOffset() = draggableState.requireOffset()

    private suspend fun animateTo(
        targetValue: HideableBottomSheetValue
    ) = draggableState.animateTo(targetValue)

    companion object {
        /**
         * The default [Saver] implementation for [HideableBottomSheetState].
         */
        fun Saver(
            confirmValueChange: (HideableBottomSheetValue) -> Boolean = { true },
            animationSpec: AnimationSpec<Float> = HideableBottomSheetDefaults.AnimationSpec
        ): Saver<HideableBottomSheetState, HideableBottomSheetValue> =
            Saver(
                save = { it.currentValue },
                restore = {
                    HideableBottomSheetState(
                        initialValue = it,
                        animationSpec = animationSpec,
                        confirmValueChange = confirmValueChange
                    )
                }
            )
    }

    fun updateAnchors(layoutHeight: Int, sheetHeight: Int, density: Density) {
        val maxDragEndPoint = layoutHeight - with(density) { 32.dp.toPx() }
        val newAnchors = DraggableAnchors<HideableBottomSheetValue> {
            HideableBottomSheetValue.values()
                .forEach { anchor ->
                    val fractionatedMaxDragEndPoint =
                        maxDragEndPoint * anchor.draggableSpaceFraction
                    val dragEndPoint =
                        layoutHeight - min(fractionatedMaxDragEndPoint, sheetHeight.toFloat())
                    anchor at dragEndPoint
                }
        }
        draggableState.updateAnchors(newAnchors)
    }

}


@Composable
fun rememberHideableBottomSheetState(
    initialValue: HideableBottomSheetValue,
    animationSpec: AnimationSpec<Float> = HideableBottomSheetDefaults.AnimationSpec,
    confirmValueChange: (HideableBottomSheetValue) -> Boolean = { true },
): HideableBottomSheetState {
    return key(initialValue) {
        rememberSaveable(
            initialValue, animationSpec, confirmValueChange,
            saver = HideableBottomSheetState.Saver(
                confirmValueChange = confirmValueChange,
                animationSpec = animationSpec
            )
        ) {
            HideableBottomSheetState(
                initialValue = initialValue,
                animationSpec = animationSpec,
                confirmValueChange = confirmValueChange
            )
        }
    }
}