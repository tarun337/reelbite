package com.reelbite.app.domain

/** A single Instagram post/reel reference extracted from user input. */
data class InstagramLink(
    val shortcode: String,
    /** Canonical URL used for opening the post and storing it. */
    val url: String,
)

object InstagramLinks {

    // Covers instagram.com/reel/X, /reels/X, /p/X, /tv/X, /<username>/reel/X, m./www. hosts and instagr.am.
    private val LINK = Regex(
        """(?:https?://)?(?:www\.|m\.)?(?:instagram\.com|instagr\.am)/(?:[A-Za-z0-9_.]+/)?(reels?|p|tv)/([A-Za-z0-9_-]{5,})""",
        RegexOption.IGNORE_CASE,
    )

    /** Extracts every distinct Instagram post link from free text (one or many pasted links). */
    fun extract(text: String): List<InstagramLink> =
        LINK.findAll(text)
            .map { match ->
                val type = match.groupValues[1].lowercase()
                val code = match.groupValues[2]
                val path = if (type == "p") "p" else "reel"
                InstagramLink(shortcode = code, url = "https://www.instagram.com/$path/$code/")
            }
            .distinctBy { it.shortcode }
            .toList()
}
