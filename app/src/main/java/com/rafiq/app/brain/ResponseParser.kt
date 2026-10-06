package com.rafiq.app.brain

import com.rafiq.app.face.Emotion

class EmotionParser {

    private val buffer = StringBuilder()
    private var decided = false

    var emotion: Emotion? = null
        private set

    fun feed(chunk: String): String {
        if (decided) return chunk
        buffer.append(chunk)

        var i = 0
        while (i < buffer.length && buffer[i].isWhitespace()) i++
        if (i >= buffer.length) { buffer.setLength(0); return "" }
        if (buffer[i] != '[') return releaseAll()

        val close = buffer.indexOf("]", i + 1)
        return if (close >= 0) {
            val tag = buffer.substring(i + 1, close).trim()
            emotion = Emotion.fromTag(tag)
            decided = true
            val rest = buffer.substring(close + 1)
            buffer.setLength(0)
            rest
        } else if (buffer.length - i > MAX_TAG) {
            releaseAll()
        } else {
            ""
        }
    }

    fun flush(): String {
        val out = buffer.toString()
        buffer.setLength(0)
        return out
    }

    private fun releaseAll(): String {
        decided = true
        val out = buffer.toString()
        buffer.setLength(0)
        return out
    }

    companion object { private const val MAX_TAG = 24 }
}

class MemoryTagParser {

    data class MemoryTag(val category: String, val content: String)

    private val buffer = StringBuilder()
    val memories = mutableListOf<MemoryTag>()

    fun feed(chunk: String): String {
        buffer.append(chunk)
        val out = StringBuilder()
        while (true) {
            val start = buffer.indexOf("[[")
            if (start < 0) {
                val last = buffer.lastIndexOf("[")
                val safe = if (last >= 0 && buffer.length - last <= 2) last else buffer.length
                if (safe > 0) { out.append(buffer, 0, safe); buffer.delete(0, safe) }
                break
            }
            if (start > 0) { out.append(buffer, 0, start); buffer.delete(0, start) }
            val end = buffer.indexOf("]]")
            if (end < 0) {
                if (buffer.length > MAX_BODY) {
                    out.append(buffer); buffer.setLength(0)
                }
                break
            }
            val raw = buffer.substring(2, end)
            buffer.delete(0, end + 2)
            val parsed = parse(raw)
            if (parsed != null) memories.add(parsed)
            else out.append("[[").append(raw).append("]]")
        }
        return out.toString()
    }

    fun flush(): String {
        val rest = buffer.toString()
        buffer.setLength(0)
        return if (rest.trimStart().startsWith("[[")) "" else rest
    }

    private fun parse(raw: String): MemoryTag? {
        var body = raw.trim()
        if (!body.startsWith("MEM", ignoreCase = true)) return null
        body = body.substring(3).trimStart()
        if (body.startsWith(":") || body.startsWith("：")) body = body.substring(1).trimStart()
        val sepIdx = body.indexOf('|').let { if (it < 0) body.indexOf('=') else it }
        if (sepIdx <= 0) return null
        val category = body.substring(0, sepIdx).trim().lowercase()
        val content = body.substring(sepIdx + 1).trim()
        if (category.isEmpty() || content.isEmpty() || content.length > 200) return null
        return MemoryTag(category, content)
    }

    companion object { private const val MAX_BODY = 160 }
}