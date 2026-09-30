package io.github.relony.pinry.ui.common

import io.github.relony.pinry.ui.create.PinFormState
import org.junit.Assert.assertEquals
import org.junit.Test

class PinFormStateTest {
    @Test
    fun commaOrEnterAddsTrimmedTagsOnce() {
        val form = PinFormState(tags = listOf("cats"))
        form.onTagInput("#art,")
        form.onTagInput(" cats ,")
        form.onTagInput("sketch")
        form.commitTagInput()
        assertEquals(listOf("cats", "art", "sketch"), form.tags)
        assertEquals("", form.tagInput)
    }

    @Test
    fun suggestionsPreferPrefixMatchesAndSkipChosenTags() {
        val form = PinFormState(tags = listOf("kitagawa"))
        form.onTagInput("ka")
        // TagRepository hands the list over sorted; prefix matches come first, then the rest in order.
        val all = listOf("art", "kafka", "kakao", "kitagawa", "makai")
        assertEquals(listOf("kafka", "kakao", "makai"), form.suggestions(all))
    }
}
