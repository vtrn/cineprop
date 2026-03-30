package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = Color.White
        )
        BlockType.CHARACTER -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace, fontSize = 15.sp, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.9f)
        )
        BlockType.DIALOGUE -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f)
        )
        else -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f)
        )
    }

    val padding = when (block.type) {
        BlockType.CHARACTER -> Modifier.padding(start = 140.dp, end = 140.dp, top = 16.dp)
        BlockType.DIALOGUE -> Modifier.padding(start = 100.dp, end = 100.dp, bottom = 12.dp)
        else -> Modifier.padding(vertical = 6.dp)
    }

    // Состояние текущего выделения пальцем
    var selectionRange by remember { mutableStateOf<IntRange?>(null) }

    val annotatedString = buildAnnotatedStringWithProps(
        text = block.text,
        props = props,
        selectedPropId = selectedPropId,
        selectionRange = selectionRange
    )

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = padding.fillMaxWidth().pointerInput(block.text) {
            detectTapGestures(
                onTap = { offset ->
                    layoutResult?.let { result ->
                        val position = result.getOffsetForPosition(offset)
                        annotatedString.getStringAnnotations("PROP", position, position)
                            .firstOrNull()?.let { onPropClick(it.item.toLong()) }
                    }
                }
            )
        }.pointerInput(block.text) {
            detectDragGestures(
                onDragStart = { offset ->
                    layoutResult?.let {
                        val start = it.getOffsetForPosition(offset)
                        selectionRange = start..start
                    }
                },
                onDrag = { change, _ ->
                    layoutResult?.let {
                        val current = it.getOffsetForPosition(change.position)
                        val start = selectionRange?.first ?: current
                        selectionRange = min(start, current)..max(start, current)
                    }
                },
                onDragEnd = {
                    val range = selectionRange
                    if (range != null && range.first != range.last) {
                        val selectedText = block.text.substring(range.first, range.last).trim()
                        if (selectedText.isNotEmpty()) {
                            onTextSelected(selectedText)
                        }
                    }
                    selectionRange = null
                }
            )
        }
    )
}

@Composable
fun buildAnnotatedStringWithProps(
    text: String,
    props: List<Prop>,
    selectedPropId: Long?,
    selectionRange: IntRange?
): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        // 1. Выделение пальцем (синий маркер)
        selectionRange?.let { range ->
            addStyle(
                style = SpanStyle(background = Color(0xFF336699).copy(alpha = 0.5f)),
                start = range.first,
                end = range.last
            )
        }

        // 2. Реквизит (желтый маркер)
        props.forEach { prop ->
            val anchor = prop.anchor
            if (!anchor.isNullOrEmpty()) {
                var index = text.indexOf(anchor, ignoreCase = true)
                while (index != -1) {
                    val isSelected = prop.id == selectedPropId
                    pushStringAnnotation("PROP", prop.id.toString())
                    addStyle(
                        style = SpanStyle(
                            background = if (isSelected) Color(0xFF6200EE) else Color(0xFFFFEB3B).copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.Black
                        ),
                        start = index,
                        end = index + anchor.length
                    )
                    pop()
                    index = text.indexOf(anchor, index + 1, ignoreCase = true)
                }
            }
        }
    }
}
