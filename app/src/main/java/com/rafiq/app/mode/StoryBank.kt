package com.rafiq.app.mode

import com.rafiq.app.offline.OfflineEngine
import kotlin.random.Random

object StoryBank {

    private val storyWords = mapOf(
        "ar" to listOf("قصه", "حكايه", "احك", "حكيني", "رويه"),
        "en" to listOf("story", "tale", "tell me a"),
        "fr" to listOf("histoire", "raconte", "conte")
    )

    private val stories = mapOf(
        "ar" to listOf(
            "في غابةٍ خضراء، كانت نملة صغيرة اسمها نملّة تحمل حبّة قمحٍ أكبر منها بكثير. توقفت اليمامة فوق الشجرة وقالت: «هل أساعدك؟». رفضت النملة بشجاعة وقالت: «سأحاول بنفسي، وإن تعبت سأطلب مساعدتك!». قطعت الطريق على مراحل صغيرة حتى وصلت لبيتها. والدرس؟ المهام الكبيرة تُنجَز بخطوات صغيرة لا تتوقف! 🐜",
            "كانت فراشةً صغيرة تريد الطيران لكن أجنحتها رقيقة جداً. ضحك الحشراتُ الكبيرة عليها وقالت: «ابقِ على الأرض!». لكنها كانت تتدرب كل صباح: ترتفع سنتيمتراً، ثم تسقط، ثم ترتفع سنتيمترين! وبعد أسبوع، طارت أعلى من الجميع. لا تسمع لمن يقول «لا تستطيع» — جرب وستفاجئ الجميع! 🦋",
            "وجد أرنبٌ صغير ساعةً ذهبيةً قرب النهر. أخذها إلى مالكها بدل أن يبيعها، فجاءت السلحفاة العجوز وقالت: «صدقك أغلى من الذهب». ومن يومها، صار الأرنب صديق كل حيوانات النهر. الصدق يصنع أصدقاءً حقيقيين! 🐢"
        ),
        "en" to listOf(
            "In a green forest, a tiny ant named Nala carried a wheat seed bigger than herself. A dove from the tree asked: \"Shall I help you?\". Nala smiled bravely: \"I'll try myself first — and if I get tired, I'll ask you!\". She cut the journey into small steps until she reached home. Big tasks are done with small steps that never stop! 🐜",
            "A little butterfly wanted to fly, but her wings were very thin. The big insects laughed: \"Stay on the ground!\". But she practiced every morning: up one centimeter, fall, up two centimeters! After a week, she flew higher than everyone. Don't listen to those who say \"you can't\" — try and surprise them all! 🦋",
            "A little rabbit found a golden watch near the river. Instead of selling it, he searched for its owner. The wise old turtle said: \"Your honesty is worth more than gold\". From that day, the rabbit became friends with every animal on the river. Honesty makes true friends! 🐢"
        ),
        "fr" to listOf(
            "Dans une forêt verte, une petite fourmi nommée Nala portait une graine plus grosse qu'elle. Une colombe demanda : « Veux-tu de l'aide ? ». Nala répondit avec courage : « Je vais essayer seule, et si je suis fatiguée, je te le dirai ! ». Elle coupa le voyage en petites étapes jusqu'à la maison. Les grandes tâches se font par petites étapes ! 🐜",
            "Un petit papillon voulait voler, mais ses ailes étaient très fragiles. Les grands insectes riaient : « Reste au sol ! ». Mais il s'entraînait chaque matin : un centimètre en haut, une chute, deux centimètres ! Après une semaine, il vola plus haut que tout le monde. N'écoute pas ceux qui disent « tu ne peux pas » ! 🦋",
            "Un petit lapin trouva une montre en or près de la rivière. Au lieu de la vendre, il chercha son propriétaire. La vieille tortue dit : « Ton honnêteté vaut plus que l'or ». Depuis ce jour, le lapin devint l'ami de tous les animaux de la rivière. L'honnêteté crée de vrais amis ! 🐢"
        )
    )

    private val riddles = mapOf(
        "ar" to listOf(
            "شيء له أسنان كثيرة لكنه لا يعض أبداً… ما هو؟ (الجواب: المشط! 🪮)",
            "شيء كلما أخذت منه كبر أكثر… ما هو؟ (الجواب: الحفرة! 🕳️)",
            "له رقبة لكن ليس له رأس… ما هو؟ (الجواب: الزجاجة! 🍾)"
        ),
        "en" to listOf(
            "It has many teeth but never bites… what is it? (A comb! 🪮)",
            "The more you take away from it, the bigger it gets… what is it? (A hole! 🕳️)",
            "It has a neck but no head… what is it? (A bottle! 🍾)"
        ),
        "fr" to listOf(
            "Il a beaucoup de dents mais ne mord jamais… qu'est-ce que c'est ? (Le peigne ! 🪮)",
            "Plus on en prend, plus il grandit… qu'est-ce que c'est ? (Le trou ! 🕳️)",
            "Il a un cou mais pas de tête… qu'est-ce que c'est ? (La bouteille ! 🍾)"
        )
    )

    private val funFacts = mapOf(
        "ar" to listOf(
            "هل تعلم؟ العسل لا يفسد أبداً! وُجد عسل في مقابر مصرية عمره 3000 سنة وصالح للأكل 🍯",
            "هل تعلم؟ الأخطبوط له 3 قلوب! 💙",
            "هل تعلم؟ لسان الزرافة أزرق وطوله 50 سم! 🦒"
        ),
        "en" to listOf(
            "Did you know? Honey never spoils! 3000-year-old honey found in Egyptian tombs was still edible 🍯",
            "Did you know? An octopus has 3 hearts! 💙",
            "Did you know? A giraffe's tongue is blue and 50 cm long! 🦒"
        ),
        "fr" to listOf(
            "Savais-tu ? Le miel ne se périme jamais ! Du miel vieux de 3000 ans trouvé en Égypte était encore bon 🍯",
            "Savais-tu ? La pieuvre a 3 cœurs ! 💙",
            "Savais-tu ? La langue de la girafe est bleue et mesure 50 cm ! 🦒"
        )
    )

    fun isStoryRequest(norm: String, lang: String): Boolean =
        OfflineEngine.hasAny(norm, storyWords.getValue(if (stories.containsKey(lang)) lang else "en"))

    fun story(lang: String): String {
        val l = if (stories.containsKey(lang)) lang else "en"
        return "📖 " + stories.getValue(l).random()
    }

    fun kidContent(lang: String): String {
        val l = if (stories.containsKey(lang)) lang else "en"
        return if (Random.nextBoolean()) "🧠 " + funFacts.getValue(l).random()
        else "🧩 " + riddles.getValue(l).random()
    }
}