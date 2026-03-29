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
 * PropImpactAnalyzer — специализированный инструмент для отслеживания реквизита при правках.
 * 
 * В кинопроизводстве важно знать, не "выпал" ли закрепленный за сценой предмет 
 * (например, 'Золотые часы') при обновлении текста сценария. 
 * Если упоминание предмета удалено, анализатор помечает его как 'orphaned'.
 */
object PropImpactAnalyzer {

    /**
     * Проводит сравнительный анализ и возвращает отчет о влиянии правок на реквизит.
     * 
     * @param diffReport Отчет о разнице между версиями текста.
     * @param existingProps Список реквизита, уже закрепленного за сценой пользователем.
     * @return Список [PropImpact] для каждого предмета.
     */
    fun analyze(diffReport: SceneDiffReport, existingProps: List<Prop>): List<PropImpact> {
        // "Новый мир" — это сумма того, что не изменилось и того, что было добавлено.
        val newWorldText = diffReport.diffs
            .filter { it.type == DiffType.UNCHANGED || it.type == DiffType.ADDED }
            .joinToString(" ") { it.block.text }
            .lowercase()

        // "Ушедший мир" — это текст, который был полностью удален в новой ревизии.
        val oldGoneText = diffReport.diffs
            .filter { it.type == DiffType.DELETED }
            .joinToString(" ") { it.block.text }
            .lowercase()

        val results = mutableListOf<PropImpact>()

        existingProps.forEach { prop ->
            val nameLower = prop.name.lowercase()
            
            when {
                // Реквизит сохранился в актуальном тексте
                newWorldText.contains(nameLower) -> {
                    results.add(PropImpact(prop.name, PropImpactType.STILL_PRESENT))
                }
                // Реквизит исчез из текста (потенциальное "сиротство")
                oldGoneText.contains(nameLower) -> {
                    results.add(PropImpact(prop.name, PropImpactType.POTENTIALLY_ORPHANED))
                }
            }
        }

        // Поиск новых упоминаний в добавленных блоках текста
        diffReport.diffs.forEachIndexed { index, diffBlock ->
            if (diffBlock.type == DiffType.ADDED) {
                val blockTextLower = diffBlock.block.text.lowercase()
                existingProps.forEach { prop ->
                    val nameLower = prop.name.lowercase()
                    if (blockTextLower.contains(nameLower)) {
                        results.add(PropImpact(prop.name, PropImpactType.NEW_MENTION, index))
                    }
                }
            }
        }

        return results.distinctBy { it.propName + it.type }
    }
}
