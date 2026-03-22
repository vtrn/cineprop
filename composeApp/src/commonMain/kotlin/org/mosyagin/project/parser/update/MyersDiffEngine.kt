package org.mosyagin.project.parser.update

import org.mosyagin.project.parser.ScriptBlock

/**
 * Движок сравнения списков блоков сценария с использованием алгоритма Майерса.
 * Оптимизирован для игнорирования незначительных различий.
 */
object MyersDiffEngine {

    fun compare(oldList: List<ScriptBlock>, newList: List<ScriptBlock>): SceneDiffReport {
        if (oldList.isEmpty() && newList.isEmpty()) return SceneDiffReport(emptyList(), 0, 0)
        
        return try {
            val n = oldList.size
            val m = newList.size
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
                    
                    while (x < n && y < m && isSame(oldList[x].text, newList[y].text)) {
                        x++
                        y++
                    }
                    v[index] = x
                    
                    if (x >= n && y >= m) {
                        trace.add(vCopy)
                        return buildReport(backtrack(trace, oldList, newList))
                    }
                }
                trace.add(vCopy)
            }
            fallback(oldList, newList)
        } catch (e: Exception) {
            fallback(oldList, newList)
        }
    }

    private fun isSame(t1: String, t2: String): Boolean {
        return TextNormalizer.normalize(t1) == TextNormalizer.normalize(t2)
    }

    private fun fallback(oldList: List<ScriptBlock>, newList: List<ScriptBlock>): SceneDiffReport {
        val diffs = mutableListOf<DiffBlock>()
        oldList.forEach { diffs.add(DiffBlock(DiffType.DELETED, it)) }
        newList.forEach { diffs.add(DiffBlock(DiffType.ADDED, it)) }
        return buildReport(diffs)
    }

    private fun backtrack(trace: List<IntArray>, oldList: List<ScriptBlock>, newList: List<ScriptBlock>): List<DiffBlock> {
        val result = mutableListOf<DiffBlock>()
        var x = oldList.size
        var y = newList.size
        val max = x + y

        for (d in trace.size - 1 downTo 0) {
            val v = trace[d]
            val k = x - y
            val index = k + max
            
            if (index < 0 || index >= v.size) continue

            val prevK = if (k == -d || (k != d && index + 1 < v.size && v[index - 1] < v[index + 1])) {
                k + 1
            } else {
                k - 1
            }
            
            val prevIndex = prevK + max
            if (prevIndex < 0 || prevIndex >= v.size) break
            
            val prevX = v[prevIndex]
            val prevY = prevX - prevK

            while (x > prevX && y > prevY && x > 0 && y > 0) {
                result.add(0, DiffBlock(DiffType.UNCHANGED, oldList[x - 1]))
                x--
                y--
            }

            if (x > prevX && x > 0) {
                result.add(0, DiffBlock(DiffType.DELETED, oldList[x - 1]))
            } else if (y > prevY && y > 0) {
                result.add(0, DiffBlock(DiffType.ADDED, newList[y - 1]))
            }
            x = prevX
            y = prevY
        }
        
        // На случай если x или y не дошли до 0
        while (x > 0 && y > 0 && isSame(oldList[x-1].text, newList[y-1].text)) {
            result.add(0, DiffBlock(DiffType.UNCHANGED, oldList[x - 1]))
            x--
            y--
        }
        while (x > 0) {
            result.add(0, DiffBlock(DiffType.DELETED, oldList[x - 1]))
            x--
        }
        while (y > 0) {
            result.add(0, DiffBlock(DiffType.ADDED, newList[y - 1]))
            y--
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
