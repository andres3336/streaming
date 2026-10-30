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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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

/** One card in a home row. */
data class MediaCardModel(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val image: String? = null,
    val progress: Float? = null,
    val badge: String? = null,
)

/** Horizontal poster/thumb row with a flat focus ring and no motion. */
@Composable
fun MediaRow(
    title: String,
    items: List<MediaCardModel>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    count: String? = null,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = StreamColors.TextPrimary,
                fontSize = TextSizes.SectionTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = count,
                    color = StreamColors.TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 2.dp, end = 24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items, key = { it.key }) { item ->
                MediaCard(model = item, onClick = { onSelect(item.key) })
            }
        }
    }
}

@Composable
fun MediaCard(
    model: MediaCardModel,
    modifier: Modifier = Modifier,
    width: Int = 196,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.CardCorner)
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(width.dp)
            .border(1.dp, if (focused) StreamColors.AccentBright else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = StreamColors.TextPrimary,
            focusedContainerColor = StreamColors.SurfaceRaised,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(Dimens.CardInnerCorner))
                    .background(StreamColors.SurfaceSunken),
            ) {
                AsyncImage(
                    model = model.image,
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.ic_channel_placeholder),
                    error = painterResource(R.drawable.ic_channel_placeholder),
                    fallback = painterResource(R.drawable.ic_channel_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (model.badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                            .background(StreamColors.Background, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = model.badge,
                            color = StreamColors.TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (model.progress != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .height(3.dp)
                            .background(StreamColors.Background, RoundedCornerShape(2.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(model.progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(StreamColors.Accent, RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = model.title,
                color = StreamColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!model.subtitle.isNullOrBlank()) {
                Text(
                    text = model.subtitle,
                    color = StreamColors.TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
