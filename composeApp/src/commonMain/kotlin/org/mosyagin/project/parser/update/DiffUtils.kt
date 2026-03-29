package org.mosyagin.project.parser.update

/**
 * Представление одной строки или слова в результате сравнения.
 */
data class DiffLine(
    val text: String,
    val type: DiffType
)

/**
 * DiffUtils — утилита для вычисления разницы между текстами.
 * Реализует алгоритм Майерса (Myers Diff Algorithm), обеспечивая кратчайший путь 
 * преобразования одного текста в другой.
 */
object DiffUtils {

    /**
     * Сравнивает два текста построчно.
     * Используется для быстрого обзора изменений в больших блоках текста.
     */
    fun diff(oldText: String, newText: String): List<DiffLine> {
        return try {
            val oldLines = oldText.lines()
            val newLines = newText.lines()
            calculateDiff(oldLines, newLines)
        } catch (e: Exception) {
            fallback(oldText.lines(), newText.lines())
        }
    }

    /**
     * Сравнивает два текста пословно.
     * Реализует гранулярное сравнение, игнорируя лишние пробелы и переносы строк.
     * Позволяет точно выделить изменения внутри предложений.
     */
    fun diffWords(oldText: String, newText: String): List<DiffLine> {
        val oldClean = oldText.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        val newClean = newText.replace("\n", " ").replace(Regex("\\s+"), " ").trim()

        val oldTokens = tokenize(oldClean)
        val newTokens = tokenize(newClean)

        return try {
            calculateDiff(oldTokens, newTokens)
        } catch (e: Exception) {
            fallback(oldTokens, newTokens)
        }
    }

    /**
     * Токенизация текста: разбивает строку на слова, пробелы и знаки препинания.
     * Это необходимо для точного выравнивания (alignment) текста в Diff Viewer.
     */
    private fun tokenize(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val res = mutableListOf<String>()
        val current = StringBuilder()
        
        for (char in text) {
            if (char.isWhitespace() || !char.isLetterOrDigit()) {
                if (current.isNotEmpty()) {
                    res.add(current.toString())
                    current.clear()
                }
                res.add(char.toString())
            } else {
                current.append(char)
            }
        }
        if (current.isNotEmpty()) res.add(current.toString())
        return res
    }

    private fun fallback(oldElements: List<String>, newElements: List<String>): List<DiffLine> {
        val result = mutableListOf<DiffLine>()
        oldElements.forEach { result.add(DiffLine(it, DiffType.DELETED)) }
        newElements.forEach { result.add(DiffLine(it, DiffType.ADDED)) }
        return result
    }

    /**
     * Основная реализация алгоритма Майерса.
     * Находит кратчайший путь в графе редактирования (SES - Shortest Edit Script).
     */
    private fun calculateDiff(oldElements: List<String>, newElements: List<String>): List<DiffLine> {
        val n = oldElements.size
        val m = newElements.size
        if (n == 0 && m == 0) return emptyList()
        
        val max = n + m
        val v = IntArray(2 * max + 1)
        val trace = mutableListOf<IntArray>()

        for (d in 0..max) {
            for (k in -d..d step 2) {
                val index = k + max
                var x = if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                    v[index + 1]
                } else {
                    v[index - 1] + 1
                }
                var y = x - k
                while (x < n && y < m && oldElements[x] == newElements[y]) {
                    x++
                    y++
                }
                v[index] = x
                if (x >= n && y >= m) {
                    trace.add(v.copyOf())
                    return backtrack(trace, oldElements, newElements)
                }
            }
            trace.add(v.copyOf())
        }
        return fallback(oldElements, newElements)
    }

    private fun backtrack(trace: List<IntArray>, oldElements: List<String>, newElements: List<String>): List<DiffLine> {
        val result = mutableListOf<DiffLine>()
        var x = oldElements.size
        var y = newElements.size
        val max = x + y

        for (d in trace.size - 1 downTo 1) {
            val v = trace[d]
            val prevV = trace[d - 1]
            val k = x - y
            val index = k + max
            
            val kPlus = k + 1
            val kMinus = k - 1
            
            val goUp = k == -d || (k != d && prevV[kMinus + max] < prevV[kPlus + max])
            
            val prevK = if (goUp) kPlus else kMinus
            val prevX = prevV[prevK + max]
            val prevY = prevX - prevK

            while (x > prevX && y > prevY) {
                result.add(0, DiffLine(oldElements[x - 1], DiffType.UNCHANGED))
                x--
                y--
            }

            if (x > prevX) {
                result.add(0, DiffLine(oldElements[x - 1], DiffType.DELETED))
                x--
            } else if (y > prevY) {
                result.add(0, DiffLine(newElements[y - 1], DiffType.ADDED))
                y--
            }
        }
        
        while (x > 0 && y > 0 && oldElements[x-1] == newElements[y-1]) {
            result.add(0, DiffLine(oldElements[x - 1], DiffType.UNCHANGED))
            x--
            y--
        }
        while (x > 0) {
            result.add(0, DiffLine(oldElements[x - 1], DiffType.DELETED))
            x--
        }
        while (y > 0) {
            result.add(0, DiffLine(newElements[y - 1], DiffType.ADDED))
            y--
        }

        return result
    }
}
