package com.iamconanpeter.swipeloom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class SwipeLoomView(context: Context) : View(context) {
    private val engine = SwipeLoomEngine()
    private val progressManager = LoomProgressManager(context.getSharedPreferences("swipe_loom_progress", Context.MODE_PRIVATE))

    private val backgroundPaint = Paint().apply { color = Color.parseColor("#111827") }
    private val pegPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB") }
    private val requiredGuidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#374151")
        strokeWidth = 8f
    }
    private val wovenCorrectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34D399")
        strokeWidth = 14f
    }
    private val wovenExtraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F87171")
        strokeWidth = 14f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        textSize = 32f
    }
    private val highlightTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#93C5FD")
        textSize = 34f
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2937")
    }

    private var touchStartPeg: SwipeLoomEngine.Peg? = null
    private var feedbackText = "Swipe between adjacent pegs"
    private var feedbackColor = Color.parseColor("#93C5FD")

    private val undoRect = RectF()
    private val nextRect = RectF()
    private val retryRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val snap = engine.snapshot()
        val puzzle = engine.currentPuzzle()

        val top = 260f
        val gridSizePx = (width.coerceAtMost(height) * 0.70f)
        val cell = if (puzzle.size > 1) gridSizePx / (puzzle.size - 1) else 0f
        val left = (width - gridSizePx) / 2f

        drawGuides(canvas, left, top, cell)
        drawWovenEdges(canvas, left, top, cell)
        drawPegs(canvas, left, top, cell)
        drawHeader(canvas, snap)

        undoRect.set(width - 270f, 64f, width - 40f, 132f)
        nextRect.set(width - 270f, 144f, width - 40f, 212f)
        retryRect.set(width - 270f, 224f, width - 40f, 292f)

        drawButton(canvas, undoRect, "Undo (${snap.undoCharges})")
        drawButton(canvas, nextRect, if (snap.levelComplete) "Next" else "Locked")
        drawButton(canvas, retryRect, "Retry")

        highlightTextPaint.color = feedbackColor
        canvas.drawText(feedbackText, 36f, height - 52f, highlightTextPaint)

        if (snap.failed) {
            canvas.drawText("Out of moves — retry to recover", 36f, height - 94f, subTextPaint)
        }

        if (snap.allPuzzlesCleared) {
            canvas.drawText("Loom complete! New patterns tomorrow.", 36f, height - 94f, subTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (undoRect.contains(event.x, event.y)) {
                val undone = engine.undo()
                if (undone) {
                    feedbackText = "Undo used to recover a move"
                    feedbackColor = Color.parseColor("#FCD34D")
                } else {
                    feedbackText = "Undo unavailable"
                    feedbackColor = Color.parseColor("#F87171")
                }
                invalidate()
                return true
            }

            if (nextRect.contains(event.x, event.y)) {
                if (engine.advancePuzzle()) {
                    feedbackText = "New pattern unlocked"
                    feedbackColor = Color.parseColor("#60A5FA")
                } else {
                    feedbackText = if (engine.snapshot().allPuzzlesCleared) {
                        "Campaign complete"
                    } else {
                        "Finish the pattern first"
                    }
                    feedbackColor = Color.parseColor("#F87171")
                }
                invalidate()
                return true
            }

            if (retryRect.contains(event.x, event.y)) {
                engine.retryPuzzle()
                feedbackText = "Pattern reset"
                feedbackColor = Color.parseColor("#93C5FD")
                invalidate()
                return true
            }

            touchStartPeg = nearestPeg(event.x, event.y)
            return true
        }

        if (event.action == MotionEvent.ACTION_UP) {
            val start = touchStartPeg
            val end = nearestPeg(event.x, event.y)
            touchStartPeg = null

            if (start != null && end != null) {
                val result = engine.swipe(start, end)
                val snap = engine.snapshot()
                when (result) {
                    SwipeLoomEngine.SwipeResult.WOVE_CORRECT -> {
                        feedbackText = "Clean stitch"
                        feedbackColor = Color.parseColor("#34D399")
                    }
                    SwipeLoomEngine.SwipeResult.WOVE_EXTRA -> {
                        feedbackText = "Extra thread costs efficiency"
                        feedbackColor = Color.parseColor("#F87171")
                    }
                    SwipeLoomEngine.SwipeResult.ALREADY_WOVEN -> {
                        feedbackText = "Already stitched"
                        feedbackColor = Color.parseColor("#FCD34D")
                    }
                    SwipeLoomEngine.SwipeResult.INVALID_MOVE -> {
                        feedbackText = "Only adjacent pegs"
                        feedbackColor = Color.parseColor("#F87171")
                    }
                    SwipeLoomEngine.SwipeResult.OUT_OF_MOVES -> {
                        feedbackText = "No moves left"
                        feedbackColor = Color.parseColor("#F87171")
                    }
                    SwipeLoomEngine.SwipeResult.LEVEL_COMPLETE -> {
                        val progress = progressManager.recordWin(engine.currentPuzzle().id, snap.starsEarned)
                        feedbackText = "Pattern complete ★${snap.starsEarned}  Streak ${progress.streakDays}"
                        feedbackColor = Color.parseColor("#60A5FA")
                    }
                    SwipeLoomEngine.SwipeResult.CAMPAIGN_COMPLETE -> {
                        feedbackText = "Campaign complete"
                        feedbackColor = Color.parseColor("#60A5FA")
                    }
                    SwipeLoomEngine.SwipeResult.LOCKED -> {
                        feedbackText = "Use Retry or Next"
                        feedbackColor = Color.parseColor("#FCD34D")
                    }
                }
                invalidate()
            }
            return true
        }

        return super.onTouchEvent(event)
    }

    private fun drawHeader(canvas: Canvas, snap: SwipeLoomEngine.Snapshot) {
        val puzzleId = engine.currentPuzzle().id
        val bestStars = progressManager.bestStars(puzzleId)

        canvas.drawText("Swipe Loom", 36f, 78f, textPaint)
        canvas.drawText("${snap.puzzleTitle} (${snap.puzzleIndex + 1}/3)", 36f, 122f, subTextPaint)
        canvas.drawText(
            "Progress ${snap.wovenCount}/${snap.requiredCount}  Moves ${snap.movesUsed}/${snap.moveLimit}",
            36f,
            166f,
            subTextPaint
        )
        canvas.drawText(
            "Mistakes ${snap.mistakes}  Stars ${snap.starsEarned}  Best ★$bestStars",
            36f,
            206f,
            subTextPaint
        )
        canvas.drawText(
            "Total ★${progressManager.totalStars()}  Streak ${progressManager.currentStreak()}",
            36f,
            246f,
            highlightTextPaint
        )
    }

    private fun drawGuides(canvas: Canvas, left: Float, top: Float, cell: Float) {
        val required = engine.currentPuzzle().requiredEdges
        required.forEach { edge ->
            val x1 = left + edge.a.col * cell
            val y1 = top + edge.a.row * cell
            val x2 = left + edge.b.col * cell
            val y2 = top + edge.b.row * cell
            canvas.drawLine(x1, y1, x2, y2, requiredGuidePaint)
        }
    }

    private fun drawWovenEdges(canvas: Canvas, left: Float, top: Float, cell: Float) {
        engine.wovenEdgesForRender().forEach { edge ->
            val x1 = left + edge.a.col * cell
            val y1 = top + edge.a.row * cell
            val x2 = left + edge.b.col * cell
            val y2 = top + edge.b.row * cell
            val paint = if (engine.isRequiredEdge(edge)) wovenCorrectPaint else wovenExtraPaint
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }

    private fun drawPegs(canvas: Canvas, left: Float, top: Float, cell: Float) {
        val size = engine.currentPuzzle().size
        repeat(size) { row ->
            repeat(size) { col ->
                canvas.drawCircle(left + col * cell, top + row * cell, 12f, pegPaint)
            }
        }
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String) {
        canvas.drawRoundRect(rect, 16f, 16f, buttonPaint)
        canvas.drawText(label, rect.left + 16f, rect.centerY() + 12f, subTextPaint)
    }

    private fun nearestPeg(x: Float, y: Float): SwipeLoomEngine.Peg? {
        val puzzle = engine.currentPuzzle()
        val top = 260f
        val gridSizePx = (width.coerceAtMost(height) * 0.70f)
        val cell = if (puzzle.size > 1) gridSizePx / (puzzle.size - 1) else 0f
        val left = (width - gridSizePx) / 2f

        var bestPeg: SwipeLoomEngine.Peg? = null
        var bestDistanceSq = Float.MAX_VALUE
        repeat(puzzle.size) { row ->
            repeat(puzzle.size) { col ->
                val px = left + col * cell
                val py = top + row * cell
                val dx = px - x
                val dy = py - y
                val distSq = dx * dx + dy * dy
                if (distSq < bestDistanceSq) {
                    bestDistanceSq = distSq
                    bestPeg = SwipeLoomEngine.Peg(row, col)
                }
            }
        }

        val maxDistance = (cell * 0.55f) * (cell * 0.55f)
        return if (bestDistanceSq <= maxDistance) bestPeg else null
    }
}
