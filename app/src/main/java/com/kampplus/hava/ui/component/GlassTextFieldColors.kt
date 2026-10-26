package com.kampplus.hava.ui.component

import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Koyu zemin üstünde duran, yarı saydam "cam" metin kutusu renkleri.
 * Arama kutusu ve "sevdiğini ekle" penceresi aynı görünümü paylaşır.
 */
@Composable
fun glassTextFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.14f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.09f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White,
    focusedLeadingIconColor = Color.White,
    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
    focusedTrailingIconColor = Color.White,
    unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
    focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
)
