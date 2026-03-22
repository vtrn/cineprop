package org.mosyagin.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.mosyagin.project.parser.BlockType
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.parser.update.*
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

    private fun loadDiff() {
        screenModelScope.launch {
            // Используем flatMapLatest для реактивного обновления при изменении данных
            sceneRepository.getSceneUserDataById(sceneUserDataId)
                .flatMapLatest { userData ->
                    if (userData == null) return@flatMapLatest flowOf(State.Error("Сцена не найдена"))
                    
                    sceneRepository.getSceneVersionsForUserData(sceneUserDataId)
                        .map { versions ->
                            if (versions.isEmpty()) {
                                return@map State.Error("Текст сценария не найден")
                            }

                            // Текущая версия (самая свежая)
                            val currentVersion = versions.first()
                            // Предыдущая версия (если есть)
                            val previousVersion = versions.getOrNull(1)

                            val currentBlocks = parser.parseBlocks(currentVersion.content)
                            val oldBlocks = previousVersion?.let { parser.parseBlocks(it.content) } ?: emptyList()

                            // Сравниваем версии
                            val report = MyersDiffEngine.compare(oldBlocks, currentBlocks)

                            State.Success(
                                sceneNumber = "${userData.seriesNumber}-${userData.sceneNumber}",
                                diffBlocks = report.diffs,
                                addedCount = report.addedCount,
                                deletedCount = report.deletedCount
                            )
                        }
                }
                .catch { e ->
                    emit(State.Error("Ошибка загрузки: ${e.message}"))
                }
                .collect {
                    _state.value = it
                }
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
                val title = when (val s = state) {
                    is SceneDiffScreenModel.State.Success -> "Сцена ${s.sceneNumber}"
                    else -> "Изменения"
                }
                CenterAlignedTopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (state is SceneDiffScreenModel.State.Success) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Button(
                            onClick = { 
                                screenModel.markAsReviewed()
                                navigator.pop()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Пометить как проверенное", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        ) { padding ->
            when (val currentState = state) {
                is SceneDiffScreenModel.State.Loading -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SceneDiffScreenModel.State.Success -> {
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        DiffLegend(currentState.addedCount, currentState.deletedCount)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        if (currentState.diffBlocks.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Изменений не обнаружено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(currentState.diffBlocks) { diffBlock ->
                                    DiffBlockItem(diffBlock)
                                }
                            }
                        }
                    }
                }
                is SceneDiffScreenModel.State.Error -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text(currentState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { navigator.pop() }) {
                                Text("Вернуться")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DiffLegend(added: Int, deleted: Int) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("Добавлено: $added", Color(0xFF2E7D32))
            LegendItem("Удалено: $deleted", Color(0xFFC62828))
        }
    }

    @Composable
    private fun LegendItem(label: String, color: Color) {
        Surface(
            color = color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun DiffBlockItem(diffBlock: DiffBlock) {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        
        val backgroundColor = when (diffBlock.type) {
            DiffType.ADDED -> if (isDark) Color(0xFF2E7D32).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
            DiffType.DELETED -> if (isDark) Color(0xFFC62828).copy(alpha = 0.2f) else Color(0xFFFBE9E7)
            DiffType.UNCHANGED -> Color.Transparent
        }
        
        val contentColor = when (diffBlock.type) {
            DiffType.ADDED -> if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
            DiffType.DELETED -> if (isDark) Color(0xFFE57373) else Color(0xFFB71C1C)
            DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
        }

        val prefix = when (diffBlock.type) {
            DiffType.ADDED -> "+ "
            DiffType.DELETED -> "- "
            DiffType.UNCHANGED -> "  "
        }

        val isDeleted = diffBlock.type == DiffType.DELETED

        val textStyle = when (diffBlock.block.type) {
            BlockType.SLUGLINE -> MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isDeleted) TextDecoration.LineThrough else null,
                fontSize = 15.sp
            )
            BlockType.CHARACTER -> MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isDeleted) TextDecoration.LineThrough else null,
                fontSize = 14.sp
            )
            BlockType.DIALOGUE -> MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isDeleted) TextDecoration.LineThrough else null,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            BlockType.PARENTHETICAL -> MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isDeleted) TextDecoration.LineThrough else null,
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            else -> MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isDeleted) TextDecoration.LineThrough else null,
                fontSize = 14.sp
            )
        }

        val horizontalPadding = when (diffBlock.block.type) {
            BlockType.CHARACTER -> 64.dp
            BlockType.DIALOGUE -> 48.dp
            BlockType.PARENTHETICAL -> 56.dp
            else -> 16.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(vertical = 4.dp, horizontal = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.width(20.dp)
                )
                Text(
                    text = if (diffBlock.block.type == BlockType.SLUGLINE) diffBlock.block.text.uppercase() else diffBlock.block.text,
                    style = textStyle,
                    color = contentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (horizontalPadding - 20.dp).coerceAtLeast(0.dp))
                )
            }
        }
    }
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
