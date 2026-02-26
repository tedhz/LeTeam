package com.leteam.locked.ui.screens.insights.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun LineChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color,
    gridColor: Color
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            // Small padding
            val paddingTop = 12f
            val paddingBottom = 18f
            val paddingHorizontal = 12f

            val chartWidth = size.width - paddingHorizontal * 2
            val chartHeight = size.height - paddingTop - paddingBottom
            if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

            val minY = (points.minOrNull() ?: 0.0).toFloat()
            val maxY = (points.maxOrNull() ?: 0.0).toFloat()
            val rangeY = (maxY - minY).takeIf { it > 0f } ?: 1f

            val steps = 3
            for (i in 0..steps) {
                val y = paddingTop + chartHeight * (i / steps.toFloat())
                drawLine(
                    color = gridColor,
                    start = Offset(paddingHorizontal, y),
                    end = Offset(paddingHorizontal + chartWidth, y),
                    strokeWidth = 1f
                )
            }

            if (points.size < 2) return@Canvas

            val path = Path()
            points.forEachIndexed { index, yValue ->
                val x = paddingHorizontal + chartWidth * (index / (points.size - 1).toFloat())
                val yNorm = ((yValue.toFloat() - minY) / rangeY)
                val y = paddingTop + chartHeight * (1f - yNorm)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            points.forEachIndexed { index, yValue ->
                val x = paddingHorizontal + chartWidth * (index / (points.size - 1).toFloat())
                val yNorm = ((yValue.toFloat() - minY) / rangeY)
                val y = paddingTop + chartHeight * (1f - yNorm)
                drawCircle(color = lineColor, radius = 7f, center = Offset(x, y))
                drawCircle(color = Color.White, radius = 3.5f, center = Offset(x, y))
            }
        }
    }
}
