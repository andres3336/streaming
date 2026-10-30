package com.andres3336.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Flat modal card drawn in its own window.
 *
 * A separate window is what makes it modal: key events can no longer reach the
 * section behind it, so D-pad focus cannot escape the dialog and Back always
 * dismisses it. Focus is grouped inside the card and restored when it closes.
 *
 * [initialFocus] is retried until the dialog window is attached, because a single
 * request from a fixed delay races with the window creation on slow TVs.
 */
@Composable
fun ModalCard(
    onDismiss: () -> Unit,
    initialFocus: FocusRequester? = null,
    width: Dp = 520.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (initialFocus != null) {
        LaunchedEffect(initialFocus) {
            repeat(20) {
                if (runCatching { initialFocus.requestFocus() }.isSuccess) {
                    return@LaunchedEffect
                }
                delay(50)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StreamColors.Scrim),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(width)
                    .focusGroup()
                    .focusRestorer()
                    .background(StreamColors.Surface, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}
