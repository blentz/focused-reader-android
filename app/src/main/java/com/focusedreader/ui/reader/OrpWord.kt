package com.focusedreader.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import com.focusedreader.reader.OrpCalculator

@Composable
fun OrpWord(
    word: String,
    wordColor: Color,
    orpColor: Color,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    pivotAnchorFraction: Float = 0.5f
) {
    if (word.isEmpty()) return
    val split = remember(word) { OrpCalculator.split(word) }
    val baseStyle: TextStyle = LocalTextStyle.current.copy(fontSize = fontSize, fontWeight = FontWeight.Medium, fontFamily = fontFamily)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Layout(
            modifier = Modifier.fillMaxWidth(),
            content = {
                Text(split.left, color = wordColor, style = baseStyle, maxLines = 1)
                Text(split.pivot.toString(), color = orpColor, style = baseStyle, maxLines = 1)
                Text(split.right, color = wordColor, style = baseStyle, maxLines = 1)
            }
        ) { measurables, constraints ->
            val unbounded = Constraints(maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity)
            val left = measurables[0].measure(unbounded)
            val pivot = measurables[1].measure(unbounded)
            val right = measurables[2].measure(unbounded)

            val totalWidth = constraints.maxWidth
            val rowHeight = maxOf(left.height, pivot.height, right.height)
            val anchorX = (totalWidth * pivotAnchorFraction).toInt()
            val pivotX = anchorX - pivot.width / 2
            val leftX = pivotX - left.width
            val rightX = pivotX + pivot.width

            layout(totalWidth, rowHeight) {
                left.place(leftX, (rowHeight - left.height) / 2)
                pivot.place(pivotX, (rowHeight - pivot.height) / 2)
                right.place(rightX, (rowHeight - right.height) / 2)
            }
        }
    }
}
