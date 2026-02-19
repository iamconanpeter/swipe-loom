# Swipe Loom — Spec (Plan Mode)

## Product framing
- **Genre:** Cozy tactical swipe puzzle
- **Target platform:** Android phones (portrait, one-hand)
- **Core loop:** Swipe adjacent pegs to weave all required pattern edges before move limit

## Q&A discovery (assumptions from iOS cozy puzzle trends)
1. **Core fantasy + 10-second hook**
   - "I can complete a handcrafted textile pattern with satisfying swipes in under a minute."
2. **Why users come back**
   - Daily pattern streak, better star scores, and clean-run (no extra thread) mastery goals.
3. **Session length targets**
   - 30s: retry one compact pattern.
   - 2m: clear a short 3-pattern set.
   - 5m: optimize stars and maintain streak.
4. **Skill vs luck balance**
   - Mostly skill (routing and economy). No random fail states during run.
5. **Fail-state fairness + frustration controls**
   - One undo charge each puzzle, explicit move budget display, instant retry.
6. **Difficulty ramp + onboarding**
   - Puzzle 1 teaches path completion, puzzle 2 introduces tighter budget, puzzle 3 combines precision + efficiency.
7. **Distinctive mechanic vs Android clones**
   - Weaving pattern grammar (required edges + optional but penalized edges) rather than match-3 or line-fill flood mechanics.
8. **Art/animation feasible for small team**
   - Minimal peg/line rendering with color feedback and polished HUD messaging.
9. **Audio/feedback plan**
   - MVP text + color feedback; post-MVP haptics and short loom/pluck audio cues.
10. **Monetization-safe design**
   - No dark pattern timers; ad/reward systems deferred until retention baseline exists.
11. **Technical constraints + performance budgets**
   - 60fps canvas drawing on low-mid devices; no heavy shaders; deterministic engine for unit tests.

## USP
**A tactile weaving puzzle where every swipe is a stitch and efficient routing is rewarded with stars and streak momentum.**

## Differentiators (3)
1. Required-edge loom patterns (clear objective readability)
2. Efficiency stars tied to mistakes + over-par moves
3. Fairness-first undo + instant retry + low-friction progression

## Retention hooks (3)
1. Star mastery per pattern (best-score chase)
2. Streak-safe return value (daily wins increment streak)
3. Variable challenge (progressive pattern complexity)

## Quality bars (3)
1. **Juice/feedback:** Immediate color-coded result per swipe
2. **Readability:** High-contrast guides vs woven lines
3. **Smoothness:** One-hand interaction, low-latency updates, instant retries

## MVP scope
- Included:
  - 3 handcrafted pattern puzzles
  - Move-limit and star system
  - Undo fairness mechanic
  - Retry/next flow
  - Persistent stars + streak
- Post-MVP:
  - Daily generated puzzle seeds
  - Audio/haptics pack
  - More theme packs and events

## Test strategy
- Unit tests for edge validation, completion conditions, fail conditions, undo behavior, star scoring, puzzle progression.
