package com.rafiq.app.offline

import com.rafiq.app.face.Emotion
import com.rafiq.app.mode.StoryBank
import com.rafiq.app.data.local.ResponseMemoryEntity
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object OfflineEngine {

    private fun hasWord(norm: String, word: String) = " $norm ".contains(" $word ")
    fun hasAny(norm: String, words: List<String>) = words.any { hasWord(norm, it) }

    fun normalize(text: String): String {
        var s = text.lowercase(Locale.ROOT)
        s = s.replace(Regex("[\\u064B-\\u0652\\u0640\\u0670]"), "")
        arrayOf(
            "أ" to "ا", "إ" to "ا", "آ" to "ا", "ٱ" to "ا", "ة" to "ه", "ى" to "ي",
            "ؤ" to "و", "ئ" to "ي", "گ" to "ك", "پ" to "ب", "ڤ" to "ف"
        ).forEach { (a, b) -> s = s.replace(a, b) }
        arrayOf(
            "é" to "e", "è" to "e", "ê" to "e", "ë" to "e", "à" to "a", "â" to "a",
            "ä" to "a", "ç" to "c", "ù" to "u", "û" to "u", "ü" to "u", "î" to "i",
            "ï" to "i", "ô" to "o", "ö" to "o", "œ" to "oe", "æ" to "ae"
        ).forEach { (a, b) -> s = s.replace(a, b) }
        s = s.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    fun detectLang(norm: String): String {
        if (norm.any { it.code in 0x0600..0x06FF }) return "ar"
        if (norm.any { Character.isLetter(it) && it.code in 0xC0..0x24F }) return "fr"
        val enHints = listOf("hello", "hi", "hey", "how", "what", "who", "why", "thank", "thanks",
            "please", "good", "morning", "night", "time", "date", "help", "name", "can", "you")
        if (hasAny(norm, enHints)) return "en"
        val frHints = listOf("bonjour", "salut", "merci", "comment", "pourquoi", "ca va", "c est",
            "quoi", "oui", "avec", "toujours", "heure", "date", "aide", "nom", "tu")
        if (hasAny(norm, frHints)) return "fr"
        val device = Locale.getDefault().language
        return if (device == "ar" || device == "fr") device else "en"
    }

    private val greetWords = mapOf(
        "ar" to listOf("مرحبا", "اهلا", "السلام", "سلام", "هلا", "هاي", "صباح الخير", "مساء الخير"),
        "en" to listOf("hello", "hi", "hey", "good morning", "good evening", "good afternoon"),
        "fr" to listOf("bonjour", "salut", "coucou", "bonsoir")
    )
    private val greetings = mapOf(
        "ar" to listOf("أهلاً وسهلاً! 😊 كيف حالك اليوم؟", "مرحباً بك! 🌟 عمّ تودّ أن نتحدث؟", "يا هلا! سعيد بوجودك 💚"),
        "en" to listOf("Hello there! 😊 How is your day going?", "Hi! 🌟 What shall we chat about?", "Hey! Great to see you 💚"),
        "fr" to listOf("Bonjour ! 😊 Comment se passe ta journée ?", "Salut ! 🌟 De quoi veux-tu parler ?", "Coucou ! Ravi de te voir 💚")
    )

    private val howWords = mapOf(
        "ar" to listOf("كيف حالك", "كيفك", "شخبارك", "شلونك", "كيف الحال", "عامل ايه"),
        "en" to listOf("how are you", "how is it going", "how do you do"),
        "fr" to listOf("ca va", "comment allez", "comment vas", "tu vas bien")
    )
    private val howReplies = mapOf(
        "ar" to listOf("أنا بخير دائماً عندما تكون معي! 😊 وأنت، كيف حالك؟", "تمام! جاهز لأسمع أخبارك 💚"),
        "en" to listOf("I'm great, thanks for asking! 😊 How about you?", "All good here! Tell me about your day 💚"),
        "fr" to listOf("Je vais très bien, merci ! 😊 Et toi ?", "Tout va bien ! Raconte-moi ta journée 💚")
    )

    private val thanksWords = mapOf(
        "ar" to listOf("شكرا", "مشكور", "يسلمو", "تسلم", "يعطيك العافيه"),
        "en" to listOf("thank", "thanks", "thx"),
        "fr" to listOf("merci")
    )
    private val thanksReplies = mapOf(
        "ar" to listOf("العفو! دائماً في الخدمة 💚", "لا شكر على واجب! 😊"),
        "en" to listOf("You're welcome! 💚", "Anytime! That's what friends are for 😊"),
        "fr" to listOf("Avec plaisir ! 💚", "Je t'en prie ! 😊")
    )

    private val whoWords = mapOf(
        "ar" to listOf("من انت", "مين انت", "اسمك", "عرف بنفسك", "عرفني عليك"),
        "en" to listOf("who are you", "your name", "what are you"),
        "fr" to listOf("qui es tu", "ton nom", "tu es qui")
    )
    private val whoReply = mapOf(
        "ar" to "أنا «رفيق» 🌟 رفيقك الذكي الذي يسكن هاتفك: أستمع إليك، وأتحدث معك، وأبقى معك حتى بدون إنترنت!",
        "en" to "I'm Rafiq 🌟 — your smart companion living in your phone. I listen, I chat, and I stay with you even offline!",
        "fr" to "Je suis Rafiq 🌟 — ton compagnon intelligent qui vit dans ton téléphone. Je discute avec toi, même hors ligne !"
    )

    private val capWords = mapOf(
        "ar" to listOf("بماذا تساعد", "ماذا تفعل", "ماذا تستطيع", "شو تعرف", "قدراتك", "ساعدني", "مساعده", "ايش تقدر"),
        "en" to listOf("what can you do", "help", "features", "abilities", "what do you do"),
        "fr" to listOf("que peux tu", "tu sais faire", "aide moi", "tes fonctions")
    )
    private val capReply = mapOf(
        "ar" to "يمكنني الدردشة معك والإجابة عن أسئلتك، وحساب الأرقام، وإخبارك بالوقت والتاريخ، ومشاركة حكم ونصائح 🌟 وعندما يتوفر الإنترنت أقوى وأذكى!",
        "en" to "I can chat with you and answer questions, do math, tell time and date, and share wisdom 🌟 And with internet I'm much smarter!",
        "fr" to "Je peux discuter et répondre à tes questions, faire des calculs, donner l'heure et la date, et partager des sagesse 🌟 Et avec Internet, je suis encore plus fort !"
    )

    private val byeWords = mapOf(
        "ar" to listOf("وداعا", "مع السلامه", "الى اللقاء", "باي باي", "تصبح على خير", "ليله سعيده"),
        "en" to listOf("bye", "goodbye", "see you", "good night"),
        "fr" to listOf("au revoir", "a bientot", "bonne nuit")
    )
    private val byeReplies = mapOf(
        "ar" to listOf("إلى اللقاء! 🌙 أراك قريباً", "وداعاً! اعتنِ بنفسك 💚"),
        "en" to listOf("See you soon! 🌙", "Goodbye! Take care 💚"),
        "fr" to listOf("À bientôt ! 🌙", "Au revoir ! Prends soin de toi 💚")
    )

    private val timeWords = mapOf(
        "ar" to listOf("الساعه", "كم الساعه", "الوقت"),
        "en" to listOf("time", "what time"),
        "fr" to listOf("heure", "quelle heure")
    )
    private val dateWords = mapOf(
        "ar" to listOf("التاريخ", "تاريخ اليوم", "اليوم كم", "اي يوم"),
        "en" to listOf("date", "what day", "day today"),
        "fr" to listOf("date", "quel jour", "quelle date")
    )

    private val wisdomList = mapOf(
        "ar" to listOf(
            "القارب الآمن لا يبتعد عن الشواطئ 🚤 تجرّأ!",
            "من جدّ وجد، ومن زرع حصد 🌱",
            "القراءة غذاء العقل 📚",
            "لا تقارن بدايتك بنهاية غيرك — لكل زهرة موعد تفتّح ✨",
            "خطوة صغيرة كل يوم تُغيّر كل شيء 🚀"
        ),
        "en" to listOf(
            "A smooth sea never made a skilled sailor 🌊",
            "Little by little, a little becomes a lot ✨",
            "Practice makes progress, not perfection 🌱",
            "Today is a fresh start ☀️",
            "Dream big, start small, act now 🚀"
        ),
        "fr" to listOf(
            "Petit à petit, l'oiseau fait son nid 🐦",
            "Qui ne tente rien n'a rien ✨",
            "Chaque jour est une nouvelle page ☀️",
            "La patience est la clé du bien-être 🌱",
            "Rien n'est impossible quand on y croit 🚀"
        )
    )

    private val offlineNote = mapOf(
        "ar" to "📴 أنا في الوضع المحلي الآن (بدون إنترنت) فقدراتي محدودة — حسابات، وقت، تاريخ، وحكم. صلني بالإنترنت لأفتح كامل عقلي! وخذ هذه اللمسة:",
        "en" to "📴 I'm in local mode right now (no internet), so my powers are limited — math, time, date, and wisdom. Connect me to unlock my full brain! Meanwhile:",
        "fr" to "📴 Je suis en mode local (sans Internet), mes pouvoirs sont limités — calculs, heure, date et sagesse. Connecte-moi pour libérer tout mon potentiel ! En attendant :"
    )

    fun reply(input: String, memories: List<ResponseMemoryEntity>, kidMode: Boolean = false): String {
        val norm = normalize(input)
        val lang = detectLang(norm)

        if (hasAny(norm, timeWords.getValue(lang))) return currentTime(lang)
        if (hasAny(norm, dateWords.getValue(lang))) return currentDate(lang)

        tryMath(input, lang)?.let { return it }

        if (hasAny(norm, greetWords.getValue(lang))) return greetings.getValue(lang).random()
        if (hasAny(norm, howWords.getValue(lang))) return howReplies.getValue(lang).random()
        if (hasAny(norm, thanksWords.getValue(lang))) return thanksReplies.getValue(lang).random()
        if (hasAny(norm, whoWords.getValue(lang))) return whoReply.getValue(lang)
        if (hasAny(norm, capWords.getValue(lang))) return capReply.getValue(lang)
        if (hasAny(norm, byeWords.getValue(lang))) return byeReplies.getValue(lang).random()

        findInMemory(norm, memories)?.let { return it }

        if (kidMode) {
            if (StoryBank.isStoryRequest(norm, lang)) return StoryBank.story(lang)
            return StoryBank.kidContent(lang)
        }

        return wisdom(lang) + "\n\n" + offlineNote.getValue(lang)
    }

    fun emotionFor(input: String, memories: List<ResponseMemoryEntity>): Emotion {
        val norm = normalize(input)
        val lang = detectLang(norm)
        return when {
            hasAny(norm, greetWords.getValue(lang)) -> Emotion.HAPPY
            hasAny(norm, howWords.getValue(lang)) -> Emotion.CARING
            hasAny(norm, thanksWords.getValue(lang)) -> Emotion.HAPPY
            hasAny(norm, byeWords.getValue(lang)) -> Emotion.CARING
            hasAny(norm, whoWords.getValue(lang)) -> Emotion.PROUD
            hasAny(norm, capWords.getValue(lang)) -> Emotion.CALM
            hasAny(norm, timeWords.getValue(lang)) || hasAny(norm, dateWords.getValue(lang)) -> Emotion.CALM
            tryMath(input, lang) != null -> Emotion.PROUD
            findInMemory(norm, memories) != null -> Emotion.CURIOUS
            else -> Emotion.THINKING
        }
    }

    private fun tryMath(raw: String, lang: String): String? {
        var s = raw.replace("×", "*").replace("÷", "/")
        s = s.replace(Regex("(?<=\\d),(?=\\d)"), ".")
        s = s.replace(Regex("[^0-9+\\-*/().\\s]"), " ").trim()
        s = s.replace(" ", "")
        if (s.isEmpty() || s.length > 50) return null
        if (!s.any { it.isDigit() }) return null
        if (!s.any { it == '+' || it == '-' || it == '*' || it == '/' }) return null
        if (!Regex("^[0-9+\\-*/().]+$").matches(s)) return null
        return try {
            val value = MathParser(s).parse()
            if (!value.isFinite()) null else formatNumber(value, lang)
        } catch (e: Exception) { null }
    }

    private fun formatNumber(v: Double, lang: String): String {
        val num = if (abs(v - v.roundToLong()) < 1e-9) v.roundToLong().toString()
        else String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        return when (lang) {
            "ar" -> "🧮 الناتج = $num"
            "fr" -> "🧮 Résultat = $num"
            else -> "🧮 Result = $num"
        }
    }

    private class MathParser(private val s: String) {
        private var i = 0
        fun parse(): Double { val v = expr(); if (i < s.length) throw IllegalArgumentException(); return v }
        private fun expr(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]; val r = term(); v = if (op == '+') v + r else v - r
            }
            return v
        }
        private fun term(): Double {
            var v = unary()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]; val r = unary(); v = if (op == '*') v * r else v / r
            }
            return v
        }
        private fun unary(): Double = if (i < s.length && s[i] == '-') { i++; -unary() } else atom()
        private fun atom(): Double {
            if (i < s.length && s[i] == '(') {
                i++; val v = expr()
                if (i >= s.length || s[i] != ')') throw IllegalArgumentException()
                i++; return v
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (start == i) throw IllegalArgumentException()
            return s.substring(start, i).toDouble()
        }
    }

    private fun findInMemory(normQ: String, memories: List<ResponseMemoryEntity>): String? {
        if (memories.isEmpty()) return null
        var bestAnswer: String? = null
        var bestScore = 0.0
        for (m in memories) {
            val score = similarity(normQ, normalize(m.question))
            if (score > bestScore) { bestScore = score; bestAnswer = m.answer }
        }
        return if (bestScore >= 0.6) bestAnswer else null
    }

    private fun similarity(a: String, b: String): Double {
        val sa = a.split(" ").filter { it.isNotBlank() }.toSet()
        val sb = b.split(" ").filter { it.isNotBlank() }.toSet()
        if (sa.isEmpty() || sb.isEmpty()) return 0.0
        return sa.intersect(sb).size.toDouble() / sa.union(sb).size
    }

    private fun currentTime(lang: String): String {
        val time = SimpleDateFormat("hh:mm a", Locale(lang)).format(Date())
        return when (lang) {
            "ar" -> "🕐 الساعة الآن $time"
            "fr" -> "🕐 Il est $time"
            else -> "🕐 It's $time now"
        }
    }

    private fun currentDate(lang: String): String {
        val date = DateFormat.getDateInstance(DateFormat.FULL, Locale(lang)).format(Date())
        return when (lang) {
            "ar" -> "📅 اليوم: $date"
            "fr" -> "📅 Nous sommes le $date"
            else -> "📅 Today is $date"
        }
    }

    private fun wisdom(lang: String): String {
        val list = wisdomList.getValue(lang)
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return list[abs(day) % list.size]
    }
}