package com.rafiq.app.voice

class SentenceSplitter {

    private val buffer = StringBuilder()

    fun feed(chunk: String): List<String> {
        buffer.append(chunk)
        val sentences = mutableListOf<String>()
        var start = 0
        for (i in buffer.indices) {
            val c = buffer[i]
            val isTerminator = c == '.' || c == '!' || c == '?' || c == '؟' ||
                    c == '؛' || c == '۔' || c == '…' || c == '\n'
            if (isTerminator && isRealBoundary(i)) {
                val sentence = buffer.substring(start, i + 1).trim()
                if (sentence.isNotEmpty()) sentences.add(sentence)
                start = i + 1
            }
        }
        if (start > 0) buffer.delete(0, start)
        return sentences
    }

    fun flush(): String? {
        val rest = buffer.toString().trim()
        buffer.clear()
        return rest.ifBlank { null }
    }

    fun reset() = buffer.clear()

    private fun isRealBoundary(index: Int): Boolean {
        val c = buffer[index]
        val before = buffer.getOrNull(index - 1)
        val after = buffer.getOrNull(index + 1)
        if (c == '.' && before?.isDigit() == true && after?.isDigit() == true) return false
        return true
    }
}