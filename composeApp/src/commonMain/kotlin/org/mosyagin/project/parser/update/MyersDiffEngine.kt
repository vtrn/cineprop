package org.mosyagin.project.parser.update

import org.mosyagin.project.parser.ScriptBlock

/**
 * Движок сравнения списков блоков сценария с использованием алгоритма Майерса.
 */
object MyersDiffEngine {

    /**
     * Сравнивает два списка блоков и возвращает детальный отчет об изменениях.
     */
    fun compare(oldList: List<ScriptBlock>, newList: List<ScriptBlock>): SceneDiffReport {
        val n = oldList.size
        val m = newList.size
        val max = n + m
        
        // Массив V для хранения максимального X для каждой диагонали K
        val v = IntArray(2 * max + 1)
        val trace = mutableListOf<IntArray>()

        // Основной цикл поиска кратчайшего пути редактирования
        for (d in 0..max) {
            val vCopy = v.copyOf()
            for (k in -d..d step 2) {
                val index = k + max
                // Выбираем направление: вниз (вставка) или вправо (удаление)
                var x = if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                    v[index + 1]
                } else {
                    v[index - 1] + 1
                }
                var y = x - k
                
                // Проходим по "змеям" (совпадающим блокам)
                while (x < n && y < m && oldList[x].text == newList[y].text) {
                    x++
                    y++
                }
                v[index] = x
                
                // Путь найден
                if (x >= n && y >= m) {
                    trace.add(vCopy)
                    return buildReport(backtrack(trace, oldList, newList))
                }
            }
            trace.add(vCopy)
        }
        return SceneDiffReport(emptyList(), 0, 0)
    }

    /**
     * Восстановление пути из трассировки.
     */
    private fun backtrack(trace: List<IntArray>, oldList: List<ScriptBlock>, newList: List<ScriptBlock>): List<DiffBlock> {
        val result = mutableListOf<DiffBlock>()
        var x = oldList.size
        var y = newList.size
        val max = x + y

        for (d in trace.size - 1 downTo 0) {
            val v = trace[d]
            val k = x - y
            val index = k + max
            
            val prevK = if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                k + 1
            } else {
                k - 1
            }
            val prevX = v[prevK + max]
            val prevY = prevX - prevK

            // Обрабатываем совпадающие блоки
            while (x > prevX && y > prevY) {
                result.add(0, DiffBlock(DiffType.UNCHANGED, oldList[x - 1]))
                x--
                y--
            }

            // Обрабатываем вставки и удаления
            if (x > prevX) {
                result.add(0, DiffBlock(DiffType.DELETED, oldList[x - 1]))
            } else if (y > prevY) {
                result.add(0, DiffBlock(DiffType.ADDED, newList[y - 1]))
            }
            x = prevX
            y = prevY
        }
        return result
    }

    private fun buildReport(diffs: List<DiffBlock>): SceneDiffReport {
        return SceneDiffReport(
            diffs = diffs,
            addedCount = diffs.count { it.type == DiffType.ADDED },
            deletedCount = diffs.count { it.type == DiffType.DELETED }
        )
    }
}
