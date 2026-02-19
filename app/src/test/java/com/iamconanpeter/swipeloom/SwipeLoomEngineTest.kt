package com.iamconanpeter.swipeloom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeLoomEngineTest {

    @Test
    fun `correct swipes complete puzzle and award stars`() {
        val engine = SwipeLoomEngine()
        val required = engine.currentPuzzle().requiredEdges.toList()

        required.forEach { edge ->
            engine.swipe(edge.a, edge.b)
        }

        val snap = engine.snapshot()
        assertTrue(snap.levelComplete)
        assertEquals(3, snap.starsEarned)
        assertEquals(required.size, snap.wovenCount)
    }

    @Test
    fun `non adjacent swipe is invalid and does not consume moves`() {
        val engine = SwipeLoomEngine()

        val result = engine.swipe(
            SwipeLoomEngine.Peg(0, 0),
            SwipeLoomEngine.Peg(2, 2)
        )

        assertEquals(SwipeLoomEngine.SwipeResult.INVALID_MOVE, result)
        assertEquals(0, engine.snapshot().movesUsed)
    }

    @Test
    fun `extra edge increases mistakes and lowers stars`() {
        val engine = SwipeLoomEngine()

        val extra = SwipeLoomEngine.ThreadEdge(
            SwipeLoomEngine.Peg(0, 1),
            SwipeLoomEngine.Peg(0, 2)
        ).ordered

        if (!engine.currentPuzzle().requiredEdges.contains(extra)) {
            engine.swipe(extra.a, extra.b)
        }

        engine.currentPuzzle().requiredEdges.forEach { edge ->
            engine.swipe(edge.a, edge.b)
        }

        val snap = engine.snapshot()
        assertTrue(snap.levelComplete)
        assertTrue(snap.mistakes >= 1)
        assertTrue(snap.starsEarned <= 2)
    }

    @Test
    fun `undo removes last edge and spends charge`() {
        val engine = SwipeLoomEngine()
        val edge = engine.currentPuzzle().requiredEdges.first()

        engine.swipe(edge.a, edge.b)
        val undoResult = engine.undo()
        val snap = engine.snapshot()

        assertTrue(undoResult)
        assertEquals(0, snap.wovenCount)
        assertEquals(0, snap.movesUsed)
        assertEquals(0, snap.undoCharges)
    }

    @Test
    fun `running out of moves fails puzzle`() {
        val puzzle = SwipeLoomEngine.PuzzleSpec(
            id = "tight",
            title = "Tight",
            size = 3,
            requiredEdges = setOf(
                SwipeLoomEngine.ThreadEdge(SwipeLoomEngine.Peg(0, 0), SwipeLoomEngine.Peg(0, 1)).ordered,
                SwipeLoomEngine.ThreadEdge(SwipeLoomEngine.Peg(1, 0), SwipeLoomEngine.Peg(1, 1)).ordered
            ),
            parMoves = 2,
            moveLimit = 1
        )
        val engine = SwipeLoomEngine(listOf(puzzle))

        val result = engine.swipe(SwipeLoomEngine.Peg(0, 0), SwipeLoomEngine.Peg(0, 1))

        assertEquals(SwipeLoomEngine.SwipeResult.OUT_OF_MOVES, result)
        assertTrue(engine.snapshot().failed)
    }

    @Test
    fun `advance puzzle only works after completion`() {
        val engine = SwipeLoomEngine()

        assertFalse(engine.advancePuzzle())

        engine.currentPuzzle().requiredEdges.forEach { edge ->
            engine.swipe(edge.a, edge.b)
        }

        assertTrue(engine.advancePuzzle())
        assertEquals(1, engine.snapshot().puzzleIndex)
    }
}
