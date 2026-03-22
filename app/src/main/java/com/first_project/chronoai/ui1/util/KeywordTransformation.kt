package com.first_project.chronoai.ui1.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

class KeywordTransformation(private val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            buildAnnotatedStringWithColors(text.text, color),
            OffsetMapping.Identity
        )
    }

    private fun buildAnnotatedStringWithColors(text: String, color: Color): AnnotatedString {
        val keywords = listOf(
            "today", "tomorrow", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "daily", "weekly", "monthly", "yearly",
            "am", "pm", "at", "by", "until"
        )
        
        val builder = AnnotatedString.Builder()
        val parts = text.split(Regex("(?<=\\s)|(?=\\s)")) // Split keeping spaces
        
        for (part in parts) {
            val lowerPart = part.trim().lowercase()
            if (keywords.contains(lowerPart) || lowerPart.matches(Regex("\\d{1,2}(:\\d{2})?"))) {
                builder.withStyle(style = SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                builder.append(part)
            }
        }
        return builder.toAnnotatedString()
    }
}
