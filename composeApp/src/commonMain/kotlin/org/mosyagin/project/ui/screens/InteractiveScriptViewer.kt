package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mosyagin.project.models.versioning.Prop
import org.mosyagin.project.parser.BlockType
import org.mosyagin.project.parser.ScriptBlock
import org.mosyagin.project.ui.components.AppLayoutType
import kotlin.math.max
import kotlin.math.min

/**
 * Просмотрщик сценария с поддержкой тем и интерактивного выделения.
 */
@Composable
fun InteractiveScriptViewer(
    blocks: List<ScriptBlock>,
    props: List<Prop>,
    selectedPropId: String?,
    onPropClick: (String) -> Unit,
    onTextSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    layoutType: AppLayoutType = AppLayoutType.DESKTOP
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            vertical = if (layoutType == AppLayoutType.MOBILE) 20.dp else 60.dp,
            horizontal = if (layoutType == AppLayoutType.MOBILE) 16.dp else 40.dp
        )
    ) {
        itemsIndexed(blocks) { _, block ->
            ScriptBlockItem(
                block = block,
                props = props,
                selectedPropId = selectedPropId,
                onPropClick = onPropClick,
                onTextSelected = onTextSelected,
                layoutType = layoutType
            )
        }
    }
}

@Composable
fun ScriptBlockItem(
    block: ScriptBlock,
    props: List<Prop>,
    selectedPropId: String?,
    onPropClick: (String) -> Unit,
    onTextSelected: (String) -> Unit,
    layoutType: AppLayoutType
) {
    val isMobile = layoutType == AppLayoutType.MOBILE
    val textColor = MaterialTheme.colorScheme.onBackground
    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val highlightColor = MaterialTheme.colorScheme.primary

    val style = when (block.type) {
        BlockType.SLUGLINE -> MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Bold, 
            fontFamily = FontFamily.Monospace, 
            fontSize = if (isMobile) 15.sp else 17.sp, 
            color = textColor
        )
        BlockType.CHARACTER -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace, 
            fontSize = if (isMobile) 14.sp else 16.sp, 
            textAlign = TextAlign.Center, 
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        BlockType.DIALOGUE -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace, 
            fontSize = if (isMobile) 14.sp else 16.sp, 
            textAlign = TextAlign.Start,
            lineHeight = if (isMobile) 18.sp else 22.sp,
            color = textColor.copy(alpha = 0.9f)
        )
        BlockType.PARENTHETICAL -> MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace, 
            fontSize = if (isMobile) 12.sp else 14.sp, 
            textAlign = TextAlign.Start,
            color = textColor.copy(alpha = 0.7f)
        )
        else -> MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace, 
            fontSize = if (isMobile) 14.sp else 16.sp, 
            lineHeight = if (isMobile) 18.sp else 22.sp,
            color = textColor.copy(alpha = 0.85f)
        )
    }

    val padding = when (block.type) {
        BlockType.CHARACTER -> Modifier.padding(top = if (isMobile) 12.dp else 24.dp, bottom = 2.dp).fillMaxWidth()
        BlockType.DIALOGUE -> {
            Modifier.padding(start = if (isMobile) 40.dp else 180.dp, end = if (isMobile) 20.dp else 150.dp, bottom = if (isMobile) 8.dp else 12.dp)
        }
        BlockType.PARENTHETICAL -> {
            Modifier.padding(start = if (isMobile) 55.dp else 220.dp, end = if (isMobile) 30.dp else 180.dp, bottom = 4.dp)
        }
        BlockType.SLUGLINE -> Modifier.padding(top = if (isMobile) 16.dp else 32.dp, bottom = if (isMobile) 8.dp else 16.dp)
        else -> Modifier.padding(vertical = if (isMobile) 4.dp else 8.dp)
    }

    var selectionRange by remember { mutableStateOf<IntRange?>(null) }
    var initialOffset by remember { mutableStateOf<Int?>(null) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val baseAnnotatedString = remember(block.text, props, selectedPropId, textColor, highlightColor) {
        val processedText = if (block.type == BlockType.CHARACTER) block.text.uppercase() else block.text
        buildAnnotatedStringWithProps(
            text = processedText,
            props = props,
            selectedPropId = selectedPropId,
            onSurfaceColor = textColor,
            selectedColor = highlightColor
        )
    }

    val finalAnnotatedString = remember(baseAnnotatedString, selectionRange, selectionColor) {
        if (selectionRange == null) baseAnnotatedString
        else buildAnnotatedString {
            append(baseAnnotatedString)
            addStyle(
                style = SpanStyle(background = selectionColor),
                start = selectionRange!!.first,
                end = selectionRange!!.last
            )
        }
    }

    Text(
        text = finalAnnotatedString,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = padding.then(
            if (block.type != BlockType.CHARACTER) Modifier.fillMaxWidth() else Modifier
        ).pointerInput(block.text) {
            detectTapGestures(onTap = { offset ->
                layoutResult?.let { result ->
                    val position = result.getOffsetForPosition(offset)
                    finalAnnotatedString.getStringAnnotations("PROP", position, position)
                        .firstOrNull()?.let { onPropClick(it.item) }
                }
            })
        }.pointerInput(block.text) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    layoutResult?.let { layout ->
                        val offsetPos = layout.getOffsetForPosition(offset)
                        val wordBoundary = layout.getWordBoundary(offsetPos)
                        initialOffset = offsetPos
                        selectionRange = wordBoundary.start..wordBoundary.end
                    }
                },
                onDrag = { change, _ ->
                    change.consume()
                    layoutResult?.let { layout ->
                        val currentOffset = layout.getOffsetForPosition(change.position)
                        initialOffset?.let { start ->
                            // Вычисляем новый диапазон от начальной точки до текущей
                            val wordAtStart = layout.getWordBoundary(start)
                            val wordAtCurrent = layout.getWordBoundary(currentOffset)
                            
                            val newStart = min(wordAtStart.start, wordAtCurrent.start)
                            val newEnd = max(wordAtStart.end, wordAtCurrent.end)
                            selectionRange = newStart..newEnd
                        }
                    }
                },
                onDragEnd = {
                    selectionRange?.let { range ->
                        val selectedText = finalAnnotatedString.text.substring(range.first, range.last).trim()
                        if (selectedText.isNotEmpty()) onTextSelected(selectedText)
                    }
                    selectionRange = null
                    initialOffset = null
                },
                onDragCancel = { 
                    selectionRange = null
                    initialOffset = null
                }
            )
        }
    )
}

fun buildAnnotatedStringWithProps(
    text: String,
    props: List<Prop>,
    selectedPropId: String?,
    onSurfaceColor: Color = Color.Unspecified,
    selectedColor: Color = Color(0xFF6200EE)
): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        props.forEach { prop ->
            val anchor = prop.anchor
            if (anchor.isNotEmpty()) {
                var index = text.indexOf(anchor, ignoreCase = true)
                while (index != -1) {
                    val isSelected = prop.id == selectedPropId
                    pushStringAnnotation("PROP", prop.id)
                    addStyle(
                        style = SpanStyle(
                            background = if (isSelected) selectedColor else Color(0xFFFFEB3B).copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else onSurfaceColor
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
