package org.mosyagin.project.parser.update

import org.mosyagin.project.models.versioning.Prop

/**
 * Тип влияния изменений текста на реквизит.
 */
enum class PropImpactType {
    STILL_PRESENT,        // Реквизит все еще упоминается в тексте
    POTENTIALLY_ORPHANED, // Упоминание реквизита удалено из текста
    NEW_MENTION           // (Экспериментально) Найдено потенциальное упоминание нового реквизита
}

/**
 * Результат анализа влияния изменений на конкретный предмет реквизита.
 */
data class PropImpact(
    val propName: String,
    val type: PropImpactType,
    val blockIndex: Int? = null
)

/**
 * Анализатор влияния правок сценария на список реквизита.
 */
object PropImpactAnalyzer {

    /**
     * Проводит анализ и возвращает отчет о том, как изменилось положение реквизита в сцене.
     */
    fun analyze(diffReport: SceneDiffReport, existingProps: List<Prop>): List<PropImpact> {
        // Собираем "новый мир" - текст, который остался или добавился
        val newWorldText = diffReport.diffs
            .filter { it.type == DiffType.UNCHANGED || it.type == DiffType.ADDED }
            .joinToString(" ") { it.block.text }
            .lowercase()

        // Собираем "ушедший мир" - текст, который был удален
        val oldGoneText = diffReport.diffs
            .filter { it.type == DiffType.DELETED }
            .joinToString(" ") { it.block.text }
            .lowercase()

        val results = mutableListOf<PropImpact>()

        // Проверяем текущий реквизит
        existingProps.forEach { prop ->
            val nameLower = prop.name.lowercase()
            
            when {
                // Если имя есть в новом тексте
                newWorldText.contains(nameLower) -> {
                    results.add(PropImpact(prop.name, PropImpactType.STILL_PRESENT))
                }
                // Если имени нет в новом, но оно было в удаленном куске
                oldGoneText.contains(nameLower) -> {
                    results.add(PropImpact(prop.name, PropImpactType.POTENTIALLY_ORPHANED))
                }
            }
        }

        // Бонус: поиск новых потенциальных упоминаний в добавленных блоках
        // Здесь мы ищем упоминания предметов, которые уже есть в списке, 
        // но которые могли быть добавлены в новые куски текста.
        diffReport.diffs.forEachIndexed { index, diffBlock ->
            if (diffBlock.type == DiffType.ADDED) {
                val blockTextLower = diffBlock.block.text.lowercase()
                existingProps.forEach { prop ->
                    val nameLower = prop.name.lowercase()
                    if (blockTextLower.contains(nameLower)) {
                        // Если этот предмет и так STILL_PRESENT, мы можем пометить блок, где он появился
                        results.add(PropImpact(prop.name, PropImpactType.NEW_MENTION, index))
                    }
                }
            }
        }

        return results.distinctBy { it.propName + it.type }
    }
}
