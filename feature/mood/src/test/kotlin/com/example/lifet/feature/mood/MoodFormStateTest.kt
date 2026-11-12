package com.example.lifet.feature.mood

import com.example.lifet.core.model.mood.MoodLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodFormStateTest {
    @Test
    fun `cannot save without a level`() {
        assertFalse(MoodFormState().canSave)
        assertTrue(MoodFormState(level = MoodLevel.GOOD).canSave)
        assertFalse(MoodFormState(level = MoodLevel.GOOD, saving = true).canSave)
    }

    @Test
    fun `toggling an emotion twice removes it`() {
        val form = MoodFormState().toggleEmotion("joy").toggleEmotion("calm")

        assertEquals(listOf("joy", "calm"), form.emotions)
        assertEquals(listOf("calm"), form.toggleEmotion("joy").emotions)
    }

    @Test
    fun `tapping the selected scale value clears it`() {
        val form = MoodFormState().withEnergy(4)

        assertEquals(4, form.energy)
        assertNull(form.withEnergy(4).energy)
        assertEquals(2, form.withEnergy(2).energy)
    }

    @Test
    fun `custom tag from input is normalized, selected and not duplicated`() {
        val form = MoodFormState(emotionInput = "  nostalgia,  ").addEmotionFromInput()

        assertEquals(listOf("nostalgia"), form.emotions)
        assertEquals("", form.emotionInput)
        assertEquals(listOf("nostalgia"), form.copy(emotionInput = "nostalgia").addEmotionFromInput().emotions)
        assertEquals(emptyList<String>(), MoodFormState(factorInput = " , ").addFactorFromInput().factors)
    }

    @Test
    fun `pending text in the tag inputs is kept when saving`() {
        val draft = MoodFormState(level = MoodLevel.GOOD, factorInput = "yoga").toDraft()

        assertEquals(listOf("yoga"), draft?.factors)
    }

    @Test
    fun `options combine defaults, past custom tags and selected ones without duplicates`() {
        val form = MoodFormState(emotions = listOf("nostalgia"), suggestedEmotions = listOf("nostalgia", "calm"))

        assertEquals(1, form.emotionOptions.count { it == "nostalgia" })
        assertEquals(1, form.emotionOptions.count { it == "calm" })
    }

    @Test
    fun `editing a loaded entry is not new and detail detection ignores the level alone`() {
        assertFalse(MoodFormState(level = MoodLevel.GOOD).hasDetail)
        assertTrue(MoodFormState(level = MoodLevel.GOOD, note = "hola").hasDetail)
        assertTrue(MoodFormState(id = 3).isEditing)
        assertFalse(MoodFormState().isEditing)
    }
}
