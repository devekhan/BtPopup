package com.afroz.btpopup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.roundToInt

/* ---------- Rotating device ---------- */

// Photos ordered from one 3/4 side, through the front, to the profile view.
// The animation sways through them and back (ping-pong) with a cross-fade.
private val NOTHING_HEADPHONE_FRAMES = listOf(
    R.drawable.nh1_0, R.drawable.nh1_1, R.drawable.nh1_2,
    R.drawable.nh1_3, R.drawable.nh1_4, R.drawable.nh1_5,
)

@Composable
fun RotatingDevice(kind: DeviceKind, modifier: Modifier = Modifier) {
    when (kind) {
        DeviceKind.NOTHING_HEADPHONE -> SwayingFrames(NOTHING_HEADPHONE_FRAMES, modifier)
        else -> SpinningIcon(modifier)
    }
}

@Composable
private fun SwayingFrames(frames: List<Int>, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "sway")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (frames.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "t",
    )
    val bob by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val k = floor(t).toInt().coerceIn(0, frames.size - 2)
    val f = (t - k).coerceIn(0f, 1f)

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        // soft light behind the device
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                    center = center, radius = size.minDimension * 0.5f,
                ),
            )
        }
        // floor shadow
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width * 0.55f
            val h = size.height * 0.07f
            drawOval(
                brush = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.97f), radius = w / 2f,
                ),
                topLeft = Offset((size.width - w) / 2f, size.height * 0.97f - h / 2f),
                size = Size(w, h),
            )
        }
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = bob.dp.toPx() }) {
            // lower frame stays opaque, next frame fades in on top: no see-through dip
            Image(painterResource(frames[k]), contentDescription = null, modifier = Modifier.fillMaxSize())
            Image(
                painterResource(frames[k + 1]), contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = f },
            )
        }
    }
}

@Composable
private fun SpinningIcon(modifier: Modifier) {
    val angle by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "angle",
    )
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ic_headphones), contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer { rotationY = angle; cameraDistance = 14f * density },
        )
    }
}

/* ---------- Popup card ---------- */

@Composable
fun PopupHost(info: PopupInfo, onDismissed: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) } // 0 = entering, 1 = shown, 2 = leaving
    var dragY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        phase = 1
        delay(5000)
        if (phase == 1) phase = 2
    }
    LaunchedEffect(phase) {
        if (phase == 2) {
            delay(380)
            onDismissed()
        }
    }

    Box(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 28.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = phase == 1,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it },
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(tween(320), targetOffsetY = { it }) + fadeOut(tween(250)),
        ) {
            PopupCard(
                info,
                Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(0, dragY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { if (dragY > 100f) phase = 2 else dragY = 0f },
                            onDragCancel = { dragY = 0f },
                        ) { _, delta -> dragY = (dragY + delta).coerceAtLeast(0f) }
                    },
            )
        }
    }
}

@Composable
private fun PopupCard(info: PopupInfo, modifier: Modifier) {
    val shape = RoundedCornerShape(36.dp)
    Column(
        modifier
            .shadow(28.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2B2B2F), Color(0xFF18181A))))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RotatingDevice(info.kind, Modifier.height(170.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            info.name,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.size(8.dp).background(Color(0xFF34C759), CircleShape))
            Spacer(Modifier.size(8.dp))
            Text("Connected", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            if (info.batteryPercent != null) {
                Spacer(Modifier.size(12.dp))
                Text("Battery ${info.batteryPercent}%", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            }
        }
    }
}
