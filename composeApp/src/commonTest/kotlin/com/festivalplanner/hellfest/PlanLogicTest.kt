package com.festivalplanner.hellfest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanLogicTest {
    private fun set(id: String, start: String, end: String) = FestivalSet(
        id = id,
        artist = id.uppercase(),
        stage = "STAGE",
        day = "2026-06-18",
        start = start,
        end = end,
    )

    @Test
    fun detectsPartialOverlapButNotTouchingIntervals() {
        val first = set("a", "2026-06-18T18:00:00+02:00", "2026-06-18T19:00:00+02:00")
        val overlap = set("b", "2026-06-18T18:45:00+02:00", "2026-06-18T19:30:00+02:00")
        val touching = set("c", "2026-06-18T19:00:00+02:00", "2026-06-18T20:00:00+02:00")

        assertTrue(first.conflictsWith(overlap))
        assertFalse(first.conflictsWith(touching))
    }

    @Test
    fun exportImportRoundTrip() {
        val encoded = PlanCodec.export("Miguel", setOf("a", "b"), "2026-06-15T12:00:00+02:00")
        val decoded = PlanCodec.import(encoded).getOrThrow()

        assertEquals("Miguel", decoded.userName)
        assertEquals(setOf("a", "b"), decoded.selectedSetIds)
        assertEquals(FESTIVAL_ID, decoded.festival)
    }

    @Test
    fun comparisonFindsSharedAndCrossPlanClashes() {
        val a = set("a", "2026-06-18T18:00:00+02:00", "2026-06-18T19:00:00+02:00")
        val b = set("b", "2026-06-18T18:30:00+02:00", "2026-06-18T19:30:00+02:00")
        val shared = set("shared", "2026-06-18T20:00:00+02:00", "2026-06-18T21:00:00+02:00")

        val result = comparePlans(listOf(a, b, shared), setOf("a", "shared"), setOf("b", "shared"))

        assertEquals(listOf("shared"), result.both.map { it.id })
        assertEquals(listOf("a" to "b"), result.crossPlanClashes.map { it.first.id to it.second.id })
    }
}
