package com.example.answers

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

data class ScreenSizes(
    val titleSize: TextUnit,
    val subtitleSize: TextUnit,
    val inputTextSize: TextUnit,
    val buttonTextSize: TextUnit,
    val inputHeight: Dp,
    val buttonHeight: Dp,
    val padding: Dp,
    val fieldWidth: Float,
    val verticalSpacing: Dp,
    val sectionSpacing: Dp
)