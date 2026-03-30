package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.parser.BlockType
import org.mosyagin.project.parser.ScriptBlock
import kotlin.math.max
import kotlin.math.min

@Composable
fun InteractiveScriptViewer(
    blocks: List<ScriptBlock>,
    props: List<Prop>,
    selectedPropId: Long?,
    onPropClick: (Long) -> Unit,
    onTextSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().background(Color(0xFF1A1A1A)),
        contentPadding = PaddingValues(vertical = 40.dp, horizontal = 24.dp)
    ) {
        itemsIndexed(blocks) { _, block ->
            ScriptBlockItem(
                block = block,
                props = props,
                selectedPropId = selectedPropId,
                onPropClick = onPropClick,
                onTextSelected = onTextSelected
            )
        }
    }
}

@Composable
fun ScriptBlockItem(
    block: ScriptBlock,
    props: List<Prop>,
    selectedPropId: Long?,
    onPropClick: (Long) -> Unit,
    onTextSelected: (String) -> Unit
) {
    val style = when (block.type) {
        BlockType.SLUGLINE -> MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp,
            color = Color.White
        )
        BlockType.CHARACTER -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.9f)
        )
        BlockType.DIALOGUE -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
        BlockType.PARENTHETICAL -> MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.7f)
        )
        BlockType.TRANSITION -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.End,
            color = Color.White
        )
        else -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }

    val padding = when (block.type) {
        BlockType.CHARACTER -> Modifier.padding(start = 140.dp, end = 140.dp, top = 16.dp)
        BlockType.DIALOGUE -> Modifier.padding(start = 100.dp, end = 100.dp, bottom = 12.dp)
        BlockType.PARENTHETICAL -> Modifier.padding(start = 120.dp, end = 120.dp)
        BlockType.SLUGLINE -> Modifier.padding(top = 32.dp, bottom = 16.dp)
        else -> Modifier.padding(vertical = 6.dp)
    }

    val annotatedString = buildAnnotatedStringWithProps(block.text, props, selectedPropId)
    var textFieldValue by remember(annotatedString) { mutableStateOf(TextFieldValue(annotatedString)) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Column(modifier = padding.fillMaxWidth()) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { 
                textFieldValue = it
                if (!it.selection.collapsed) {
                    val selectionStart = min(it.selection.start, it.selection.end)
                    val selectionEnd = max(it.selection.start, it.selection.end)
                    val selectedText = it.text.substring(selectionStart, selectionEnd)
                    onTextSelected(selectedText)
                }
            },
            readOnly = true,
            textStyle = style,
            cursorBrush = SolidColor(Color.Transparent),
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(annotatedString) {
                    detectTapGestures { offset ->
                        layoutResult?.let { result ->
                            val position = result.getOffsetForPosition(offset)
                            annotatedString.getStringAnnotations("PROP", position, position)
                                .firstOrNull()?.let { annotation ->
                                    onPropClick(annotation.item.toLong())
                                }
                        }
                    }
                }
        )
    }
}

@Composable
fun buildAnnotatedStringWithProps(
    text: String,
    props: List<Prop>,
    selectedPropId: Long?
): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        val textLower = text.lowercase()
        
        val matches = props.flatMap { prop ->
            val anchorLower = prop.anchor.lowercase()
            if (anchorLower.isEmpty()) return@flatMap emptyList<Match>()
            
            val matchesForProp = mutableListOf<Match>()
            var index = textLower.indexOf(anchorLower)
            while (index != -1) {
                matchesForProp.add(Match(index, index + anchorLower.length, prop))
                index = textLower.indexOf(anchorLower, index + 1)
            }
            matchesForProp
        }.sortedBy { it.start }

        matches.forEach { match ->
            if (match.start >= lastIndex) {
                append(text.substring(lastIndex, match.start))
                
                val isSelected = match.prop.id == selectedPropId
                
                pushStringAnnotation("PROP", match.prop.id.toString())
                withStyle(
                    SpanStyle(
                        color = if (isSelected) Color.White else Color(0xFFBB86FC),
                        background = if (isSelected) Color(0xFF6200EE) else Color(0xFFBB86FC).copy(alpha = 0.2f),
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (match.prop.isOrphaned) TextDecoration.LineThrough else TextDecoration.None
                    )
                ) {
                    append(text.substring(match.start, match.end))
                }
                pop()
                lastIndex = match.end
            }
        }
        
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

private data class Match(val start: Int, val end: Int, val prop: Prop)
