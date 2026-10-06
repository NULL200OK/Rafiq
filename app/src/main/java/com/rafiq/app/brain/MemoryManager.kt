package com.rafiq.app.brain

import com.rafiq.app.data.local.UserMemoryEntity

object MemoryManager {

    private val aliases = mapOf(
        "اسم" to "name", "الاسم" to "name",
        "يحب" to "likes", "يحبها" to "likes", "هوايات" to "likes", "مفضله" to "likes",
        "يكره" to "dislikes", "يكرهها" to "dislikes",
        "هدف" to "goal", "اهداف" to "goal", "حلم" to "goal", "احلام" to "goal",
        "ميعاد" to "date", "مناسبه" to "date", "عيد ميلاد" to "date",
        "معلومه" to "fact", "حقيقه" to "fact", "عمل" to "fact", "وظيفه" to "fact", "عمر" to "fact"
    )

    fun normalizeCategory(raw: String): String {
        val c = raw.trim().lowercase()
        return aliases[c] ?: c
    }

    fun categoryEmoji(category: String): String = when (category) {
        "name" -> "👤"
        "likes" -> "⭐"
        "dislikes" -> "🚫"
        "goal" -> "🎯"
        "date" -> "📅"
        else -> "💡"
    }

    fun userName(memories: List<UserMemoryEntity>): String =
        memories.firstOrNull { it.category == "name" }?.content.orEmpty()

    fun promptSection(memories: List<UserMemoryEntity>): String? {
        if (memories.isEmpty()) return null
        val lines = memories.take(30).joinToString("\n") { "- ${it.category}: ${it.content}" }
        return "\n\nذاكرة عن هذا المستخدم (استعملها طبيعياً عند المناسبة ولا تكررها حرفياً):\n$lines"
    }
}