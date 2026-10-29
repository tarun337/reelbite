package com.reelbite.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reelbite.app.ui.theme.BrandGradientHorizontal
import java.io.File
import kotlin.math.absoluteValue

private val placeholderBrushes = listOf(
    Brush.linearGradient(listOf(Color(0xFF3B1F14), Color(0xFF7A2E1C))),
    Brush.linearGradient(listOf(Color(0xFF2B1638), Color(0xFF5E2A6E))),
    Brush.linearGradient(listOf(Color(0xFF14302A), Color(0xFF1F6150))),
    Brush.linearGradient(listOf(Color(0xFF3A2C0E), Color(0xFF7A5A12))),
    Brush.linearGradient(listOf(Color(0xFF15233D), Color(0xFF284A7E))),
    Brush.linearGradient(listOf(Color(0xFF3A1222), Color(0xFF7C2244))),
)

/** Deterministic, pleasant gradient for items that have no image yet. */
fun placeholderBrush(seed: String): Brush = placeholderBrushes[seed.hashCode().absoluteValue % placeholderBrushes.size]

/** Cached reel thumbnail, falling back to an emoji on a gradient. */
@Composable
fun FoodImage(
    path: String?,
    emoji: String,
    seed: String,
    modifier: Modifier = Modifier,
    emojiSize: Int = 40,
    contentDescription: String? = null,
) {
    Box(modifier.background(placeholderBrush(seed)), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = emojiSize.sp, modifier = Modifier.alpha(0.9f))
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Primary call-to-action with the brand gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val shape = RoundedCornerShape(height / 2)
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier.height(height),
    ) {
        // fillMaxHeight (not fillMaxSize): Surface propagates min constraints, so this fills the
        // width only when the caller asks for it (fillMaxWidth) and wraps content otherwise (FAB).
        Box(
            Modifier
                .fillMaxHeight()
                .background(
                    if (enabled) BrandGradientHorizontal else Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.surfaceContainerHighest),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            val contentColor = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 22.dp)) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = contentColor)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, color = contentColor, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** Small rounded label, e.g. "3 reels". */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = Color.Black.copy(alpha = 0.55f),
    content: Color = Color.White,
) {
    Surface(shape = CircleShape, color = container, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            Text(text, color = content, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Large emoji inside a gradient circle, used by empty states. */
@Composable
fun EmojiHalo(emoji: String, modifier: Modifier = Modifier, size: Dp = 112.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0x66FF7A45), Color(0x229B4DFF), Color.Transparent))),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value / 2.2f).sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = modifier,
    )
}

val ScreenPadding = PaddingValues(horizontal = 16.dp)
