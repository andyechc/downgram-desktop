package com.andyechc.downgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T> CupertinoSegmentedControl(
    selectedValue: T,
    options: List<Pair<T, String>>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedBg = MaterialTheme.colorScheme.primary
    val selectedText = MaterialTheme.colorScheme.onPrimary
    val unselectedText = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selectedValue
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(2.dp)
                    .then(
                        if (isSelected) Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedBg)
                        else Modifier
                    )
                    .clickable { onValueChange(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) selectedText else unselectedText.copy(alpha = 0.6f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}
