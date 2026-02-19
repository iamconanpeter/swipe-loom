# Swipe Loom — Technical Plan

## Architecture
- **MainActivity**: host custom canvas view.
- **SwipeLoomView**: rendering + touch interpretation + UI feedback loop.
- **SwipeLoomEngine**: pure Kotlin game-state machine.
- **LoomProgressManager**: SharedPreferences persistence for best stars/streak/total stars.

## Core model
- `Peg(row, col)`
- `ThreadEdge(a, b)` normalized and adjacency constrained
- `PuzzleSpec(id, title, size, requiredEdges, parMoves, moveLimit)`
- `Snapshot` for render-safe read model

## Gameplay rules
1. Valid move = swipe between adjacent in-bounds pegs.
2. First-time edge consumes one move.
3. Required edge increases progress.
4. Extra edge increases mistakes (hurts star rating).
5. Finish when all required edges woven.
6. Fail when move limit reached before completion.
7. One undo per puzzle to recover a mistaken move.

## Scoring/progression
- Stars:
  - 3★: no mistakes and <= 1 over par
  - 2★: <=1 mistake and <=2 over par
  - 1★: otherwise on completion
- Persistence:
  - Best stars per puzzle
  - Total accumulated stars
  - Consecutive win-day streak

## UI feedback polish
- Required edges shown as guide rails
- Correct woven edges (green), extra edges (red)
- Contextual status messages and color changes
- Inline action controls: Undo / Next / Retry

## Validation plan
- `./gradlew test assembleDebug`
- Keep engine deterministic and isolated for tests.
