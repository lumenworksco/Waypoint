package com.florian.waypoint

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Apple-Fitness-style three concentric activity rings: Vertical / Runs / Time.
 *
 * Each ring fills smoothly from 0% → its goal as [progress] changes. Values
 * clamp at 100% (no "over-achieve" loops yet — could add later). Drawn with
 * rounded stroke caps and a subtle dimmed background track so a barely-started
 * ring still shows its shape.
 *
 * The palette mimics Apple's:
 *   - Move (outer) = red
 *   - Exercise (middle) = green
 *   - Stand (inner) = blue
 */

data class RingProgress(
    /** 0.0 to 1.0+ */
    val verticalPct: Float,
    val runsPct: Float,
    val timePct: Float
)

/** Apple Fitness colors, tweaked slightly toward iOS system colors. */
private val MoveColor = Color(0xFFFA2D48)
private val ExerciseColor = Color(0xFFA5F438)
private val StandColor = Color(0xFF38D6FF)

@Composable
fun ActivityRings(
    progress: RingProgress,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 140.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 14.dp,
    animate: Boolean = true,
) {
    val animSpec = tween<Float>(durationMillis = 900)
    val animVert by animateFloatAsState(
        targetValue = progress.verticalPct.coerceAtMost(1f),
        animationSpec = animSpec,
        label = "vert"
    )
    val animRuns by animateFloatAsState(
        targetValue = progress.runsPct.coerceAtMost(1f),
        animationSpec = animSpec,
        label = "runs"
    )
    val animTime by animateFloatAsState(
        targetValue = progress.timePct.coerceAtMost(1f),
        animationSpec = animSpec,
        label = "time"
    )

    Canvas(modifier = modifier.size(size)) {
        val sw = strokeWidth.toPx()
        val padding = sw / 2f + 2f
        val fullSize = this.size.minDimension

        // Outermost ring: Vertical (red)
        drawRing(
            color = MoveColor,
            progress = if (animate) animVert else progress.verticalPct.coerceAtMost(1f),
            topLeft = Offset(padding, padding),
            diameter = fullSize - 2 * padding,
            strokeWidth = sw
        )

        // Middle ring: Runs (green)
        val midPad = padding + sw + 4f
        drawRing(
            color = ExerciseColor,
            progress = if (animate) animRuns else progress.runsPct.coerceAtMost(1f),
            topLeft = Offset(midPad, midPad),
            diameter = fullSize - 2 * midPad,
            strokeWidth = sw
        )

        // Innermost ring: Time (blue)
        val innerPad = midPad + sw + 4f
        drawRing(
            color = StandColor,
            progress = if (animate) animTime else progress.timePct.coerceAtMost(1f),
            topLeft = Offset(innerPad, innerPad),
            diameter = fullSize - 2 * innerPad,
            strokeWidth = sw
        )
    }
}

private fun DrawScope.drawRing(
    color: Color,
    progress: Float,
    topLeft: Offset,
    diameter: Float,
    strokeWidth: Float,
) {
    if (diameter <= 0) return
    val size = Size(diameter, diameter)
    // Dim background track
    drawArc(
        color = color.copy(alpha = 0.18f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
    // Active arc — start at -90° so we begin at 12 o'clock
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

/**
 * Compact legend shown below the rings — colored dot + label/current/goal.
 */
@Composable
fun RingsLegend(
    vertical: Double,
    verticalGoal: Double,
    runs: Int,
    runsGoal: Int,
    durationMs: Long,
    durationGoalMs: Long,
    imperial: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        LegendRow(
            color = MoveColor,
            label = "Vertical",
            current = formatVertical(vertical, imperial),
            goal = formatVertical(verticalGoal, imperial)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LegendRow(
            color = ExerciseColor,
            label = "Runs",
            current = runs.toString(),
            goal = runsGoal.toString()
        )
        Spacer(modifier = Modifier.height(6.dp))
        LegendRow(
            color = StandColor,
            label = "Time",
            current = formatDuration(durationMs),
            goal = formatDuration(durationGoalMs)
        )
    }
}

@Composable
private fun LegendRow(color: Color, label: String, current: String, goal: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Surface(
            modifier = Modifier.size(10.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$current / $goal",
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Compute the three ring percentages from today's aggregated stats vs the
 * user's configured goals. Each is uncapped (caller decides whether to clamp)
 * so we can detect "closed" crossings.
 */
fun computeRingProgress(
    vertical: Double,
    verticalGoal: Double,
    runs: Int,
    runsGoal: Int,
    durationMs: Long,
    durationGoalMs: Long,
): RingProgress = RingProgress(
    verticalPct = if (verticalGoal > 0) (vertical / verticalGoal).toFloat() else 0f,
    runsPct = if (runsGoal > 0) (runs.toFloat() / runsGoal) else 0f,
    timePct = if (durationGoalMs > 0) (durationMs.toFloat() / durationGoalMs) else 0f,
)
