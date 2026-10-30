package com.andres3336.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.andres3336.streaming.R
import kotlinx.coroutines.delay

/**
 * Poster grid for Películas and Series: 2:3 covers with the rating on the top left,
 * the favourite star on the top right and a two-line title under the artwork.
 *
 * Seven columns keep the cinematic stage dense enough that a second row is visible
 * under the focused title's details.
 */
@Composable
fun <T> PosterGrid(
    items: List<T>,
    key: (T) -> Any,
    title: (T) -> String,
    image: (T) -> String?,
    rating: (T) -> String? = { null },
    favorite: (T) -> Boolean = { false },
    onClick: (T) -> Unit,
    onToggleFavorite: ((T) -> Unit)? = null,
    onFocusItem: ((T) -> Unit)? = null,
    emptyMessage: String = "No hay elementos",
    focusFirst: Boolean = true,
    gridFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    gridState: LazyGridState? = null,
    initialFocusKey: Any? = null,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyMessage,
                color = StreamColors.TextSecondary,
                fontSize = 17.sp,
            )
        }
        return
    }

    val state = gridState ?: rememberLazyGridState()
    val targetFocus = remember { FocusRequester() }
    val targetIndex = remember(items, initialFocusKey) {
        items.indexOfFirst { key(it) == initialFocusKey }.takeIf { it >= 0 } ?: 0
    }
    var focusRequested by remember { mutableStateOf(false) }

    // Re-entering the screen after a detail restores the last focused poster instead
    // of jumping back to the first one.
    LaunchedEffect(items, initialFocusKey, focusFirst) {
        if (!focusFirst || focusRequested || items.isEmpty()) return@LaunchedEffect
        focusRequested = true
        runCatching { state.scrollToItem(targetIndex) }
        delay(120)
        runCatching { (gridFocusRequester ?: targetFocus).requestFocus() }
    }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(7),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.GridPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GridGapH),
        verticalArrangement = Arrangement.spacedBy(Dimens.GridGapV),
    ) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            val focusModifier = if (index == targetIndex) {
                Modifier.focusRequester(gridFocusRequester ?: targetFocus)
            } else {
                Modifier
            }
            val leftModifier = if (leftFocusRequester != null && index % 7 == 0) {
                Modifier.focusProperties { left = leftFocusRequester }
            } else {
                Modifier
            }
            PosterCard(
                title = title(item),
                image = image(item),
                rating = rating(item),
                favorite = favorite(item),
                modifier = focusModifier.then(leftModifier),
                onClick = { onClick(item) },
                onLongClick = onToggleFavorite?.let { toggle -> { toggle(item) } },
                onFocus = onFocusItem?.let { report -> { report(item) } },
            )
        }
    }
}

@Composable
private fun PosterCard(
    title: String,
    image: String?,
    rating: String?,
    favorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onFocus: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(Dimens.CardCorner)
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        focused -> StreamColors.AccentBright
        favorite -> StreamColors.Favorite
        else -> Color.Transparent
    }
    val borderWidth = if (focused) 2.dp else Dimens.FavoriteBorder
    val ratingText = ratingLabel(rating)

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, shape)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus?.invoke()
            },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = StreamColors.Panel,
            contentColor = StreamColors.TextPrimary,
            focusedContainerColor = StreamColors.CardHover,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(Dimens.CardInnerCorner))
                    .background(StreamColors.Card),
            ) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.ic_channel_placeholder),
                    error = painterResource(R.drawable.ic_channel_placeholder),
                    fallback = painterResource(R.drawable.ic_channel_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (ratingText != null) {
                    PosterOverlay(
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                    ) {
                        Text(
                            text = ratingText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
                if (favorite) {
                    PosterOverlay(
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    ) {
                        Text(
                            text = "\u2605",
                            color = StreamColors.Favorite,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = StreamColors.TextPrimary,
                fontSize = 12.5.sp,
                lineHeight = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height((30 * LocalDensity.current.fontScale).dp),
            )
        }
    }
}

/** Solid chip over the artwork, sized for a 2:3 poster. */
@Composable
private fun PosterOverlay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(StreamColors.BadgeScrim, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Keeps Xtream's raw score; no Rotten Tomatoes percentage is invented. */
private fun ratingLabel(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (value.any { it.isLetter() || it == '%' }) {
        value.take(6)
    } else {
        "★ ${value.take(4)}"
    }
}
