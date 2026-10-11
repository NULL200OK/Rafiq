package com.rafiq.app.voice

class SentenceSplitter {

    private val buffer = StringBuilder()

    fun feed(chunk: String): List<String> {
        buffer.append(chunk)
        val sentences = mutableListOf<String>()
        var start = 0
        for (i in buffer.indices) {
            val c = buffer[i]
            val isStrongTerminator = c == '.' || c == '!' || c == '?' ||
                    c == '؟' || c == '۔' || c == '…' || c == '\n'
            val isWeakTerminator = c == '،' || c == ',' || c == '؛' || c == ';'

            if (isStrongTerminator && isRealBoundary(i)) {
                val sentence = buffer.substring(start, i + 1).trim()
                if (sentence.isNotEmpty()) sentences.add(sentence)
                start = i + 1
            } else if (isWeakTerminator && isRealBoundary(i)) {
                val current = buffer.substring(start, i + 1).trim()
                if (current.length > 60) {
                    if (current.isNotEmpty()) sentences.add(current)
                    start = i + 1
                }
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
