package com.iamconanpeter.swipeloom

import kotlin.math.abs

class SwipeLoomEngine(
    private val puzzles: List<PuzzleSpec> = defaultPuzzles()
) {
    data class Peg(val row: Int, val col: Int)

    data class ThreadEdge(val a: Peg, val b: Peg) {
        init {
            require(isAdjacent(a, b)) { "Edges must connect adjacent pegs" }
        }

        companion object {
            private fun isAdjacent(p1: Peg, p2: Peg): Boolean =
                abs(p1.row - p2.row) + abs(p1.col - p2.col) == 1
        }

        val ordered: ThreadEdge
            get() = if (a.row < b.row || (a.row == b.row && a.col <= b.col)) this else ThreadEdge(b, a)
    }

    data class PuzzleSpec(
        val id: String,
        val title: String,
        val size: Int,
        val requiredEdges: Set<ThreadEdge>,
        val parMoves: Int,
        val moveLimit: Int
    )

    enum class SwipeResult {
        WOVE_CORRECT,
        WOVE_EXTRA,
        ALREADY_WOVEN,
        INVALID_MOVE,
        OUT_OF_MOVES,
        LEVEL_COMPLETE,
        CAMPAIGN_COMPLETE,
        LOCKED
    }

    data class Snapshot(
        val puzzleIndex: Int,
        val puzzleTitle: String,
        val puzzleSize: Int,
        val requiredCount: Int,
        val wovenCount: Int,
        val movesUsed: Int,
        val moveLimit: Int,
        val mistakes: Int,
        val undoCharges: Int,
        val starsEarned: Int,
        val totalStars: Int,
        val levelComplete: Boolean,
        val allPuzzlesCleared: Boolean,
        val failed: Boolean
    )

    private var puzzleIndex = 0
    private var wovenEdges = linkedSetOf<ThreadEdge>()
    private var moveHistory = mutableListOf<ThreadEdge>()
    private var movesUsed = 0
    private var mistakes = 0
    private var undoCharges = 1
    private var starsEarned = 0
    private var totalStars = 0
    private var levelComplete = false
    private var failed = false
    private var allPuzzlesCleared = false

    fun swipe(from: Peg, to: Peg): SwipeResult {
        if (allPuzzlesCleared) return SwipeResult.CAMPAIGN_COMPLETE
        if (levelComplete || failed) return SwipeResult.LOCKED
        if (!isWithinBounds(from) || !isWithinBounds(to) || !areAdjacent(from, to)) {
            return SwipeResult.INVALID_MOVE
        }

        val edge = ThreadEdge(from, to).ordered
        if (wovenEdges.contains(edge)) return SwipeResult.ALREADY_WOVEN

        wovenEdges.add(edge)
        moveHistory.add(edge)
        movesUsed += 1

        if (!currentPuzzle().requiredEdges.contains(edge)) {
            mistakes += 1
        }

        if (isPuzzleComplete()) {
            levelComplete = true
            starsEarned = calculateStars()
            totalStars += starsEarned
            return SwipeResult.LEVEL_COMPLETE
        }

        if (movesUsed >= currentPuzzle().moveLimit) {
            failed = true
            return SwipeResult.OUT_OF_MOVES
        }

        return if (currentPuzzle().requiredEdges.contains(edge)) {
            SwipeResult.WOVE_CORRECT
        } else {
            SwipeResult.WOVE_EXTRA
        }
    }

    fun undo(): Boolean {
        if (undoCharges <= 0 || moveHistory.isEmpty() || levelComplete || failed || allPuzzlesCleared) return false

        val edge = moveHistory.removeLast()
        wovenEdges.remove(edge)
        movesUsed = maxOf(0, movesUsed - 1)
        if (!currentPuzzle().requiredEdges.contains(edge)) {
            mistakes = maxOf(0, mistakes - 1)
        }
        undoCharges -= 1
        return true
    }

    fun retryPuzzle() {
        resetPuzzleState()
    }

    fun advancePuzzle(): Boolean {
        if (!levelComplete || allPuzzlesCleared) return false
        if (puzzleIndex == puzzles.lastIndex) {
            allPuzzlesCleared = true
            return false
        }

        puzzleIndex += 1
        resetPuzzleState()
        return true
    }

    fun snapshot(): Snapshot = Snapshot(
        puzzleIndex = puzzleIndex,
        puzzleTitle = currentPuzzle().title,
        puzzleSize = currentPuzzle().size,
        requiredCount = currentPuzzle().requiredEdges.size,
        wovenCount = wovenEdges.count { currentPuzzle().requiredEdges.contains(it) },
        movesUsed = movesUsed,
        moveLimit = currentPuzzle().moveLimit,
        mistakes = mistakes,
        undoCharges = undoCharges,
        starsEarned = starsEarned,
        totalStars = totalStars,
        levelComplete = levelComplete,
        allPuzzlesCleared = allPuzzlesCleared,
        failed = failed
    )

    fun currentPuzzle(): PuzzleSpec = puzzles[puzzleIndex]

    fun wovenEdgesForRender(): Set<ThreadEdge> = wovenEdges.toSet()

    fun isRequiredEdge(edge: ThreadEdge): Boolean = currentPuzzle().requiredEdges.contains(edge.ordered)

    private fun resetPuzzleState() {
        wovenEdges = linkedSetOf()
        moveHistory = mutableListOf()
        movesUsed = 0
        mistakes = 0
        undoCharges = 1
        starsEarned = 0
        levelComplete = false
        failed = false
    }

    private fun isPuzzleComplete(): Boolean = currentPuzzle().requiredEdges.all { wovenEdges.contains(it) }

    private fun calculateStars(): Int {
        val overPar = (movesUsed - currentPuzzle().parMoves).coerceAtLeast(0)
        return when {
            mistakes == 0 && overPar <= 1 -> 3
            mistakes <= 1 && overPar <= 2 -> 2
            else -> 1
        }
    }

    private fun areAdjacent(p1: Peg, p2: Peg): Boolean = abs(p1.row - p2.row) + abs(p1.col - p2.col) == 1

    private fun isWithinBounds(peg: Peg): Boolean {
        val size = currentPuzzle().size
        return peg.row in 0 until size && peg.col in 0 until size
    }

    internal fun forceCurrentPuzzleForTest(index: Int) {
        puzzleIndex = index
        resetPuzzleState()
    }

    companion object {
        private fun e(r1: Int, c1: Int, r2: Int, c2: Int): ThreadEdge =
            ThreadEdge(Peg(r1, c1), Peg(r2, c2)).ordered

        fun defaultPuzzles(): List<PuzzleSpec> = listOf(
            PuzzleSpec(
                id = "loom_square",
                title = "Starter Weave",
                size = 4,
                requiredEdges = setOf(
                    e(0, 0, 0, 1),
                    e(0, 1, 1, 1),
                    e(1, 1, 1, 2),
                    e(1, 2, 2, 2),
                    e(2, 2, 2, 1),
                    e(2, 1, 3, 1)
                ),
                parMoves = 6,
                moveLimit = 9
            ),
            PuzzleSpec(
                id = "loom_ribbon",
                title = "Ribbon Turn",
                size = 5,
                requiredEdges = setOf(
                    e(0, 1, 1, 1),
                    e(1, 1, 1, 2),
                    e(1, 2, 2, 2),
                    e(2, 2, 2, 3),
                    e(2, 3, 3, 3),
                    e(3, 3, 3, 2),
                    e(3, 2, 4, 2)
                ),
                parMoves = 7,
                moveLimit = 11
            ),
            PuzzleSpec(
                id = "loom_cross",
                title = "Festival Cross",
                size = 5,
                requiredEdges = setOf(
                    e(1, 2, 2, 2),
                    e(2, 2, 3, 2),
                    e(2, 1, 2, 2),
                    e(2, 2, 2, 3),
                    e(1, 1, 2, 1),
                    e(2, 3, 3, 3),
                    e(1, 3, 2, 3),
                    e(3, 1, 3, 2)
                ),
                parMoves = 8,
                moveLimit = 13
            )
        )
    }
}
