package com.andres3336.streaming.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/**
 * Fixed-height header above the Películas/Series poster grid: title, meta and the
 * synopsis. No artwork and no buttons behind it.
 *
 * Every line reserves its height, so moving across posters never shifts the grid up
 * or down when a title is shorter or has no synopsis. Heights follow the text scale.
 */
@Composable
fun CinematicDetails(
    title: String,
    meta: List<String>,
    synopsis: String?,
) {
    val fontScale = LocalDensity.current.fontScale
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        Text(
            text = title,
            color = StreamColors.TextPrimary,
            fontSize = 30.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .height((36 * fontScale).dp),
        )
        Text(
            text = meta.joinToString("  \u00b7  "),
            color = StreamColors.TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .height((16 * fontScale).dp),
        )
        Spacer(Modifier.height((4 * fontScale).dp))
        Text(
            text = synopsis.orEmpty(),
            color = StreamColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .height((30 * fontScale).dp),
        )
    }
}
