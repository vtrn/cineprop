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
        val oldLines = oldText.lines()
        val newLines = newText.lines()
        return calculateDiff(oldLines, newLines)
    }

    private fun calculateDiff(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        val n = oldLines.size
        val m = newLines.size
        val max = n + m
        val v = IntArray(2 * max + 1)
        val trace = mutableListOf<IntArray>()

        for (d in 0..max) {
            val vCopy = v.copyOf()
            for (k in -d..d step 2) {
                val index = k + max
                var x = if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
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
        return emptyList()
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
            
            val prevK = if (k == -d || (k != d && v[index - 1] < v[index + 1])) {
                k + 1
            } else {
                k - 1
            }
            val prevX = v[prevK + max]
            val prevY = prevX - prevK

            while (x > prevX && y > prevY) {
                result.add(0, DiffLine(oldLines[x - 1], DiffType.UNCHANGED))
                x--
                y--
            }

            if (x > prevX) {
                result.add(0, DiffLine(oldLines[x - 1], DiffType.DELETED))
            } else if (y > prevY) {
                result.add(0, DiffLine(newLines[y - 1], DiffType.ADDED))
            }
            x = prevX
            y = prevY
        }
        return result
    }
}
