package io.mocha.accompanist.data.model.lyrics

data class SyncedLyrics(
    val lines: List<ISyncedLine>,
    val title: String = "",
    val id: String = "0",
    val artists: List<String>? = listOf<String>(),
) {
    fun getCurrentFirstHighlightLineIndexByTime(time: Int): Int {
//        val index = lines.indexOfFirst { line ->
//            time in line.start..line.end
//        }
//        if (index ==-1) {
        var index = 0
        for (line in lines) {
            if (time in line.start..line.end)
                return index
            else {
                val previousLine = lines[if (index - 1 > 0) index - 1 else 0]
                if (time in previousLine.end..line.start)
                    return if (index - 1 > 0) index - 1 else 0
            }
            index++
        }
//        }
        return 0
    }
}
