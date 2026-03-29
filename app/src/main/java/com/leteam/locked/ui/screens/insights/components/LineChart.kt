package com.leteam.locked.ui.screens.insights.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LineChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color,
    gridColor: Color
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val yLabelStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.DarkGray
    )
    val tooltipStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    Box(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { tapOffset ->
                if (points.size < 2) return@detectTapGestures

                val yAxisWidth = 44f
                val paddingRight = 12f
                val chartWidth = size.width - yAxisWidth - paddingRight
                if (chartWidth <= 0f) return@detectTapGestures

                val nearest = points.indices.minByOrNull { index ->
                    val x = yAxisWidth + chartWidth * (index / (points.size - 1).toFloat())
                    abs(tapOffset.x - x)
                } ?: -1

                selectedIndex = if (selectedIndex == nearest) -1 else nearest
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val paddingTop = 12f
            val paddingBottom = 18f
            val yAxisWidth = 44f
            val paddingRight = 12f

            val chartWidth = size.width - yAxisWidth - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom
            if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

            val minY = (points.minOrNull() ?: 0.0).toFloat()
            val maxY = (points.maxOrNull() ?: 0.0).toFloat()
            val rangeY = (maxY - minY).takeIf { it > 0f } ?: 1f

            val steps = 3

            for (i in 0..steps) {
                val fraction = i / steps.toFloat()
                val y = paddingTop + chartHeight * fraction
                val labelValue = maxY - fraction * rangeY

                drawLine(
                    color = gridColor,
                    start = Offset(yAxisWidth, y),
                    end = Offset(yAxisWidth + chartWidth, y),
                    strokeWidth = 1f
                )

                val labelText = formatYLabel(labelValue)
                val measured = textMeasurer.measure(labelText, yLabelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    style = yLabelStyle,
                    topLeft = Offset(
                        x = yAxisWidth - measured.size.width - 6f,
                        y = y - measured.size.height / 2f
                    )
                )
            }

            if (points.size < 2) return@Canvas

            val positions = points.mapIndexed { index, yValue ->
                val x = yAxisWidth + chartWidth * (index / (points.size - 1).toFloat())
                val yNorm = (yValue.toFloat() - minY) / rangeY
                val y = paddingTop + chartHeight * (1f - yNorm)
                Offset(x, y)
            }

            val path = Path()
            positions.forEachIndexed { index, offset ->
                if (index == 0) path.moveTo(offset.x, offset.y)
                else path.lineTo(offset.x, offset.y)
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

            positions.forEachIndexed { index, offset ->
                val isSelected = index == selectedIndex
                drawCircle(color = lineColor, radius = if (isSelected) 10f else 7f, center = offset)
                drawCircle(color = Color.White, radius = if (isSelected) 5f else 3.5f, center = offset)
            }

            if (selectedIndex in points.indices) {
                val pos = positions[selectedIndex]
                val value = points[selectedIndex]
                val tooltipText = "${formatYLabel(value.toFloat())} lbs"
                val measured = textMeasurer.measure(tooltipText, tooltipStyle)

                val tooltipPadH = 10f
                val tooltipPadV = 6f
                val tooltipW = measured.size.width + tooltipPadH * 2
                val tooltipH = measured.size.height + tooltipPadV * 2

                var tooltipLeft = pos.x - tooltipW / 2f
                tooltipLeft = tooltipLeft.coerceIn(4f, size.width - tooltipW - 4f)
                val tooltipTop = (pos.y - tooltipH - 16f).coerceAtLeast(4f)

                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(tooltipLeft, tooltipTop),
                    size = Size(tooltipW, tooltipH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = tooltipText,
                    style = tooltipStyle,
                    topLeft = Offset(
                        x = tooltipLeft + tooltipPadH,
                        y = tooltipTop + tooltipPadV
                    )
                )
            }
        }
    }
}

private fun formatYLabel(value: Float): String {
    return if (value >= 1000f) {
        "${(value / 1000f * 10).roundToInt() / 10f}k"
    } else {
        value.roundToInt().toString()
    }
}