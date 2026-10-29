package com.reelbite.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RecipeClassifierTest {

    private fun name(caption: String) = RecipeClassifier.classify(caption).name
    private fun key(caption: String) = RecipeClassifier.classify(caption).key

    @Test
    fun `two biryani reels and one chicken fry reel make two groups`() {
        val reel1 = "Hyderabadi Chicken Dum Biryani 🔥 Full recipe below\n" +
            "Marinate chicken with curd, garam masala, fried onions…\n#biryani #hyderabadibiryani #foodie"
        val reel2 = "Sunday special biryani at home! Easy one-pot method 😋 #chickenbiryani #biryanilover #reels"
        val reel3 = "Crispy Andhra Chicken Fry | Kodi Vepudu 🌶️\nFry the onions till golden… #chickenfry #spicy"

        assertEquals("Chicken Biryani", name(reel1))
        assertEquals("Chicken Biryani", name(reel2))
        assertEquals(key(reel1), key(reel2))
        assertEquals("Chicken Fry", name(reel3))
        assertNotEquals(key(reel1), key(reel3))
    }

    @Test
    fun `main dish beats cooking style mentioned in the caption`() {
        assertEquals("Chicken Biryani", name("Chicken fry piece biryani recipe"))
        assertEquals("Chicken Fried Rice", name("Restaurant style chicken fried rice"))
        assertEquals("Paneer Tikka", name("Paneer tikka in air fryer"))
    }

    @Test
    fun `spice mixes and cooking steps are not dishes`() {
        assertEquals("Chicken Curry", name("Simple chicken curry with garam masala and curry leaves"))
        assertEquals("Mutton Curry", name("Mutton curry — fry the onions until brown"))
    }

    @Test
    fun `glued hashtags are understood`() {
        assertEquals("Mutton Biryani", name("#muttonbiryani #dumbiryani #foodstagram"))
        assertEquals("Chicken 65", name("#chicken65 #starter #indianfood"))
        assertEquals("Egg Curry", name("Dhaba style #eggcurry"))
    }

    @Test
    fun `spelling variants map to the same group`() {
        assertEquals(key("Chicken biryani"), key("Chiken biriyani"))
        assertEquals(key("Chicken biryani"), key("murgh briyani"))
    }

    @Test
    fun `fixed name dishes and ingredient only captions`() {
        assertEquals("Butter Chicken", name("Best butter chicken you'll ever make"))
        assertEquals("Pav Bhaji", name("Mumbai street style pav bhaji"))
        assertEquals("Paneer Dishes", name("Try this paneer recipe tonight"))
    }

    @Test
    fun `unrecognised captions are unsorted`() {
        val result = RecipeClassifier.classify("Watch till the end 😂 #viral #trending")
        assertFalse(result.recognised)
        assertEquals(RecipeClassifier.UNSORTED.key, result.key)
        assertFalse(RecipeClassifier.classify(null).recognised)
    }
}

class InstagramLinksTest {

    @Test
    fun `extracts reels posts and share links`() {
        val text = """
            https://www.instagram.com/reel/C9abcDEF123/?igsh=MWQ1ZGUxMzBkMA==
            check this instagram.com/p/XYZ12345ab/ and
            https://instagram.com/reels/QWERTY9876/
            https://www.instagram.com/some.chef/reel/ABCDE12345/
            duplicate https://www.instagram.com/reel/C9abcDEF123/
        """.trimIndent()
        val links = InstagramLinks.extract(text)
        assertEquals(listOf("C9abcDEF123", "XYZ12345ab", "QWERTY9876", "ABCDE12345"), links.map { it.shortcode })
        assertEquals("https://www.instagram.com/reel/C9abcDEF123/", links[0].url)
        assertEquals("https://www.instagram.com/p/XYZ12345ab/", links[1].url)
    }

    @Test
    fun `ignores non instagram links`() {
        assertEquals(0, InstagramLinks.extract("https://youtube.com/watch?v=abc https://example.com/reel/abcdef").size)
    }
}
