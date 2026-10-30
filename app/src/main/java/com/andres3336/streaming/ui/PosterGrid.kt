package com.andres3336.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
 * Four-column poster grid used by Películas, Series, Continue and Favoritos.
 *
 * Cards use a 16:9 image so two full rows fit above the fold at TV densities.
 * Badges follow the wireframe order: favorite on the upper left, rating and
 * language on the upper right.
 */
@Composable
fun <T> PosterGrid(
    items: List<T>,
    key: (T) -> Any,
    title: (T) -> String,
    subtitle: (T) -> String?,
    image: (T) -> String?,
    rating: (T) -> String? = { null },
    language: (T) -> String? = { null },
    favorite: (T) -> Boolean = { false },
    onClick: (T) -> Unit,
    onToggleFavorite: ((T) -> Unit)? = null,
    onFocusItem: ((T) -> Unit)? = null,
    emptyMessage: String = "No hay elementos",
    focusFirst: Boolean = true,
    gridFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
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

    val state = rememberLazyGridState()
    val firstFocus = remember { FocusRequester() }
    val autoFocus = gridFocusRequester ?: firstFocus
    var focusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(items.size, focusFirst) {
        if (!focusFirst || focusRequested || items.isEmpty()) return@LaunchedEffect
        delay(250)
        runCatching { autoFocus.requestFocus() }
        focusRequested = true
    }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.GridPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GridGapH),
        verticalArrangement = Arrangement.spacedBy(Dimens.GridGapV),
    ) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            val focusModifier = when {
                index != 0 -> Modifier
                gridFocusRequester != null -> Modifier.focusRequester(gridFocusRequester)
                else -> Modifier.focusRequester(firstFocus)
            }
            val leftModifier = if (leftFocusRequester != null && index % 4 == 0) {
                Modifier.focusProperties { left = leftFocusRequester }
            } else {
                Modifier
            }
            PosterCard(
                title = title(item),
                subtitle = subtitle(item),
                image = image(item),
                rating = rating(item),
                language = language(item),
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
    subtitle: String?,
    image: String?,
    rating: String?,
    language: String?,
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
    val languageText = language?.takeIf { it.isNotBlank() }

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
                .padding(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
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
                if (favorite) {
                    FavoriteStar(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                }
                if (ratingText != null || languageText != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ratingText?.let { Badge(text = it) }
                        languageText?.let { Badge(text = it) }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            ) {
                Text(
                    text = title,
                    color = StreamColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = subtitle?.takeIf { it.isNotBlank() }
                if (sub != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = sub,
                        color = StreamColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .background(StreamColors.BadgeScrim, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun FavoriteStar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(StreamColors.BadgeScrim, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "★",
            color = StreamColors.Favorite,
            fontSize = 13.sp,
        )
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
