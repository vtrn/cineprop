package org.mosyagin.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup

@Composable
fun ScreenplayViewer(
    rawText: String,
    existingAnchors: List<String>,
    onAnchorCreateRequest: (selectedText: String, isProp: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Форматируем текст один раз при изменении входных данных
    val formattedText = remember(rawText, existingAnchors) {
        formatScreenplay(rawText, existingAnchors)
    }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(formattedText)) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { 
                // Разрешаем только изменение выделения, так как readOnly = true
                textFieldValue = it 
            },
            readOnly = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp
            ),
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 40.dp)
        )

        // Показываем меню действий, если есть выделение
        if (textFieldValue.selection.length > 0 && textLayoutResult != null) {
            val selection = textFieldValue.selection
            val layout = textLayoutResult!!
            
            // Вычисляем позицию для Popup (над началом выделения)
            val rect = layout.getBoundingBox(selection.start)
            val popupOffset = IntOffset(
                x = rect.left.toInt(),
                y = (rect.top - 60).toInt() // Смещение вверх над строкой
            )

            Popup(
                offset = popupOffset,
                onDismissRequest = { textFieldValue = textFieldValue.copy(selection = TextRange.Zero) }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                    shadowElevation = 4.dp,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val selectedText = textFieldValue.text.substring(selection.start, selection.end)
                        
                        TextButton(
                            onClick = {
                                onAnchorCreateRequest(selectedText, true)
                                textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Inventory, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Реквизит", style = MaterialTheme.typography.labelLarge)
                        }

                        VerticalDivider(modifier = Modifier.height(32.dp).align(Alignment.CenterVertically))

                        TextButton(
                            onClick = {
                                onAnchorCreateRequest(selectedText, false)
                                textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Заметка", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Применяет эвристики форматирования "Американка" к тексту сценария.
 */
private fun formatScreenplay(text: String, anchors: List<String>): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        var lastWasCharacter = false
        var lastWasParenthetical = false

        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                append("\n")
                lastWasCharacter = false
                lastWasParenthetical = false
                return@forEach
            }

            // Эвристики типов строк
            val isParenthetical = trimmed.startsWith("(")
            val isCharacter = trimmed == trimmed.uppercase() && !isParenthetical && trimmed.any { it.isLetter() }
            val isDialog = (lastWasCharacter || lastWasParenthetical) && !isCharacter && !isParenthetical

            val start = this.length
            append(line)
            val end = this.length
            append("\n")

            // Применяем ParagraphStyle для отступов
            val style = when {
                isCharacter -> ParagraphStyle(textIndent = TextIndent(firstLine = 160.sp))
                isDialog -> ParagraphStyle(textIndent = TextIndent(firstLine = 80.sp))
                isParenthetical -> ParagraphStyle(textIndent = TextIndent(firstLine = 120.sp))
                else -> ParagraphStyle() // Action
            }
            addStyle(style, start, end)

            // Жирный шрифт для имен
            if (isCharacter) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            }

            lastWasCharacter = isCharacter
            lastWasParenthetical = isParenthetical
        }

        // Подсветка существующих якорей
        anchors.forEach { anchor ->
            if (anchor.isBlank()) return@forEach
            var startIndex = 0
            while (true) {
                val index = text.indexOf(anchor, startIndex, ignoreCase = true)
                if (index == -1) break
                addStyle(
                    SpanStyle(background = Color(0x4DFFEB3B)), // Полупрозрачный желтый маркер
                    index, 
                    index + anchor.length
                )
                startIndex = index + anchor.length
            }
        }
    }
}
