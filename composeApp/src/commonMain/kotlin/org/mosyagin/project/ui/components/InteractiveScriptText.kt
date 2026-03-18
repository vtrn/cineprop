package org.mosyagin.project.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mosyagin.project.Prop
import androidx.compose.foundation.text.BasicText

@Composable
fun InteractiveScriptText(
    fullText: String,
    props: List<Prop>,
    onWordLongClick: (String) -> Unit // Передаем найденное слово в колбэк
) {
    val annotatedString = buildAnnotatedString {
        append(fullText)
        props.forEach { prop ->
            val index = fullText.indexOf(prop.name, ignoreCase = true)
            if (index != -1) {
                addStyle(SpanStyle(color = Color(0xFFFF9800), fontWeight = FontWeight.Bold), index, index + prop.name.length)
            }
        }
    }

    // Используем pointerInput для определения нажатия на конкретное слово
    BasicText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp,
            fontSize = 15.sp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { offset ->
                    val layoutResult = textLayoutResult ?: return@detectTapGestures
                    val position = layoutResult.getOffsetForPosition(offset)
                    val word = findWordAtOffset(fullText, position)
                    if (word.isNotBlank()) onWordLongClick(word)
                })
            }
    )
}

// Утилита: ищет слово по позиции курсора
fun findWordAtOffset(text: String, offset: Int): String {
    val start = text.lastIndexOfAny(charArrayOf(' ', '\n', '.', ','), offset - 1).let { if (it == -1) 0 else it + 1 }
    val end = text.indexOfAny(charArrayOf(' ', '\n', '.', ',', '!'), offset).let { if (it == -1) text.length else it }
    return text.substring(start, end).trim { !it.isLetterOrDigit() }
}

// Хак для получения LayoutResult (нужен для определения позиции клика)
var textLayoutResult: TextLayoutResult? = null