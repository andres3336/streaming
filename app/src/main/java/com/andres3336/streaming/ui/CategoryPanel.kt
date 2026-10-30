package com.andres3336.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

data class CategoryEntry(
    val id: String,
    val label: String,
    val favorite: Boolean = false,
)

/**
 * The category rail inside the content area (20-25% of the wireframe).
 * Rows are plain surfaces instead of bordered buttons so the list reads as a
 * sidebar, not as a second navigation bar.
 */
@Composable
fun CategoryPanel(
    entries: List<CategoryEntry>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Categorías",
    selectedFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(StreamColors.Panel, RoundedCornerShape(Dimens.PanelCorner))
            .padding(Dimens.PanelPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                color = StreamColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                text = entries.size.toString(),
                color = StreamColors.TextMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 4.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                CategoryRow(
                    entry = entry,
                    selected = entry.id == selectedId,
                    onClick = { onSelect(entry.id) },
                    selectedFocusRequester = if (entry.id == selectedId) {
                        selectedFocusRequester
                    } else {
                        null
                    },
                    rightFocusRequester = if (entry.id == selectedId) {
                        rightFocusRequester
                    } else {
                        null
                    },
                )
            }
        }
        footer?.let {
            Spacer(Modifier.height(8.dp))
            it()
        }
    }
}

@Composable
private fun CategoryRow(
    entry: CategoryEntry,
    selected: Boolean,
    onClick: () -> Unit,
    selectedFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
) {
    val shape = RoundedCornerShape(9.dp)
    var focused by remember { mutableStateOf(false) }
    val ring = when {
        focused -> StreamColors.Accent
        else -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selectedFocusRequester != null) {
                    Modifier.focusRequester(selectedFocusRequester)
                } else {
                    Modifier
                },
            )
            .focusProperties {
                if (rightFocusRequester != null) {
                    right = rightFocusRequester
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .border(2.dp, ring, shape),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) StreamColors.SelectedRow else Color.Transparent,
            contentColor = if (selected) StreamColors.TextPrimary else StreamColors.TextSecondary,
            focusedContainerColor = StreamColors.CardHover,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.02f,
            pressedScale = 0.98f,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(
                        color = if (selected) StreamColors.Accent else Color.Transparent,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
            Spacer(Modifier.width(9.dp))
            if (entry.favorite) {
                Text(
                    text = "★",
                    color = StreamColors.Favorite,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = entry.label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
