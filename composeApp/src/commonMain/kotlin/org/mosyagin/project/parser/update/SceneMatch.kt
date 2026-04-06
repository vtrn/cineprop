package org.mosyagin.project.parser.update

import org.mosyagin.project.parser.ParsedScene

/**
 * Иерархия классов, описывающая результат сопоставления сцены из новой версии сценария
 * с существующими данными в базе.
 */
sealed class SceneMatch {
    /**
     * Полное совпадение текста и метаданных.
     */
    data class Exact(
        val oldSceneUserDataId: String, // Изменено на String
        val scene: ParsedScene
    ) : SceneMatch()

    /**
     * Частичное совпадение (Fuzzy). Текст изменен, но сцена узнаваема.
     */
    data class Fuzzy(
        val oldSceneUserDataId: String, // Изменено на String
        val scene: ParsedScene,
        val score: Double
    ) : SceneMatch()

    /**
     * Абсолютно новая сцена, которой не было в предыдущей версии.
     */
    data class New(
        val scene: ParsedScene
    ) : SceneMatch()

    /**
     * Сцена, которая присутствовала в старой версии, но отсутствует в новой.
     */
    data class Deleted(
        val oldSceneUserDataId: String, // Изменено на String
        val oldSceneNumber: String,
        val oldSceneTitle: String
    ) : SceneMatch()

    /**
     * Сложный случай: одна старая сцена была разделена на несколько новых.
     */
    data class Split(
        val oldSceneUserDataId: String, // Изменено на String
        val newScenes: List<ParsedScene>
    ) : SceneMatch()

    /**
     * Сложный случай: несколько старых сцен были объединены в одну новую.
     */
    data class Merged(
        val oldSceneUserIds: List<String>, // Изменено на String
        val newScene: ParsedScene
    ) : SceneMatch()
}

/**
 * Статистика по результатам обновления сценария.
 */
data class UpdateStats(
    val exactCount: Int = 0,
    val fuzzyCount: Int = 0,
    val newCount: Int = 0,
    val deletedCount: Int = 0,
    val splitCount: Int = 0,
    val mergedCount: Int = 0
) {
    /**
     * Общее количество изменений (исключая полностью идентичные сцены).
     */
    val totalChanges: Int
        get() = fuzzyCount + newCount + deletedCount + splitCount + mergedCount

    /**
     * Возвращает true, если в новой версии нет никаких изменений относительно старой.
     */
    fun isEmpty(): Boolean = totalChanges == 0 && exactCount == 0
}

/**
 * Описание конкретного изменения для отображения в UI (отчет об обновлении).
 */
data class SceneChange(
    val sceneNumber: String,
    val sceneTitle: String,
    val matchType: SceneMatch,
    val description: String
)
