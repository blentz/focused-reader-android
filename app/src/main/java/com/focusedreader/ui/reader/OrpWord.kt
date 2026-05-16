package com.focusedreader.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.focusedreader.reader.OrpCalculator

@Composable
fun OrpWord(
    word: String,
    wordColor: Color,
    orpColor: Color,
    fontSize: TextUnit,
    pivotAnchorFraction: Float = 0.38f,
    modifier: Modifier = Modifier
) {
    if (word.isEmpty()) return
    val split = remember(word) { OrpCalculator.split(word) }
    val baseStyle: TextStyle = LocalTextStyle.current.copy(fontSize = fontSize, fontWeight = FontWeight.Medium)

    Box(modifier = modifier.fillMaxSize()) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                Text(split.left, color = wordColor, style = baseStyle, maxLines = 1)
                Text(split.pivot.toString(), color = orpColor, style = baseStyle, maxLines = 1)
                Text(split.right, color = wordColor, style = baseStyle, maxLines = 1)
            }
        ) { measurables, constraints ->
            val unbounded = constraints.copy(minWidth = 0, maxWidth = Int.MAX_VALUE / 4)
            val leftPlaceable = measurables[0].measure(unbounded)
            val pivotPlaceable = measurables[1].measure(unbounded)
            val rightPlaceable = measurables[2].measure(unbounded)

            val totalWidth = constraints.maxWidth
            val totalHeight = constraints.maxHeight
            val anchorX = (totalWidth * pivotAnchorFraction).toInt()
            val centerY = (totalHeight - pivotPlaceable.height) / 2

            val pivotX = anchorX - pivotPlaceable.width / 2
            val leftX = pivotX - leftPlaceable.width
            val rightX = pivotX + pivotPlaceable.width

            layout(totalWidth, totalHeight) {
                leftPlaceable.place(leftX, centerY)
                pivotPlaceable.place(pivotX, centerY)
                rightPlaceable.place(rightX, centerY)
            }
        }
    }
}
