package org.mosyagin.project.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.parser.BlockType
import org.mosyagin.project.parser.update.DiffType
import org.mosyagin.project.parser.update.DiffUtils
import org.mosyagin.project.parser.update.PropImpact
import org.mosyagin.project.parser.update.PropImpactType
import org.mosyagin.project.ui.components.AppLayoutType
import org.mosyagin.project.ui.components.LocalAppLayoutType

data class SceneDiffScreen(val sceneUserDataId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SceneDiffViewModel> { parametersOf(sceneUserDataId) }
        val state by screenModel.state.collectAsState()
        val layoutType = LocalAppLayoutType.current

        Scaffold(
            topBar = {
                val title = (state as? SceneDiffViewModel.DiffState.Success)?.sceneNumber ?: ""
                CenterAlignedTopAppBar(
                    title = { Text("Сцена $title: Изменения", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        if (state is SceneDiffViewModel.DiffState.Success) {
                            val s = state as SceneDiffViewModel.DiffState.Success
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                                Surface(color = Color(0xFF2E7D32).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("+${s.addedCount}", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Surface(color = Color(0xFFC62828).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("-${s.deletedCount}", color = Color(0xFFE53935), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (state is SceneDiffViewModel.DiffState.Success) {
                    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                        Button(
                            onClick = { 
                                screenModel.markAsReviewed()
                                navigator.pop()
                            },
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Пометить как проверенное")
                        }
                    }
                }
            }
        ) { padding ->
            when (val s = state) {
                is SceneDiffViewModel.DiffState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is SceneDiffViewModel.DiffState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { 
                    Text(s.message, color = MaterialTheme.colorScheme.error) 
                }
                is SceneDiffViewModel.DiffState.Success -> {
                    if (layoutType == AppLayoutType.MOBILE) {
                        MobileUnifiedDiff(s.rows, s.propImpacts, padding)
                    } else {
                        DesktopSideBySideDiff(s.rows, s.propImpacts, padding)
                    }
                }
            }
        }
    }

    @Composable
    private fun DesktopSideBySideDiff(rows: List<SideBySideDiffRow>, impacts: List<PropImpact>, padding: PaddingValues) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            val orphaned = impacts.filter { it.type == PropImpactType.POTENTIALLY_ORPHANED }
            if (orphaned.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 60.dp, vertical = 24.dp)) {
                        Text(
                            "СИРОТСКИЙ РЕКВИЗИТ (упоминание удалено из новой версии сценария):",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            orphaned.forEach { impact ->
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.LinkOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(impact.propName, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }

            items(rows) { row ->
                SideBySideRow(row)
            }
        }
    }

    @Composable
    private fun SideBySideRow(row: SideBySideDiffRow) {
        val colorScheme = MaterialTheme.colorScheme
        val isDark = colorScheme.surface.luminance() < 0.5f
        
        val addedBg = if (isDark) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9).copy(alpha = 0.6f)
        val deletedBg = if (isDark) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFBE9E7).copy(alpha = 0.6f)

        val connectorColor = when (row.type) {
            DiffType.ADDED -> Color(0xFF2E7D32).copy(alpha = 0.3f)
            DiffType.DELETED -> Color(0xFFC62828).copy(alpha = 0.3f)
            else -> if (row.oldBlock?.text != row.newBlock?.text) Color(0xFF1976D2).copy(alpha = 0.3f) else Color.Transparent
        }

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.weight(1f).fillMaxHeight().background(if (row.type == DiffType.DELETED || (row.type == DiffType.UNCHANGED && row.oldBlock?.text != row.newBlock?.text)) deletedBg else Color.Transparent)) {
                if (row.oldBlock != null) {
                    val annotatedText = if (row.oldBlock.text != row.newBlock?.text) {
                        renderWordDiff(row.oldBlock.text, row.newBlock?.text ?: "", isOld = true, isDark = isDark)
                    } else {
                        buildAnnotatedString { append(row.oldBlock.text) }
                    }
                    DiffText(annotatedText, row.oldBlock.type, isOld = true)
                }
            }

            Box(Modifier.width(40.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                if (connectorColor != Color.Transparent) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path().apply {
                            val leftTop = if (row.oldBlock != null) 0f else size.height / 2
                            val leftBottom = if (row.oldBlock != null) size.height else size.height / 2
                            val rightTop = if (row.newBlock != null) 0f else size.height / 2
                            val rightBottom = if (row.newBlock != null) size.height else size.height / 2
                            
                            moveTo(0f, leftTop)
                            lineTo(size.width, rightTop)
                            lineTo(size.width, rightBottom)
                            lineTo(0f, leftBottom)
                            close()
                        }
                        drawPath(path, connectorColor)
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxHeight().background(if (row.type == DiffType.ADDED || (row.type == DiffType.UNCHANGED && row.oldBlock?.text != row.newBlock?.text)) addedBg else Color.Transparent)) {
                if (row.newBlock != null) {
                    val annotatedText = if (row.oldBlock?.text != row.newBlock.text) {
                        renderWordDiff(row.oldBlock?.text ?: "", row.newBlock.text, isOld = false, isDark = isDark)
                    } else {
                        buildAnnotatedString { append(row.newBlock.text) }
                    }
                    DiffText(annotatedText, row.newBlock.type, isOld = false)
                }
            }
        }
    }

    @Composable
    private fun MobileUnifiedDiff(rows: List<SideBySideDiffRow>, impacts: List<PropImpact>, padding: PaddingValues) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 100.dp)) {
            val orphaned = impacts.filter { it.type == PropImpactType.POTENTIALLY_ORPHANED }
            if (orphaned.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            "СИРОТСКИЙ РЕКВИЗИТ (упоминание удалено):", 
                            color = MaterialTheme.colorScheme.error, 
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            orphaned.forEach { impact ->
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        impact.propName,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }

            items(rows) { row ->
                val type = row.newBlock?.type ?: row.oldBlock?.type ?: BlockType.ACTION
                if (row.oldBlock?.text != row.newBlock?.text) {
                    val annotatedText = renderMobileWordDiff(row.oldBlock?.text ?: "", row.newBlock?.text ?: "")
                    MobileDiffItem(annotatedText, type, DiffType.UNCHANGED)
                } else {
                    MobileDiffItem(buildAnnotatedString { append(row.newBlock?.text ?: "") }, type, DiffType.UNCHANGED)
                }
            }
        }
    }

    @Composable
    private fun MobileDiffItem(text: AnnotatedString, type: BlockType, diffType: DiffType) {
        val bgColor = when(diffType) {
            DiffType.ADDED -> Color(0xFFE8F5E9).copy(alpha = 0.5f)
            DiffType.DELETED -> Color(0xFFFBE9E7).copy(alpha = 0.5f)
            else -> Color.Transparent
        }
        
        val isHeader = type == BlockType.SLUGLINE || type == BlockType.CHARACTER
        val isDialogue = type == BlockType.DIALOGUE || type == BlockType.PARENTHETICAL

        Box(Modifier.fillMaxWidth().background(bgColor).padding(vertical = 8.dp, horizontal = 16.dp)) {
            Text(
                text = if (isHeader) text.toUpperCasePreservingStyles() else text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(
                    start = if (type == BlockType.CHARACTER) 40.dp else if (isDialogue) 20.dp else 0.dp
                )
            )
        }
    }

    private fun renderMobileWordDiff(oldText: String, newText: String): AnnotatedString {
        return buildAnnotatedString {
            val diffs = DiffUtils.diffWords(oldText, newText)
            diffs.forEach { diff ->
                when (diff.type) {
                    DiffType.ADDED -> {
                        withStyle(SpanStyle(
                            color = Color(0xFF2E7D32), 
                            background = Color(0xFFE8F5E9),
                            fontWeight = FontWeight.Bold
                        )) { append(diff.text) }
                    }
                    DiffType.DELETED -> {
                        withStyle(SpanStyle(
                            color = Color(0xFFC62828), 
                            background = Color(0xFFFBE9E7),
                            textDecoration = TextDecoration.LineThrough
                        )) { append(diff.text) }
                    }
                    DiffType.UNCHANGED -> append(diff.text)
                }
            }
        }
    }

    @Composable
    private fun DiffText(text: AnnotatedString, type: BlockType, isOld: Boolean) {
        val isHeader = type == BlockType.SLUGLINE || type == BlockType.CHARACTER
        val isDialogue = type == BlockType.DIALOGUE || type == BlockType.PARENTHETICAL
        
        Text(
            text = if (isHeader) text.toUpperCasePreservingStyles() else text,
            textAlign = if (isHeader) TextAlign.Center else TextAlign.Start,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 22.sp,
                fontSize = 16.sp,
                color = if (isOld) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isDialogue) 140.dp else 60.dp,
                    end = if (isDialogue) 140.dp else 60.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
        )
    }

    private fun renderWordDiff(oldText: String, newText: String, isOld: Boolean, isDark: Boolean): AnnotatedString {
        val diffs = DiffUtils.diffWords(oldText, newText)
        return buildAnnotatedString {
            diffs.forEach { diff ->
                val style = when (diff.type) {
                    DiffType.ADDED -> if (!isOld) SpanStyle(
                        background = if (isDark) Color(0xFF2E7D32).copy(alpha = 0.4f) else Color(0xFFC8E6C9), 
                        fontWeight = FontWeight.Bold
                    ) else null
                    DiffType.DELETED -> if (isOld) SpanStyle(
                        background = if (isDark) Color(0xFFC62828).copy(alpha = 0.4f) else Color(0xFFFFCDD2), 
                        textDecoration = TextDecoration.LineThrough
                    ) else null
                    DiffType.UNCHANGED -> null
                }
                if (style != null) {
                    withStyle(style) { append(diff.text) }
                } else {
                    if ((isOld && diff.type != DiffType.ADDED) || (!isOld && diff.type != DiffType.DELETED)) {
                        append(diff.text)
                    }
                }
            }
        }
    }
}

private fun AnnotatedString.toUpperCasePreservingStyles(): AnnotatedString {
    return buildAnnotatedString {
        append(text.uppercase())
        spanStyles.forEach { addStyle(it.item, it.start, it.end) }
    }
}
