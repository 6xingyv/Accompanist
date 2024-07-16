package io.mocha.accompanist.data.parser

import android.util.Log
import androidx.compose.ui.Alignment
import androidx.core.text.isDigitsOnly
import io.mocha.accompanist.data.model.lyrics.SyncedLyrics
import io.mocha.accompanist.data.model.lyrics.karaoke.KaraokeLine
import io.mocha.accompanist.data.model.lyrics.karaoke.KaraokeSyllable

object LyricifySyllableParser:LyricsParser {
    private val parser = Regex("(.*?)\\((\\d+),(\\d+)\\)")
    override fun parse(lines:List<String>): SyncedLyrics {
        val data = lines.map { line->
            parseLine(line)
        }
        return SyncedLyrics(lines = data)
    }
    fun parseLine(line:String):KaraokeLine {
        val real:String
        var isAccompaniment: Boolean = false
        val alignment: Alignment
        val attributes = mutableListOf<Int>()
        if (line.contains("]") and line.contains("[") and (line.indexOf("]")-line.indexOf("[")==2)) {
            real = line.substring(startIndex = (line.indexOf(']')+1))
            val regex = Regex("\\[(\\d+)]")
            val attribute = regex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            attributes.add(attribute)
            if(attribute !in 0..5) {
                isAccompaniment = true
            }
            alignment = if ( attribute==2 || attribute==5 || attribute==8) {
                Alignment.TopStart
            } else {
                Alignment.TopEnd
            }

        }
        else {
            real = line
            isAccompaniment = false
            alignment = Alignment.TopStart
        }
        val data = parser.findAll(real)
        val syllables = data.map { matched->
            val result = matched.groupValues
            if (result.size == 4 && result[2].isDigitsOnly()&& result[3].isDigitsOnly()) {
                //println("${result[1]} -(${result[2]},${result[2].toInt() +result[3].toInt()})")
                KaraokeSyllable(
                    content =result[1],
                    start = result[2].toInt(),
                    end = result[2].toInt() +result[3].toInt(),
                )
            } else  {
                KaraokeSyllable(
                content ="Error",
                start = 0,
                end = 0,
            ) }
        }.toList()
        Log.e("Attr",attributes.toString())
        return KaraokeLine(syllables,"测试样式",isAccompaniment,alignment,syllables[0].start,syllables.last().end)
    }
}