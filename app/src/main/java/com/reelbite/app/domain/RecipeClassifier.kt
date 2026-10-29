package com.reelbite.app.domain

import java.util.Locale

/** Where a reel should be filed. [key] is stable; [name] is what the user sees. */
data class Classification(
    val key: String,
    val name: String,
    val emoji: String,
    val recognised: Boolean,
)

/**
 * Offline, keyword-based recipe classifier.
 *
 * It reads a reel caption (title text + hashtags), finds the most prominent dish
 * ("biryani", "fry", "pizza"…) and main ingredient ("chicken", "paneer"…), and combines
 * them into a group such as "Chicken Biryani" or "Chicken Fry".
 *
 * Scoring:
 *  - words near the start of the caption (the title) weigh most, body text less, hashtags least;
 *  - longer phrases are matched first and consumed, so "fried rice" never counts as "fry"
 *    and "garam masala" never counts as "masala";
 *  - main-dish words (tier 2) win close calls against cooking-style words (tier 1).
 */
object RecipeClassifier {

    val UNSORTED = Classification(key = "unsorted", name = "Unsorted", emoji = "📥", recognised = false)

    private const val TITLE_WINDOW = 120
    private const val WEIGHT_TITLE = 3.0
    private const val WEIGHT_BODY = 1.5
    private const val WEIGHT_HASHTAG = 1.0
    private const val MAIN_DISH_BONUS = 0.6
    private const val HASHTAG_POSITION_BASE = 1_000_000

    private val URL = Regex("""https?://\S+""")
    private val MENTION = Regex("""@[\p{L}\p{N}_.]+""")
    private val HASHTAG = Regex("""#([\p{L}\p{M}\p{N}_]+)""")
    private val NON_WORD = Regex("""[^\p{L}\p{M}\p{N}]+""")

    private class Term<T : Any>(val alias: String, val target: T?) {
        val joined: String = alias.replace(" ", "")
    }

    private class Score {
        var total = 0.0
        var firstPos = Int.MAX_VALUE
        fun add(weight: Double, position: Int) {
            total += weight
            if (position < firstPos) firstPos = position
        }
    }

    private class Prepared(val plain: String, val hashtags: List<String>)

    private val dishTerms: List<Term<Dish>> =
        buildTerms(DishCatalog.dishes.flatMap { dish -> dish.aliases.map { it to dish } })

    private val ingredientTerms: List<Term<Ingredient>> =
        buildTerms(DishCatalog.ingredients.flatMap { ing -> ing.aliases.map { it to ing } })

    /** Classifies free text (a caption or a dish name typed by the user). */
    fun classify(text: String?): Classification {
        if (text.isNullOrBlank()) return UNSORTED
        val prepared = prepare(text)

        val dish = score(prepared, dishTerms).entries
            .maxWithOrNull(
                compareBy<Map.Entry<Dish, Score>> { it.value.total + if (it.key.tier >= 2) MAIN_DISH_BONUS else 0.0 }
                    .thenByDescending { it.value.firstPos },
            )?.key

        val ingredient = score(prepared, ingredientTerms).entries
            .maxWithOrNull(
                compareBy<Map.Entry<Ingredient, Score>> { it.value.total }
                    .thenByDescending { it.value.firstPos },
            )?.key

        return when {
            dish != null && dish.takesIngredient && ingredient != null -> Classification(
                key = "${dish.id}|${ingredient.id}",
                name = dish.template.replace("{p}", ingredient.display),
                emoji = dish.emoji,
                recognised = true,
            )
            dish != null && dish.takesIngredient ->
                Classification(key = dish.id, name = dish.defaultName, emoji = dish.emoji, recognised = true)
            dish != null ->
                Classification(key = dish.id, name = dish.template, emoji = dish.emoji, recognised = true)
            ingredient != null -> Classification(
                key = "ingredient|${ingredient.id}",
                name = "${ingredient.display} Dishes",
                emoji = ingredient.emoji,
                recognised = true,
            )
            else -> UNSORTED
        }
    }

    /** Normalised form used to compare group names typed by users. */
    fun normalize(text: String): String =
        NON_WORD.replace(text.lowercase(Locale.ROOT), " ").trim()

    private fun <T : Any> buildTerms(pairs: List<Pair<String, T>>): List<Term<T>> {
        val real = pairs.map { (alias, target) -> Term(normalize(alias), target) }
        val ignored = DishCatalog.ignorePhrases.map { Term<T>(normalize(it), null) }
        return (real + ignored)
            .filter { it.alias.isNotEmpty() }
            .sortedByDescending { it.alias.length }
    }

    private fun prepare(raw: String): Prepared {
        var text = raw.lowercase(Locale.ROOT)
        text = URL.replace(text, " ")
        text = MENTION.replace(text, " ")
        val hashtags = HASHTAG.findAll(text)
            .map { it.groupValues[1].replace("_", "") }
            .filter { it.isNotEmpty() }
            .toList()
        val body = HASHTAG.replace(text, " ")
        val plain = " " + NON_WORD.replace(body, " ").trim() + " "
        return Prepared(plain, hashtags)
    }

    private fun <T : Any> score(prepared: Prepared, terms: List<Term<T>>): Map<T, Score> {
        val scores = LinkedHashMap<T, Score>()
        fun record(target: T?, weight: Double, position: Int) {
            if (target != null) scores.getOrPut(target) { Score() }.add(weight, position)
        }

        // 1) Free text: whole-word / whole-phrase matches. Matches are blanked out so
        //    shorter terms can't re-match inside a longer phrase.
        val plain = StringBuilder(prepared.plain)
        for (term in terms) {
            val needles = if (term.alias.contains(' ')) listOf(term.alias, term.joined) else listOf(term.alias)
            for (word in needles) {
                val needle = " $word "
                var idx = plain.indexOf(needle)
                while (idx >= 0) {
                    record(term.target, if (idx < TITLE_WINDOW) WEIGHT_TITLE else WEIGHT_BODY, idx)
                    for (i in idx + 1..idx + word.length) plain.setCharAt(i, ' ')
                    idx = plain.indexOf(needle, idx + 1)
                }
            }
        }

        // 2) Hashtags are glued words (#hyderabadichickenbiryani). Longer terms may appear
        //    anywhere inside; short ones (fry, egg, veg) only at the start or end of the tag.
        prepared.hashtags.forEachIndexed { tagIndex, tag ->
            val work = StringBuilder(tag)
            val position = HASHTAG_POSITION_BASE + tagIndex
            for (term in terms) {
                val word = term.joined
                if (word.length >= 5) {
                    var idx = work.indexOf(word)
                    while (idx >= 0) {
                        record(term.target, WEIGHT_HASHTAG, position)
                        blank(work, idx, word.length)
                        idx = work.indexOf(word, idx + word.length)
                    }
                } else {
                    if (work.startsWith(word)) {
                        record(term.target, WEIGHT_HASHTAG, position)
                        blank(work, 0, word.length)
                    }
                    if (work.length > word.length && work.endsWith(word)) {
                        record(term.target, WEIGHT_HASHTAG, position)
                        blank(work, work.length - word.length, word.length)
                    }
                }
            }
        }
        return scores
    }

    private fun blank(sb: StringBuilder, start: Int, length: Int) {
        for (i in start until start + length) sb.setCharAt(i, ' ')
    }
}
