package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.parser.BlockType
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.parser.update.DiffBlock
import org.mosyagin.project.parser.update.DiffType
import org.mosyagin.project.parser.update.DiffUtils
import org.mosyagin.project.parser.update.MyersDiffEngine
import org.mosyagin.project.repository.SceneRepository

class SceneDiffScreenModel(
    private val sceneUserDataId: Long,
    private val sceneRepository: SceneRepository,
    private val parser: ScriptParser
) : ScreenModel {

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    sealed class State {
        object Loading : State()
        data class Success(
            val sceneNumber: String,
            val diffBlocks: List<DiffBlock>,
            val addedCount: Int,
            val deletedCount: Int
        ) : State()
        data class Error(val message: String) : State()
    }

    init {
        loadDiff()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDiff() {
        screenModelScope.launch {
            sceneRepository.getSceneUserDataById(sceneUserDataId)
                .flatMapLatest { userData ->
                    if (userData == null) return@flatMapLatest flowOf(State.Error("Сцена не найдена"))
                    
                    sceneRepository.getSceneVersionsForUserData(sceneUserDataId)
                        .map { versions ->
                            if (versions.isEmpty()) {
                                return@map State.Error("Текст сценария не найден")
                            }

                            val currentVersion = versions.first()
                            val previousVersion = versions.getOrNull(1)

                            val currentBlocks = parser.parseBlocks(currentVersion.content)
                            val oldBlocks = previousVersion?.let { parser.parseBlocks(it.content) } ?: emptyList()

                            val report = MyersDiffEngine.compare(oldBlocks, currentBlocks)

                            State.Success(
                                sceneNumber = "${userData.seriesNumber}-${userData.sceneNumber}",
                                diffBlocks = report.diffs,
                                addedCount = report.addedCount,
                                deletedCount = report.deletedCount
                            )
                        }
                }
                .catch { e -> emit(State.Error("Ошибка: ${e.message}")) }
                .collect { _state.value = it }
        }
    }

    fun markAsReviewed() {
        screenModelScope.launch {
            sceneRepository.updateSceneUserDataReviewStatus(0L, sceneUserDataId)
        }
    }
}

data class SceneDiffScreen(val sceneUserDataId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SceneDiffScreenModel> { parametersOf(sceneUserDataId) }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = {
                val title = (state as? SceneDiffScreenModel.State.Success)?.sceneNumber ?: ""
                CenterAlignedTopAppBar(
                    title = { Text("Сцена $title: Изменения", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            },
            bottomBar = {
                if (state is SceneDiffScreenModel.State.Success) {
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
                is SceneDiffScreenModel.State.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is SceneDiffScreenModel.State.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { 
                    Text(s.message, color = MaterialTheme.colorScheme.error) 
                }
                is SceneDiffScreenModel.State.Success -> {
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        DiffLegend(s.addedCount, s.deletedCount)
                        HorizontalDivider(modifier = Modifier.alpha(0.1f))
                        
                        val processedItems = remember(s.diffBlocks) {
                            groupDiffBlocks(s.diffBlocks)
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(processedItems) { item ->
                                when (item) {
                                    is DiffItem.Single -> DiffBlockItem(item.block)
                                    is DiffItem.Merged -> WordDiffBlockItem(item.oldText, item.newText, item.blockType)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Группирует идущие подряд DELETED и ADDED блоки одного типа для пословного сравнения.
     */
    private fun groupDiffBlocks(blocks: List<DiffBlock>): List<DiffItem> {
        val result = mutableListOf<DiffItem>()
        var i = 0
        while (i < blocks.size) {
            val current = blocks[i]
            
            // Если находим последовательность DELETED
            if (current.type == DiffType.DELETED) {
                val deletedGroup = mutableListOf<DiffBlock>()
                var j = i
                while (j < blocks.size && blocks[j].type == DiffType.DELETED && blocks[j].block.type == current.block.type) {
                    deletedGroup.add(blocks[j])
                    j++
                }
                
                // Проверяем, идут ли следом ADDED того же типа
                val addedGroup = mutableListOf<DiffBlock>()
                var k = j
                while (k < blocks.size && blocks[k].type == DiffType.ADDED && blocks[k].block.type == current.block.type) {
                    addedGroup.add(blocks[k])
                    k++
                }
                
                if (addedGroup.isNotEmpty()) {
                    // У нас есть пара групп для склейки и пословного сравнения
                    result.add(DiffItem.Merged(
                        oldText = deletedGroup.joinToString(" ") { it.block.text },
                        newText = addedGroup.joinToString(" ") { it.block.text },
                        blockType = current.block.type
                    ))
                    i = k
                } else {
                    // Группы ADDED нет, просто добавляем удаленные блоки по одному или группой
                    deletedGroup.forEach { result.add(DiffItem.Single(it)) }
                    i = j
                }
            } else {
                result.add(DiffItem.Single(current))
                i++
            }
        }
        return result
    }

    sealed class DiffItem {
        data class Single(val block: DiffBlock) : DiffItem()
        data class Merged(val oldText: String, val newText: String, val blockType: BlockType) : DiffItem()
    }

    @Composable
    private fun WordDiffBlockItem(oldText: String, newText: String, blockType: BlockType) {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val wordDiffs = DiffUtils.diffWords(oldText, newText)
        
        val annotatedString = buildAnnotatedString {
            wordDiffs.forEach { diff ->
                val color = when(diff.type) {
                    DiffType.ADDED -> if(isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                    DiffType.DELETED -> if(isDark) Color(0xFFE57373) else Color(0xFFC62828)
                    DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
                }
                val bgColor = when(diff.type) {
                    DiffType.ADDED -> color.copy(alpha = 0.15f)
                    DiffType.DELETED -> color.copy(alpha = 0.15f)
                    DiffType.UNCHANGED -> Color.Transparent
                }
                
                withStyle(SpanStyle(
                    color = color,
                    background = bgColor,
                    textDecoration = if(diff.type == DiffType.DELETED) TextDecoration.LineThrough else null
                )) {
                    append(diff.text)
                }
            }
        }

        val style = getBlockStyle(blockType)
        val paddingStart = getBlockPadding(blockType)

        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp)) {
            Row {
                Text(
                    text = "* ", 
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), 
                    modifier = Modifier.width(16.dp), 
                    style = style
                )
                Text(
                    text = if(blockType == BlockType.SLUGLINE) annotatedString.uppercase() else annotatedString,
                    style = style,
                    modifier = Modifier.fillMaxWidth().padding(start = (paddingStart - 16.dp).coerceAtLeast(0.dp))
                )
            }
        }
    }

    @Composable
    private fun DiffBlockItem(diff: DiffBlock) {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val bgColor = when(diff.type) {
            DiffType.ADDED -> if(isDark) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFFE8F5E9)
            DiffType.DELETED -> if(isDark) Color(0xFFB71C1C).copy(alpha = 0.15f) else Color(0xFFFBE9E7)
            else -> Color.Transparent
        }
        
        val style = getBlockStyle(diff.block.type)
        val paddingStart = getBlockPadding(diff.block.type)
        val color = when(diff.type) {
            DiffType.ADDED -> if(isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
            DiffType.DELETED -> if(isDark) Color(0xFFE57373) else Color(0xFFC62828)
            else -> MaterialTheme.colorScheme.onSurface
        }

        Box(Modifier.fillMaxWidth().background(bgColor).padding(vertical = 4.dp, horizontal = 16.dp)) {
            Row {
                Text(
                    text = if(diff.type == DiffType.ADDED) "+" else if(diff.type == DiffType.DELETED) "-" else " ", 
                    color = color.copy(alpha = 0.5f), 
                    modifier = Modifier.width(16.dp), 
                    style = style
                )
                Text(
                    text = if(diff.block.type == BlockType.SLUGLINE) diff.block.text.uppercase() else diff.block.text,
                    style = style.copy(
                        color = color,
                        textDecoration = if(diff.type == DiffType.DELETED) TextDecoration.LineThrough else null
                    ),
                    modifier = Modifier.fillMaxWidth().padding(start = (paddingStart - 16.dp).coerceAtLeast(0.dp))
                )
            }
        }
    }

    @Composable
    private fun getBlockStyle(type: BlockType) = when(type) {
        BlockType.SLUGLINE -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        BlockType.CHARACTER -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
        BlockType.DIALOGUE -> MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        else -> MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    }

    private fun getBlockPadding(type: BlockType) = when(type) {
        BlockType.CHARACTER -> 60.dp
        BlockType.DIALOGUE -> 40.dp
        BlockType.PARENTHETICAL -> 50.dp
        else -> 16.dp
    }

    @Composable
    private fun DiffLegend(added: Int, deleted: Int) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem("+$added", Color(0xFF2E7D32))
            LegendItem("-$deleted", Color(0xFFC62828))
        }
    }

    @Composable
    private fun LegendItem(text: String, color: Color) {
        Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
            Text(text, color = color, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

private fun AnnotatedString.uppercase() = buildAnnotatedString {
    append(this@uppercase.text.uppercase())
    this@uppercase.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
}
