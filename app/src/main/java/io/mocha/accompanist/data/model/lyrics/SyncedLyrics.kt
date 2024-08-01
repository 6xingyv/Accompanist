package io.mocha.accompanist.data.model.lyrics

import androidx.compose.runtime.Stable

@Stable
data class SyncedLyrics(
    val lines: List<ISyncedLine>,
    val title: String = "",
    val id: String = "0",
    val artists: List<String>? = listOf<String>(),
) {
    fun getCurrentFirstHighlightLineIndexByTime(time: Int): Int {
        if (lines.isEmpty()) return 0

        // 使用二分查找来优化性能
        var low = 0
        var high = lines.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val line = lines[mid]

            when {
                time in line.start..line.end -> return mid
                time < line.start -> high = mid - 1
                else -> low = mid + 1
            }
        }

        // 如果没找到精确匹配，找到最接近的区间
        return when {
            low > 0 && time < lines[low].start -> low - 1
            low < lines.size -> low
            else -> lines.size - 1
        }
    }
}
