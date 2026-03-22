package org.mosyagin.project.parser.update

/**
 * Представление одной строки в результате сравнения.
 */
data class DiffLine(
    val text: String,
    val type: DiffType
)

/**
 * Утилита для вычисления разницы между текстами.
 * Использует алгоритм Майерса (Myers Diff Algorithm).
 */
object DiffUtils {

    /**
     * Сравнивает два текста построчно.
     */
    fun diff(oldText: String, newText: String): List<DiffLine> {
        return try {
            val oldLines = oldText.lines()
            val newLines = newText.lines()
            calculateDiff(oldLines, newLines)
        } catch (e: Exception) {
            // Фолбек при ошибке: помечаем всё старое как удаленное, новое как добавленное
            fallback(oldText.lines(), newText.lines())
        }
    }

    private fun fallback(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        val result = mutableListOf<DiffLine>()
        oldLines.forEach { result.add(DiffLine(it, DiffType.DELETED)) }
        newLines.forEach { result.add(DiffLine(it, DiffType.ADDED)) }
        return result
    }

    private fun calculateDiff(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        val n = oldLines.size
        val m = newLines.size
        if (n == 0 && m == 0) return emptyList()
        
        val max = n + m
        val v = IntArray(2 * max + 1)
        val trace = mutableListOf<IntArray>()

        for (d in 0..max) {
            val vCopy = v.copyOf()
            for (k in -d..d step 2) {
                val index = k + max
                var x = if (k == -d || (k != d && index + 1 < v.size && index - 1 >= 0 && v[index - 1] < v[index + 1])) {
                    v[index + 1]
                } else {
                    v[index - 1] + 1
                }
                var y = x - k
                while (x < n && y < m && oldLines[x] == newLines[y]) {
                    x++
                    y++
                }
                v[index] = x
                if (x >= n && y >= m) {
                    trace.add(vCopy)
                    return backtrack(trace, oldLines, newLines)
                }
            }
            trace.add(vCopy)
        }
        return fallback(oldLines, newLines)
    }

    private fun backtrack(trace: List<IntArray>, oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        val result = mutableListOf<DiffLine>()
        var x = oldLines.size
        var y = newLines.size
        val max = x + y

        for (d in trace.size - 1 downTo 0) {
            val v = trace[d]
            val k = x - y
            val index = k + max
            
            if (index < 0 || index >= v.size) continue

            val prevK = if (k == -d || (k != d && index + 1 < v.size && index - 1 >= 0 && v[index - 1] < v[index + 1])) {
                k + 1
            } else {
                k - 1
            }
            
            val prevIndex = prevK + max
            if (prevIndex < 0 || prevIndex >= v.size) break
            
            val prevX = v[prevIndex]
            val prevY = prevX - prevK

            while (x > prevX && y > prevY && x > 0 && y > 0) {
                result.add(0, DiffLine(oldLines[x - 1], DiffType.UNCHANGED))
                x--
                y--
            }

            if (x > prevX && x > 0) {
                result.add(0, DiffLine(oldLines[x - 1], DiffType.DELETED))
            } else if (y > prevY && y > 0) {
                result.add(0, DiffLine(newLines[y - 1], DiffType.ADDED))
            }
            x = prevX
            y = prevY
        }
        
        // Обработка остатков
        while (x > 0 && y > 0 && oldLines[x-1] == newLines[y-1]) {
            result.add(0, DiffLine(oldLines[x - 1], DiffType.UNCHANGED))
            x--
            y--
        }
        while (x > 0) {
            result.add(0, DiffLine(oldLines[x - 1], DiffType.DELETED))
            x--
        }
        while (y > 0) {
            result.add(0, DiffLine(newLines[y - 1], DiffType.ADDED))
            y--
        }

        return result
    }
}
