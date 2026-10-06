package com.rafiq.app.mode

object TrainerBank {

    private val practice = mapOf(
        "fr" to listOf(
            "Décris ta journée en trois phrases. 🌤️",
            "Présente ton meilleur ami en deux phrases. 🧑‍🤝‍🧑",
            "Raconte ton dernier repas : qu'est-ce que tu as mangé ? 🍽️",
            "Invente une phrase avec le mot « incroyable ». ✨",
            "Décris ta ville favorite en deux phrases. 🏙️"
        ),
        "en" to listOf(
            "Describe your day in three sentences. 🌤️",
            "Introduce your best friend in two sentences. 🧑‍🤝‍🧑",
            "Tell me about your last meal. 🍽️",
            "Make a sentence with the word \"amazing\". ✨",
            "Describe your favorite city in two sentences. 🏙️"
        ),
        "ar" to listOf(
            "صف يومك في ثلاث جمل. 🌤️",
            "قدّم صديقك المفضل بجملتين. 🧑‍🤝‍🧑",
            "حدثني عن وجبتك الأخيرة. 🍽️",
            "اكتب جملة تستخدم فيها كلمة «مذهل». ✨",
            "صف مدينتك المفضلة بجملتين. 🏙️"
        )
    )

    fun practice(targetLang: String): String {
        val l = if (practice.containsKey(targetLang)) targetLang else "en"
        return practice.getValue(l).random()
    }
}